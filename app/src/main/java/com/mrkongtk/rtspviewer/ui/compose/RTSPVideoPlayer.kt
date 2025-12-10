package com.mrkongtk.rtspviewer.ui.compose

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.ui.theme.OnOverlayerBackgroundColor
import com.mrkongtk.rtspviewer.ui.theme.OverlayerBackgroundColor
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.RTSPVideoPlayerViewModel
import kotlinx.coroutines.flow.map

/**
 * A Composable component that renders a real-time RTSP video stream using Media3 (ExoPlayer).
 *
 * This component acts as the View layer in the MVVM architecture:
 * 1. It observes the [RTSPVideoPlayerViewModel] for state changes (Buffering, Playing, Error).
 * 2. It manages the [ExoPlayer] lifecycle (creation, preparation, and release).
 * 3. It handles the UI for the video surface, including overlay controls and error messages.
 *
 * @param viewModel The Hilt-injected ViewModel that holds the stream configuration and playback state.
 * @param modifier The modifier to be applied to the root layout of the player.
 */
@Composable
fun RTSPVideoPlayer(
    viewModel: RTSPVideoPlayerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current

    // -- State Observation --
    // Collect the comprehensive playback state (Idle, Buffering, Ready, Playing)
    val playerState by viewModel.state.collectAsStateWithLifecycle(null)

    // Specifically observe the error field to trigger the Error UI if playback fails
    val errorDescription by viewModel.state.map { it.error }.collectAsStateWithLifecycle(null)

    // Collect the stream configuration data (URI, TCP preferences)
    val data by viewModel.data.collectAsStateWithLifecycle(null)

    // State to dynamically adjust the player's aspect ratio based on the incoming video stream
    var videoSizeRatio by remember { mutableFloatStateOf(16f / 9f) }

    // -- Player Listener Configuration --
    // We define the listener inside a 'remember' block to prevent recreating the object on every recomposition.
    // This listener bridges ExoPlayer's internal events to our ViewModel's state.
    val playerListener: Player.Listener = remember {
        object : Player.Listener {
            /**
             * Called when the player encounters a fatal error (e.g., connection lost, malformed RTSP).
             */
            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                viewModel.updatePlaybackError(error)
            }

            /**
             * Called when the internal state changes (e.g., buffering to ready).
             * We map the raw integer to our domain-specific [RTSPVideoPlayerPlaybackState] enum.
             */
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                RTSPVideoPlayerPlaybackState.fromValue(playbackState)?.let {
                    viewModel.updatePlaybackState(it)
                } ?: run {
                    Log.e("RTSPVideoPlayer", "Cannot cast ExoPlayer state: $playbackState")
                }
            }

            /**
             * Called when the Play/Pause state changes.
             */
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Playing)
                } else {
                    viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Ready)
                }
            }

            /**
             * Called when the video resolution is determined.
             * We calculate the aspect ratio here to ensure the UI fits the video content perfectly.
             */
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                val result = videoSize.width.toFloat() / videoSize.height.toFloat()
                // Prevent division by zero or infinite values
                videoSizeRatio = if (result.isFinite() && result > 0) {
                    result
                } else {
                    16f / 9f
                }
            }
        }
    }

    // ---------------------------------------------------------
    // 1. ExoPlayer Initialization
    // ---------------------------------------------------------
    // Initialize the ExoPlayer instance. 'remember' ensures the player persists across
    // recompositions but is re-created if the context changes (rare).
    val exoPlayer: ExoPlayer? = remember {
        try {
            ExoPlayer.Builder(context).build().apply {
                addListener(playerListener)
            }
        } catch (e: Error) {
            // Handle devices that might crash on codec initialization
            Log.e("RTSPVideoPlayer", "Failed to initialize ExoPlayer", e)
            null
        }
    }

    // Prepare the Media Source only when data is available and the player is currently Idle.
    // This prevents re-preparing the player during active playback.
    exoPlayer?.let { player ->
        data?.let { streamData ->
            if (playerState?.playback == RTSPVideoPlayerPlaybackState.Idle && playerState?.error == null) {
                val mediaSource = RtspMediaSource.Factory()
                    // Crucial for RTSP: Forces RTP over TCP.
                    // This is often required for viewing streams over the internet or through firewalls
                    // where UDP packets might be dropped or unordered.
                    .setForceUseRtpTcp(streamData.forceTcp)
                    .createMediaSource(MediaItem.fromUri(streamData.uri))

                player.setMediaSource(mediaSource)
                player.prepare()
            }
        }
    }

    // ---------------------------------------------------------
    // 2. UI Rendering Logic
    // ---------------------------------------------------------

    // Priority 1: Error State
    // If the ViewModel reports an error, we display the error message instead of the video.
    errorDescription?.localizedMessage?.let { errorMessage ->
        Column(
            modifier = modifier
                .aspectRatio(videoSizeRatio, false)
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    // Priority 2: Video Player
    // If initialization succeeded, render the player view.
        ?: exoPlayer?.let { player ->
            Box(
                modifier = modifier
                    .aspectRatio(videoSizeRatio, false)
            ) {
                // AndroidView bridges the legacy View-based PlayerView into Jetpack Compose.
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            // Hide native controls to use our custom overlay or clean feed
                            useController = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            // Toggle pause on tap
                            if (playerState?.playback == RTSPVideoPlayerPlaybackState.Playing) {
                                player.pause()
                            }
                        }
                )

                // Overlay: Play Button (When Ready/Paused)
                if (playerState?.playback == RTSPVideoPlayerPlaybackState.Ready) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(OverlayerBackgroundColor) // Semi-transparent scrim
                            .clickable { player.play() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = stringResource(R.string.player_state_play),
                            tint = OnOverlayerBackgroundColor,
                            modifier = Modifier.fillMaxSize(0.2f)
                        )
                    }
                }
                // Overlay: Loading Spinner (When Buffering)
                else if (playerState?.playback == RTSPVideoPlayerPlaybackState.Buffering) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(OverlayerBackgroundColor)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
        // Priority 3: Fallback / Placeholder
        // Shown if the player failed to initialize but no specific error was thrown yet.
        ?: run {
            Column(
                modifier = modifier
                    .aspectRatio(videoSizeRatio, false),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RTSP Video Player Initializing...")
            }
        }

    // ---------------------------------------------------------
    // 3. Resource Cleanup
    // ---------------------------------------------------------
    // DisposableEffect guarantees that the player is released when this Composable
    // is removed from the screen (e.g., user navigates back).
    // Failure to release the player causes memory leaks and keeps the decoder hardware active.
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.let {
                it.removeListener(playerListener)
                it.release()
            }
        }
    }
}

@Preview(
    name = "Day",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RTSPVideoPlayerPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Note: In a real preview, you should pass a Mock ViewModel
            // or use a wrapper that doesn't rely on Hilt injection.
            val viewModel: RTSPVideoPlayerViewModel =
                hiltViewModel<RTSPVideoPlayerViewModel, RTSPVideoPlayerViewModel.Factory>(
                    creationCallback = {
                        it.create("rtsp://192.168.1.10", false)
                    }
                )
            RTSPVideoPlayer(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

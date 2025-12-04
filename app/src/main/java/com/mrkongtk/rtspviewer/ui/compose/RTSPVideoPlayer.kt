package com.mrkongtk.rtspviewer.ui.compose

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.RTSPVideoPlayerViewModel
import kotlinx.coroutines.flow.map

/**
 * A Composable function that renders an RTSP video stream using Media3 (ExoPlayer).
 *
 * This component handles the lifecycle of the ExoPlayer, maps playback states to the
 * ViewModel, and handles UI rendering for both successful streaming and error states.
 *
 * @param uri The RTSP URI to stream (e.g., "rtsp://192.168.1.10").
 * @param forceTcp If true, forces the player to use RTP over TCP. Useful for unstable networks.
 * @param viewModel The ViewModel used to track playback state and errors.
 * @param modifier Modifier for styling the root layout.
 */
@Composable
fun RTSPVideoPlayer(
    uri: String,
    forceTcp: Boolean,
    viewModel: RTSPVideoPlayerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current

    // Observe error state from the ViewModel to trigger UI changes
    val errorDescription by viewModel.state.map { it.error }.collectAsStateWithLifecycle(null)

    // Define a listener to bridge ExoPlayer events (Errors, State Changes) to the ViewModel
    val playerListener: Player.Listener = remember {
        object : Player.Listener {
            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                viewModel.updatePlaybackError(error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                // Convert raw integer state to domain-specific Enum
                RTSPVideoPlayerPlaybackState.fromValue(playbackState)?.let {
                    viewModel.updatePlaybackState(it)
                } ?: run {
                    Log.e("RTSPVideoPlayer", "Cannot cast state: $playbackState")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // 1. Player Initialization
    // ---------------------------------------------------------
    // Create and remember the ExoPlayer instance.
    // We use 'remember' to ensure the player survives recompositions,
    // but is recreated if the Key (not currently set) changes or on initial composition.
    val exoPlayer: ExoPlayer? = remember {
        try {
            ExoPlayer.Builder(context).build().apply {
                // Configure the RTSP Media Source
                val mediaSource = RtspMediaSource.Factory()
                    // Crucial for RTSP: Force TCP if UDP ports are blocked or connection is unstable
                    .setForceUseRtpTcp(forceTcp)
                    .createMediaSource(MediaItem.fromUri(uri))

                setMediaSource(mediaSource)
                addListener(playerListener)
                prepare()
                playWhenReady = true // Auto-start playback when buffering is complete
            }
        } catch (_: Error) {
            // Fallback if the device assumes it cannot support the player
            null
        }
    }

    // ---------------------------------------------------------
    // 2. UI Rendering Logic
    // ---------------------------------------------------------
    // Priority 1: Display Error if one exists in the ViewModel state
    errorDescription?.localizedMessage?.let { errorMessage ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f, false)
                .background(MaterialTheme.colorScheme.tertiary),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(errorMessage, color = MaterialTheme.colorScheme.onTertiary)
        }
    }
    // Priority 2: Display the Video Player if initialization was successful
        ?: exoPlayer?.let { player ->
            // Use AndroidView to bridge the traditional View-based PlayerView into Compose
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false // Hide native media controls for a "Clean Feed" look
                    }
                },
                modifier = modifier
            )
        }
        // Priority 3: Fallback UI (Loading/Placeholder) if player is null but no error yet
        ?: run {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f, false),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RTSP Video Player")
            }
        }

    // ---------------------------------------------------------
    // 3. Resource Cleanup
    // ---------------------------------------------------------
    // DisposableEffect ensures the player is released when this Composable
    // is removed from the screen, preventing memory leaks and battery drain.
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
            // Note: Hilt injection might fail in standard Previews without a mock module.
            // Often better to pass a specific state or mock VM for previews.
            val viewModel: RTSPVideoPlayerViewModel = hiltViewModel()
            RTSPVideoPlayer(
                uri = "rtsp://192.168.1.10",
                forceTcp = false,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
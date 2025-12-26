package com.mrkongtk.rtspviewer.ui.compose

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.OptIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.ui.theme.OnOverlayerBackgroundColor
import com.mrkongtk.rtspviewer.ui.theme.OverlayerBackgroundColor
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmodel.RTSPVideoPlayerViewModel
import kotlinx.coroutines.flow.map

/**
 * A stateful Composable that manages the lifecycle and logic of an RTSP Video Player.
 *
 * This component acts as the **Controller** in the UI layer:
 * 1. Initializes the [ExoPlayer].
 * 2. Observes the [RTSPVideoPlayerViewModel] for playback state and stream configuration.
 * 3. Handles media source preparation (forcing TCP/UDP).
 * 4. Manages Android Lifecycle events (pausing player when app goes background).
 *
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel The Hilt-injected ViewModel managing stream data and state.
 * @param playerFactory A factory lambda to create the ExoPlayer instance.
 *                      Useful for dependency injection or testing (mocking the player).
 */
@OptIn(UnstableApi::class)
@Composable
fun RTSPVideoPlayer(
    modifier: Modifier = Modifier,
    viewModel: RTSPVideoPlayerViewModel = hiltViewModel(),
    playerFactory: (Context) -> ExoPlayer = { context -> ExoPlayer.Builder(context).build() },
) {
    val context: Context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    // -- State Observation --
    // Collect the comprehensive playback state (Idle, Buffering, Ready, Playing)
    val playerState by viewModel.state.collectAsStateWithLifecycle(null)

    // Specifically observe the error field to trigger the Error UI if playback fails
    val errorDescription by viewModel.state.map { it.error }.collectAsStateWithLifecycle(null)

    // Collect the stream configuration data (URI, TCP preferences)
    val data by viewModel.data.collectAsStateWithLifecycle(null)

    // State to dynamically adjust the player's aspect ratio based on the incoming video stream.
    // Default is 16:9 until metadata is loaded.
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    // -- Player Initialization --
    // Initialize the ExoPlayer instance. 'remember' ensures the player persists across
    // recompositions but is re-created if the context changes.
    val exoPlayer: ExoPlayer? = remember {
        try {
            playerFactory(context)
        } catch (e: Error) {
            Log.e("RTSPVideoPlayer", "Failed to initialize ExoPlayer", e)
            null
        }
    }

    // -- Media Preparation --
    // Triggers when 'data' (stream config) changes or the player is first created.
    LaunchedEffect(data, exoPlayer) {
        exoPlayer?.let { player ->
            data?.let { streamData ->
                // Only load media if we are Idle and not currently in an error state
                if (playerState?.playback == RTSPVideoPlayerPlaybackState.Idle && playerState?.error == null) {
                    val mediaSource = RtspMediaSource.Factory()
                        // Critical for RTSP: Forces RTP over TCP if configured.
                        // This is often required for viewing streams over the internet or through firewalls
                        // where UDP packets might be dropped or unordered.
                        .setForceUseRtpTcp(streamData.forceTcp)
                        .createMediaSource(MediaItem.fromUri(streamData.uri))

                    player.setMediaSource(mediaSource)
                    player.prepare()
                }
            }
        }
    }

    // -- Player Event Listeners --
    // Bridges ExoPlayer's internal events to our ViewModel's state and local UI state.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
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
             * Syncs the "Is Playing" boolean with the ViewModel state.
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
             * We calculate the aspect ratio here to ensure the Compose Box fits the video content exactly
             * without black bars inside the player surface.
             */
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                val result = videoSize.width.toFloat() / videoSize.height.toFloat()
                // Prevent division by zero or infinite values
                videoAspectRatio = if (result.isFinite() && result > 0) {
                    result
                } else {
                    16f / 9f
                }
            }
        }
        exoPlayer?.addListener(listener)

        // Cleanup: Stop and release player when the Composable leaves the composition.
        onDispose {
            exoPlayer?.let {
                it.removeListener(listener)
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        }
    }

    // -- Lifecycle Management --
    // Ensures the player stops/pauses when the app goes into the background (e.g., user hits Home).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Render the UI content
    RTSPVideoPlayerContent(
        modifier = modifier,
        exoPlayer = exoPlayer,
        playbackState = playerState?.playback ?: RTSPVideoPlayerPlaybackState.Idle,
        videoAspectRatio = videoAspectRatio,
        errorMessage = errorDescription?.localizedMessage,
        onPlayClick = {
            exoPlayer?.let {
                it.playWhenReady = true
                it.prepare()
            }
        },
        onPauseClick = { exoPlayer?.stop() }
    )
}

/**
 * The stateless UI component for the player.
 * Handles the actual rendering of the [AndroidView] (Surface) and the overlay controls.
 *
 * @param playbackState The current state of playback (Buffering, Playing, etc.) used to show/hide overlays.
 * @param videoAspectRatio The calculated aspect ratio to size the player Box.
 * @param errorMessage If not null, displays the error overlay instead of the video.
 */
@OptIn(UnstableApi::class)
@Composable
fun RTSPVideoPlayerContent(
    modifier: Modifier = Modifier,
    exoPlayer: ExoPlayer?,
    playbackState: RTSPVideoPlayerPlaybackState,
    videoAspectRatio: Float,
    errorMessage: String?,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(videoAspectRatio, false), // Enforce video aspect ratio
        contentAlignment = Alignment.Center,
    ) {
        // Priority 1: Show Error if exists
        errorMessage?.let { msg ->
            ErrorOverlay(Modifier.fillMaxSize(), msg)
        } ?: exoPlayer?.let { player ->
            // Priority 2: Show Video Player Surface
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        // Hide native ExoPlayer controls to use our custom overlay
                        useController = false
                    }
                },
                update = { playerView ->
                    // Bind the player to the view if changed
                    if (playerView.player != player) {
                        playerView.player = player
                    }
                },
            )

            // Priority 3: Show State-based Overlays (Loading, Play Button, Pause Area)
            when (playbackState) {
                RTSPVideoPlayerPlaybackState.Buffering -> LoadingOverlay(Modifier.fillMaxSize())
                RTSPVideoPlayerPlaybackState.Ready -> {
                    PlayButtonOverlay(Modifier.fillMaxSize()) {
                        onPlayClick()
                    }
                }

                RTSPVideoPlayerPlaybackState.Playing -> {
                    // Invisible overlay to catch clicks for pausing
                    PauseButtonOverlay(Modifier.fillMaxSize()) {
                        onPauseClick()
                    }
                }

                else -> {}
            }
        } ?: run {
            // Fallback: Player is not initialized yet
            EmptyPlayerOverlay(Modifier.fillMaxSize())
        }
    }
}

/**
 * Displays an error message on a background surface.
 */
@Composable
fun ErrorOverlay(modifier: Modifier = Modifier, errorMessage: String) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
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

/**
 * Displays a placeholder while the player is initializing.
 */
@Composable
fun EmptyPlayerOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RTSP Video Player Initializing...")
    }
}

/**
 * Displays a circular progress indicator with a semi-transparent background.
 */
@Composable
fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(OverlayerBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Displays a large Play icon with a semi-transparent background.
 */
@Composable
fun PlayButtonOverlay(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(OverlayerBackgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = "Play",
            tint = OnOverlayerBackgroundColor,
            modifier = Modifier.fillMaxSize(0.2f)
        )
    }
}

/**
 * An invisible overlay that captures clicks to pause the video.
 */
@Composable
fun PauseButtonOverlay(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
    ) {
    }
}

@SuppressLint("ViewModelConstructorInComposable")
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
            // Note: In a real preview, you should ideally pass a Mock ViewModel
            // or use a wrapper that doesn't rely on Hilt injection.
            val viewModel = RTSPVideoPlayerViewModel(null, false)
            RTSPVideoPlayer(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

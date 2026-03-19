package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.shared.ui.theme.OnOverlayerBackgroundColor
import com.mrkongtk.rtspviewer.shared.ui.theme.OverlayerBackgroundColor
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.viewmodel.RTSPVideoPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The main stateful entry point for the RTSP Video Player screen.
 *
 * This Composable acts as the Controller in the MVVM pattern:
 * 1. **State Management**: Collects stream data and playback status from [RTSPVideoPlayerViewModel].
 * 2. **Lifecycle Safety**: Manages the Android Lifecycle to stop high-bandwidth RTSP streams when the app is backgrounded.
 * 3. **UI Delegation**: Passes processed state to the stateless [RTSPVideoPlayerContent].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel The ViewModel managing the player instance and stream state.
 */
@Composable
fun RTSPVideoPlayer(
    modifier: Modifier = Modifier,
    viewModel: RTSPVideoPlayerViewModel,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // -- State Observation --

    // Uses collectAsStateWithLifecycle to automatically stop flow collection when the UI is not visible,
    // which is critical for reducing CPU/Battery usage in video-heavy apps.
    val playerState by viewModel.state.collectAsStateWithLifecycle(null)
    val errorDescription = playerState?.error

    // Tracks the aspect ratio (width/height). Defaults to 16:9 until the RTSP stream provides metadata.
    val videoAspectRatio by viewModel.videoAspectRatio.collectAsStateWithLifecycle(16f / 9f)

    // -- Lifecycle Management --

    // Explicitly stops the RTSP stream when the Activity/Fragment moves to the background.
    // RTSP streams are often high-bandwidth; failing to stop them can lead to significant data usage.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopVideo()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    RTSPVideoPlayerContent(
        modifier = modifier,
        player = viewModel.videoPlayer,
        videoAspectRatio = videoAspectRatio,
        playbackState = playerState?.playback ?: RTSPVideoPlayerPlaybackState.Idle,
        errorMessage = errorDescription?.message,
        onPlayClick = { viewModel.playVideo() },
        onPauseClick = { viewModel.stopVideo() },
        onImageAvailable = { viewModel.imageAvailable(it) }
    )
}

/**
 * Stateless content for the RTSP Video Player.
 *
 * This Composable handles the layout and decides which overlays to show based on the current
 * playback state and error status. It uses [BoxWithConstraints] to adapt the video size
 * to the available screen space while maintaining the correct aspect ratio.
 *
 * @param modifier The modifier for this layout.
 * @param player The [RTSPVideoPlayer] instance used for rendering.
 * @param videoAspectRatio The current aspect ratio of the video.
 * @param playbackState The current state of playback (e.g., Playing, Buffering, Idle).
 * @param errorMessage An optional error message to display in an overlay.
 * @param onPlayClick Callback triggered when the user taps the play button.
 * @param onPauseClick Callback triggered when the user taps to pause (stop) the stream.
 * @param onImageAvailable Callback for processing captured video frames.
 */
@Composable
internal fun RTSPVideoPlayerContent(
    modifier: Modifier = Modifier,
    player: RTSPVideoPlayer,
    videoAspectRatio: Float,
    playbackState: RTSPVideoPlayerPlaybackState,
    errorMessage: String?,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onImageAvailable: (ImageBitmap) -> Unit,
) {

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val isLandscape = maxWidth > maxHeight

        val childModifier = if (isLandscape) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxWidth().wrapContentHeight()
        }.aspectRatio(
            videoAspectRatio,
            matchHeightConstraintsFirst = isLandscape
        )

        errorMessage?.let { msg ->
            ErrorOverlay(childModifier, msg)
        } ?: player.getPlayer<Any>()?.let {

            RTSPVideoPlayerDisplay(
                modifier = childModifier,
                player = player,
                onImageAvailable = onImageAvailable
            )

            // Playback State Overlays
            when (playbackState) {
                RTSPVideoPlayerPlaybackState.Buffering -> {
                    LoadingOverlay(childModifier)
                }

                RTSPVideoPlayerPlaybackState.Idle,
                RTSPVideoPlayerPlaybackState.Ready -> {
                    PlayButtonOverlay(childModifier, onPlayClick)
                }

                RTSPVideoPlayerPlaybackState.Playing -> {
                    // Transparent overlay to detect clicks for pausing the stream
                    PauseButtonOverlay(childModifier, onPauseClick)
                }

                else -> {}
            }
        } ?: run {
            EmptyPlayerOverlay(childModifier)
        }
    }
}

/**
 * Expect function for platform-specific video rendering.
 *
 * On Android, this typically wraps an `AndroidView` containing a `PlayerView` or `TextureView`.
 * On other platforms, it uses the respective native rendering surface.
 *
 * @param modifier The modifier for the display surface.
 * @param player The [RTSPVideoPlayer] instance providing the video data.
 * @param onImageAvailable Callback invoked when a frame is captured from the stream.
 */
@Composable
expect fun RTSPVideoPlayerDisplay(
    modifier: Modifier = Modifier,
    player: RTSPVideoPlayer,
    onImageAvailable: (ImageBitmap) -> Unit,
)

/**
 * Overlay shown when an error occurs (e.g., Connection Timeout, Auth Failure).
 *
 * @param modifier The modifier for the overlay.
 * @param errorMessage The description of the error to display.
 */
@Composable
internal fun ErrorOverlay(modifier: Modifier = Modifier, errorMessage: String) {
    Column(
        modifier = modifier
            .testTag("error_overlay")
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

/**
 * Placeholder UI shown while the ViewModel is initializing the Player instance.
 *
 * @param modifier The modifier for the placeholder.
 */
@Composable
internal fun EmptyPlayerOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.testTag("empty_player_overlay"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RTSP Video Player Initializing...")
    }
}

/**
 * Indeterminate progress indicator shown during stream buffering.
 *
 * @param modifier The modifier for the loading overlay.
 */
@Composable
internal fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag("loading_overlay")
            .background(OverlayerBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Large Play icon overlay for non-playing states.
 *
 * @param modifier The modifier for the play button overlay.
 * @param onClick Callback triggered when the overlay is clicked.
 */
@Composable
internal fun PlayButtonOverlay(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .testTag("play_button_overlay")
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
 * A transparent click-interceptor used to trigger a pause action.
 *
 * @param modifier The modifier for the pause overlay.
 * @param onClick Callback triggered when the overlay is clicked.
 */
@Composable
internal fun PauseButtonOverlay(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .testTag("pause_button_overlay")
            .clickable(onClick = onClick),
    )
}

@Preview
@Composable
private fun RTSPVideoPlayerPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Note: This preview uses a manually instantiated ViewModel with nulls
            // for dependencies. Real-world previews might require a MockViewModel or
            // specifically separating logic further into a 'Component' level.
            val videoPlayer = object : RTSPVideoPlayer {
                override val currentState: StateFlow<RTSPVideoPlayerPlaybackState>
                    get() = MutableStateFlow(RTSPVideoPlayerPlaybackState.Idle)
                override val videoAspectRatio: StateFlow<Float>
                    get() = MutableStateFlow(0.0f)
                override val error: StateFlow<Throwable?>
                    get() = MutableStateFlow(null)

                override fun prepare(uri: String, forceTcp: Boolean) {}

                override fun play() {}

                override fun stop() {}

                override fun release() {}

                override fun <T> getPlayer(): T? {
                    return null
                }
            }
            val viewModel = RTSPVideoPlayerViewModel(
                videoPlayer,
                null,
                false,
                onImageAvailable = null
            )
            RTSPVideoPlayer(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

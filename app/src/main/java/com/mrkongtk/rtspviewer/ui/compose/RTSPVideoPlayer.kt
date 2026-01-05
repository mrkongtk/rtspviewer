package com.mrkongtk.rtspviewer.ui.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.ui.theme.OnOverlayerBackgroundColor
import com.mrkongtk.rtspviewer.ui.theme.OverlayerBackgroundColor
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmodel.RTSPVideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

/**
 * The main stateful entry point for the RTSP Video Player screen.
 *
 * This Composable acts as the Controller in the MVVM pattern:
 * 1. It collects state from the [RTSPVideoPlayerViewModel].
 * 2. It manages the Android Lifecycle to ensure the player stops when the app is backgrounded.
 * 3. It passes the data down to the stateless [RTSPVideoPlayerContent] for rendering.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel The Hilt-injected ViewModel that holds the ExoPlayer instance and stream logic.
 */
@OptIn(UnstableApi::class)
@Composable
fun RTSPVideoPlayer(
    modifier: Modifier = Modifier,
    viewModel: RTSPVideoPlayerViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // -- State Observation --
    // collectAsStateWithLifecycle is lifecycle-aware; it stops collecting flows
    // when the app goes to the background, saving resources.
    val playerState by viewModel.state.collectAsStateWithLifecycle(null)

    // Collects specific error messages to display in the UI (if any).
    val errorDescription by viewModel.state.map { it.error }.collectAsStateWithLifecycle(null)

    // Collects the aspect ratio of the incoming video stream to resize the player container.
    // Defaults to 16:9 until metadata is available.
    val videoAspectRatio by viewModel.videoAspectRatio.collectAsStateWithLifecycle(16f / 9f)

    // -- Lifecycle Management --
    // It is critical to stop/release video players when the Activity pauses to prevent
    // memory leaks, battery drain, and background data usage.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // When user leaves the app (Home button, switching apps), stop the player.
                Lifecycle.Event.ON_PAUSE -> viewModel.stopVideo()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup observer when the Composable is removed from the composition.
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Render the stateless UI content
    RTSPVideoPlayerContent(
        modifier = modifier,
        player = viewModel.player,
        playbackState = playerState?.playback ?: RTSPVideoPlayerPlaybackState.Idle,
        videoAspectRatio = videoAspectRatio,
        errorMessage = errorDescription?.localizedMessage,
        onPlayClick = { viewModel.playVideo() }, // Triggers stream preparation
        onPauseClick = { viewModel.stopVideo() },    // Stops the stream
        onImageAvailable = { viewModel.imageAvailable(it) }
    )
}

/**
 * The stateless UI renderer for the video player.
 *
 * It utilizes a [Box] to layer UI elements using Z-index logic (first item is bottom, last is top):
 * 1. Bottom Layer: The Video Surface (AndroidView)
 * 2. Middle Layer: Error Messages (if present)
 * 3. Top Layer: Control Overlays (Loading spinners, Play/Pause buttons)
 *
 * @param modifier Modifier for styling.
 * @param player The underlying Media3 ExoPlayer instance.
 * @param playbackState The current status of the player (Buffering, Playing, etc.).
 * @param videoAspectRatio The ratio (width/height) to enforce on the video container.
 * @param errorMessage A readable error string, or null if no error exists.
 * @param onPlayClick Callback triggered when the Play overlay is clicked.
 * @param onPauseClick Callback triggered when the active video area is clicked.
 * @param onImageAvailable Callback triggered when a snapshot of the video is captured.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun RTSPVideoPlayerContent(
    modifier: Modifier = Modifier,
    player: Player?,
    playbackState: RTSPVideoPlayerPlaybackState,
    videoAspectRatio: Float,
    errorMessage: String?,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onImageAvailable: (Bitmap) -> Unit,
) {
    // Reference to the Android View system's PlayerView to access the underlying SurfaceView/TextureView
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Snapshot Loop: Captures a frame every 5 seconds while the component is active
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)

            playerViewRef?.let {
                // Only capture if currently playing to avoid black/empty bitmaps
                if (it.player?.isPlaying == true) {
                    captureSnapshot(it) { bitmap ->
                        onImageAvailable(bitmap)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(
                videoAspectRatio,
                matchHeightConstraintsFirst = false
            ), // Dynamically sizing based on video content
        contentAlignment = Alignment.Center,
    ) {
        // LAYER 1: Error Handling
        // If an error exists, we show it immediately and hide the player behind it (or don't render it).
        errorMessage?.let { msg ->
            ErrorOverlay(Modifier.fillMaxSize(), msg)
        } ?: player?.let { player ->

            // LAYER 2: Video Surface
            // Since ExoPlayer relies on SurfaceView/TextureView, we use AndroidView to bridge
            // the legacy View system into Jetpack Compose.
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        // We disable the native controller because we are drawing our own custom UI overlays
                        useController = false
                        playerViewRef = this
                    }
                },
                update = { playerView ->
                    // Binds the ExoPlayer instance to the View whenever the Composable recomposes.
                    // The check ensures we don't re-assign the player unnecessarily, which can cause flickering.
                    if (playerView.player != player) {
                        playerView.player = player
                    }
                    playerViewRef = playerView
                },
            )

            // LAYER 3: Interactive Overlays
            // Renders controls based on the specific playback state
            when (playbackState) {
                RTSPVideoPlayerPlaybackState.Buffering -> {
                    LoadingOverlay(Modifier.fillMaxSize())
                }
                RTSPVideoPlayerPlaybackState.Idle,
                RTSPVideoPlayerPlaybackState.Ready -> {
                    PlayButtonOverlay(Modifier.fillMaxSize()) {
                        onPlayClick()
                    }
                }
                RTSPVideoPlayerPlaybackState.Playing -> {
                    // An invisible overlay that catches click events to pause the video
                    PauseButtonOverlay(Modifier.fillMaxSize()) {
                        onPauseClick()
                    }
                }
                else -> {}
            }
        } ?: run {
            // Fallback: Displayed if the ViewModel hasn't created the ExoPlayer instance yet
            EmptyPlayerOverlay(Modifier.fillMaxSize())
        }
    }
}

/**
 * Displays a centered error message with a solid background.
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
 * Displays a placeholder text while the Player initializes.
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
 * Displays a circular progress indicator.
 * Used when the video is buffering or connecting to the RTSP stream.
 */
@Composable
internal fun LoadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag("loading_overlay")
            .background(OverlayerBackgroundColor), // Semi-transparent background
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Displays a large "Play" icon centered over the video area.
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
            modifier = Modifier.fillMaxSize(0.2f) // Icon takes up 20% of the container size
        )
    }
}

/**
 * An invisible overlay used to capture click events during playback.
 * Clicking this will trigger the [onPauseClick] action.
 */
@Composable
internal fun PauseButtonOverlay(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .testTag("pause_button_overlay")
            .clickable(onClick = onClick),
    ) {
        // Intentionally empty: acts as a transparent click listener
    }
}

/**
 * Captures a bitmap snapshot of the current video frame.
 *
 * This function handles the difference between [TextureView] and [SurfaceView] backends:
 * 1. [TextureView]: Simple bitmap extraction via `getBitmap`.
 * 2. [SurfaceView]: Requires the asynchronous [PixelCopy] API because the surface buffer is separate from the app window.
 *
 * @param playerView The [PlayerView] containing the video surface.
 * @param onBitmapReady Callback invoked with the captured [Bitmap] on success.
 */
@OptIn(UnstableApi::class)
internal fun captureSnapshot(playerView: PlayerView, onBitmapReady: (Bitmap) -> Unit) {

    // Case 1: The underlying view is a TextureView (easier, but less performant for video)
    (playerView.videoSurfaceView as? TextureView)?.let { textureView ->
        textureView.bitmap?.let(onBitmapReady)
    }
    // Case 2: The underlying view is a SurfaceView (standard for ExoPlayer)
        ?: (playerView.videoSurfaceView as? SurfaceView)?.let { surfaceView ->
            try {
                // Create a blank bitmap matching the surface dimensions
                val bitmap = createBitmap(surfaceView.width, surfaceView.height)

                // Request a copy of the Surface's pixels into the Bitmap
                PixelCopy.request(
                    surfaceView,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            onBitmapReady(bitmap)
                        }
                    },
                    Handler(Looper.getMainLooper()) // Callback runs on the main thread
                )
            } catch (e: Exception) {
                Log.e("RTSPVideoPlayer", "cannot capture bitmap", e)
            }
        }
}

/**
 * Preview Composable for UI development.
 * Note: Manually constructs the ViewModel as Hilt injection does not work in standard Previews.
 */
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
            // Mock ViewModel setup for preview purposes
            val viewModel =
                RTSPVideoPlayerViewModel(LocalContext.current, null, false, onImageAvailable = null)
            RTSPVideoPlayer(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

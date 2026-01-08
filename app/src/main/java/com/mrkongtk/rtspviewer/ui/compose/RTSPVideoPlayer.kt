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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
 * 1. **State Management**: Collects stream data and playback status from [RTSPVideoPlayerViewModel].
 * 2. **Lifecycle Safety**: Manages the Android Lifecycle to stop high-bandwidth RTSP streams when the app is backgrounded.
 * 3. **UI Delegation**: Passes processed state to the stateless [RTSPVideoPlayerContent].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param viewModel The Hilt-injected ViewModel managing the Media3 ExoPlayer instance.
 */
@OptIn(UnstableApi::class)
@Composable
fun RTSPVideoPlayer(
    modifier: Modifier = Modifier,
    viewModel: RTSPVideoPlayerViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    var orientation by remember { mutableIntStateOf(configuration.orientation) }

    // -- State Observation --

    // Uses collectAsStateWithLifecycle to automatically stop flow collection when the UI is not visible,
    // which is critical for reducing CPU/Battery usage in video-heavy apps.
    val playerState by viewModel.state.collectAsStateWithLifecycle(null)
    val errorDescription by viewModel.state.map { it.error }.collectAsStateWithLifecycle(null)

    // Tracks the aspect ratio (width/height). Defaults to 16:9 until the RTSP stream provides metadata.
    val videoAspectRatio by viewModel.videoAspectRatio.collectAsStateWithLifecycle(16f / 9f)

    // Update orientation state when configuration changes (e.g., screen rotation)
    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect { orientation = it }
    }

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

    // Render the stateless UI content
    RTSPVideoPlayerContent(
        modifier = modifier.aspectRatio(
            videoAspectRatio,
            // In landscape, we prioritize height to ensure the video fits the screen properly.
            matchHeightConstraintsFirst = orientation == Configuration.ORIENTATION_LANDSCAPE
        ),
        player = viewModel.player,
        playbackState = playerState?.playback ?: RTSPVideoPlayerPlaybackState.Idle,
        errorMessage = errorDescription?.localizedMessage,
        onPlayClick = { viewModel.playVideo() },
        onPauseClick = { viewModel.stopVideo() },
        onImageAvailable = { viewModel.imageAvailable(it) }
    )
}

/**
 * The stateless UI renderer for the video player.
 *
 * Uses a [Box] to layer elements via Z-index logic:
 * 1. **Bottom**: The [AndroidView] (ExoPlayer Surface).
 * 2. **Middle**: Error messages or placeholders.
 * 3. **Top**: Interaction overlays (Loading indicators, Play/Pause buttons).
 *
 * @param player The Media3 Player instance.
 * @param playbackState The current status (Idle, Buffering, Ready, Playing).
 * @param errorMessage Localized error string, if any.
 * @param onImageAvailable Callback providing a [Bitmap] of the current frame for thumbnailing/analysis.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun RTSPVideoPlayerContent(
    modifier: Modifier = Modifier,
    player: Player?,
    playbackState: RTSPVideoPlayerPlaybackState,
    errorMessage: String?,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onImageAvailable: (Bitmap) -> Unit,
) {
    // Stores a reference to the PlayerView to perform bitmap captures (PixelCopy)
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Snapshot Loop: Periodically captures the current video frame while the Composable is active.
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000) // Capture frequency: 5 seconds
            playerViewRef?.let {
                // Prevent capturing black frames or placeholders when the player is stopped
                if (it.player?.isPlaying == true) {
                    captureSnapshot(it) { bitmap ->
                        onImageAvailable(bitmap)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        errorMessage?.let { msg ->
            ErrorOverlay(Modifier.fillMaxSize(), msg)
        } ?: player?.let { playerInstance ->

            // Bridge to the legacy View system. PlayerView is required as Compose does not
            // yet have a native high-performance Video Surface implementation.
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        useController = false // Custom UI is handled by Compose Overlays
                        playerViewRef = this
                    }
                },
                update = { view ->
                    if (view.player != playerInstance) {
                        view.player = playerInstance
                    }
                    playerViewRef = view
                },
            )

            // Playback State Overlays
            when (playbackState) {
                RTSPVideoPlayerPlaybackState.Buffering -> {
                    LoadingOverlay(Modifier.fillMaxSize())
                }
                RTSPVideoPlayerPlaybackState.Idle,
                RTSPVideoPlayerPlaybackState.Ready -> {
                    PlayButtonOverlay(Modifier.fillMaxSize(), onPlayClick)
                }
                RTSPVideoPlayerPlaybackState.Playing -> {
                    // Transparent overlay to detect clicks for pausing the stream
                    PauseButtonOverlay(Modifier.fillMaxSize(), onPauseClick)
                }
                else -> {}
            }
        } ?: run {
            EmptyPlayerOverlay(Modifier.fillMaxSize())
        }
    }
}

/**
 * Overlay shown when an error occurs (e.g., Connection Timeout, Auth Failure).
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
 */
@Composable
internal fun PauseButtonOverlay(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .testTag("pause_button_overlay")
            .clickable(onClick = onClick),
    )
}

/**
 * Performs a frame capture of the current video surface.
 *
 * **Logic**:
 * 1. If using [TextureView], uses [TextureView.getBitmap] (synchronous, easier).
 * 2. If using [SurfaceView], uses [PixelCopy] (asynchronous).
 *
 * Note: [PixelCopy] is necessary for [SurfaceView] because its contents are not
 * managed by the standard View hierarchy drawing pass.
 */
@OptIn(UnstableApi::class)
internal fun captureSnapshot(playerView: PlayerView, onBitmapReady: (Bitmap) -> Unit) {
    val surfaceView = playerView.videoSurfaceView

    // TextureView Branch
    (surfaceView as? TextureView)?.let { textureView ->
        textureView.bitmap?.let(onBitmapReady)
    }
    // SurfaceView Branch (Standard ExoPlayer default)
        ?: (surfaceView as? SurfaceView)?.let { sv ->
            try {
                if (sv.width > 0 && sv.height > 0) {
                    val bitmap = createBitmap(sv.width, sv.height)
                    PixelCopy.request(
                        sv,
                        bitmap,
                        { result ->
                            if (result == PixelCopy.SUCCESS) {
                                onBitmapReady(bitmap)
                            }
                        },
                        Handler(Looper.getMainLooper()) // Callback runs on the main thread
                    )
                }
            } catch (e: Exception) {
                Log.e("RTSPVideoPlayer", "Failed to capture snapshot", e)
            }
        }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(name = "Day", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Night", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
            val viewModel = RTSPVideoPlayerViewModel(
                LocalContext.current,
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

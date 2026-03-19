package com.mrkongtk.rtspviewer.shared.ui.compose


import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mrkongtk.rtspviewer.shared.player.ExoVideoPlayer
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


@OptIn(UnstableApi::class)
@Composable
actual fun RTSPVideoPlayerDisplay(
    modifier: Modifier,
    player: RTSPVideoPlayer,
    onImageAvailable: (ImageBitmap) -> Unit,
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

    (player as? ExoVideoPlayer)?.getPlayer<Player>()?.let { playerInstance ->
        // Bridge to the legacy View system. PlayerView is required as Compose does not
        // yet have a native high-performance Video Surface implementation.
        AndroidView(
            modifier = modifier,
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
    }
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
@androidx.annotation.OptIn(UnstableApi::class)
internal fun captureSnapshot(playerView: PlayerView, onBitmapReady: (ImageBitmap) -> Unit) {
    val surfaceView = playerView.videoSurfaceView

    // TextureView Branch
    (surfaceView as? TextureView)?.let { textureView ->
        textureView.bitmap?.asImageBitmap()?.let(onBitmapReady)
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
                                onBitmapReady(bitmap.asImageBitmap())
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
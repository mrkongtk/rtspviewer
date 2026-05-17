package com.mrkongtk.rtspviewer.shared.player

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class ExoVideoPlayerTest : RTSPVideoPlayerTest() {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    override fun createPlayer(): RTSPVideoPlayer {
        return ExoVideoPlayer(context)
    }

    override fun runOnMainThreadSync(action: () -> Unit) {
        instrumentation.runOnMainSync {
            action()
        }
    }

    private fun getListener(player: ExoVideoPlayer): Player.Listener {
        val listenerField = ExoVideoPlayer::class.java.getDeclaredField("listener")
        listenerField.isAccessible = true
        return listenerField.get(player) as Player.Listener
    }

    @Test
    fun testRTSPOptionsForceTcp() {
        instrumentation.runOnMainSync {
            val player = createPlayer()
            // We use a real RTSP extension check if possible or mock the factory
            player.prepare(uri, forceTcp = true)

            val exo = player.getPlayer<ExoPlayer>()!!
            val mediaItem = exo.currentMediaItem
            assertNotNull(mediaItem)
            assertEquals(uri, mediaItem.localConfiguration?.uri.toString())
        }
    }

    @Test
    fun testPlayerErrorMapping() {
        instrumentation.runOnMainSync {
            val player = ExoVideoPlayer(context)
            val listener = getListener(player)

            val exception = PlaybackException(
                "Test Error", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            )

            listener.onPlayerErrorChanged(exception)
            assertEquals(exception, player.error.value)
        }
    }

    @Test
    fun testAspectRatioUpdate() {
        instrumentation.runOnMainSync {
            val player = ExoVideoPlayer(context)
            val listener = getListener(player)

            // Test standard 16:9
            listener.onVideoSizeChanged(VideoSize(1920, 1080))
            assertEquals(16f / 9f, player.videoAspectRatio.value)

            // Test 4:3
            listener.onVideoSizeChanged(VideoSize(640, 480))
            assertEquals(4f / 3f, player.videoAspectRatio.value)

            // Test invalid size (0 width) - should fallback to 16:9
            listener.onVideoSizeChanged(VideoSize(0, 1080))
            assertEquals(16f / 9f, player.videoAspectRatio.value)

            // Test invalid size (0 height) - should fallback to 16:9
            listener.onVideoSizeChanged(VideoSize(1920, 0))
            assertEquals(16f / 9f, player.videoAspectRatio.value)
        }
    }

    @Test
    fun testPlaybackStateMapping() {
        instrumentation.runOnMainSync {
            val player = ExoVideoPlayer(context)
            val listener = getListener(player)

            listener.onPlaybackStateChanged(Player.STATE_READY)
            assertEquals(RTSPVideoPlayerPlaybackState.Ready, player.currentState.value)

            listener.onPlaybackStateChanged(Player.STATE_BUFFERING)
            assertEquals(RTSPVideoPlayerPlaybackState.Buffering, player.currentState.value)

            listener.onPlaybackStateChanged(Player.STATE_ENDED)
            assertEquals(RTSPVideoPlayerPlaybackState.Ended, player.currentState.value)

            listener.onPlaybackStateChanged(Player.STATE_IDLE)
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, player.currentState.value)
        }
    }

    @Test
    fun testIsPlayingMapping() {
        instrumentation.runOnMainSync {
            val player = ExoVideoPlayer(context)
            val listener = getListener(player)

            // Set to Ready first
            listener.onPlaybackStateChanged(Player.STATE_READY)
            assertEquals(RTSPVideoPlayerPlaybackState.Ready, player.currentState.value)

            // Trigger isPlaying = true
            listener.onIsPlayingChanged(true)
            assertEquals(RTSPVideoPlayerPlaybackState.Playing, player.currentState.value)

            // Trigger isPlaying = false - should go back to Ready if it was Playing
            listener.onIsPlayingChanged(false)
            assertEquals(RTSPVideoPlayerPlaybackState.Ready, player.currentState.value)
        }
    }

    @Test
    fun testPlayPauseLogic() {
        instrumentation.runOnMainSync {
            val player = createPlayer()
            val exo = player.getPlayer<ExoPlayer>()!!

            player.prepare(uri, forceTcp = true)

            player.play()
            assertEquals(true, exo.playWhenReady)

            player.stop()
            assertEquals(false, exo.playWhenReady)
        }
    }

    @Test
    fun testPlayWhenIdlePreparesPlayer() {
        instrumentation.runOnMainSync {
            val player = ExoVideoPlayer(context)
            val exo = player.getPlayer<ExoPlayer>()!!
            
            assertEquals(Player.STATE_IDLE, exo.playbackState)
            
            player.play()
            
            // Calling play() when Idle should call exo.prepare()
            // In ExoPlayer, calling prepare() doesn't immediately change state to BUFFERING on the same thread
            // but we can check if it's no longer IDLE or if playWhenReady is true
            assertEquals(true, exo.playWhenReady)
        }
    }

    @Test
    fun testPlayWhenReady() {
        instrumentation.runOnMainSync {
            val player = ExoVideoPlayer(context)
            val exo = player.getPlayer<ExoPlayer>()!!
            val listener = getListener(player)

            listener.onPlaybackStateChanged(Player.STATE_READY)
            
            player.play()
            assertEquals(true, exo.playWhenReady)
        }
    }

    @Test
    fun testReleaseState() {
        instrumentation.runOnMainSync {
            val player = ExoVideoPlayer(context)
            val listener = getListener(player)

            player.release()
            assertEquals(RTSPVideoPlayerPlaybackState.Released, player.currentState.value)

            // Further listener events should be ignored
            listener.onPlaybackStateChanged(Player.STATE_READY)
            assertEquals(RTSPVideoPlayerPlaybackState.Released, player.currentState.value)

            listener.onIsPlayingChanged(true)
            assertEquals(RTSPVideoPlayerPlaybackState.Released, player.currentState.value)
        }
    }
}

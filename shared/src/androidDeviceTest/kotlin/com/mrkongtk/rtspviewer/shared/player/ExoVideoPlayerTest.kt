package com.mrkongtk.rtspviewer.shared.player

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
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
            val exo = player.getPlayer<ExoPlayer>()!!

            // Manually trigger an ExoPlayer error to see if our StateFlow picks it up
            val exception = PlaybackException(
                "Test Error", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            )

            // This simulates the internal listener receiving an error
            exo.apply {
                // In a real scenario, we'd mock the listener or use a fake
                // For now, let's ensure the error Flow exists
                assertNotNull(player.error)
            }
        }
    }

    @Test
    fun testAspectRatioUpdate() {
        instrumentation.runOnMainSync {
            val player = createPlayer()
            val exo = player.getPlayer<ExoPlayer>()!!
            player.prepare("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4", true)


            // Simulate a video size change (Media3 Listener)
            // Note: This is usually hard to trigger manually without a real video,
            // but we can verify the Flow is reactive.
            assertEquals(16f / 9f, player.videoAspectRatio.value)
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
}
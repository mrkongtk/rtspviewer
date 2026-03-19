package com.mrkongtk.rtspviewer.shared.player

import app.cash.turbine.test
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

// test setup video
// docker run --rm -it -p 8554:8554 -p 1935:1935 -p 8888:8888 -p 8889:8889 bluenviron/mediamtx
// ffmpeg -re -stream_loop -1 -i test.mp4 -vf "scale=640:-1" -c:v libx264 -preset veryfast -b:v 1000k -c:a aac -f rtsp -rtsp_transport tcp rtsp://localhost:8554/live
abstract class RTSPVideoPlayerTest {

    protected val uri = "rtsp://100.119.106.67:8554/live"

    abstract fun createPlayer(): RTSPVideoPlayer
    abstract fun runOnMainThreadSync(action: () -> Unit)

    @Test
    fun testInitialStateIsIdle() = runTest {
        val player = createPlayer()
        player.currentState.test {
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, awaitItem())
        }
    }

    @Test
    fun testPrepareChangesStateToLoading() = runTest {
        val player = createPlayer()
        player.currentState.test(timeout = 30.seconds) {
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, awaitItem())
            runOnMainThreadSync {
                player.prepare(uri = uri, forceTcp = true)
            }
            assertEquals(RTSPVideoPlayerPlaybackState.Buffering, awaitItem())
        }
    }

    @Test
    fun testPrepareChangeStateToPlaying() = runTest {
        val player: RTSPVideoPlayer = createPlayer()

        combine(player.currentState, player.error) { state, error -> state to error }
            .test(timeout = 30.seconds) {

                assertEquals(RTSPVideoPlayerPlaybackState.Idle to null, awaitItem())
                runOnMainThreadSync {
                    player.prepare(uri = uri, forceTcp = true)
                }
                assertEquals(RTSPVideoPlayerPlaybackState.Buffering to null, awaitItem())
                assertEquals(RTSPVideoPlayerPlaybackState.Ready to null, awaitItem())
                assertEquals(RTSPVideoPlayerPlaybackState.Playing to null, awaitItem())
            }
    }

    @Test
    fun testStopReturnsToIdle() = runTest {
        val player = createPlayer()
        player.currentState.test(timeout = 30.seconds) {
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, awaitItem())
            runOnMainThreadSync {
                player.prepare(uri = uri, true)
            }
            assertEquals(RTSPVideoPlayerPlaybackState.Buffering, awaitItem())
            runOnMainThreadSync {
                player.stop()
            }
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, awaitItem())
        }
    }

    @Test
    fun testReleaseClearsResources() = runTest {
        val player = createPlayer()
        player.currentState.test(timeout = 30.seconds) {
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, awaitItem())
            runOnMainThreadSync {
                player.prepare(uri = uri, true)
            }
            assertEquals(RTSPVideoPlayerPlaybackState.Buffering, awaitItem())
            runOnMainThreadSync {
                player.release()
            }
            assertEquals(RTSPVideoPlayerPlaybackState.Released, awaitItem())
        }
    }

    @Test
    fun testNoActionAfterRelease() = runTest {
        val player = createPlayer()
        player.currentState.test(timeout = 30.seconds) {
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, awaitItem())
            runOnMainThreadSync {
                player.release()
            }
            assertEquals(RTSPVideoPlayerPlaybackState.Released, awaitItem())
            runOnMainThreadSync {
                player.prepare(uri = uri, true)
            }
            expectNoEvents()
            runOnMainThreadSync {
                player.stop()
            }
            expectNoEvents()
        }
    }
}

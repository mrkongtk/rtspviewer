package com.mrkongtk.rtspviewer.shared.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpace
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RTSPVideoPlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeVideoPlayer: FakeRTSPVideoPlayer

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeVideoPlayer = FakeRTSPVideoPlayer()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        uri: String? = null,
        forceTcp: Boolean = false,
        onImageAvailable: ((ImageBitmap) -> Unit)? = null
    ): RTSPVideoPlayerViewModel {
        return RTSPVideoPlayerViewModel(fakeVideoPlayer, uri, forceTcp, onImageAvailable)
    }

    @Test
    fun init_starts_loading_immediately_if_URI_is_provided() = runTest {
        val testUri = "rtsp://camera1"
        val testTcp = true
        val viewModel = createViewModel(uri = testUri, forceTcp = testTcp)

        assertEquals(testUri, fakeVideoPlayer.lastPreparedUri)
        assertEquals(testTcp, fakeVideoPlayer.lastPreparedForceTcp)

        viewModel.data.test {
            val data = awaitItem()
            assertEquals(testUri, data?.uri)
            assertEquals(testTcp, data?.forceTcp)
        }
    }

    @Test
    fun init_does_not_prepare_if_URI_is_null() = runTest {
        createViewModel(uri = null)
        assertEquals(null, fakeVideoPlayer.lastPreparedUri)
    }

    @Test
    fun state_combines_player_state_and_errors_correctly() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            // Initial state
            assertEquals(RTSPVideoPlayerPlaybackState.Idle, awaitItem().playback)

            // Update player state
            fakeVideoPlayer.emitState(RTSPVideoPlayerPlaybackState.Buffering)
            assertEquals(RTSPVideoPlayerPlaybackState.Buffering, awaitItem().playback)

            // Update player error
            val exception = RuntimeException("Connection Failed")
            fakeVideoPlayer.emitError(exception)
            val stateWithError = awaitItem()
            assertEquals(exception, stateWithError.error)
            assertEquals(RTSPVideoPlayerPlaybackState.Buffering, stateWithError.playback)
        }
    }

    @Test
    fun videoAspectRatio_reflects_player_aspect_ratio() = runTest {
        val viewModel = createViewModel()
        viewModel.videoAspectRatio.test {
            assertEquals(16f / 9f, awaitItem())

            fakeVideoPlayer.emitAspectRatio(4f / 3f)
            assertEquals(4f / 3f, awaitItem())
        }
    }

    @Test
    fun updateData_merges_partial_updates_correctly() = runTest {
        val viewModel = createViewModel(uri = "rtsp://old", forceTcp = false)

        // Update only TCP: should keep old URI
        viewModel.updateData(forceTcp = true)
        assertEquals("rtsp://old", fakeVideoPlayer.lastPreparedUri)
        assertEquals(true, fakeVideoPlayer.lastPreparedForceTcp)

        // Update only URI: should keep the true TCP from previous update
        viewModel.updateData(uri = "rtsp://new")
        assertEquals("rtsp://new", fakeVideoPlayer.lastPreparedUri)
        assertEquals(true, fakeVideoPlayer.lastPreparedForceTcp)
    }

    @Test
    fun play_and_stop_delegates_calls_to_videoPlayer() = runTest {
        val viewModel = createViewModel()

        viewModel.playVideo()
        assertTrue(fakeVideoPlayer.playCalled)

        viewModel.stopVideo()
        assertTrue(fakeVideoPlayer.stopCalled)
    }

    @Test
    fun imageAvailable_invokes_callback_with_bitmap() = runTest {
        var captured: ImageBitmap? = null
        val viewModel = createViewModel(onImageAvailable = { captured = it })

        // Create a dummy bitmap for common code
        val dummyBitmap = object : ImageBitmap {
            override val config: androidx.compose.ui.graphics.ImageBitmapConfig get() = throw NotImplementedError()
            override val hasAlpha: Boolean get() = false
            override val height: Int get() = 1
            override val colorSpace: ColorSpace
                get() = TODO("Not yet implemented")
            override val width: Int get() = 1
            override fun prepareToDraw() {}
            override fun readPixels(
                buffer: IntArray,
                startX: Int,
                startY: Int,
                width: Int,
                height: Int,
                bufferOffset: Int,
                stride: Int
            ) {
            }
        }

        viewModel.imageAvailable(dummyBitmap)
        assertEquals(dummyBitmap, captured)
    }

    @Test
    fun onCleared_side_effect_releases_the_video_player() = runTest {
        // Since onCleared is protected, and we are in Common code (no Java reflection),
        // we create a subclass to expose it for testing.
        class TestableViewModel(
            player: RTSPVideoPlayer,
            uri: String?,
            tcp: Boolean
        ) : RTSPVideoPlayerViewModel(player, uri, tcp, null) {
            fun simulateCleared() = onCleared()
        }

        val viewModel = TestableViewModel(fakeVideoPlayer, null, false)
        viewModel.simulateCleared()

        assertTrue(fakeVideoPlayer.released)
    }

    /**
     * A Fake implementation of [RTSPVideoPlayer] to avoid platform dependencies.
     */
    private class FakeRTSPVideoPlayer : RTSPVideoPlayer {
        private val _currentState = MutableStateFlow(RTSPVideoPlayerPlaybackState.Idle)
        override val currentState: StateFlow<RTSPVideoPlayerPlaybackState> = _currentState

        private val _error = MutableStateFlow<Throwable?>(null)
        override val error: StateFlow<Throwable?> = _error

        private val _videoAspectRatio = MutableStateFlow(16f / 9f)
        override val videoAspectRatio: StateFlow<Float> = _videoAspectRatio

        var lastPreparedUri: String? = null
        var lastPreparedForceTcp: Boolean = false
        var playCalled = false
        var stopCalled = false
        var released = false

        override fun prepare(uri: String, forceTcp: Boolean) {
            lastPreparedUri = uri
            lastPreparedForceTcp = forceTcp
        }

        override fun play() {
            playCalled = true
        }

        override fun stop() {
            stopCalled = true
        }

        override fun release() {
            released = true
        }

        override fun <T> getPlayer(): T? = null

        fun emitState(state: RTSPVideoPlayerPlaybackState) {
            _currentState.value = state
        }

        fun emitError(t: Throwable?) {
            _error.value = t
        }

        fun emitAspectRatio(ratio: Float) {
            _videoAspectRatio.value = ratio
        }
    }
}
package com.mrkongtk.rtspviewer.viewmodel

import androidx.media3.common.PlaybackException
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class RTSPVideoPlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", true)

        val currentState = viewModel.state.value
        assertEquals(RTSPVideoPlayerPlaybackState.Idle, currentState.playback)
        assertNull(currentState.error)
    }

    @Test
    fun `updatePlaybackState updates the state flow`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", true)

        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Buffering)

        assertEquals(RTSPVideoPlayerPlaybackState.Buffering, viewModel.state.value.playback)

        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Playing)
        assertEquals(RTSPVideoPlayerPlaybackState.Playing, viewModel.state.value.playback)
    }

    @Test
    fun `updatePlaybackError updates the error state`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", true)
        val mockException = mock<PlaybackException>()

        viewModel.updatePlaybackError(mockException)

        assertEquals(mockException, viewModel.state.value.error)

        // Ensure playback state wasn't accidentally reset
        assertEquals(RTSPVideoPlayerPlaybackState.Idle, viewModel.state.value.playback)
    }

    @Test
    fun `data flow emits correct configuration`() = runTest {
        val uri = "rtsp://192.168.1.1"
        val forceTcp = true
        val viewModel = RTSPVideoPlayerViewModel(uri, forceTcp)

        // We can collect the 'data' flow to verify it emits the initial params
        val data = viewModel.data.firstOrNull()

        assertNotNull(data)
        assertEquals(uri, data?.uri)
        assertEquals(true, data?.forceTcp)
    }
}

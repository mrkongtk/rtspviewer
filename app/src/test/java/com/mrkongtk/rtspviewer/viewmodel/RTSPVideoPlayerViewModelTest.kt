package com.mrkongtk.rtspviewer.viewmodel

import androidx.media3.common.PlaybackException
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Unit tests for [RTSPVideoPlayerViewModel].
 *
 * This class validates:
 * 1. State management (Playback status, Errors).
 * 2. Data flow emissions (URI and Configuration).
 * 3. Logic for Aspect Ratio calculations, including edge cases.
 *
 * It uses [StandardTestDispatcher] to simulate Coroutine behavior on the Main thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RTSPVideoPlayerViewModelTest {

    // TestDispatcher controls the execution of coroutines in tests (allows pausing/advancing time)
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Replaces the Main dispatcher with the test dispatcher before every test.
     * This is necessary because ViewModels use `viewModelScope`, which defaults to Dispatchers.Main.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Resets the Main dispatcher to the original after every test to prevent memory leaks
     * or interference with other tests.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that the ViewModel initializes with the expected default values:
     * - Playback state is Idle.
     * - No errors exist.
     * - Aspect ratio defaults to 16:9.
     */
    @Test
    fun `initial state is correct`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", true)

        val currentState = viewModel.state.value
        assertEquals(RTSPVideoPlayerPlaybackState.Idle, currentState.playback)
        assertNull(currentState.error)
        assertEquals(16f / 9f, viewModel.videoAspectRatio.value, 0.001f)
    }

    /**
     * Verifies that calling [updatePlaybackState] correctly emits the new state
     * to the observable state flow.
     */
    @Test
    fun `updatePlaybackState updates the state flow`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", true)

        // Test transition to Buffering
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Buffering)
        assertEquals(RTSPVideoPlayerPlaybackState.Buffering, viewModel.state.value.playback)

        // Test transition to Playing
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Playing)
        assertEquals(RTSPVideoPlayerPlaybackState.Playing, viewModel.state.value.playback)
    }

    /**
     * Verifies that exceptions are correctly stored in the state when an error occurs,
     * and that passing null correctly clears the error state.
     */
    @Test
    fun `updatePlaybackError updates the error state`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", true)
        val mockException = mock<PlaybackException>() // Create a mock object for the exception

        // Set an error
        viewModel.updatePlaybackError(mockException)
        assertEquals(mockException, viewModel.state.value.error)

        // Verify we can clear the error by passing null
        viewModel.updatePlaybackError(null)
        assertNull(viewModel.state.value.error)
    }

    /**
     * Verifies that the combined data flow correctly emits the initialization parameters
     * (URI and TCP preference).
     */
    @Test
    fun `data flow emits correct configuration`() = runTest {
        val uri = "rtsp://192.168.1.1"
        val forceTcp = true
        val viewModel = RTSPVideoPlayerViewModel(uri, forceTcp)

        // collect the first emission from the flow
        val data = viewModel.data.first()

        assertEquals(uri, data?.uri)
        assertEquals(true, data?.forceTcp)
    }

    /**
     * Verifies behavior when the URI provided is null.
     * Ensures the ViewModel initializes without crashing and maintains a clean state.
     */
    @Test
    fun `data flow does not emit if URI is null`() = runTest {
        // Initialize with null URI
        val viewModel = RTSPVideoPlayerViewModel(null, false)

        // This test primarily ensures no exception is thrown during init
        // and that the error state remains clean.
        assertNull(viewModel.state.value.error)
    }

    /**
     * Verifies the mathematical calculation of the video aspect ratio (Width / Height).
     * Tests both standard Landscape (16:9) and Portrait (9:16) scenarios.
     */
    @Test
    fun `updateVideoAspectRatio calculates correct ratio`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", false)

        // 1920x1080 (16:9) -> Expect ~1.777
        viewModel.updateVideoAspectRatio(1920f, 1080f)
        assertEquals(1.777f, viewModel.videoAspectRatio.value, 0.001f)

        // 1080x1920 (9:16 - Portrait) -> Expect ~0.5625
        viewModel.updateVideoAspectRatio(1080f, 1920f)
        assertEquals(0.5625f, viewModel.videoAspectRatio.value, 0.001f)
    }

    /**
     * Verifies robustness of the aspect ratio calculation.
     * Ensures that invalid dimensions (Zero, Negative, or Infinite) fall back
     * to the default 16:9 ratio to prevent UI layout crashes.
     */
    @Test
    fun `updateVideoAspectRatio handles invalid inputs gracefully`() = runTest {
        val viewModel = RTSPVideoPlayerViewModel("rtsp://test", false)

        // Zero width/height -> Should default to 16:9
        viewModel.updateVideoAspectRatio(0f, 0f)
        assertEquals(16f / 9f, viewModel.videoAspectRatio.value, 0.001f)

        // Negative values -> Should default to 16:9
        viewModel.updateVideoAspectRatio(-100f, 100f)
        assertEquals(16f / 9f, viewModel.videoAspectRatio.value, 0.001f)

        // Infinite values -> Should default to 16:9
        viewModel.updateVideoAspectRatio(Float.POSITIVE_INFINITY, 100f)
        assertEquals(16f / 9f, viewModel.videoAspectRatio.value, 0.001f)
    }
}

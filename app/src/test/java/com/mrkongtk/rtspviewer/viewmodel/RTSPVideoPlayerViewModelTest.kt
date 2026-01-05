package com.mrkongtk.rtspviewer.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.PlaybackException
import app.cash.turbine.test
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Unit tests for [RTSPVideoPlayerViewModel].
 *
 * **Testing Strategy:**
 * 1. **ExoPlayer Handling:** In a standard JVM unit test environment, `ExoPlayer.Builder(context).build()`
 *    will throw an exception because Android system classes are not available (unless using Robolectric).
 *    We utilize this behavior to verify the ViewModel's error handling in the `init` block.
 * 2. **Internal Method Testing:** Since we cannot easily mock the real ExoPlayer listeners without
 *    Robolectric or complex static mocking, we test the internal methods (`updatePlaybackState`,
 *    `updateVideoAspectRatio`) directly. This simulates the callbacks the player would send.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RTSPVideoPlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper to instantiate the ViewModel with optional parameters.
     * Updated to include the onImageAvailable callback.
     */
    private fun createViewModel(
        uri: String? = null,
        forceTcp: Boolean = false,
        onImageAvailable: ((Bitmap) -> Unit)? = null
    ): RTSPVideoPlayerViewModel {
        return RTSPVideoPlayerViewModel(mockContext, uri, forceTcp, onImageAvailable)
    }

    @Test
    fun `init - catches ExoPlayer creation failure, sets player to null and updates error state`() =
        runTest {
            val viewModel = createViewModel()

            // 1. Assert Player is null (due to Builder failure in JVM test environment)
            assertNull(
                "Public player property should be null in JVM tests due to Builder failure",
                viewModel.player
            )

            // 2. Verify the initial state emission contains the initialization error
            viewModel.state.test {
                val initialState = awaitItem()
                assertEquals(RTSPVideoPlayerPlaybackState.Idle, initialState.playback)
                assertNotNull(
                    "State should contain the exception from ExoPlayer builder",
                    initialState.error
                )
            }
        }

    @Test
    fun `updatePlaybackState - updates the state flow correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            // Skip the initial state (which contains the init error)
            skipItems(1)

            // Test transition to Buffering
            viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Buffering)
            val bufferingState = awaitItem()
            assertEquals(RTSPVideoPlayerPlaybackState.Buffering, bufferingState.playback)

            // Test transition to Playing
            viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Playing)
            val playingState = awaitItem()
            assertEquals(RTSPVideoPlayerPlaybackState.Playing, playingState.playback)
            assertNull("Error should remain null during normal state changes", playingState.error)
        }
    }

    @Test
    fun `updateError - updates and clears the error state`() = runTest {
        val viewModel = createViewModel()
        val mockException = mock<PlaybackException>()

        viewModel.state.test {
            skipItems(1) // Skip init error

            // 1. Set a specific error
            viewModel.updateError(mockException)
            val errorState = awaitItem()
            assertEquals(mockException, errorState.error)

            // 2. Clear the error
            viewModel.updateError(null)
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    @Test
    fun `updateVideoAspectRatio - calculates correct ratio`() = runTest {
        val viewModel = createViewModel()

        viewModel.videoAspectRatio.test {
            val initialRatio = awaitItem()
            assertEquals(16f / 9f, initialRatio, 0.001f)

            // 1080x1920 (9:16 - Portrait) -> Expect ~0.5625
            viewModel.updateVideoAspectRatio(1080f, 1920f)
            val portraitRatio = awaitItem()
            assertEquals(0.5625f, portraitRatio, 0.001f)

            // 1920x1080 (16:9 - Landscape) -> Expect ~1.777
            viewModel.updateVideoAspectRatio(1920f, 1080f)
            val landscapeRatio = awaitItem()
            assertEquals(1.777f, landscapeRatio, 0.001f)
        }
    }

    @Test
    fun `updateVideoAspectRatio - handles invalid inputs by defaulting to 16_9`() = runTest {
        val viewModel = createViewModel()
        val defaultRatio = 16f / 9f

        viewModel.videoAspectRatio.test {
            awaitItem() // Initial

            // Set to something valid first to ensure we detect a reset
            viewModel.updateVideoAspectRatio(100f, 100f)
            assertEquals(1.0f, awaitItem(), 0.001f)

            // Case 1: Zero width/height (Results in NaN)
            viewModel.updateVideoAspectRatio(0f, 0f)
            assertEquals("Should revert to 16:9 on 0x0", defaultRatio, awaitItem(), 0.001f)

            // Reset to valid
            viewModel.updateVideoAspectRatio(100f, 100f)
            awaitItem()

            // Case 2: Zero height (Results in Infinity)
            viewModel.updateVideoAspectRatio(1920f, 0f)
            assertEquals("Should revert to 16:9 on div/0", defaultRatio, awaitItem(), 0.001f)

            // Reset to valid
            viewModel.updateVideoAspectRatio(100f, 100f)
            awaitItem()

            // Case 3: Negative dimensions (Logic check: result > 0)
            viewModel.updateVideoAspectRatio(-1920f, 1080f)
            assertEquals(
                "Should revert to 16:9 on negative dimensions",
                defaultRatio,
                awaitItem(),
                0.001f
            )
        }
    }

    @Test
    fun `init - processes initial URI and TCP settings into Data flow`() = runTest {
        val testUri = "rtsp://camera"
        val testTcp = true

        val viewModel = createViewModel(uri = testUri, forceTcp = testTcp)

        viewModel.data.test {
            val data = awaitItem()
            assertNotNull(data)
            assertEquals(testUri, data?.uri)
            assertEquals(testTcp, data?.forceTcp)
        }
    }

    @Test
    fun `updateData - handles partial and full updates logic`() = runTest {
        // Start with no data
        val viewModel = createViewModel(uri = null, forceTcp = false)

        viewModel.data.test {
            assertNull("Initial data should be null", awaitItem())

            // 1. Set URI only (TCP should default to false as per logic)
            val uri1 = "rtsp://1"
            viewModel.updateData(uri = uri1)
            val state1 = awaitItem()
            assertEquals(uri1, state1?.uri)
            assertEquals(false, state1?.forceTcp)

            // 2. Update TCP only (URI should persist from oldData)
            viewModel.updateData(forceTcp = true)
            val state2 = awaitItem()
            assertEquals("URI should persist", uri1, state2?.uri)
            assertEquals("TCP should update", true, state2?.forceTcp)

            // 3. Update both
            val uri2 = "rtsp://2"
            viewModel.updateData(uri = uri2, forceTcp = false)
            val state3 = awaitItem()
            assertEquals(uri2, state3?.uri)
            assertEquals(false, state3?.forceTcp)
        }
    }

    @Test
    fun `playVideo - is safe to call when player is null`() = runTest {
        val viewModel = createViewModel()
        // In this test env, player is null.
        // Calling playVideo should just do nothing and NOT throw NullPointerException.
        try {
            viewModel.playVideo()
        } catch (e: Exception) {
            throw AssertionError("playVideo threw exception when player was null", e)
        }
    }

    @Test
    fun `stopVideo - is safe to call when player is null`() = runTest {
        val viewModel = createViewModel()
        try {
            viewModel.stopVideo()
        } catch (e: Exception) {
            throw AssertionError("stopVideo threw exception when player was null", e)
        }
    }

    @Test
    fun `imageAvailable - invokes callback with captured bitmap`() = runTest {
        var capturedBitmap: Bitmap? = null
        val mockBitmap = mock<Bitmap>()

        val viewModel = createViewModel(onImageAvailable = { bitmap ->
            capturedBitmap = bitmap
        })

        // Trigger the view model method
        viewModel.imageAvailable(mockBitmap)

        assertNotNull("Callback should have been triggered", capturedBitmap)
        assertEquals("Callback should receive the exact bitmap", mockBitmap, capturedBitmap)
    }
}

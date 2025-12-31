package com.mrkongtk.rtspviewer.ui.compose

import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Instrumentation tests for the [RTSPVideoPlayerContent] Composable.
 *
 * **Testing Strategy:**
 * - **Component Isolation:** We test [RTSPVideoPlayerContent] directly instead of the stateful [RTSPVideoPlayer] wrapper.
 *   This allows us to bypass the ViewModel's internal ExoPlayer creation and inject a Mock.
 * - **Mocking:** The [ExoPlayer] is mocked using Mockito to prevent real resource allocation and network calls.
 * - **State Verification:** We pass explicit states (Buffering, Error, Playing) to ensure the UI overlays appear correctly.
 * - **Callback Verification:** We ensure that clicking the UI buttons triggers the correct lambdas.
 */
@OptIn(UnstableApi::class) // Required for Media3/ExoPlayer APIs
class RTSPVideoPlayerTest {

    /**
     * Standard Compose JUnit rule used to set content, find nodes, and perform actions.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock the ExoPlayer interface.
    private val mockExoPlayer: ExoPlayer = mock()

    /**
     * Sets up common stubbing for the ExoPlayer mock.
     *
     * The [androidx.media3.ui.PlayerView] queries these properties immediately upon attachment.
     * Returning default values prevents NullPointerExceptions during the UI rendering phase.
     */
    @Before
    fun setup() {
        whenever(mockExoPlayer.applicationLooper).thenReturn(Looper.getMainLooper())
        whenever(mockExoPlayer.videoSize).thenReturn(VideoSize.UNKNOWN)
        whenever(mockExoPlayer.currentPosition).thenReturn(0L)
        whenever(mockExoPlayer.duration).thenReturn(0L)
        whenever(mockExoPlayer.isPlaying).thenReturn(false)
    }

    /**
     * Verifies that the player renders the view without obstructions when in the Idle state.
     */
    @Test
    fun testContent_rendersPlayerView() {
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                exoPlayer = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Idle,
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {}
            )
        }

        // Wait for composition to settle
        composeTestRule.waitForIdle()

        // Although PlayerView is inside an AndroidView and hard to query via tags directly,
        // we verify that no error or loading overlays are blocking it while in the Idle state.
        composeTestRule.onNodeWithTag("loading_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("error_overlay").assertDoesNotExist()
    }

    /**
     * Verifies visual state: When state is [RTSPVideoPlayerPlaybackState.Buffering],
     * the UI should show the LoadingOverlay.
     */
    @Test
    fun testBufferingState_displaysLoadingOverlay() {
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                exoPlayer = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Buffering,
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {}
            )
        }

        // Assert: The loading indicator with testTag "loading_overlay" is visible
        composeTestRule.onNodeWithTag("loading_overlay").assertIsDisplayed()

        // Ensure play button is not visible during buffering
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    /**
     * Verifies visual state: When an error message is provided, the ErrorOverlay is shown.
     */
    @Test
    fun testErrorState_displaysErrorOverlayWithText() {
        // Arrange
        val errorMsg = "RTSP Connection Failed"

        // Act
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                exoPlayer = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Idle,
                videoAspectRatio = 1.77f,
                errorMessage = errorMsg,
                onPlayClick = {},
                onPauseClick = {}
            )
        }

        // Assert: The error overlay and the specific text message are displayed
        composeTestRule.onNodeWithTag("error_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    }

    /**
     * Verifies interaction: When state is [RTSPVideoPlayerPlaybackState.Ready],
     * the Play button is shown, and clicking it triggers the onPlayClick callback.
     */
    @Test
    fun testReadyState_displaysPlayButton_andClickTriggersCallback() {
        // Mock the callback
        var playClicked = false

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                exoPlayer = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Ready,
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = { playClicked = true },
                onPauseClick = {}
            )
        }

        // Assert: Play button exists
        val playButton = composeTestRule.onNodeWithTag("play_button_overlay")
        playButton.assertIsDisplayed()

        // Act: Click it
        playButton.performClick()

        // Assert: Callback fired
        assert(playClicked) { "Expected onPlayClick to be called" }
    }

    /**
     * Verifies interaction: When state is [RTSPVideoPlayerPlaybackState.Playing],
     * the transparent Pause overlay is present, and clicking it triggers the onPauseClick callback.
     */
    @Test
    fun testPlayingState_displaysPauseOverlay_andClickTriggersCallback() {
        // Mock the callback
        var pauseClicked = false

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                exoPlayer = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Playing,
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = { pauseClicked = true }
            )
        }

        // Assert: Play button should be gone
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()

        // Act: Click the "pause_button_overlay" (usually a scrim covering the video area)
        composeTestRule.onNodeWithTag("pause_button_overlay").performClick()

        // Assert: Callback fired
        assert(pauseClicked) { "Expected onPauseClick to be called" }
    }

    /**
     * Verifies that the aspect ratio modifier is applied correctly without exceptions.
     *
     * Note: We cannot easily measure the exact pixels in a standard instrumented test without
     * taking screenshots, but we can verify the composable renders without crashing given a specific ratio.
     */
    @Test
    fun testAspectRatio_updatesWithoutCrash() {
        val wideRatio = 2.35f // Cinematic format

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                exoPlayer = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Playing,
                videoAspectRatio = wideRatio,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {}
            )
        }

        composeTestRule.waitForIdle()
        // If content is set and idle without exception, the aspect ratio logic in the parent Box is valid.
        composeTestRule.onNodeWithTag("pause_button_overlay").assertIsDisplayed()
    }
}

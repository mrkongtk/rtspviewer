package com.mrkongtk.rtspviewer.ui.compose

import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(UnstableApi::class)
class RTSPVideoPlayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockExoPlayer: ExoPlayer = mock()

    @Before
    fun setup() {
        // Prevent NullPointerExceptions in PlayerView internals
        whenever(mockExoPlayer.applicationLooper).thenReturn(Looper.getMainLooper())
        whenever(mockExoPlayer.videoSize).thenReturn(VideoSize.UNKNOWN)
        whenever(mockExoPlayer.currentPosition).thenReturn(0L)
        whenever(mockExoPlayer.duration).thenReturn(0L)
        whenever(mockExoPlayer.isPlaying).thenReturn(false)
    }

    // =========================================================================
    // VISUAL STATE TESTS
    // =========================================================================

    @Test
    fun testInitialization_whenPlayerIsNull_displaysInitializingText() {
        // Scenario: ViewModel hasn't created the player yet
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = null, // Null player
                playbackState = RTSPVideoPlayerPlaybackState.Idle,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        composeTestRule.onNodeWithTag("empty_player_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("RTSP Video Player Initializing...").assertIsDisplayed()

        // Ensure controls are not shown prematurely
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    @Test
    fun testBufferingState_displaysLoadingOverlay() {
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Buffering,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        composeTestRule.onNodeWithTag("loading_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    @Test
    fun testErrorState_displaysErrorOverlay_andHidesPlayer() {
        val errorMsg = "Connection Refused"

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Playing, // Even if state says playing
                errorMessage = errorMsg, // Error is present
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        // 1. Error overlay must be visible
        composeTestRule.onNodeWithTag("error_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()

        // 2. Critical logic check:
        // If an error exists, the player and its interaction overlays should not be rendered.
        composeTestRule.onNodeWithTag("pause_button_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loading_overlay").assertDoesNotExist()
    }

    @Test
    fun testEndedState_displaysNoOverlays() {
        // According to the `when(playbackState)` block, `Ended` falls into `else -> {}`
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Ended,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("pause_button_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loading_overlay").assertDoesNotExist()
    }

    // =========================================================================
    // INTERACTION TESTS
    // =========================================================================

    @Test
    fun testReadyState_displaysPlayButton_andClickTriggersCallback() {
        var playClicked = false

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Ready,
                errorMessage = null,
                onPlayClick = { playClicked = true },
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        composeTestRule.onNodeWithTag("play_button_overlay")
            .assertIsDisplayed()
            .performClick()

        assert(playClicked) { "Expected onPlayClick to be invoked" }
    }

    @Test
    fun testPlayingState_displaysPauseOverlay_andClickTriggersCallback() {
        var pauseClicked = false

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Playing,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = { pauseClicked = true },
                onImageAvailable = {}
            )
        }

        // Play button should be gone
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()

        // Pause overlay (invisible click handler) should exist
        composeTestRule.onNodeWithTag("pause_button_overlay")
            .assertExists()
            .performClick()

        assert(pauseClicked) { "Expected onPauseClick to be invoked" }
    }

    // =========================================================================
    // LAYOUT LOGIC TESTS
    // =========================================================================

    @Test
    fun testAspectRatio_calculatesHeightCorrectly() {
        // In the updated code, the aspect ratio is applied via a modifier.
        // We test that the content respects the aspectRatio modifier applied to it.

        val testRatio = 2.0f // 2:1 ratio

        composeTestRule.setContent {
            Box(Modifier.size(width = 200.dp, height = 500.dp)) {
                RTSPVideoPlayerContent(
                    modifier = Modifier.aspectRatio(testRatio), // Mimics the RTSPVideoPlayer wrapper logic
                    player = mockExoPlayer,
                    playbackState = RTSPVideoPlayerPlaybackState.Idle,
                    errorMessage = null,
                    onPlayClick = {},
                    onPauseClick = {},
                    onImageAvailable = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // We use the play button overlay to measure dimensions as it fills the container.
        val bounds = composeTestRule.onNodeWithTag("play_button_overlay").getUnclippedBoundsInRoot()

        // Width is constrained to 200.dp by the parent
        assert(bounds.width == 200.dp)

        // Height should be Width / Ratio (200 / 2.0 = 100)
        assert(bounds.height == 100.dp) {
            "Expected height 100.dp for 2.0 aspect ratio, but got ${bounds.height}"
        }
    }

    @Test
    fun testAspectRatio_verticalVideo() {
        val testRatio = 0.5f // 1:2 ratio

        composeTestRule.setContent {
            Box(Modifier.size(width = 100.dp, height = 500.dp)) {
                RTSPVideoPlayerContent(
                    modifier = Modifier.aspectRatio(testRatio),
                    player = mockExoPlayer,
                    playbackState = RTSPVideoPlayerPlaybackState.Idle,
                    errorMessage = null,
                    onPlayClick = {},
                    onPauseClick = {},
                    onImageAvailable = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        val bounds = composeTestRule.onNodeWithTag("play_button_overlay").getUnclippedBoundsInRoot()

        assert(bounds.width == 100.dp)
        // Height should be Width / Ratio (100 / 0.5 = 200)
        assert(bounds.height == 200.dp) {
            "Expected height 200.dp for 0.5 aspect ratio, but got ${bounds.height}"
        }
    }
}

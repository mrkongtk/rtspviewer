package com.mrkongtk.rtspviewer.ui.compose

import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
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
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {}
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
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {}
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
                videoAspectRatio = 1.77f,
                errorMessage = errorMsg, // Error is present
                onPlayClick = {},
                onPauseClick = {}
            )
        }

        // 1. Error overlay must be visible
        composeTestRule.onNodeWithTag("error_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()

        // 2. Critical: The Logic `errorMessage?.let { ... } ?: player` implies
        // that if an error exists, the player (and its overlays) are NOT rendered.
        // We verify the play/pause overlays are absent.
        composeTestRule.onNodeWithTag("pause_button_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("loading_overlay").assertDoesNotExist()
    }

    @Test
    fun testEndedState_displaysNoOverlays() {
        // According to the `when(playbackState)` block in the source code,
        // `Ended` falls into `else -> {}`, meaning no specific overlay is drawn.

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockExoPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Ended,
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {}
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
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = { playClicked = true },
                onPauseClick = {}
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
                videoAspectRatio = 1.77f,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = { pauseClicked = true }
            )
        }

        // Play button should be gone
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()

        // Pause overlay (invisible click handler) should exist
        composeTestRule.onNodeWithTag("pause_button_overlay")
            .assertExists() // Might not be "Displayed" visually if transparent/empty, but exists in tree
            .performClick()

        assert(pauseClicked) { "Expected onPauseClick to be invoked" }
    }

    // =========================================================================
    // LAYOUT LOGIC TESTS
    // =========================================================================

    @Test
    fun testAspectRatio_calculatesHeightCorrectly() {
        // To test aspect ratio, we wrap the component in a Box with a fixed width.
        // We then assert that the height adjusts based on the ratio provided.

        // Setup: Container Width = 200dp.
        // Case A: Ratio 2.0 (2:1) -> Expected Height = 100dp

        composeTestRule.setContent {
            Box(Modifier.size(200.dp)) {
                RTSPVideoPlayerContent(
                    player = mockExoPlayer,
                    playbackState = RTSPVideoPlayerPlaybackState.Idle,
                    videoAspectRatio = 2.0f, // Width / Height = 2.0
                    errorMessage = null,
                    onPlayClick = {},
                    onPauseClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Find the player content (we use the play button as a proxy for the content box size
        // since the root Box in content doesn't have a specific tag, but overlays fill max size)
        val bounds = composeTestRule.onNodeWithTag("play_button_overlay").getUnclippedBoundsInRoot()

        // Assert Width is 200.dp
        assert(bounds.width == 200.dp)

        // Assert Height is 100.dp (200 / 2.0)
        // Note: Floating point rendering might have slight sub-pixel variance,
        // but Compose tests usually handle dp rounding well.
        assert(bounds.height == 100.dp) {
            "Expected height 100.dp for 2.0 aspect ratio, but got ${bounds.height}"
        }
    }

    @Test
    fun testAspectRatio_verticalVideo() {
        // Setup: Container Width = 100dp.
        // Case B: Ratio 0.5 (9:16 approx) -> Expected Height = 200dp

        composeTestRule.setContent {
            // We give the parent enough height constraint to grow
            Box(Modifier.size(width = 100.dp, height = 300.dp)) {
                RTSPVideoPlayerContent(
                    player = mockExoPlayer,
                    playbackState = RTSPVideoPlayerPlaybackState.Idle,
                    videoAspectRatio = 0.5f,
                    errorMessage = null,
                    onPlayClick = {},
                    onPauseClick = {}
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag("play_button_overlay").getUnclippedBoundsInRoot()

        assert(bounds.width == 100.dp)
        assert(bounds.height == 200.dp) {
            "Expected height 200.dp for 0.5 aspect ratio, but got ${bounds.height}"
        }
    }
}

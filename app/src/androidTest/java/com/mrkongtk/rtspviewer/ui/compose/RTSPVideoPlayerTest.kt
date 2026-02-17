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
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * UI Tests for the stateless [RTSPVideoPlayerContent] component.
 *
 * These tests verify that the UI correctly reacts to different playback states,
 * error conditions, and layout constraints independently of the ViewModel logic.
 */
@OptIn(UnstableApi::class)
class RTSPVideoPlayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mocking the Player interface to avoid heavy ExoPlayer initialization
    private val mockPlayer: Player = mock()

    @Before
    fun setup() {
        // Stub basic Player methods to prevent PlayerView internal crashes
        whenever(mockPlayer.applicationLooper).thenReturn(Looper.getMainLooper())
        whenever(mockPlayer.videoSize).thenReturn(VideoSize.UNKNOWN)
        whenever(mockPlayer.isPlaying).thenReturn(false)
    }

    // =========================================================================
    // OVERLAY & STATE TESTS
    // =========================================================================

    @Test
    fun initialization_whenPlayerIsNull_displaysEmptyOverlay() {
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = null,
                playbackState = RTSPVideoPlayerPlaybackState.Idle,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        composeTestRule.onNodeWithTag("empty_player_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("RTSP Video Player Initializing...").assertIsDisplayed()

        // Ensure interactive overlays are hidden
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    @Test
    fun bufferingState_displaysLoadingOverlay() {
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Buffering,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        composeTestRule.onNodeWithTag("loading_overlay").assertIsDisplayed()
        // Ensure other overlays are not visible
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("error_overlay").assertDoesNotExist()
    }

    @Test
    fun errorState_displaysErrorOverlay_andOverridesPlaybackOverlays() {
        val testError = "404 Not Found"

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Buffering, // Should be ignored if error exists
                errorMessage = testError,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        // Error message must take precedence
        composeTestRule.onNodeWithTag("error_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText(testError).assertIsDisplayed()

        // Loading or Play buttons must not show during an error
        composeTestRule.onNodeWithTag("loading_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    @Test
    fun readyState_displaysPlayButton() {
        var playInvoked = false

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Ready,
                errorMessage = null,
                onPlayClick = { playInvoked = true },
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        composeTestRule.onNodeWithTag("play_button_overlay")
            .assertIsDisplayed()
            .performClick()

        assertTrue("Callback onPlayClick should be triggered", playInvoked)
    }

    @Test
    fun playingState_displaysPauseOverlay() {
        var pauseInvoked = false

        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Playing,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = { pauseInvoked = true },
                onImageAvailable = {}
            )
        }

        // In 'Playing' state, we show a transparent click interceptor (PauseButtonOverlay)
        composeTestRule.onNodeWithTag("pause_button_overlay")
            .assertExists()
            .performClick()

        assertTrue("Callback onPauseClick should be triggered", pauseInvoked)
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    // =========================================================================
    // LAYOUT & MODIFIER TESTS
    // =========================================================================

    @Test
    fun aspectRatio_horizontalRatio_calculatesHeightCorrectly() {
        val horizontalRatio = 2.0f // 2:1 (Width:Height)
        val containerWidth = 200.dp

        composeTestRule.setContent {
            // Wrap in a Box to provide fixed width constraints
            Box(Modifier.size(width = containerWidth, height = 1000.dp)) {
                RTSPVideoPlayerContent(
                    modifier = Modifier.aspectRatio(horizontalRatio),
                    player = mockPlayer,
                    playbackState = RTSPVideoPlayerPlaybackState.Idle,
                    errorMessage = null,
                    onPlayClick = {},
                    onPauseClick = {},
                    onImageAvailable = {}
                )
            }
        }

        // Measure the resulting bounds of the overlay which fills the player content
        val bounds = composeTestRule.onNodeWithTag("play_button_overlay").getUnclippedBoundsInRoot()

        // Assert: Height = Width / Ratio => 200 / 2 = 100
        assertTrue("Width should match container", bounds.width == containerWidth)
        assertTrue("Height should be 100dp for 2.0 ratio, but ${bounds.height}", bounds.height == 100.dp)
    }

    @Test
    fun aspectRatio_verticalRatio_calculatesHeightCorrectly() {
        val verticalRatio = 0.5f // 1:2 (Width:Height)
        val containerWidth = 100.dp

        composeTestRule.setContent {
            Box(Modifier.size(width = containerWidth, height = 1000.dp)) {
                RTSPVideoPlayerContent(
                    modifier = Modifier.aspectRatio(verticalRatio),
                    player = mockPlayer,
                    playbackState = RTSPVideoPlayerPlaybackState.Idle,
                    errorMessage = null,
                    onPlayClick = {},
                    onPauseClick = {},
                    onImageAvailable = {}
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag("play_button_overlay").getUnclippedBoundsInRoot()

        // Assert: Height = Width / Ratio => 100 / 0.5 = 200
        assertTrue("Width should match container", bounds.width == containerWidth)
        assertTrue("Height should be 200dp for 0.5 ratio", bounds.height == 200.dp)
    }

    @Test
    fun endedState_hidesAllOverlays() {
        composeTestRule.setContent {
            RTSPVideoPlayerContent(
                player = mockPlayer,
                playbackState = RTSPVideoPlayerPlaybackState.Ended,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        // Per the 'when' block in code, Ended state does not show Loading or Play buttons
        composeTestRule.onNodeWithTag("loading_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("pause_button_overlay").assertDoesNotExist()
    }
}

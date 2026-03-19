package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI Tests for the stateless [RTSPVideoPlayerContent] component.
 *
 * These tests verify that the UI correctly reacts to different playback states,
 * error conditions, and layout constraints independently of the ViewModel logic.
 *
 * This common test replaces the platform-specific Android instrumentation test.
 */
@OptIn(ExperimentalTestApi::class)
class RTSPVideoPlayerTest {

    /**
     * A fake implementation of [RTSPVideoPlayer] for testing UI state changes.
     */
    private class FakeRTSPVideoPlayer(
        private val underlyingPlayer: Any? = null
    ) : RTSPVideoPlayer {
        override val currentState = MutableStateFlow(RTSPVideoPlayerPlaybackState.Idle)
        override val videoAspectRatio = MutableStateFlow(16f / 9f)
        override val error = MutableStateFlow<Throwable?>(null)
        override fun prepare(uri: String, forceTcp: Boolean) {}
        override fun play() {}
        override fun stop() {}
        override fun release() {}

        @Suppress("UNCHECKED_CAST")
        override fun <T> getPlayer(): T? = underlyingPlayer as? T
    }

    // =========================================================================
    // OVERLAY & STATE TESTS
    // =========================================================================

    @Test
    fun initialization_whenUnderlyingPlayerIsNull_displaysEmptyOverlay() = runComposeUiTest {
        val fakePlayer = FakeRTSPVideoPlayer(underlyingPlayer = null)
        setContent {
            RTSPVideoPlayerContent(
                player = fakePlayer,
                videoAspectRatio = 16f / 9f,
                playbackState = RTSPVideoPlayerPlaybackState.Idle,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        onNodeWithTag("empty_player_overlay").assertIsDisplayed()
        onNodeWithText("RTSP Video Player Initializing...").assertIsDisplayed()

        // Ensure interactive overlays are hidden
        onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    @Test
    fun bufferingState_displaysLoadingOverlay() = runComposeUiTest {
        val fakePlayer =
            FakeRTSPVideoPlayer(underlyingPlayer = Any()) // Non-null to avoid initialization overlay
        setContent {
            RTSPVideoPlayerContent(
                player = fakePlayer,
                videoAspectRatio = 16f / 9f,
                playbackState = RTSPVideoPlayerPlaybackState.Buffering,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        onNodeWithTag("loading_overlay").assertIsDisplayed()
        // Ensure other overlays are not visible
        onNodeWithTag("play_button_overlay").assertDoesNotExist()
        onNodeWithTag("error_overlay").assertDoesNotExist()
    }

    @Test
    fun errorState_displaysErrorOverlay_andOverridesPlaybackOverlays() = runComposeUiTest {
        val testError = "404 Not Found"
        val fakePlayer = FakeRTSPVideoPlayer(underlyingPlayer = Any())

        setContent {
            RTSPVideoPlayerContent(
                player = fakePlayer,
                videoAspectRatio = 16f / 9f,
                playbackState = RTSPVideoPlayerPlaybackState.Buffering, // Should be ignored if error exists
                errorMessage = testError,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        // Error message must take precedence
        onNodeWithTag("error_overlay").assertIsDisplayed()
        onNodeWithText(testError).assertIsDisplayed()

        // Loading or Play buttons must not show during an error
        onNodeWithTag("loading_overlay").assertDoesNotExist()
        onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    @Test
    fun readyState_displaysPlayButton() = runComposeUiTest {
        var playInvoked = false
        val fakePlayer = FakeRTSPVideoPlayer(underlyingPlayer = Any())

        setContent {
            RTSPVideoPlayerContent(
                player = fakePlayer,
                videoAspectRatio = 16f / 9f,
                playbackState = RTSPVideoPlayerPlaybackState.Ready,
                errorMessage = null,
                onPlayClick = { playInvoked = true },
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        onNodeWithTag("play_button_overlay")
            .assertIsDisplayed()
            .performClick()

        assertTrue(playInvoked, "Callback onPlayClick should be triggered")
    }

    @Test
    fun playingState_displaysPauseOverlay() = runComposeUiTest {
        var pauseInvoked = false
        val fakePlayer = FakeRTSPVideoPlayer(underlyingPlayer = Any())

        setContent {
            RTSPVideoPlayerContent(
                player = fakePlayer,
                videoAspectRatio = 16f / 9f,
                playbackState = RTSPVideoPlayerPlaybackState.Playing,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = { pauseInvoked = true },
                onImageAvailable = {}
            )
        }

        // In 'Playing' state, we show a transparent click interceptor (PauseButtonOverlay)
        onNodeWithTag("pause_button_overlay")
            .assertExists()
            .performClick()

        assertTrue(pauseInvoked, "Callback onPauseClick should be triggered")
        onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    // =========================================================================
    // LAYOUT & MODIFIER TESTS
    // =========================================================================

    @Test
    fun aspectRatio_horizontalRatio_calculatesHeightCorrectly() = runComposeUiTest {
        val ratio = 2.0f // 2:1 (Width:Height)
        val containerWidth = 200.dp
        val fakePlayer = FakeRTSPVideoPlayer(underlyingPlayer = Any())

        setContent {
            // Wrap in a Box to provide fixed width constraints
            Box(Modifier.size(width = containerWidth, height = 1000.dp)) {
                RTSPVideoPlayerContent(
                    player = fakePlayer,
                    videoAspectRatio = ratio,
                    playbackState = RTSPVideoPlayerPlaybackState.Idle,
                    errorMessage = null,
                    onPlayClick = {},
                    onPauseClick = {},
                    onImageAvailable = {}
                )
            }
        }

        // Measure the resulting bounds of the overlay which fills the player content
        val bounds = onNodeWithTag("play_button_overlay").getUnclippedBoundsInRoot()

        // Assert: Height = Width / Ratio => 200 / 2 = 100
        assertEquals(bounds.width, containerWidth, "Width should match container")
        assertEquals(
            bounds.height,
            100.dp,
            "Height should be 100dp for 2.0 ratio, but ${bounds.height}"
        )
    }

    @Test
    fun endedState_hidesAllOverlays() = runComposeUiTest {
        val fakePlayer = FakeRTSPVideoPlayer(underlyingPlayer = Any())

        setContent {
            RTSPVideoPlayerContent(
                player = fakePlayer,
                videoAspectRatio = 16f / 9f,
                playbackState = RTSPVideoPlayerPlaybackState.Ended,
                errorMessage = null,
                onPlayClick = {},
                onPauseClick = {},
                onImageAvailable = {}
            )
        }

        // Per the 'when' block in code, Ended state does not show Loading or Play buttons
        onNodeWithTag("loading_overlay").assertDoesNotExist()
        onNodeWithTag("play_button_overlay").assertDoesNotExist()
        onNodeWithTag("pause_button_overlay").assertDoesNotExist()
    }
}

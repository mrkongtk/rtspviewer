package com.mrkongtk.rtspviewer.ui.compose

import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.viewmodel.RTSPVideoPlayerViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Integration tests for the [RTSPVideoPlayer] Composable.
 *
 * This test class verifies the interaction between the Compose UI, the [RTSPVideoPlayerViewModel],
 * and the underlying [ExoPlayer].
 *
 * Strategies used:
 * 1. **Mocking ExoPlayer**: We mock the heavy media player to avoid real network/codec usage.
 * 2. **Real ViewModel**: We use the actual ViewModel implementation to ensure the state logic
 *    correctly drives the UI changes.
 * 3. **ComposeTestRule**: Used to set content and interact with UI nodes.
 */
class RTSPVideoPlayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // 1. Mock ExoPlayer using Mockito
    // We mock the player interface because we are testing the UI wrapper, not ExoPlayer itself.
    private val mockExoPlayer: ExoPlayer = mock()

    // 2. Real ViewModel
    // We use the real VM to test the integration between View and ViewModel logic.
    // This ensures that when the UI calls a function, the VM updates the state, and the UI reacts.
    private val viewModel = RTSPVideoPlayerViewModel(
        initialUri = "rtsp://127.0.0.1/test",
        initialForceTcp = true
    )

    /**
     * Setup method to configure default behaviors for the Mock ExoPlayer.
     * These stubs are necessary because the Composable or ViewModel might access these properties
     * immediately upon initialization, and returning null would cause a crash.
     */
    @Before
    fun setup() {
        // Prevent crashes when accessing playback parameters
        whenever(mockExoPlayer.playbackParameters).thenReturn(PlaybackParameters.DEFAULT)
        // Prevent crashes when checking the Looper (often checked by ExoPlayer internal assertions)
        whenever(mockExoPlayer.applicationLooper).thenReturn(Looper.getMainLooper())
    }

    /**
     * Verifies that when the Composable enters the composition:
     * 1. It sets the MediaSource on the player.
     * 2. It prepares the player.
     * 3. It attaches a Player.Listener.
     */
    @Test
    fun testInitialization_createsPlayerAndPrepares() {
        // Arrange
        whenever(mockExoPlayer.isPlaying).thenReturn(false)

        // Act
        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert
        // Verify the MediaSource was set (verifies the LaunchedEffect ran)
        verify(mockExoPlayer).setMediaSource(any<MediaSource>())
        // Verify prepare was called
        verify(mockExoPlayer).prepare()
        // Verify the listener was attached
        verify(mockExoPlayer, atLeastOnce()).addListener(any())
    }

    /**
     * Verifies that when the ViewModel reports a [RTSPVideoPlayerPlaybackState.Buffering] state,
     * the UI correctly displays the Loading Overlay (CircularProgressIndicator).
     */
    @Test
    fun testBufferingState_displaysLoadingOverlay() {
        // Arrange: Manually force the VM into Buffering state
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Buffering)

        // Act
        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert
        // Uses the testTag "loading_overlay" defined in the LoadingOverlay composable
        composeTestRule.onNodeWithTag("loading_overlay").assertIsDisplayed()
    }

    /**
     * Verifies that when the ViewModel contains an error:
     * 1. The Error Overlay is displayed.
     * 2. The specific error message text is visible to the user.
     */
    @Test
    fun testErrorState_displaysErrorOverlayWithText() {
        // Arrange
        val errorMessage = "Connection Refused"
        val exception = PlaybackException(
            errorMessage,
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )

        // Update VM to Error State
        viewModel.updatePlaybackError(exception)

        // Act
        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert
        // Check overlay exists via tag
        composeTestRule.onNodeWithTag("error_overlay").assertIsDisplayed()
        // Check specific text exists on screen
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    /**
     * Verifies the "Play" workflow:
     * 1. State is Ready -> Play button is visible.
     * 2. User Clicks Play -> ExoPlayer is instructed to play.
     */
    @Test
    fun testReadyState_displaysPlayButton_andClickStartsPlayback() {
        // Arrange
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Ready)

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert: Button is visible
        val playButton = composeTestRule.onNodeWithTag("play_button_overlay")
        playButton.assertIsDisplayed()

        // Act: Click play
        playButton.performClick()

        // Assert: The 'onPlayClick' lambda in UI calls these methods on the player
        verify(mockExoPlayer).playWhenReady = true
        verify(mockExoPlayer, atLeastOnce()).prepare()
    }

    /**
     * Verifies the "Pause/Stop" workflow:
     * 1. State is Playing -> Pause overlay (clickable area) is present.
     * 2. User Clicks Overlay -> ExoPlayer is instructed to stop.
     */
    @Test
    fun testPlayingState_displaysPauseOverlay_andClickStopsPlayback() {
        // Arrange
        // We must ensure the VM thinks it's playing so the UI renders the PauseButtonOverlay
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Playing)
        whenever(mockExoPlayer.isPlaying).thenReturn(true)

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Act
        // 1. Verify Play button is GONE (Should not show play button while playing)
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()

        // 2. Find Pause Overlay (which is often invisible/transparent but clickable)
        val pauseOverlay = composeTestRule.onNodeWithTag("pause_button_overlay")

        // 3. Click it
        pauseOverlay.performClick()

        // Assert
        // The 'onPauseClick' lambda in UI calls exoPlayer.stop()
        verify(mockExoPlayer).stop()
    }

    /**
     * Critical Integration Test:
     * Verifies the feedback loop: ExoPlayer Event -> Listener -> ViewModel -> State Update.
     *
     * We capture the `Player.Listener` that the composable adds to the mock player,
     * then we manually invoke callback methods on that listener to simulate player events.
     */
    @Test
    fun testPlayerListener_eventsUpdateViewModelState() {
        val listenerCaptor = argumentCaptor<Player.Listener>()

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // 1. Capture the listener created inside the Composable using Mockito's argumentCaptor
        verify(mockExoPlayer, atLeastOnce()).addListener(listenerCaptor.capture())
        val capturedListener = listenerCaptor.firstValue

        // 2. Simulate ExoPlayer State Change -> BUFFERING
        capturedListener.onPlaybackStateChanged(Player.STATE_BUFFERING)
        // Assert VM state updated automatically
        assert(viewModel.state.value.playback == RTSPVideoPlayerPlaybackState.Buffering)

        // 3. Simulate ExoPlayer State Change -> READY
        capturedListener.onPlaybackStateChanged(Player.STATE_READY)
        assert(viewModel.state.value.playback == RTSPVideoPlayerPlaybackState.Ready)

        // 4. Simulate IsPlaying -> True (Playing)
        capturedListener.onIsPlayingChanged(true)
        assert(viewModel.state.value.playback == RTSPVideoPlayerPlaybackState.Playing)
    }

    /**
     * Verifies lifecycle cleanup.
     * When the Composable is removed from the UI tree (Disposed), it must release the ExoPlayer
     * to prevent memory leaks and keep system resources free.
     */
    @Test
    fun testOnDispose_releasesPlayer() {
        // Control whether the Composable is present in the hierarchy
        val showPlayer = mutableStateOf(true)

        composeTestRule.setContent {
            if (showPlayer.value) {
                RTSPVideoPlayer(
                    viewModel = viewModel,
                    playerFactory = { mockExoPlayer }
                )
            }
        }

        // Pre-assertion: Verify release hasn't been called yet while UI is visible
        verify(mockExoPlayer, never()).release()

        // Act: Remove Composable from composition
        showPlayer.value = false
        composeTestRule.waitForIdle() // Wait for recomposition/cleanup

        // Assert: Cleanup occurred in DisposableEffect
        verify(mockExoPlayer).removeListener(any())
        verify(mockExoPlayer).release()
    }

    /**
     * Verifies that the `onVideoSizeChanged` listener logic executes.
     * While we cannot easily assert the visual aspect ratio change in a unit test,
     * this ensures the listener is hooked up and processes the event without crashing.
     */
    @Test
    fun testVideoSizeChange_calculatesAspectRatio() {
        val listenerCaptor = argumentCaptor<Player.Listener>()

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        verify(mockExoPlayer, atLeastOnce()).addListener(listenerCaptor.capture())
        val listener = listenerCaptor.firstValue

        // Act: Simulate video size change event from the player
        listener.onVideoSizeChanged(VideoSize(1920, 1080))

        // If the code didn't crash, the calculation in onVideoSizeChanged worked.
        composeTestRule.waitForIdle()
    }
}

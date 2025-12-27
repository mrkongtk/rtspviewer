package com.mrkongtk.rtspviewer.ui.compose

import android.os.Looper
import androidx.annotation.OptIn
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.mrkongtk.rtspviewer.data.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.viewmodel.RTSPVideoPlayerViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Instrumentation tests for the [RTSPVideoPlayer] Composable.
 *
 * **Testing Strategy:**
 * - **Integration Level:** This tests the integration between the Compose UI and the [RTSPVideoPlayerViewModel].
 * - **Mocking:** The [ExoPlayer] is mocked using Mockito. This is crucial because we do not want to
 *   initialize a real heavy-weight video player or make actual network connections during UI tests.
 * - **State Verification:** We manipulate the ViewModel state to verify the UI reacts correctly (e.g., showing loading spinners).
 * - **Interaction Verification:** We verify that UI clicks result in specific method calls on the mocked Player.
 *
 * NOTE: This must run in `androidTest` as it requires an Android environment (Looper, Context, etc.).
 */
@OptIn(UnstableApi::class) // Required for Media3/ExoPlayer APIs
class RTSPVideoPlayerTest {

    /**
     * Standard Compose JUnit rule used to set content, find nodes, and perform actions.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock the ExoPlayer interface.
    // This allows us to verify calls like player.prepare() or player.release() without running real media logic.
    private val mockExoPlayer: ExoPlayer = mock()

    // We use a real instance of the ViewModel to test the actual state-flow logic between the VM and the UI.
    private lateinit var viewModel: RTSPVideoPlayerViewModel

    @Before
    fun setup() {
        // Initialize ViewModel with test arguments
        viewModel = RTSPVideoPlayerViewModel(
            initialUri = "rtsp://192.168.1.1/stream",
            initialForceTcp = true
        )

        // -- Common Stubbing for ExoPlayer --
        // When the PlayerView is attached to the window, it immediately queries the player for
        // properties like looping, duration, and position.
        // If these return null (the default for mocks), the test will crash with NullPointerException.
        whenever(mockExoPlayer.applicationLooper).thenReturn(Looper.getMainLooper())
        whenever(mockExoPlayer.playbackParameters).thenReturn(PlaybackParameters.DEFAULT)
        whenever(mockExoPlayer.videoSize).thenReturn(VideoSize.UNKNOWN)
        whenever(mockExoPlayer.currentPosition).thenReturn(0L)
        whenever(mockExoPlayer.duration).thenReturn(0L)
        whenever(mockExoPlayer.isPlaying).thenReturn(false)
    }

    /**
     * Verifies that when the Composable first launches:
     * 1. A MediaSource is created and assigned to the player.
     * 2. The player is prepared.
     * 3. A listener is attached to update the ViewModel.
     */
    @Test
    fun testInitialization_createsPlayer_setsMediaSource_andPrepares() {
        // Act: Render the composable
        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer } // Inject the mock
            )
        }

        composeTestRule.waitForIdle()

        // Assert: Verify ExoPlayer methods were called
        // 'verify' checks if the specific method was called on the mock object.
        verify(mockExoPlayer).setMediaSource(any<MediaSource>())
        verify(mockExoPlayer).prepare()
        verify(mockExoPlayer, atLeastOnce()).addListener(any())
    }

    /**
     * Verifies visual state: When the ViewModel is in [Buffering], the UI should show the LoadingOverlay.
     */
    @Test
    fun testBufferingState_displaysLoadingOverlay() {
        // Arrange: Manually force the ViewModel into the Buffering state
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Buffering)

        // Act
        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert: Look for the Composable with testTag "loading_overlay"
        composeTestRule.onNodeWithTag("loading_overlay").assertIsDisplayed()

        // Ensure invalid states (like the play button) are NOT visible
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()
    }

    /**
     * Verifies visual state: When an error occurs, the ErrorOverlay is shown with the correct text.
     */
    @Test
    fun testErrorState_displaysErrorOverlayWithText() {
        // Arrange: Create a fake network exception
        val errorMsg = "RTSP Connection Failed"
        val exception = PlaybackException(
            errorMsg,
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        )
        viewModel.updatePlaybackError(exception)

        // Act
        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert
        composeTestRule.onNodeWithTag("error_overlay").assertIsDisplayed()
        // Verify the specific error text is visible to the user
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    }

    /**
     * Verifies interaction: Clicking the "Play" button triggers `player.playWhenReady = true`.
     */
    @Test
    fun testReadyState_displaysPlayButton_andClickStartsPlayback() {
        // Arrange: Set state to Ready (loaded, but paused/stopped)
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Ready)

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert: Play button exists
        val playButton = composeTestRule.onNodeWithTag("play_button_overlay")
        playButton.assertIsDisplayed()

        // Act: Click it
        playButton.performClick()

        // Assert: Check ExoPlayer mock received the command
        verify(mockExoPlayer).playWhenReady = true
        verify(mockExoPlayer, atLeastOnce()).prepare()
    }

    /**
     * Verifies interaction: Clicking the screen while playing triggers `player.stop()`.
     */
    @Test
    fun testPlayingState_displaysPauseOverlay_andClickStopsPlayback() {
        // Arrange: ViewModel says Playing, and Mock Player says it is playing
        viewModel.updatePlaybackState(RTSPVideoPlayerPlaybackState.Playing)
        whenever(mockExoPlayer.isPlaying).thenReturn(true)

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Assert: Play button should be gone
        composeTestRule.onNodeWithTag("play_button_overlay").assertDoesNotExist()

        // Act: Click the "pause_button_overlay" (often an invisible scrim over the video)
        composeTestRule.onNodeWithTag("pause_button_overlay").performClick()

        // Assert: Verify stop() was called
        verify(mockExoPlayer).stop()
    }

    /**
     * Advanced Test: Verifies the bi-directional communication.
     * We capture the `Player.Listener` that the Composable attaches to the Mock Player,
     * then manually trigger events on that listener to ensure the ViewModel updates.
     */
    @Test
    fun testPlayerListener_eventsUpdateViewModelState() {
        // 'argumentCaptor' is a Mockito tool to capture objects passed to methods
        val listenerCaptor = argumentCaptor<Player.Listener>()

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        // Capture the listener passed to mockExoPlayer.addListener(...)
        verify(mockExoPlayer, atLeastOnce()).addListener(listenerCaptor.capture())
        val capturedListener = listenerCaptor.firstValue

        // --- Test 1: Simulate Buffering Event ---
        // We pretend the player just moved to buffering state
        capturedListener.onPlaybackStateChanged(Player.STATE_BUFFERING)
        // Assert VM state updated automatically
        assertEquals(RTSPVideoPlayerPlaybackState.Buffering, viewModel.state.value.playback)

        // --- Test 2: Simulate Ready Event ---
        capturedListener.onPlaybackStateChanged(Player.STATE_READY)
        assertEquals(RTSPVideoPlayerPlaybackState.Ready, viewModel.state.value.playback)

        // --- Test 3: Simulate Playing Event ---
        capturedListener.onIsPlayingChanged(true)
        assertEquals(RTSPVideoPlayerPlaybackState.Playing, viewModel.state.value.playback)
    }

    /**
     * Verifies that when the video resolution changes (e.g., stream starts),
     * the ViewModel calculates the correct Aspect Ratio.
     */
    @Test
    fun testVideoSizeChange_updatesViewModelAspectRatio() {
        val listenerCaptor = argumentCaptor<Player.Listener>()

        composeTestRule.setContent {
            RTSPVideoPlayer(
                viewModel = viewModel,
                playerFactory = { mockExoPlayer }
            )
        }

        verify(mockExoPlayer, atLeastOnce()).addListener(listenerCaptor.capture())
        val listener = listenerCaptor.firstValue

        // Act: Simulate video size detected (e.g., 100x200 -> Aspect Ratio 0.5)
        val width = 100
        val height = 200
        listener.onVideoSizeChanged(VideoSize(width, height))

        // Assert: Check ViewModel state directly
        val expectedRatio = width.toFloat() / height.toFloat()
        assertEquals(expectedRatio, viewModel.videoAspectRatio.value, 0.01f)
    }

    /**
     * Verifies Lifecycle cleanup.
     * Checks if `player.release()` is called when the Composable is removed from the screen.
     */
    @Test
    fun testOnDispose_releasesPlayer() {
        // 1. Control variable to determine if the Composable is currently in the composition
        val showPlayer = mutableStateOf(true)

        composeTestRule.setContent {
            if (showPlayer.value) {
                RTSPVideoPlayer(
                    viewModel = viewModel,
                    playerFactory = { mockExoPlayer }
                )
            }
        }

        // 2. Setup: Tell the mock it is currently playing.
        // This ensures the DisposableEffect cleanup logic enters the `if (player.isPlaying) { stop() }` block.
        whenever(mockExoPlayer.isPlaying).thenReturn(true)

        // 3. Act: Toggle state to false. This triggers onDispose.
        showPlayer.value = false
        composeTestRule.waitForIdle()

        // 4. Assert: Cleanup methods were called
        verify(mockExoPlayer).removeListener(any()) // Listener removed
        verify(mockExoPlayer).stop() // Playback stopped
        verify(mockExoPlayer).release() // Resources released
    }
}

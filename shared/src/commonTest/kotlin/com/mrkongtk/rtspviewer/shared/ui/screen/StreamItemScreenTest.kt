package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.shared.ui.screen.action.StreamItemScreenActions
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.viewmodel.RTSPVideoPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Multiplatform UI instrumentation tests for [StreamItemScreen].
 */
@OptIn(ExperimentalTestApi::class)
class StreamItemScreenTest {

    private class FakeRTSPVideoPlayer : RTSPVideoPlayer {
        val currentStateFlow = MutableStateFlow(RTSPVideoPlayerPlaybackState.Idle)
        override val currentState: StateFlow<RTSPVideoPlayerPlaybackState> = currentStateFlow
        val videoAspectRatioFlow = MutableStateFlow(1.77f)
        override val videoAspectRatio: StateFlow<Float> = videoAspectRatioFlow
        val errorFlow = MutableStateFlow<Throwable?>(null)
        override val error: StateFlow<Throwable?> = errorFlow

        override fun prepare(uri: String, forceTcp: Boolean) {}
        override fun play() {
            currentStateFlow.value = RTSPVideoPlayerPlaybackState.Playing
        }

        override fun stop() {
            currentStateFlow.value = RTSPVideoPlayerPlaybackState.Idle
        }

        override fun release() {
            currentStateFlow.value = RTSPVideoPlayerPlaybackState.Released
        }
        @Suppress("UNCHECKED_CAST")
        override fun <T> getPlayer(): T? = Any() as? T
    }

    private class MockScreenActions : StreamItemScreenActions {
        var onEditCalled = false
        var onDeleteCalled = false
        var lastCapturedBitmap: ImageBitmap? = null

        override fun onEditItemSelected() { onEditCalled = true }
        override fun onDeleteItemSelected() { onDeleteCalled = true }
        override fun onImageAvailable(item: RTSPItem, bitmap: ImageBitmap) {
            lastCapturedBitmap = bitmap
        }
    }

    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Front Door Cam",
        uri = "rtsp://admin:password123@192.168.1.50:554/live",
        tags = listOf("Outdoor", "Entry"),
        order = 0,
        forceTcp = true
    )

    private lateinit var fakePlayer: FakeRTSPVideoPlayer
    private var capturedViewModel: RTSPVideoPlayerViewModel? = null

    @BeforeTest
    fun setup() {
        fakePlayer = FakeRTSPVideoPlayer()
        capturedViewModel = null
        startKoin {
            modules(module {
                viewModel { (uri: String?, forceTcp: Boolean, onImageAvailable: ((ImageBitmap) -> Unit)?) ->
                    val vm = RTSPVideoPlayerViewModel(fakePlayer, uri, forceTcp, onImageAvailable)
                    capturedViewModel = vm
                    vm
                }
            })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun portraitMode_displaysCorrectMetadataAndElements() = runComposeUiTest {
        val actions = MockScreenActions()
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(width = 360.dp, height = 760.dp),
                        item = sampleItem,
                        screenActions = actions
                    )
                }
            }
        }
        onNodeWithTag("VideoPlayer").assertIsDisplayed()
        onNodeWithTag("Name").assertIsDisplayed().assertTextEquals("Front Door Cam")
        onNodeWithTag("Uri").assertIsDisplayed().assertTextEquals("rtsp://***:***@192.168.1.50:554/live")
        onNodeWithTag("Tag Outdoor").assertIsDisplayed()
        onNodeWithTag("ForceTCP").assertIsOn()
        onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    @Test
    fun landscapeMode_showsImmersivePlayerOnly() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(width = 360.dp, height = 200.dp),
                        item = sampleItem,
                        screenActions = MockScreenActions()
                    )
                }
            }
        }
        onNodeWithTag("VideoPlayerLandscape").assertIsDisplayed()
        onNodeWithTag("Name").assertDoesNotExist()
        onNodeWithTag("MoreButton").assertDoesNotExist()
    }

    @Test
    fun editAction_triggersCallback() = runComposeUiTest {
        val actions = MockScreenActions()
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = sampleItem,
                        screenActions = actions
                    )
                }
            }
        }
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("EditButton").performClick()
        assertTrue(actions.onEditCalled)
    }

    @Test
    fun deleteFlow_confirmsDeletion() = runComposeUiTest {
        val actions = MockScreenActions()
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = sampleItem,
                        screenActions = actions
                    )
                }
            }
        }
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("DeleteButton").performClick()
        onNodeWithTag("DeleteConfirmButton").performClick()
        assertTrue(actions.onDeleteCalled)
    }

    @Test
    fun deleteFlow_cancelsDeletion() = runComposeUiTest {
        val actions = MockScreenActions()
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = sampleItem,
                        screenActions = actions
                    )
                }
            }
        }
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("DeleteButton").performClick()
        onNodeWithTag("DeleteCancelButton").performClick()
        assertFalse(actions.onDeleteCalled)
    }

    @Test
    fun metadata_reflectsForceTcpOff() = runComposeUiTest {
        val tcpOffItem = sampleItem.copy(forceTcp = false)
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = tcpOffItem,
                        screenActions = MockScreenActions()
                    )
                }
            }
        }
        onNodeWithTag("ForceTCP").assertIsOff()
    }

    @Test
    fun noTags_rendersWithoutTagNodes() = runComposeUiTest {
        val itemNoTags = sampleItem.copy(tags = emptyList())
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(width = 360.dp, height = 760.dp),
                        item = itemNoTags,
                        screenActions = MockScreenActions()
                    )
                }
            }
        }
        onNodeWithTag("Name").assertIsDisplayed()
        onNodeWithTag("Tag Outdoor").assertDoesNotExist()
        onNodeWithTag("Tag Entry").assertDoesNotExist()
    }

    @Test
    fun initialMoreOptionTrue_displaysMenuInitially() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(width = 400.dp, height = 800.dp),
                        item = sampleItem,
                        moreOption = true,
                        screenActions = MockScreenActions()
                    )
                }
            }
        }
        onNodeWithTag("EditButton").assertIsDisplayed()
        onNodeWithTag("DeleteButton").assertIsDisplayed()
    }

    @Test
    fun playbackState_buffering_showsLoadingOverlay() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = sampleItem,
                        screenActions = MockScreenActions()
                    )
                }
            }
        }
        fakePlayer.currentStateFlow.value = RTSPVideoPlayerPlaybackState.Buffering
        onNodeWithTag("loading_overlay").assertIsDisplayed()
    }

    @Test
    fun playbackError_showsErrorOverlay() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = sampleItem,
                        screenActions = MockScreenActions()
                    )
                }
            }
        }
        fakePlayer.errorFlow.value = RuntimeException("Connection Failed")
        onNodeWithTag("error_overlay").assertIsDisplayed()
        onNodeWithText("Connection Failed").assertIsDisplayed()
    }

    @Test
    fun playbackState_playing_showsPauseButton() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = sampleItem,
                        screenActions = MockScreenActions()
                    )
                }
            }
        }
        fakePlayer.currentStateFlow.value = RTSPVideoPlayerPlaybackState.Playing
        onNodeWithTag("pause_button_overlay").assertIsDisplayed()
    }

    @Test
    fun onImageAvailable_triggersAction() = runComposeUiTest {
        val actions = MockScreenActions()
        val fakeBitmap = ImageBitmap(1, 1)

        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        modifier = Modifier.size(400.dp, 800.dp),
                        item = sampleItem,
                        screenActions = actions
                    )
                }
            }
        }

        capturedViewModel?.imageAvailable(fakeBitmap)
        assertEquals(fakeBitmap, actions.lastCapturedBitmap)
    }
}

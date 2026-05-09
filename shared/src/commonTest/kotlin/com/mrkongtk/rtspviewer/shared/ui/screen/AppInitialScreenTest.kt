package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.formatText
import com.mrkongtk.rtspviewer.shared.viewmodel.AppBarViewModel
import com.mrkongtk.rtspviewer.shared.viewmodel.AppViewModel
import com.mrkongtk.rtspviewer.shared.viewmodel.RTSPVideoPlayerViewModel
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.app_name
import rtspviewer.shared.generated.resources.back_button
import rtspviewer.shared.generated.resources.screen_add_rtsp_item
import rtspviewer.shared.generated.resources.screen_rtsp_display
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Multiplatform UI tests for [AppInitialScreen].
 *
 * These tests verify the root-level integration of navigation, app bar state,
 * and the primary screen transitions. They use in-memory fakes for data
 * persistence to ensure fast and deterministic execution across platforms.
 */
@OptIn(ExperimentalTestApi::class)
class AppInitialScreenTest {

    private lateinit var appViewModel: AppViewModel
    private lateinit var appBarViewModel: AppBarViewModel
    private lateinit var mockRepo: FakeRTSPItemRepository
    private lateinit var mockFileRepo: FakeFileRepository

    private val testItem = RTSPItem(
        id = 1L,
        name = "Front Door",
        uri = "rtsp://1.1.1.1",
        tags = listOf("Home"),
        order = 0
    )

    @BeforeTest
    fun setup() {
        mockRepo = FakeRTSPItemRepository()
        mockFileRepo = FakeFileRepository()
        // AppViewModel requires both repositories in the latest version
        appViewModel = AppViewModel(mockFileRepo, mockRepo)
        appBarViewModel = AppBarViewModel()

        startKoin {
            modules(module {
                viewModel { (uri: String?, forceTcp: Boolean, onImageAvailable: ((ImageBitmap) -> Unit)?) ->
                    RTSPVideoPlayerViewModel(FakeRTSPVideoPlayer(), uri, forceTcp, onImageAvailable)
                }
            })
        }
    }

    @kotlin.test.AfterTest
    fun tearDown() {
        stopKoin()
    }

    /**
     * Verifies that the app starts on the listing screen and shows the empty state message
     * when no streams are configured.
     */
    @Test
    fun appInitialScreen_startDestination_showsEmptyStateAndCorrectTitle() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    AppInitialScreen(
                        viewModel = appViewModel,
                        appBarViewModel = appBarViewModel
                    )
                }
            }
        }

        // Verify the empty list message is shown
        onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()

        // Verify the Top Bar title matches the app name
        val appName = runBlocking { getString(Res.string.app_name) }
        onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    /**
     * Verifies that clicking the 'Add' action in the dropdown menu navigates the
     * user to the Add Stream screen.
     */
    @Test
    fun appInitialScreen_clickAdd_navigatesToAddScreen() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    AppInitialScreen(
                        viewModel = appViewModel,
                        appBarViewModel = appBarViewModel
                    )
                }
            }
        }

        // 1. Open the Dropdown menu (Add button is hidden inside it)
        onNodeWithTag("MoreButton").performClick()

        // 2. Click the Add button now that it's visible
        onNodeWithTag("AddButton").performClick()

        // 3. Verify we are on the Add screen
        onNodeWithTag("AddStreamItemScreenRoot").assertIsDisplayed()

        // 4. Verify AppBar title updated to "Add stream"
        val addTitle = runBlocking { getString(Res.string.screen_add_rtsp_item) }
        onNodeWithTag("AppBarTitle").assertTextEquals(addTitle)
    }

    /**
     * Verifies that selecting an item from the list navigates to the display screen
     * and updates the AppBar with a dynamic title containing the item's name.
     */
    @Test
    fun appInitialScreen_selectItem_navigatesToDisplayAndShowsDynamicTitle() = runComposeUiTest {
        // 1. Prepare data so the list isn't empty
        mockRepo.items.value = listOf(testItem)

        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    AppInitialScreen(
                        viewModel = appViewModel,
                        appBarViewModel = appBarViewModel
                    )
                }
            }
        }

        // 2. Click on the item card (using the specific test tag with ID)
        onNodeWithTag("StreamListItem: 1").performClick()

        // 3. Verify we reached the detail/display screen
        onNodeWithTag("StreamItemScreenRoot").assertIsDisplayed()

        // 4. Verify Dynamic Title (e.g., "RTSP: Front Door")
        val template = runBlocking { getString(Res.string.screen_rtsp_display) }
        val expectedTitle = template.formatText(testItem.name)
        onNodeWithTag("AppBarTitle").assertTextEquals(expectedTitle)
    }

    /**
     * Verifies that the back button in the AppBar correctly returns the user
     * to the previous screen.
     */
    @Test
    fun appInitialScreen_backButton_returnsToStart() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    AppInitialScreen(
                        viewModel = appViewModel,
                        appBarViewModel = appBarViewModel
                    )
                }
            }
        }

        // 1. Navigate away from home to the Add screen
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").performClick()

        // 2. Click the back arrow in the AppBar
        val backDesc = runBlocking { getString(Res.string.back_button) }
        onNodeWithContentDescription(backDesc).performClick()

        // 3. Verify we are back on the start screen (empty list state)
        onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()

        val appName = runBlocking { getString(Res.string.app_name) }
        onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    /**
     * Verifies that in Portrait mode, the detail screen shows both the video player
     * and the metadata/actions (signaled by the presence of the 'More' button).
     */
    @Test
    fun appInitialScreen_portrait_showsDetailsAndActions() = runComposeUiTest {
        mockRepo.items.value = listOf(testItem)

        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    AppInitialScreen(
                        viewModel = appViewModel,
                        appBarViewModel = appBarViewModel
                    )
                }
            }
        }

        // Navigate to display
        onNodeWithTag("StreamListItem: 1").performClick()

        // In portrait, the 'MoreButton' (action menu) should be visible
        onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    /**
     * Verifies that in Landscape mode, the screen enters a fullscreen video experience,
     * hiding metadata and secondary action buttons.
     */
    @Test
    fun appInitialScreen_landscape_showsFullscreenVideoAndHidesDetails() = runComposeUiTest {
        mockRepo.items.value = listOf(testItem)

        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    // Wrap in a landscape-proportioned Box to trigger the screen's internal 
                    // BoxWithConstraints landscape logic.
                    Box(modifier = Modifier.size(width = 800.dp, height = 400.dp)) {
                        AppInitialScreen(
                            viewModel = appViewModel,
                            appBarViewModel = appBarViewModel
                        )
                    }
                }
            }
        }

        // 3. Navigate to the detail screen
        onNodeWithTag("StreamListItem: 1").performClick()

        // 4. Assertions for Landscape
        // Verify landscape-specific video player is shown
        onNodeWithTag("VideoPlayerLandscape").assertIsDisplayed()

        // Verify portrait-only elements are NOT displayed in landscape
        onNodeWithTag("MoreButton").assertDoesNotExist()
        onNodeWithTag("Name").assertDoesNotExist()
        onNodeWithTag("Uri").assertDoesNotExist()
    }

    // --- Fakes for Multiplatform Testing ---

    /** No-op player fake used to satisfy screen dependencies in tests. */
    private class FakeRTSPVideoPlayer : RTSPVideoPlayer {
        override val currentState: StateFlow<RTSPVideoPlayerPlaybackState> =
            MutableStateFlow(RTSPVideoPlayerPlaybackState.Idle)
        override val videoAspectRatio: StateFlow<Float> = MutableStateFlow(1.77f)
        override val error: StateFlow<Throwable?> = MutableStateFlow(null)
        override fun prepare(uri: String, forceTcp: Boolean) {}
        override fun play() {}
        override fun stop() {}
        override fun release() {}
        override fun <T> getPlayer(): T? = null
    }

    /** In-memory fake for file I/O operations. */
    private class FakeFileRepository : FileRepository {
        override suspend fun readJPEG(file: Path): ImageBitmap? = null
        override suspend fun writeJPEG(file: Path, bitmap: ImageBitmap) = true
        override fun getCacheDir(): Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }

    /** In-memory fake for RTSP item metadata management. */
    private class FakeRTSPItemRepository : RTSPItemRepository {
        override val items = MutableStateFlow<List<RTSPItem>>(emptyList())
        override val cachedPreviews = MutableStateFlow<Map<Long, ImageBitmap>>(emptyMap())

        override suspend fun addItem(item: RTSPItem): Long {
            val id = (items.value.maxOfOrNull { it.id } ?: 0L) + 1L
            val newItem = item.copy(id = id)
            items.update { it + newItem }
            return id
        }

        override suspend fun reorderItems(items: List<RTSPItem>): Int {
            this.items.value = items
            return 1
        }

        override suspend fun updateItem(item: RTSPItem): Int {
            items.update { list ->
                list.map { if (it.id == item.id) item else it }
            }
            return 1
        }

        override suspend fun deleteItem(item: RTSPItem): Int {
            items.update { list ->
                list.filter { it.id != item.id }
            }
            return 1
        }

        override fun previewPathFor(item: RTSPItem): Path {
            return FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "preview_${item.id}.jpg"
        }

        override fun cachePreviewFor(item: RTSPItem, bitmap: ImageBitmap) {
            cachedPreviews.update { it + (item.id to bitmap) }
        }

        override fun removeCachedPreviews() {
            cachedPreviews.value = emptyMap()
        }
    }
}

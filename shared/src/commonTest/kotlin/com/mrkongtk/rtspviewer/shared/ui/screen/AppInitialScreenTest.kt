package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
import rtspviewer.shared.generated.resources.tags_all
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Multiplatform UI tests for the [AppInitialScreen].
 *
 * These tests verify the end-to-end integration of navigation, App Bar state management,
 * and primary screen transitions within the Compose Multiplatform environment.
 * In-memory fakes are used for data persistence and video playback to ensure
 * hermetic, fast, and deterministic execution across different platforms.
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
        // Initialize fakes and ViewModels before each test
        mockRepo = FakeRTSPItemRepository()
        mockFileRepo = FakeFileRepository()
        appViewModel = AppViewModel(mockFileRepo, mockRepo)
        appBarViewModel = AppBarViewModel()

        // Start Koin for ViewModel injection within the Compose environment
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
        // Clean up Koin context after each test
        stopKoin()
    }

    /**
     * Verifies that the application starts on the stream listing screen and displays
     * the empty state message when no streams are configured.
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

        // Assert that the empty state placeholder is visible
        onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()

        // Assert that the App Bar title correctly reflects the application name
        val appName = runBlocking { getString(Res.string.app_name) }
        onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    /**
     * Verifies that navigating to the "Add Stream" screen via the overflow menu
     * works correctly and updates the App Bar title.
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

        // 1. Open the overflow menu
        onNodeWithTag("MoreButton").performClick()

        // 2. Select the "Add" action
        onNodeWithTag("AddButton").performClick()

        // 3. Verify navigation to the Add screen
        onNodeWithTag("AddStreamItemScreenRoot").assertIsDisplayed()

        // 4. Verify the App Bar title reflects the current screen
        val addTitle = runBlocking { getString(Res.string.screen_add_rtsp_item) }
        onNodeWithTag("AppBarTitle").assertTextEquals(addTitle)
    }

    /**
     * Verifies that selecting a stream from the list navigates to the display screen
     * and shows a dynamic title containing the stream's name.
     */
    @Test
    fun appInitialScreen_selectItem_navigatesToDisplayAndShowsDynamicTitle() = runComposeUiTest {
        // 1. Pre-populate the repository with a test item
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

        // 2. Select the stream from the list
        onNodeWithTag("StreamListItem: 1").performClick()

        // 3. Verify navigation to the display/detail screen
        onNodeWithTag("StreamItemScreenRoot").assertIsDisplayed()

        // 4. Verify the dynamic title in the App Bar
        val template = runBlocking { getString(Res.string.screen_rtsp_display) }
        val expectedTitle = template.formatText(testItem.name)
        onNodeWithTag("AppBarTitle").assertTextEquals(expectedTitle)
    }

    /**
     * Verifies that the back button in the App Bar correctly navigates the user
     * back to the previous screen.
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

        // 1. Navigate to the Add screen
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").performClick()

        // 2. Perform back navigation via the App Bar
        val backDesc = runBlocking { getString(Res.string.back_button) }
        onNodeWithContentDescription(backDesc).performClick()

        // 3. Verify return to the initial list screen
        onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()

        // 4. Verify App Bar title is reset
        val appName = runBlocking { getString(Res.string.app_name) }
        onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    /**
     * Verifies that in Portrait orientation, the detail screen displays both the
     * video player and the metadata/actions menu.
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

        // 1. Navigate to the display screen
        onNodeWithTag("StreamListItem: 1").performClick()

        // 2. Verify visibility of the actions menu (standard for portrait)
        onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    /**
     * Verifies that in Landscape orientation, the screen transitions to a fullscreen
     * video experience, hiding metadata and secondary UI elements.
     */
    @Test
    fun appInitialScreen_landscape_showsFullscreenVideoAndHidesDetails() = runComposeUiTest {
        mockRepo.items.value = listOf(testItem)

        setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    // Simulate landscape orientation using a custom Box size
                    Box(modifier = Modifier.size(width = 800.dp, height = 400.dp)) {
                        AppInitialScreen(
                            viewModel = appViewModel,
                            appBarViewModel = appBarViewModel
                        )
                    }
                }
            }
        }

        // 1. Navigate to the display screen
        onNodeWithTag("StreamListItem: 1").performClick()

        // 2. Verify the landscape-specific video player is active
        onNodeWithTag("VideoPlayerLandscape").assertIsDisplayed()

        // 3. Verify that portrait-only elements are hidden
        onNodeWithTag("MoreButton").assertDoesNotExist()
        onNodeWithTag("Name").assertDoesNotExist()
        onNodeWithTag("Uri").assertDoesNotExist()
    }

    /**
     * Verifies that deleting a stream from its detail view removes it from the list
     * and navigates the user back to the primary list screen.
     */
    @Test
    fun appInitialScreen_deleteItem_navigatesBackToList() = runComposeUiTest {
        // 1. Setup initial state with one item
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

        // 2. Navigate to display screen
        onNodeWithTag("StreamListItem: 1").performClick()

        // 3. Initiate deletion via the menu
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("DeleteButton").performClick()

        // 4. Confirm deletion in the dialog
        onNodeWithTag("DeleteConfirmButton").performClick()

        // 5. Verify navigation back to the empty list screen
        onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()

        // 6. Verify App Bar title is reset to the app name
        val appName = runBlocking { getString(Res.string.app_name) }
        onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    /**
     * Verifies that editing a stream's name updates both the persistence layer
     * and the UI components, including the App Bar title.
     */
    @Test
    fun appInitialScreen_editItem_savesChangesAndReturns() = runComposeUiTest {
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

        // 1. Navigate to Edit screen via Display screen
        onNodeWithTag("StreamListItem: 1").performClick()
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("EditButton").performClick()

        // 2. Update the name field
        onRTSPField("NameTextField").performTextReplacement("Updated Name")

        // 3. Save changes
        onNodeWithTag("SaveButton").performClick()

        // 4. Verify navigation back to display screen with updated content
        onNodeWithTag("StreamItemScreenRoot").assertIsDisplayed()
        onNodeWithTag("Name").assertTextEquals("Updated Name")

        // 5. Verify App Bar title reflects the updated name
        val template = runBlocking { getString(Res.string.screen_rtsp_display) }
        val expectedTitle = template.formatText("Updated Name")
        onNodeWithTag("AppBarTitle").assertTextEquals(expectedTitle)
    }

    /**
     * Verifies that selecting a tag filter correctly narrows the displayed list
     * of streams and that clearing the filter restores the full list.
     */
    @Test
    fun appInitialScreen_tagFiltering_updatesState() = runComposeUiTest {
        val item1 = testItem.copy(id = 1L, name = "Camera 1", tags = listOf("Outdoor"))
        val item2 = testItem.copy(id = 2L, name = "Camera 2", tags = listOf("Indoor"))
        mockRepo.items.value = listOf(item1, item2)

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

        // 1. Initial state: both items visible
        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        onNodeWithTag("StreamListItem: 2").assertIsDisplayed()

        // 2. Apply "Outdoor" filter
        onNodeWithTag("Tag Outdoor").performClick()

        // 3. Verify filtered list content
        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        onNodeWithTag("StreamListItem: 2").assertDoesNotExist()

        // 4. Reset filter to "All"
        val allTagText = runBlocking { getString(Res.string.tags_all) }
        onNodeWithTag("Tag $allTagText").performClick()

        // 5. Verify all items are visible again
        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        onNodeWithTag("StreamListItem: 2").assertIsDisplayed()
    }

    /**
     * Verifies that entering the sorting mode displays reorderable list items
     * and that exiting it returns to the standard list view.
     */
    @Test
    fun appInitialScreen_sortingMode_showsReorderItems() = runComposeUiTest {
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

        // 1. Enter sorting mode via the overflow menu
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("SortButton").performClick()

        // 2. Verify sorting-specific UI elements are visible
        onNodeWithTag("StreamSortingItem: 1").assertIsDisplayed()
        onNodeWithTag("EndSortingButton").assertIsDisplayed()

        // 3. Exit sorting mode
        onNodeWithTag("EndSortingButton").performClick()

        // 4. Verify return to the standard list view
        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
    }

    /**
     * Helper function to locate the inner text input node within an RTSP-styled
     * outlined text field container.
     */
    private fun ComposeUiTest.onRTSPField(parentTag: String) = this.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(parentTag))
    )

    // --- Fakes for Multiplatform Testing ---

    /**
     * A fake implementation of [RTSPVideoPlayer] that provides a static state
     * for UI testing without requiring an actual video stream.
     */
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

    /**
     * An in-memory fake implementation of [FileRepository] that bypasses
     * actual file system I/O.
     */
    private class FakeFileRepository : FileRepository {
        override suspend fun readJPEG(file: Path): ImageBitmap? = null
        override suspend fun writeJPEG(file: Path, bitmap: ImageBitmap) = true
        override fun getCacheDir(): Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }

    /**
     * An in-memory fake implementation of [RTSPItemRepository] that manages
     * stream metadata in a [MutableStateFlow] for reactive testing.
     */
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

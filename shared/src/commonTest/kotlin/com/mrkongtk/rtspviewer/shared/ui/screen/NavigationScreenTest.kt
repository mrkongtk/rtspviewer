package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.shared.ui.navigation.AppScreen
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.shared.ui.screen.action.NavigationScreenActions
import com.mrkongtk.rtspviewer.shared.ui.state.AppUiState
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.viewmodel.AppViewModel
import com.mrkongtk.rtspviewer.shared.viewmodel.RTSPVideoPlayerViewModel
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
import rtspviewer.shared.generated.resources.tags_all
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
/** UI integration tests for [NavigationScreen] with in-memory fakes. */
class NavigationScreenTest {

    private lateinit var repository: FakeRTSPItemRepository
    private lateinit var fileRepository: FakeFileRepository
    private lateinit var viewModel: AppViewModel

    private var capturedOnImageAvailable: ((ImageBitmap) -> Unit)? = null
    private var lastHeaderVisibility: Boolean? = null

    @BeforeTest
    fun setup() {
        repository = FakeRTSPItemRepository()
        fileRepository = FakeFileRepository()
        viewModel = AppViewModel(fileRepository, repository)
        capturedOnImageAvailable = null
        lastHeaderVisibility = null

        startKoin {
            modules(module {
                viewModel { (uri: String?, forceTcp: Boolean, onImageAvailable: ((ImageBitmap) -> Unit)?) ->
                    capturedOnImageAvailable = onImageAvailable
                    RTSPVideoPlayerViewModel(FakeRTSPVideoPlayer(), uri, forceTcp, onImageAvailable)
                }
            })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun ComposeUiTest.launchScreen(onNavController: (NavHostController) -> Unit = {}) {
        var capturedNavController: NavHostController? = null
        setContent {
            val navController = rememberNavController()
            capturedNavController = navController
            RTSPViewerTheme {
                val uiState by viewModel.uiState.collectAsState(AppUiState())

                val action = object : NavigationScreenActions {
                    override fun onListingItemSelected(item: RTSPItem) {
                        viewModel.select(item)
                    }

                    override fun onAddItemRequested(newItem: RTSPItem) {
                        viewModel.addItem(newItem)
                    }

                    override fun onItemsReordered(newOrderedList: List<RTSPItem>) {
                        viewModel.reorderItems(newOrderedList)
                    }

                    override fun onEditItemRequested(updatedItem: RTSPItem) {
                        viewModel.editItem(updatedItem)
                    }

                    override fun onDeleteItemRequested(deleteItem: RTSPItem) {
                        viewModel.deleteItem(deleteItem)
                    }

                    override fun onImageAvailable(
                        item: RTSPItem,
                        snapshot: ImageBitmap
                    ) {
                        viewModel.savePreview(item, snapshot)
                    }

                    override fun onTagSelected(tag: String?) {
                        viewModel.select(tag)
                    }

                    override fun onHeaderVisibilityChange(isVisible: Boolean) {
                        lastHeaderVisibility = isVisible
                    }
                }

                NavigationScreen(
                    navController = navController,
                    uiState = uiState,
                    screenActions = action,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        waitForIdle()
        capturedNavController?.let(onNavController)
    }

    @Test
    fun emptyState_showsNoStreamingMessage() = runComposeUiTest {
        repository.items.value = emptyList()
        launchScreen { }

        onNodeWithTag("StreamListScreenEmptyText")
            .assertIsDisplayed()
    }

    @Test
    fun populatedList_showsItemsAndTags() = runComposeUiTest {
        val item1 = RTSPItem(1, "Cam One", "rtsp://1", listOf("LivingRoom"), 0)
        repository.items.value = listOf(item1)

        launchScreen { }

        onNodeWithText("Cam One").assertIsDisplayed()
        onNodeWithTag("Tag LivingRoom").assertIsDisplayed()
    }

    @Test
    fun tagFiltering_filtersListCorrectly() = runComposeUiTest {
        val allLabel = runBlocking { getString(Res.string.tags_all) }

        val item1 = RTSPItem(1, "Kitchen", "rtsp://1", listOf("Indoor"), 0)
        val item2 = RTSPItem(2, "Gate", "rtsp://2", listOf("Outdoor"), 1)
        repository.items.value = listOf(item1, item2)

        launchScreen { }

        onNodeWithText("Kitchen").assertIsDisplayed()
        onNodeWithText("Gate").assertIsDisplayed()

        onNodeWithTag("Tag Outdoor").performClick()

        onNodeWithText("Gate").assertIsDisplayed()
        onNodeWithText("Kitchen").assertDoesNotExist()

        onNodeWithTag("Tag $allLabel").performClick()

        onNodeWithText("Kitchen").assertIsDisplayed()
        onNodeWithText("Gate").assertIsDisplayed()
    }

    @Test
    fun clickAddButton_navigatesToAddScreen() = runComposeUiTest {
        repository.items.value = emptyList()
        var navController: NavHostController? = null
        launchScreen { navController = it }

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").performClick()

        waitForIdle()
        assertEquals(
            AppScreen.AddRTSPItem.name,
            navController?.currentBackStackEntry?.destination?.route
        )
        onNodeWithTag("AddStreamItemScreenRoot").assertIsDisplayed()
    }

    @Test
    fun addItemFlow_verifiesRepositoryCall() = runComposeUiTest {
        repository.items.value = emptyList()
        var navController: NavHostController? = null
        launchScreen { navController = it }

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").performClick()

        onRTSPField("NameTextField").performTextInput("Front Door")
        onRTSPField("UriTextField").performTextInput("rtsp://admin:admin@192.168.1.50")
        onRTSPField("TagsTextField").performTextInput("Home,Security")

        onNodeWithTag("ForceTCPCheckbox").performClick()

        onNodeWithTag("SaveButton").performClick()

        val addedItem = repository.items.value.firstOrNull { it.name == "Front Door" }
        assertEquals("Front Door", addedItem?.name)
        assertEquals("rtsp://admin:admin@192.168.1.50", addedItem?.uri)
        assertEquals(listOf("Home", "Security"), addedItem?.tags)
        assertEquals(true, addedItem?.forceTcp)

        waitForIdle()
        assertEquals(AppScreen.Start.name, navController?.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun clickItem_navigatesToDisplayScreen() = runComposeUiTest {
        val item1 = RTSPItem(1, "Cam One", "rtsp://1", emptyList(), 0)
        repository.items.value = listOf(item1)
        var navController: NavHostController? = null
        launchScreen { navController = it }

        onNodeWithTag("StreamListItem: 1").performClick()

        waitForIdle()
        assertEquals(
            AppScreen.RTSPDisplay.name,
            navController?.currentBackStackEntry?.destination?.route
        )
        onNodeWithTag("StreamItemScreenRoot").assertIsDisplayed()
        onNodeWithText("Cam One").assertIsDisplayed()
    }

    @Test
    fun deleteItem_navigatesBackToList() = runComposeUiTest {
        val item1 = RTSPItem(1, "Delete Me", "rtsp://1", emptyList(), 0)
        repository.items.value = listOf(item1)
        var navController: NavHostController? = null
        launchScreen { navController = it }

        onNodeWithTag("StreamListItem: 1").performClick()

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("DeleteButton").performClick()

        onNodeWithTag("DeleteConfirmButton").performClick()

        waitForIdle()
        assertEquals(
            AppScreen.Start.name,
            navController?.currentBackStackEntry?.destination?.route
        )
        assertEquals(0, repository.items.value.size)
    }

    @Test
    fun deleteItem_cancel_staysOnDisplayScreen() = runComposeUiTest {
        val item1 = RTSPItem(1, "Don't Delete Me", "rtsp://1", emptyList(), 0)
        repository.items.value = listOf(item1)
        var navController: NavHostController? = null
        launchScreen { navController = it }

        onNodeWithTag("StreamListItem: 1").performClick()
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("DeleteButton").performClick()

        onNodeWithTag("DeleteCancelButton").performClick()

        waitForIdle()
        assertEquals(
            AppScreen.RTSPDisplay.name,
            navController?.currentBackStackEntry?.destination?.route
        )
        assertEquals(1, repository.items.value.size)
    }

    @Test
    fun editItemFlow_verifiesRepositoryCall() = runComposeUiTest {
        val item1 = RTSPItem(1, "Old Name", "rtsp://old", emptyList(), 0)
        repository.items.value = listOf(item1)
        var navController: NavHostController? = null
        launchScreen { navController = it }

        onNodeWithTag("StreamListItem: 1").performClick()

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("EditButton").performClick()

        onRTSPField("NameTextField").performTextReplacement("New Name")

        onNodeWithTag("SaveButton").performClick()

        waitForIdle()
        assertEquals(
            AppScreen.RTSPDisplay.name,
            navController?.currentBackStackEntry?.destination?.route
        )

        val updatedItem = repository.items.value.first { it.id == 1L }
        assertEquals("New Name", updatedItem.name)

        onNodeWithText("New Name").assertIsDisplayed()
    }

    @Test
    fun displayScreen_imageAvailable_persistsPreview() = runComposeUiTest {
        val item1 = RTSPItem(1, "Cam", "rtsp://1", emptyList(), 0)
        repository.items.value = listOf(item1)
        launchScreen { }

        onNodeWithTag("StreamListItem: 1").performClick()
        waitForIdle()

        val bitmap = ImageBitmap(10, 10)
        capturedOnImageAvailable?.invoke(bitmap)

        waitForIdle()
        assertEquals(bitmap, repository.cachedPreviews.value[1L])
    }

    @Test
    fun navigation_updatesHeaderVisibility() = runComposeUiTest {
        val item1 = RTSPItem(1, "Cam", "rtsp://1", emptyList(), 0)
        repository.items.value = listOf(item1)
        launchScreen { }

        onNodeWithTag("StreamListItem: 1").performClick()
        waitForIdle()

        assertEquals(true, lastHeaderVisibility)
    }

    @Test
    fun addItem_invalidData_saveDisabled() = runComposeUiTest {
        repository.items.value = emptyList()
        launchScreen { }

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").performClick()

        onNodeWithTag("SaveButton").assertIsNotEnabled()

        onRTSPField("NameTextField").performTextInput("Name")
        onNodeWithTag("SaveButton").assertIsNotEnabled()

        onRTSPField("UriTextField").performTextInput("invalid-uri")
        onNodeWithTag("SaveButton").assertIsNotEnabled()

        onRTSPField("UriTextField").performTextReplacement("rtsp://valid")
        onNodeWithTag("SaveButton").assertIsEnabled()
    }

    @Test
    fun reorderItems_verifiesModeSwitch() = runComposeUiTest {
        val item1 = RTSPItem(1, "One", "rtsp://1", emptyList(), 0)
        repository.items.value = listOf(item1)
        launchScreen { }

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("SortButton").performClick()

        onNodeWithTag("DraggableLazyColumn").assertIsDisplayed()
        onNodeWithTag("EndSortingButton").assertIsDisplayed()

        onNodeWithTag("EndSortingButton").performClick()

        onNodeWithTag("LazyColumn").assertIsDisplayed()
    }

    /** Returns the inner text input node under the tagged RTSP field container. */
    private fun ComposeUiTest.onRTSPField(parentTag: String) = onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(parentTag))
    )

    /** In-memory fake that mimics repository state updates for UI tests. */

    private class FakeRTSPItemRepository : RTSPItemRepository {
        override val items = MutableStateFlow<List<RTSPItem>>(emptyList())
        override val cachedPreviews = MutableStateFlow<Map<Long, ImageBitmap>>(emptyMap())

        override suspend fun addItem(item: RTSPItem): Long {
            val id = (items.value.maxOfOrNull { it.id } ?: 0L) + 1L
            val newItem = item.copy(id = id)
            items.update { it + newItem }
            return id
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

        override suspend fun reorderItems(items: List<RTSPItem>): Int {
            this.items.value = items
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

    /** Minimal fake for file I/O interactions used by the view model. */
    private class FakeFileRepository : FileRepository {
        override suspend fun writeJPEG(file: Path, bitmap: ImageBitmap): Boolean = true
        override suspend fun readJPEG(file: Path): ImageBitmap? = null
        override fun getCacheDir(): Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }

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
}

package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.shared.ui.navigation.AppScreen
import com.mrkongtk.rtspviewer.shared.ui.screen.action.NavigationScreenActions
import com.mrkongtk.rtspviewer.shared.ui.state.AppUiState
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.getString
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.tags_all
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NavigationScreenTest {

    private lateinit var repository: FakeRTSPItemRepository
    private lateinit var fileRepository: FakeFileRepository
    private lateinit var viewModel: AppViewModel

    @BeforeTest
    fun setup() {
        repository = FakeRTSPItemRepository()
        fileRepository = FakeFileRepository()
        viewModel = AppViewModel(fileRepository, repository)
    }

    private fun ComposeUiTest.launchScreen(onNavController: (NavHostController) -> Unit) {
        setContent {
            val navController = rememberNavController()
            onNavController(navController)
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
                }

                NavigationScreen(
                    navController = navController,
                    uiState = uiState,
                    screenActions = action,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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

        // Verify the stream name is displayed within the list item
        onNodeWithText("Cam One").assertIsDisplayed()

        // Verify the tag chip is displayed
        onNodeWithTag("Tag LivingRoom").assertIsDisplayed()
    }

    @Test
    fun tagFiltering_filtersListCorrectly() = runComposeUiTest {
        val allLabel = runBlocking { getString(Res.string.tags_all) }

        val item1 = RTSPItem(1, "Kitchen", "rtsp://1", listOf("Indoor"), 0)
        val item2 = RTSPItem(2, "Gate", "rtsp://2", listOf("Outdoor"), 1)
        repository.items.value = listOf(item1, item2)

        launchScreen { }

        // Initially both are visible
        onNodeWithText("Kitchen").assertIsDisplayed()
        onNodeWithText("Gate").assertIsDisplayed()

        // Click the "Outdoor" tag chip
        onNodeWithTag("Tag Outdoor").performClick()

        // Kitchen should disappear, Gate remains
        onNodeWithText("Gate").assertIsDisplayed()
        onNodeWithText("Kitchen").assertDoesNotExist()

        // Click the "All" chip to reset the filter
        onNodeWithTag("Tag $allLabel").performClick()

        onNodeWithText("Kitchen").assertIsDisplayed()
        onNodeWithText("Gate").assertIsDisplayed()
    }

    @Test
    fun clickAddButton_navigatesToAddScreen() = runComposeUiTest {
        repository.items.value = emptyList()
        var navController: NavHostController? = null
        launchScreen { navController = it }

        // First, click the More button to show the menu
        onNodeWithTag("MoreButton").performClick()

        // Now the AddButton should be visible
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

        // Open Menu and Navigate to Add Screen
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").performClick()

        // Fill out the form
        onRTSPField("NameTextField").performTextInput("Front Door")
        onRTSPField("UriTextField").performTextInput("rtsp://admin:admin@192.168.1.50")
        onRTSPField("TagsTextField").performTextInput("Home,Security")

        // Toggle the Force TCP checkbox
        onNodeWithTag("ForceTCPCheckbox").performClick()

        // Click Save
        onNodeWithTag("SaveButton").performClick()

        // Verify the repository state
        val addedItem = repository.items.value.firstOrNull { it.name == "Front Door" }
        assertEquals("Front Door", addedItem?.name)
        assertEquals("rtsp://admin:admin@192.168.1.50", addedItem?.uri)
        assertEquals(listOf("Home", "Security"), addedItem?.tags)
        assertEquals(true, addedItem?.forceTcp)

        // Verify navigation back to Start
        waitForIdle()
        assertEquals(AppScreen.Start.name, navController?.currentBackStackEntry?.destination?.route)
    }

    /**
     * Helper to find the actual input field inside the custom RTSPTextField component.
     */
    private fun ComposeUiTest.onRTSPField(parentTag: String) = onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(parentTag))
    )

    // --- Fakes ---

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

    private class FakeFileRepository : FileRepository {
        override suspend fun writeJPEG(file: Path, bitmap: ImageBitmap): Boolean = true
        override suspend fun readJPEG(file: Path): ImageBitmap? = null
        override fun getCacheDir(): Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
    }
}

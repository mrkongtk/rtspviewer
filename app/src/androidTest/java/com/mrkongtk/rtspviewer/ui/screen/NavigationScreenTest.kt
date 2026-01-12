package com.mrkongtk.rtspviewer.ui.screen

import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.FileRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.viewmodel.AppBarViewModel
import com.mrkongtk.rtspviewer.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Instrumented UI Test for the Navigation Screen.
 *
 * This test verifies the integration between the Navigation, the ViewModels,
 * and the UI components using mocked repositories.
 */
@RunWith(AndroidJUnit4::class)
class NavigationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository: RTSPItemRepository = mock()
    private val fileRepository: FileRepository = mock()

    private lateinit var viewModel: AppViewModel
    private lateinit var appBarViewModel: AppBarViewModel
    private lateinit var navController: TestNavHostController

    // State flows to simulate database/cache changes
    private val itemsFlow = MutableStateFlow<List<RTSPItem>>(emptyList())
    private val cachedPreviewsFlow = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    @Before
    fun setup() {
        // Setup repository mocks to return our local state flows
        whenever(repository.items).thenReturn(itemsFlow)
        whenever(repository.cachedPreviews).thenReturn(cachedPreviewsFlow)

        runBlocking {
            // Note: repository.loadData() was removed as the VM now observes flows directly
            whenever(repository.addItem(any())).thenReturn(1L)
            whenever(repository.updateItem(any())).thenReturn(1)
            whenever(repository.previewPathFor(any())).thenReturn(File("mock_path"))
            whenever(fileRepository.readJPEG(any())).thenReturn(null)
        }

        whenever(repository.removeCachedPreviews()).thenAnswer { }

        // Initialize ViewModels with mocked dependencies
        viewModel = AppViewModel(repository, fileRepository)
        appBarViewModel = AppBarViewModel()
    }

    private fun launchScreen() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            // We pass the ViewModels explicitly to bypass Hilt injection in tests
            AppInitialScreen(
                navController = navController,
                viewModel = viewModel,
                appBarViewModel = appBarViewModel
            )
        }
    }

    @Test
    fun emptyState_showsNoStreamingMessage() {
        itemsFlow.value = emptyList()
        launchScreen()

        composeTestRule
            .onNodeWithTag("StreamListScreenEmptyText")
            .assertIsDisplayed()
    }

    @Test
    fun populatedList_showsItemsAndTags() {
        val item1 = RTSPItem(1, "Cam One", "rtsp://1", listOf("LivingRoom"), 0)
        itemsFlow.value = listOf(item1)

        launchScreen()

        // Verify the stream name is displayed
        composeTestRule.onNodeWithText("Cam One").assertIsDisplayed()

        // Verify the tag chip is displayed
        composeTestRule.onNodeWithTag("Tag LivingRoom").assertIsDisplayed()
    }

    @Test
    fun tagFiltering_filtersListCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val allLabel = context.getString(R.string.tags_all)

        val item1 = RTSPItem(1, "Kitchen", "rtsp://1", listOf("Indoor"), 0)
        val item2 = RTSPItem(2, "Gate", "rtsp://2", listOf("Outdoor"), 1)
        itemsFlow.value = listOf(item1, item2)

        launchScreen()

        // Initially both are visible in the LazyColumn
        composeTestRule.onNodeWithText("Kitchen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gate").assertIsDisplayed()

        // Click the "Outdoor" tag chip
        composeTestRule.onNodeWithTag("Tag Outdoor").performClick()

        // Kitchen should disappear (filtered out), Gate remains
        composeTestRule.onNodeWithText("Gate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kitchen").assertDoesNotExist()

        // Click the "All" chip to reset the filter
        composeTestRule.onNodeWithTag("Tag $allLabel").performClick()

        composeTestRule.onNodeWithText("Kitchen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gate").assertIsDisplayed()
    }

    @Test
    fun clickAddButton_navigatesToAddScreen() {
        itemsFlow.value = emptyList()
        launchScreen()

        composeTestRule.onNodeWithTag("AddButton").performClick()

        composeTestRule.waitForIdle()
        assertEquals(
            AppScreen.AddRTSPItem.name,
            navController.currentBackStackEntry?.destination?.route
        )
        composeTestRule.onNodeWithTag("AddStreamItemScreenRoot").assertIsDisplayed()
    }

    @Test
    fun addItemFlow_verifiesRepositoryCall() {
        itemsFlow.value = emptyList()
        launchScreen()

        // Navigate to the form
        composeTestRule.onNodeWithTag("AddButton").performClick()

        // Interact with custom RTSPTextFields using the parent helper
        onRTSPField("NameTextField").performTextInput("Front Door")
        onRTSPField("UriTextField").performTextInput("rtsp://admin:admin@192.168.1.50")
        onRTSPField("TagsTextField").performTextInput("Home,Security")

        // Toggle the Force TCP checkbox
        composeTestRule.onNodeWithTag("ForceTCPCheckbox").performClick()

        // Click Save
        composeTestRule.onNodeWithTag("SaveButton").performClick()

        // Verify the repository received the correctly mapped data
        runBlocking {
            verify(repository).addItem(org.mockito.kotlin.check { item ->
                assertEquals("Front Door", item.name)
                assertEquals("rtsp://admin:admin@192.168.1.50", item.uri)
                // FieldsValue parses "Home,Security" into a list
                assertEquals(listOf("Home", "Security"), item.tags)
                assertEquals(true, item.forceTcp)
            })
        }

        // Verify we popped back to the start screen
        composeTestRule.waitForIdle()
        assertEquals(AppScreen.Start.name, navController.currentBackStackEntry?.destination?.route)
    }

    /**
     * Helper to find the actual input field inside the custom RTSPTextField component.
     *
     * Because RTSPTextField wraps the OutlinedTextField in a Column, we look for
     * the tag "RTSPOutlinedTextField" that is a descendant of the specific field's row tag.
     */
    private fun onRTSPField(parentTag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(parentTag))
    )
}

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
 * Updated to reflect:
 * 1. Tag filtering logic in StreamListScreen.
 * 2. RTSPTextField nested tag structure.
 * 3. AppViewModel dependency on FileRepository.
 */
@RunWith(AndroidJUnit4::class)
class NavigationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository: RTSPItemRepository = mock()
    private val fileRepository: FileRepository = mock()
    private lateinit var viewModel: AppViewModel
    private lateinit var navController: TestNavHostController

    private val itemsFlow = MutableStateFlow<List<RTSPItem>>(emptyList())
    private val cachedPreviewsFlow = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    @Before
    fun setup() {
        whenever(repository.items).thenReturn(itemsFlow)
        whenever(repository.cachedPreviews).thenReturn(cachedPreviewsFlow)

        runBlocking {
            whenever(repository.loadData()).thenAnswer { }
            whenever(repository.addItem(any())).thenReturn(1L)
            whenever(repository.updateItem(any())).thenReturn(1)
            whenever(repository.previewPathFor(any())).thenReturn(File("mock_path"))
            whenever(fileRepository.readJPEG(any())).thenReturn(null)
        }

        whenever(repository.removeCachedPreviews()).thenAnswer { }

        viewModel = AppViewModel(repository, fileRepository)
    }

    private fun launchScreen() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavigationScreen(
                navController = navController,
                viewModel = viewModel
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

        // Verify Item is displayed
        composeTestRule.onNodeWithText("Cam One").assertIsDisplayed()

        // Verify Tag filtering chip is displayed
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

        // Initially both are visible
        composeTestRule.onNodeWithText("Kitchen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gate").assertIsDisplayed()

        // Click "Outdoor" tag
        composeTestRule.onNodeWithTag("Tag Outdoor").performClick()

        // Kitchen should disappear, Gate remains
        composeTestRule.onNodeWithText("Gate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kitchen").assertDoesNotExist()

        // Click "All" to reset
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

        composeTestRule.onNodeWithTag("AddButton").performClick()

        // Use the helper to interact with custom RTSPTextFields
        onRTSPField("NameTextField").performTextInput("Front Door")
        onRTSPField("UriTextField").performTextInput("rtsp://admin:admin@192.168.1.50")
        onRTSPField("TagsTextField").performTextInput("Home,Security")

        // Force TCP toggle (Testing the Checkbox in EditStreamItemScreen)
        composeTestRule.onNodeWithTag("ForceTCPCheckbox").performClick()

        composeTestRule.onNodeWithTag("SaveButton").performClick()

        runBlocking {
            verify(repository).addItem(org.mockito.kotlin.check { item ->
                assertEquals("Front Door", item.name)
                assertEquals("rtsp://admin:admin@192.168.1.50", item.uri)
                assertEquals(listOf("Home", "Security"), item.tags)
                assertEquals(true, item.forceTcp)
            })
        }

        composeTestRule.waitForIdle()
        assertEquals(AppScreen.Start.name, navController.currentBackStackEntry?.destination?.route)
    }

    /**
     * Helper to find the actual input field inside the RTSPTextField component.
     * It looks for the OutlinedTextField (tagged in RTSPTextField.kotlin)
     * that is a child of the specific form row tag.
     */
    private fun onRTSPField(parentTag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(parentTag))
    )
}

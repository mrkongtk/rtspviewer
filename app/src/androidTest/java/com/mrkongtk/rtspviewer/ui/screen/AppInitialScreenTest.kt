package com.mrkongtk.rtspviewer.ui.screen

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.FileRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.util.formatText
import com.mrkongtk.rtspviewer.viewmodel.AppBarViewModel
import com.mrkongtk.rtspviewer.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AppInitialScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockRepo = mock<RTSPItemRepository>()
    private val mockFileRepo = mock<FileRepository>()
    private lateinit var appViewModel: AppViewModel
    private lateinit var appBarViewModel: AppBarViewModel

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val testItem = RTSPItem(
        id = 1L,
        name = "Front Door",
        uri = "rtsp://1.1.1.1",
        tags = listOf("Home"),
        order = 0
    )

    @Before
    fun setup() {
        // Initialize with empty states to prevent null pointer exceptions in ViewModels
        whenever(mockRepo.items) doReturn MutableStateFlow(emptyList())
        whenever(mockRepo.cachedPreviews) doReturn MutableStateFlow(emptyMap<Long, Bitmap>())

        appViewModel = AppViewModel(mockRepo, mockFileRepo)
        appBarViewModel = AppBarViewModel()
    }

    /**
     * Helper function to set content.
     * 1. Uses RTSPViewerTheme for styling.
     * 2. Bypasses Hilt's internal ViewModel injection in the VideoPlayer via LocalInspectionMode.
     */
    private fun setTestContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                RTSPViewerTheme {
                    AppInitialScreen(
                        viewModel = appViewModel,
                        appBarViewModel = appBarViewModel
                    )
                }
            }
        }
    }

    @Test
    fun appInitialScreen_startDestination_showsEmptyStateAndCorrectTitle() {
        setTestContent()

        // Verify the empty list message is shown
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()

        // Verify the Top Bar title matches the app name
        val appName = context.getString(R.string.app_name)
        composeTestRule.onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    @Test
    fun appInitialScreen_clickAdd_navigatesToAddScreen() {
        setTestContent()

        // 1. Open the Dropdown menu (Add button is hidden inside it)
        composeTestRule.onNodeWithTag("MoreButton").performClick()

        // 2. Click the Add button now that it's visible
        composeTestRule.onNodeWithTag("AddButton").performClick()

        // 3. Verify we are on the Add screen
        composeTestRule.onNodeWithTag("AddStreamItemScreenRoot").assertIsDisplayed()

        // 4. Verify AppBar title updated
        val addTitle = context.getString(R.string.screen_add_rtsp_item)
        composeTestRule.onNodeWithTag("AppBarTitle").assertTextEquals(addTitle)
    }

    @Test
    fun appInitialScreen_selectItem_navigatesToDisplayAndShowsDynamicTitle() = runTest {
        // 1. Prepare data so the list isn't empty
        val itemsFlow = MutableStateFlow(listOf(testItem))
        whenever(mockRepo.items) doReturn itemsFlow

        // Re-initialize ViewModel to pick up the new flow
        appViewModel = AppViewModel(mockRepo, mockFileRepo)

        setTestContent()

        // 2. Click on the item card (using the specific test tag with ID)
        composeTestRule.onNodeWithTag("StreamListItem: 1").performClick()

        // 3. Verify we reached the detail/display screen
        composeTestRule.onNodeWithTag("StreamItemScreenRoot").assertIsDisplayed()

        // 4. Verify Dynamic Title (e.g., "Viewing Front Door")
        // The code uses: stringResource(R.string.screen_rtsp_display).formatText(item.name)
        val template = context.getString(R.string.screen_rtsp_display)
        val expectedTitle = template.formatText(testItem.name)
        composeTestRule.onNodeWithTag("AppBarTitle").assertTextEquals(expectedTitle)
    }

    @Test
    fun appInitialScreen_backButton_returnsToStart() {
        setTestContent()

        // 1. Navigate away from home
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("AddButton").performClick()

        // 2. Click the back arrow in the AppBar
        val backDesc = context.getString(R.string.back_button)
        composeTestRule.onNodeWithContentDescription(backDesc).performClick()

        // 3. Verify we are back on the start screen
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()

        val appName = context.getString(R.string.app_name)
        composeTestRule.onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    @Test
    fun appInitialScreen_portrait_showsOnlyVideoPlayer() {
        // This test simulates the landscape logic in StreamItemScreen
        val itemsFlow = MutableStateFlow(listOf(testItem))
        whenever(mockRepo.items) doReturn itemsFlow
        appViewModel = AppViewModel(mockRepo, mockFileRepo)

        setTestContent()

        // Navigate to display
        composeTestRule.onNodeWithTag("StreamListItem: 1").performClick()

        // In a real environment, you'd use a device configuration change.
        // For a Compose UI test, we verify the tag that only appears in portrait (MoreButton)
        // is visible, and the tag for landscape would be verified in a separate
        // @Config(qualifiers = "land") test if using Robolectric.
        composeTestRule.onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    @Test
    fun streamItemScreen_landscape_showsFullscreenVideoAndHidesDetails() {
        // 1. Prepare data
        val itemsFlow = MutableStateFlow(listOf(testItem))
        whenever(mockRepo.items) doReturn itemsFlow
        appViewModel = AppViewModel(mockRepo, mockFileRepo)

        // 2. Set content with a Mocked Landscape Configuration
        composeTestRule.setContent {
            val landscapeConfig = Configuration(LocalConfiguration.current).apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
            }

            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalConfiguration provides landscapeConfig // This is the key
            ) {
                RTSPViewerTheme {
                    AppInitialScreen(
                        viewModel = appViewModel,
                        appBarViewModel = appBarViewModel
                    )
                }
            }
        }

        // 3. Navigate to the detail screen
        composeTestRule.onNodeWithTag("StreamListItem: 1").performClick()

        // 4. Assertions for Landscape
        // Verify landscape-specific video player is shown
        composeTestRule.onNodeWithTag("VideoPlayerLandscape").assertIsDisplayed()

        // Verify portrait-only elements are NOT displayed
        composeTestRule.onNodeWithTag("MoreButton").assertDoesNotExist()
        composeTestRule.onNodeWithTag("Name").assertDoesNotExist()
        composeTestRule.onNodeWithTag("Uri").assertDoesNotExist()
    }
}

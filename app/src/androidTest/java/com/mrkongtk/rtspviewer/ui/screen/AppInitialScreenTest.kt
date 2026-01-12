package com.mrkongtk.rtspviewer.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
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
        whenever(mockRepo.items) doReturn MutableStateFlow(emptyList())
        whenever(mockRepo.cachedPreviews) doReturn MutableStateFlow(emptyMap<Long, Bitmap>())

        appViewModel = AppViewModel(mockRepo, mockFileRepo)
        appBarViewModel = AppBarViewModel()
    }

    /**
     * Helper function to set content while bypassing Hilt for the internal VideoPlayer
     */
    private fun setTestContent() {
        composeTestRule.setContent {
            // This bypasses the hiltViewModel() call inside VideoPlayer
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
    fun appInitialScreen_startDestination_isStartScreen() {
        setTestContent()

        composeTestRule.onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()
        val appName = context.getString(R.string.app_name)
        composeTestRule.onNodeWithTag("AppBarTitle").assertTextEquals(appName)
    }

    @Test
    fun appInitialScreen_clickAdd_navigatesToAddScreen() {
        setTestContent()

        composeTestRule.onNodeWithTag("AddButton").performClick()
        composeTestRule.onNodeWithTag("AddStreamItemScreenRoot").assertIsDisplayed()

        val addTitle = context.getString(R.string.screen_add_rtsp_item)
        composeTestRule.onNodeWithTag("AppBarTitle").assertTextEquals(addTitle)
    }

    @Test
    fun appInitialScreen_selectItem_navigatesToDisplayAndShowsDynamicTitle() = runTest {
        // 1. Prepare data
        val itemsFlow = MutableStateFlow(listOf(testItem))
        whenever(mockRepo.items) doReturn itemsFlow
        appViewModel = AppViewModel(mockRepo, mockFileRepo)

        setTestContent()

        // 2. Click on the item
        composeTestRule.onNodeWithTag("StreamListItem: 1").performClick()

        // 3. Verify Navigation
        composeTestRule.onNodeWithTag("StreamItemScreenRoot").assertIsDisplayed()

        // 4. Verify Dynamic Title
        val template = context.getString(R.string.screen_rtsp_display)
        val expectedTitle = template.formatText(testItem.name)
        composeTestRule.onNodeWithTag("AppBarTitle").assertTextEquals(expectedTitle)
    }

    @Test
    fun appInitialScreen_backButton_returnsToStart() {
        setTestContent()

        composeTestRule.onNodeWithTag("AddButton").performClick()

        val backDesc = context.getString(R.string.back_button)
        composeTestRule.onNodeWithContentDescription(backDesc).performClick()

        composeTestRule.onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()
    }
}

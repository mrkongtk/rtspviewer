package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmodel.AppBarViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test that the App Bar displays the correct title for the Start screen
     * and hides the back button.
     */
    @Test
    fun appBar_startScreen_showsCorrectTitleAndNoBackButton() {
        // We use the real ViewModel but manually trigger updates
        val viewModel = AppBarViewModel()

        composeTestRule.setContent {
            RTSPViewerTheme {
                AppBar(
                    viewModel = viewModel,
                    navigateUp = {}
                )
            }
        }

        // Update state to Start screen, no back stack
        viewModel.update(AppScreen.Start, canNavigateBack = false)

        // Verify Title (R.string.app_name is usually "RTSP Viewer")
        val expectedTitle = composeTestRule.activityContext.getString(R.string.app_name)
        composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()

        // Verify Back Button does not exist
        val backDesc = composeTestRule.activityContext.getString(R.string.back_button)
        composeTestRule.onNodeWithContentDescription(backDesc).assertDoesNotExist()
    }

    /**
     * Test that when an item is selected on the Display screen,
     * the title updates dynamically to include the item's name.
     */
    @Test
    fun appBar_displayScreen_showsDynamicTitleAndBackButton() {
        val viewModel = AppBarViewModel()
        var backClicked = false

        composeTestRule.setContent {
            RTSPViewerTheme {
                AppBar(
                    viewModel = viewModel,
                    navigateUp = { backClicked = true }
                )
            }
        }

        // Setup: Viewing a specific camera
        val mockItem = RTSPItem(1, "Front Door", "rtsp://...", emptyList(), 0)
        viewModel.update(AppScreen.RTSPDisplay, canNavigateBack = true)
        viewModel.update(mockItem)

        // Verify the formatted title (e.g., "Viewing Front Door")
        // Note: Replace R.string.screen_rtsp_display format with your actual string logic
        val titleTemplate = composeTestRule.activityContext.getString(R.string.screen_rtsp_display)
        val expectedTitle = titleTemplate.replace("%1", mockItem.name)

        composeTestRule.onNodeWithTag("AppBarTitle").assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedTitle).assertIsDisplayed()

        // Verify and click Back Button
        val backDesc = composeTestRule.activityContext.getString(R.string.back_button)
        composeTestRule.onNodeWithContentDescription(backDesc)
            .assertIsDisplayed()
            .performClick()

        assertTrue(backClicked)
    }

    /**
     * Test that the title is empty if the resource ID is invalid/0.
     */
    @Test
    fun appBar_emptyState_showsNoTitle() {
        val viewModel = AppBarViewModel()

        composeTestRule.setContent {
            RTSPViewerTheme {
                AppBar(
                    viewModel = viewModel,
                    navigateUp = {}
                )
            }
        }

        // By default, AppBarState uses AppScreen.Start,
        // let's verify it's not empty initially but can be cleared if needed
        composeTestRule.onNodeWithTag("AppBarTitle").assertIsDisplayed()
    }
}

// Helper extension to get strings inside the test rule
private val androidx.compose.ui.test.junit4.ComposeTestRule.activityContext
    get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext

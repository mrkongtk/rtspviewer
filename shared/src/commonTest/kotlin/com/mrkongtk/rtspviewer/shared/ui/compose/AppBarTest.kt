package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.navigation.AppScreen
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.formatText
import com.mrkongtk.rtspviewer.shared.viewmodel.AppBarViewModel
import org.jetbrains.compose.resources.getString
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.app_name
import rtspviewer.shared.generated.resources.back_button
import rtspviewer.shared.generated.resources.screen_add_rtsp_item
import rtspviewer.shared.generated.resources.screen_edit_rtsp_item
import rtspviewer.shared.generated.resources.screen_rtsp_display
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * UI tests for the [AppBar] component using the Compose Multiplatform testing framework.
 *
 * These tests verify the reactive behavior of the application's global navigation header.
 * They ensure that the [AppBar] correctly interprets state from [AppBarViewModel] to:
 * 1. Display the appropriate localized and dynamically formatted titles.
 * 2. Toggle the visibility of the navigation (back) icon based on the navigation stack state.
 * 3. Invoke the provided `navigateUp` callback when the user interacts with the back button.
 *
 * The tests leverage [runComposeUiTest] to provide a consistent environment for UI validation
 * across different platforms in this Kotlin Multiplatform project.
 */
@OptIn(ExperimentalTestApi::class)
class AppBarTest {

    /**
     * Verifies that the [AppBar] displays the default application name and hides the
     * back button when the user is on the Start screen.
     */
    @Test
    fun appBar_startScreen_showsCorrectTitleAndNoBackButton() = runComposeUiTest {
        // We use the real ViewModel but manually trigger updates
        val viewModel = AppBarViewModel()

        setContent {
            RTSPViewerTheme {
                AppBar(
                    viewModel = viewModel,
                    navigateUp = {}
                )
            }
        }

        // Update state to Start screen, no back stack
        viewModel.update(AppScreen.Start, canNavigateBack = false)

        // Verify Title specifically on the tagged node
        val expectedTitle = getString(Res.string.app_name)
        onNodeWithTag("AppBarTitle")
            .assertIsDisplayed()
            .assertTextEquals(expectedTitle)

        // Verify Back Button does not exist
        val backDesc = getString(Res.string.back_button)
        onNodeWithContentDescription(backDesc).assertDoesNotExist()
    }

    /**
     * Verifies that when viewing a specific RTSP stream, the [AppBar] title is
     * dynamically formatted to include the stream's name (e.g., "Viewing: Front Door").
     * Also ensures the back button is visible and functional.
     */
    @Test
    fun appBar_displayScreen_showsDynamicTitleAndBackButton() = runComposeUiTest {
        val viewModel = AppBarViewModel()
        var backClicked = false

        setContent {
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

        // Verify the formatted title using the same utility as the UI
        val titleTemplate = getString(Res.string.screen_rtsp_display)
        val expectedTitle = titleTemplate.formatText(mockItem.name)

        onNodeWithTag("AppBarTitle")
            .assertIsDisplayed()
            .assertTextEquals(expectedTitle)

        // Verify and click Back Button
        val backDesc = getString(Res.string.back_button)
        onNodeWithContentDescription(backDesc)
            .assertIsDisplayed()
            .performClick()

        assertTrue(backClicked)
    }

    /**
     * Verifies that the "Add Stream" screen displays its unique static title
     * and provides a functional back button for navigation.
     */
    @Test
    fun appBar_addScreen_showsCorrectTitleAndBackButton() = runComposeUiTest {
        val viewModel = AppBarViewModel()
        var backClicked = false

        setContent {
            RTSPViewerTheme {
                AppBar(
                    viewModel = viewModel,
                    navigateUp = { backClicked = true }
                )
            }
        }

        viewModel.update(AppScreen.AddRTSPItem, canNavigateBack = true)

        val expectedTitle = getString(Res.string.screen_add_rtsp_item)
        onNodeWithTag("AppBarTitle")
            .assertIsDisplayed()
            .assertTextEquals(expectedTitle)

        onNodeWithContentDescription(getString(Res.string.back_button))
            .assertIsDisplayed()
            .performClick()

        assertTrue(backClicked)
    }

    /**
     * Verifies that the "Edit Stream" screen displays a dynamic title containing
     * the name of the item being edited, ensuring context-aware navigation UI.
     */
    @Test
    fun appBar_editScreen_showsDynamicTitleAndBackButton() = runComposeUiTest {
        val viewModel = AppBarViewModel()
        var backClicked = false

        setContent {
            RTSPViewerTheme {
                AppBar(
                    viewModel = viewModel,
                    navigateUp = { backClicked = true }
                )
            }
        }

        val mockItem = RTSPItem(1, "Backyard", "rtsp://...", emptyList(), 0)
        viewModel.update(AppScreen.EditRTSPItem, canNavigateBack = true)
        viewModel.update(mockItem)

        val titleTemplate = getString(Res.string.screen_edit_rtsp_item)
        val expectedTitle = titleTemplate.formatText(mockItem.name)

        onNodeWithTag("AppBarTitle")
            .assertIsDisplayed()
            .assertTextEquals(expectedTitle)

        onNodeWithContentDescription(getString(Res.string.back_button))
            .assertIsDisplayed()
            .performClick()

        assertTrue(backClicked)
    }

    /**
     * Ensures that upon initial composition, before any manual updates, the [AppBar]
     * gracefully defaults to the main application name.
     */
    @Test
    fun appBar_initialState_showsMainScreenTitle() = runComposeUiTest {
        val viewModel = AppBarViewModel()

        setContent {
            RTSPViewerTheme {
                AppBar(
                    viewModel = viewModel,
                    navigateUp = {}
                )
            }
        }

        val expectedTitle = getString(Res.string.app_name)
        onNodeWithTag("AppBarTitle")
            .assertIsDisplayed()
            .assertTextEquals(expectedTitle)
    }
}

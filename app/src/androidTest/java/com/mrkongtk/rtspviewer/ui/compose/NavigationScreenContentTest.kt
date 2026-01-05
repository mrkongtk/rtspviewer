package com.mrkongtk.rtspviewer.ui

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.data.AppUiState
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.NavigationScreenContent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

/**
 * UI Test suite for [NavigationScreenContent].
 *
 * This class verifies the navigation flows (List -> Detail -> Edit/Add) and ensures
 * that interaction events (Click, Save, Delete) trigger the correct callbacks
 * and navigation route changes.
 */
class NavigationScreenContentTest {

    /**
     * The ComposeTestRule allows us to set content and interact with UI nodes.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    // -- Navigation Controller for testing --
    private lateinit var navController: TestNavHostController

    // -- Mock Callbacks to verify business logic interactions --
    private val onListingItemSelected: (RTSPItem) -> Unit = mock()
    private val onAddItemRequested: (RTSPItem) -> Unit = mock()
    private val onItemsReordered: (List<RTSPItem>) -> Unit = mock()
    private val onEditItemRequested: (RTSPItem) -> Unit = mock()
    private val onDeleteItemRequested: (RTSPItem) -> Unit = mock()
    private val onImageAvailable: (RTSPItem, Bitmap) -> Unit = mock()

    // -- Dummy Data for testing --
    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Test Camera",
        uri = "rtsp://192.168.1.1",
        tags = listOf("Home"),
        order = 0,
        forceTcp = false
    )

    /**
     * Sets up the test environment before every test.
     * Initializes the TestNavHostController and mounts the NavigationScreenContent.
     */
    @Before
    fun setup() {
        composeTestRule.setContent {
            // Provide LocalInspectionMode to potentially bypass complex rendering or animations during tests
            CompositionLocalProvider(LocalInspectionMode provides true) {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())

                // Prepare initial state with a populated list
                val uiState = AppUiState(
                    items = listOf(sampleItem),
                    selectedItem = sampleItem // Pre-select to allow testing flows starting from Detail view
                )

                NavigationScreenContent(
                    navController = navController,
                    uiState = uiState,
                    onListingItemSelected = onListingItemSelected,
                    onAddItemRequested = onAddItemRequested,
                    onItemsReordered = onItemsReordered,
                    onEditItemRequested = onEditItemRequested,
                    onDeleteItemRequested = onDeleteItemRequested,
                    onImageAvailable = onImageAvailable
                )
            }
        }
    }

    /**
     * Verifies that the app launches directly into the Stream List screen.
     */
    @Test
    fun startDestination_isStreamListScreen() {
        // Assert: The current route matches the Start (List) screen
        assertEquals(AppScreen.Start.name, navController.currentBackStackEntry?.destination?.route)

        // Assert: The "Add" FAB is visible, confirming we are on the list screen
        composeTestRule.onNodeWithTag("AddButton").assertIsDisplayed()
    }

    /**
     * Tests the navigation from the List screen to the Detail screen.
     */
    @Test
    fun navigateToDetail_whenItemClicked() {
        // Action: Click on the specific list item identified by its Test Tag
        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem.id}").performClick()

        // Assert: The viewmodel callback was invoked with the correct item
        verify(onListingItemSelected).invoke(eq(sampleItem))

        // Assert: Navigation controller moved to the RTSPDisplay route
        assertEquals(
            AppScreen.RTSPDisplay.name,
            navController.currentBackStackEntry?.destination?.route
        )
    }

    /**
     * Tests the navigation from the List screen to the Add Item form.
     */
    @Test
    fun navigateToAddScreen_whenFabClicked() {
        // Action: Click the Floating Action Button
        composeTestRule.onNodeWithTag("AddButton").performClick()

        // Assert: Route changed to AddRTSPItem
        assertEquals(
            AppScreen.AddRTSPItem.name,
            navController.currentBackStackEntry?.destination?.route
        )

        // Assert: The Save button (specific to the form) is now visible
        composeTestRule.onNodeWithTag("SaveButton").assertIsDisplayed()
    }

    /**
     * comprehensive test of the "Add Item" flow:
     * 1. Navigate to Add Screen
     * 2. Fill in text fields
     * 3. Click Save
     * 4. Verify callback and navigation pop
     */
    @Test
    fun addItemFlow_fillsFormAndSaves() {
        // 1. Navigate to Add Screen
        composeTestRule.onNodeWithTag("AddButton").performClick()

        // 2. Input valid data using helper function to find nested text fields
        onRTSPField("NameTextField").performTextInput("New Cam")
        onRTSPField("UriTextField").performTextInput("rtsp://10.0.0.5")

        // 3. Click Save
        composeTestRule.onNodeWithTag("SaveButton").performClick()

        // Assert: The onAddItemRequested callback was triggered
        verify(onAddItemRequested).invoke(any())

        // Assert: Navigation popped back to the Start screen
        assertEquals(AppScreen.Start.name, navController.currentBackStackEntry?.destination?.route)
    }

    /**
     * Comprehensive test of the "Edit Item" flow:
     * 1. Go to Detail -> Menu -> Edit
     * 2. Modify a field
     * 3. Click Save
     * 4. Verify callback and navigation
     */
    @Test
    fun editItemFlow_modifiesAndSaves() {
        // 1. Navigate to Detail Screen
        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem.id}").performClick()

        // 2. Open Top Bar Menu and select Edit
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("EditButton").performClick()

        // Assert: We are on the Edit screen
        assertEquals(
            AppScreen.EditRTSPItem.name,
            navController.currentBackStackEntry?.destination?.route
        )

        // 4. Modify Name (appends text to existing value)
        onRTSPField("NameTextField").performTextInput(" Updated")

        // 5. Click Save
        composeTestRule.onNodeWithTag("SaveButton").performClick()

        // Assert: The update callback was triggered
        verify(onEditItemRequested).invoke(any())

        // Assert: Navigation returns to the previous screen (RTSPDisplay)
        assertEquals(
            AppScreen.RTSPDisplay.name,
            navController.currentBackStackEntry?.destination?.route
        )
    }

    /**
     * Tests the Delete flow, including the confirmation dialog interaction.
     */
    @Test
    fun deleteItemFlow_showsDialogAndConfirms() {
        // 1. Navigate to Detail Screen
        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem.id}").performClick()

        // 2. Open Menu and select Delete
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("DeleteButton").performClick()

        // Assert: Delete Confirmation Dialog appears
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertIsDisplayed()

        // 4. Click Confirm on the dialog
        composeTestRule.onNodeWithTag("DeleteConfirmButton").performClick()

        // Assert: Delete callback triggered with the specific item
        verify(onDeleteItemRequested).invoke(eq(sampleItem))

        // Assert: Navigation pops back to the List screen (Start)
        assertEquals(AppScreen.Start.name, navController.currentBackStackEntry?.destination?.route)
    }

    /**
     * Helper function to find a specific text field within the custom `RTSPOutlinedTextField` composable.
     *
     * @param tag The Test Tag of the parent container (e.g., "NameTextField").
     */
    private fun onRTSPField(tag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

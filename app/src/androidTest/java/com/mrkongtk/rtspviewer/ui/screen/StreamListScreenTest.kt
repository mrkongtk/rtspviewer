package com.mrkongtk.rtspviewer.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * UI Test Suite for [StreamListScreen].
 *
 * This class verifies the visual states (Empty vs Populated) and user interactions
 * (Clicks, Navigation triggers) of the StreamList screen using Jetpack Compose UI Tests.
 */
class StreamListScreenTest {

    /**
     * The Compose Test Rule.
     * Using [createAndroidComposeRule] provides access to the Activity context
     * (useful for string resources) and handles the Compose setup/teardown.
     */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Helper interface for Mockito verification.
     *
     * Since mocking Kotlin lambdas (e.g., `(RTSPItem) -> Unit`) can be verbose or tricky
     * with Mockito, we define this interface to proxy the callbacks. This allows us to
     * easily use `verify(actions).methodName()` in our assertions.
     */
    interface ScreenActions {
        fun onItemSelected(item: RTSPItem)
        fun onAddItemSelected()
        fun onItemsReordered(items: List<RTSPItem>)
    }

    // Create a mock of our helper interface to track callback invocations
    private val actions: ScreenActions = mock()

    // -------------------------------------------------------------------------
    // Sample Data
    // -------------------------------------------------------------------------

    private val sampleItem1 = RTSPItem(
        id = 1L,
        name = "Front Door",
        uri = "rtsp://192.168.1.50",
        tags = listOf("Outdoor"),
        order = 0,
        forceTcp = false
    )

    private val sampleItem2 = RTSPItem(
        id = 2L,
        name = "Kitchen",
        uri = "rtsp://192.168.1.51",
        tags = listOf("Indoor"),
        order = 1,
        forceTcp = true
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Scenario: The database returns an empty list.
     * Expected Result: The specific empty state message is shown, and the list component is hidden.
     */
    @Test
    fun streamListScreen_whenListIsEmpty_showsEmptyStateMessage() {
        // Arrange
        val emptyList = emptyList<RTSPItem>()

        // Act
        composeTestRule.setContent {
            StreamListScreen(
                itemList = emptyList,
                onItemSelected = actions::onItemSelected,
                onAddItemSelected = actions::onAddItemSelected,
                onItemsReordered = actions::onItemsReordered
            )
        }

        // Assert
        // Check that the specific empty state text tag exists and is displayed
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText")
            .assertIsDisplayed()

        // Ensure the list component itself is NOT displayed to avoid layout clutter
        composeTestRule.onNodeWithTag("StreamListScreenListRoot")
            .assertDoesNotExist()
    }

    /**
     * Scenario: The database returns a list of items.
     * Expected Result: The empty state message is hidden, the list is visible,
     * and the specific item data (names) is rendered on screen.
     */
    @Test
    fun streamListScreen_whenListHasItems_showsListAndHidesEmptyMessage() {
        // Arrange
        val items = listOf(sampleItem1, sampleItem2)

        // Act
        composeTestRule.setContent {
            StreamListScreen(
                itemList = items,
                onItemSelected = actions::onItemSelected,
                onAddItemSelected = actions::onAddItemSelected,
                onItemsReordered = actions::onItemsReordered
            )
        }

        // Assert
        // Empty text should be gone
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText")
            .assertDoesNotExist()

        // List root should be visible
        composeTestRule.onNodeWithTag("StreamListScreenListRoot")
            .assertIsDisplayed()

        // Verify specific items are rendered using the `testTag` defined in the Composable
        // and that their display names are visible to the user.
        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem1.id}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleItem1.name)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem2.id}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleItem2.name)
            .assertIsDisplayed()
    }

    /**
     * Scenario: User clicks the floating "Add" button.
     * Expected Result: The `onAddItemSelected` callback is triggered.
     */
    @Test
    fun streamListScreen_whenAddButtonIsClicked_triggersCallback() {
        // Arrange
        composeTestRule.setContent {
            StreamListScreen(
                itemList = emptyList(),
                onItemSelected = actions::onItemSelected,
                onAddItemSelected = actions::onAddItemSelected,
                onItemsReordered = actions::onItemsReordered
            )
        }

        // Act
        composeTestRule.onNodeWithTag("AddButton")
            .performClick()

        // Assert (Mockito verify)
        verify(actions).onAddItemSelected()
    }

    /**
     * Scenario: User clicks on a specific stream item in the list.
     * Expected Result: The `onItemSelected` callback is triggered with the correct data object.
     */
    @Test
    fun streamListScreen_whenItemIsClicked_triggersItemSelectedCallback() {
        // Arrange
        val items = listOf(sampleItem1)
        composeTestRule.setContent {
            StreamListScreen(
                itemList = items,
                onItemSelected = actions::onItemSelected,
                onAddItemSelected = actions::onAddItemSelected,
                onItemsReordered = actions::onItemsReordered
            )
        }

        // Act
        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem1.id}")
            .performClick()

        // Assert (Mockito verify)
        // Verify that onItemSelected was called specifically with sampleItem1
        verify(actions).onItemSelected(sampleItem1)
    }

    /**
     * Scenario: The list is fully populated.
     * Expected Result: The floating action button (FAB) remains visible and accessible.
     */
    @Test
    fun streamListScreen_alwaysShowsAddButton_evenWhenListIsPopulated() {
        // Arrange
        val items = listOf(sampleItem1, sampleItem2)
        composeTestRule.setContent {
            StreamListScreen(
                itemList = items,
                onItemSelected = {},
                onAddItemSelected = {},
                onItemsReordered = {}
            )
        }

        // Assert
        // The FAB uses Box(modifier = Modifier.fillMaxSize()), so it should appear
        // regardless of whether the list is empty or full.
        composeTestRule.onNodeWithTag("AddButton")
            .assertIsDisplayed()
    }
}

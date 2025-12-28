package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.times

/**
 * UI Test Suite for the [StreamListItem] Composable.
 *
 * This class uses the AndroidX Compose Test library to verify:
 * 1. UI Rendering: Ensuring data maps correctly to visual elements.
 * 2. User Interaction: Ensuring click events trigger the expected callbacks.
 */
class StreamListItemTest {

    /**
     * The [createComposeRule] is required to set the content and interact with
     * the Compose hierarchy. It acts as the entry point for all UI testing actions.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test: Content Verification.
     * Scenario: A valid RTSPItem is provided to the composable.
     * Expected Result: The item's name and the navigation icon are visible to the user.
     */
    @Test
    fun streamListItem_displaysCorrectInfo() {
        // ---------------------------------------------------------------------
        // ARRANGE: Prepare the data and environment
        // ---------------------------------------------------------------------
        val testItem = RTSPItem(
            id = 1L,
            name = "Front Door Camera",
            uri = "rtsp://192.168.1.55",
            tags = emptyList(),
            order = 0,
            forceTcp = false
        )

        // Fetch the target context to retrieve string resources (e.g., R.string.detail)
        // This ensures the test checks for the actual localized string used in the app.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedIconDescription = context.getString(R.string.detail)

        // ---------------------------------------------------------------------
        // ACT: Render the composable
        // ---------------------------------------------------------------------
        composeTestRule.setContent {
            // We wrap the component in the App Theme to ensure typography and
            // colors are applied, simulating the real app environment.
            RTSPViewerTheme {
                StreamListItem(
                    data = testItem,
                    onClick = {} // No-op for this test
                )
            }
        }

        // ---------------------------------------------------------------------
        // ASSERT: Verify the UI state
        // ---------------------------------------------------------------------

        // 1. Verify the camera name is displayed
        composeTestRule
            .onNodeWithText("Front Door Camera")
            .assertIsDisplayed()

        // 2. Verify the icon is displayed using its Content Description
        composeTestRule
            .onNodeWithContentDescription(expectedIconDescription)
            .assertIsDisplayed()
    }

    /**
     * Test: Interaction Verification.
     * Scenario: The user clicks on the StreamListItem.
     * Expected Result: The onClick callback is invoked exactly once with the correct data item.
     */
    @Test
    fun streamListItem_onClick_triggersCallback() {
        // ---------------------------------------------------------------------
        // ARRANGE: Prepare data and mocks
        // ---------------------------------------------------------------------
        val testItem = RTSPItem(
            id = 100L,
            name = "Backyard",
            uri = "rtsp://192.168.1.56",
            tags = listOf("Outdoor"),
            order = 1
        )

        // Mock the callback function: (RTSPItem) -> Unit
        // We use Mockito to track interactions with this lambda.
        val mockOnClick = mock<Function1<RTSPItem, Unit>>()

        // ---------------------------------------------------------------------
        // ACT: Render and Perform Interaction
        // ---------------------------------------------------------------------
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = testItem,
                    // Pass the mock's invoke method as the callback
                    onClick = { mockOnClick.invoke(it) }
                )
            }
        }

        // Simulate a user click on the node containing the text "Backyard"
        composeTestRule
            .onNodeWithText("Backyard")
            .performClick()

        // ---------------------------------------------------------------------
        // ASSERT: Verify the interaction logic
        // ---------------------------------------------------------------------

        // Verify that the mock function was called exactly 1 time with 'testItem'
        verify(mockOnClick, times(1)).invoke(testItem)
    }
}

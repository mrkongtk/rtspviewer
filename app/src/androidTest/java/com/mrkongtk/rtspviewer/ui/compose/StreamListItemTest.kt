package com.mrkongtk.rtspviewer.ui.compose

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * UI Test Suite for the [StreamListItem] Composable.
 *
 * This class uses the AndroidX Compose Test library to verify:
 * 1. UI Rendering: Ensuring data maps correctly to visual elements (Text, Tags, Images).
 * 2. Visual Logic: Ensuring preview images appear/disappear based on data.
 * 3. User Interaction: Ensuring click events trigger the expected callbacks.
 */
class StreamListItemTest {

    /**
     * The [createComposeRule] is required to set the content and interact with
     * the Compose hierarchy. It acts as the entry point for all UI testing actions.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test: Content Verification (Basic).
     * Scenario: A valid RTSPItem is provided without a preview image.
     * Expected Result: The item's name and the navigation icon are visible.
     */
    @Test
    fun streamListItem_displaysCorrectInfo() {
        // ---------------------------------------------------------------------
        // ARRANGE
        // ---------------------------------------------------------------------
        val testItem = RTSPItem(
            id = 1L,
            name = "Front Door Camera",
            uri = "rtsp://192.168.1.55",
            tags = emptyList(),
            order = 0,
            forceTcp = false
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedIconDescription = context.getString(R.string.detail)

        // ---------------------------------------------------------------------
        // ACT
        // ---------------------------------------------------------------------
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = testItem,
                    preview = null, // Testing state without image
                    onClick = {}
                )
            }
        }

        // ---------------------------------------------------------------------
        // ASSERT
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
     * Test: Content Verification (Tags).
     * Scenario: A valid RTSPItem is provided with a list of tags.
     * Expected Result: The tags are displayed and identifiable by their test tags.
     */
    @Test
    fun streamListItem_displaysTags() {
        // ---------------------------------------------------------------------
        // ARRANGE
        // ---------------------------------------------------------------------
        val tags = listOf("Outdoor", "Security", "Home")
        val testItem = RTSPItem(
            id = 1L,
            name = "Garden Cam",
            uri = "rtsp://192.168.1.55",
            tags = tags,
            order = 0,
            forceTcp = false
        )

        // ---------------------------------------------------------------------
        // ACT
        // ---------------------------------------------------------------------
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = testItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        // ---------------------------------------------------------------------
        // ASSERT
        // ---------------------------------------------------------------------

        // Verify every tag in the list is displayed using the testTag modifier logic
        // defined in the Composable: .testTag("Tag $tag")
        tags.forEach { tag ->
            composeTestRule
                .onNodeWithTag("Tag $tag", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(tag)
        }
    }

    /**
     * Test: Preview Image Logic (Visible).
     * Scenario: A valid Bitmap is provided.
     * Expected Result: An Image node exists with the formatted content description.
     */
    @Test
    fun streamListItem_showsPreviewImage_whenAvailable() {
        // ---------------------------------------------------------------------
        // ARRANGE
        // ---------------------------------------------------------------------
        val testItem = RTSPItem(
            id = 2L,
            name = "Garage",
            uri = "rtsp://192.168.1.55",
            tags = emptyList(),
            order = 0
        )

        // Create a dummy bitmap for testing
        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Construct the expected accessibility description: "Preview for Garage"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedDescription = context.getString(R.string.rtsp_item_preview_description)
            .replace("%1", testItem.name)

        // ---------------------------------------------------------------------
        // ACT
        // ---------------------------------------------------------------------
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = testItem,
                    preview = dummyBitmap, // Provide bitmap
                    onClick = {}
                )
            }
        }

        // ---------------------------------------------------------------------
        // ASSERT
        // ---------------------------------------------------------------------
        composeTestRule
            .onNodeWithContentDescription(expectedDescription)
            .assertIsDisplayed()
    }

    /**
     * Test: Preview Image Logic (Hidden).
     * Scenario: The preview bitmap is null.
     * Expected Result: The Image node with the preview description does not exist in the hierarchy.
     */
    @Test
    fun streamListItem_hidesPreviewImage_whenNull() {
        // ---------------------------------------------------------------------
        // ARRANGE
        // ---------------------------------------------------------------------
        val testItem = RTSPItem(
            id = 3L,
            name = "Kitchen",
            uri = "rtsp://192.168.1.55",
            tags = emptyList(),
            order = 0
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedDescription = context.getString(R.string.rtsp_item_preview_description)
            .replace("%1", testItem.name)

        // ---------------------------------------------------------------------
        // ACT
        // ---------------------------------------------------------------------
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = testItem,
                    preview = null, // Null bitmap
                    onClick = {}
                )
            }
        }

        // ---------------------------------------------------------------------
        // ASSERT
        // ---------------------------------------------------------------------
        composeTestRule
            .onNodeWithContentDescription(expectedDescription)
            .assertDoesNotExist()
    }

    /**
     * Test: Interaction Verification.
     * Scenario: The user clicks on the StreamListItem.
     * Expected Result: The onClick callback is invoked exactly once with the correct data item.
     */
    @Test
    fun streamListItem_onClick_triggersCallback() {
        // ---------------------------------------------------------------------
        // ARRANGE
        // ---------------------------------------------------------------------
        val testItem = RTSPItem(
            id = 100L,
            name = "Backyard",
            uri = "rtsp://192.168.1.56",
            tags = listOf("Outdoor"),
            order = 1
        )

        // Mock the callback function using mockito-kotlin
        val mockOnClick = mock<Function1<RTSPItem, Unit>>()

        // ---------------------------------------------------------------------
        // ACT
        // ---------------------------------------------------------------------
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = testItem,
                    preview = null,
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
        // ASSERT
        // ---------------------------------------------------------------------

        // Verify that the mock function was called exactly 1 time with 'testItem'
        verify(mockOnClick, times(1)).invoke(testItem)
    }
}

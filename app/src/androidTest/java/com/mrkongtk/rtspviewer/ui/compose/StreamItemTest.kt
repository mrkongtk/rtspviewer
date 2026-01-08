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
 * UI Test Suite for the [StreamItem] Composable.
 *
 * This suite verifies the mapping between the [RTSPItem] domain model and the
 * Material 3 ElevatedCard UI, ensuring that tags, previews, and interaction
 * callbacks behave as expected.
 */
class StreamItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test: Basic Metadata Rendering.
     * Verifies that the item name and the navigation chevron are present.
     */
    @Test
    fun streamListItem_displaysCorrectInfo() {
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

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = testItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        // Verify Name visibility
        composeTestRule
            .onNodeWithText("Front Door Camera")
            .assertIsDisplayed()

        // Verify Navigation Icon visibility (via accessibility description)
        composeTestRule
            .onNodeWithContentDescription(expectedIconDescription)
            .assertIsDisplayed()
    }

    /**
     * Test: Tag FlowRow Rendering.
     * Verifies that multiple tags are rendered and identifiable by the
     * dynamic test tags generated in the Composable logic.
     */
    @Test
    fun streamListItem_displaysTags_withCorrectTestTags() {
        val tags = listOf("Outdoor", "Security", "4K")
        val testItem = RTSPItem(
            id = 1L,
            name = "Garden Cam",
            uri = "rtsp://192.168.1.55",
            tags = tags,
            order = 0
        )

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = testItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        // Validate each tag specifically using the pattern: .testTag("Tag $tag")
        tags.forEach { tag ->
            composeTestRule
                .onNodeWithTag("Tag $tag", useUnmergedTree = true)
                .assertIsDisplayed()
                .assertTextEquals(tag)
        }
    }

    /**
     * Test: Preview Image Visibility.
     * Verifies that when a bitmap is provided, an Image is rendered with the
     * correct dynamic content description.
     */
    @Test
    fun streamListItem_showsPreviewImage_withDynamicDescription() {
        val itemName = "Garage"
        val testItem = RTSPItem(
            id = 2L,
            name = itemName,
            uri = "rtsp://192.168.1.55",
            tags = emptyList(),
            order = 0
        )

        val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // The implementation uses .replace("%1", data.name)
        val expectedDescription = context.getString(R.string.rtsp_item_preview_description)
            .replace("%1", itemName)

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = testItem,
                    preview = dummyBitmap,
                    onClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(expectedDescription)
            .assertIsDisplayed()
    }

    /**
     * Test: Conditional Rendering (Null Preview).
     * Ensures that no Image component is present in the layout tree if no preview exists.
     */
    @Test
    fun streamListItem_doesNotRenderImage_whenPreviewIsNull() {
        val testItem = RTSPItem(
            id = 3L,
            name = "Kitchen",
            uri = "rtsp://192.168.1.55",
            tags = emptyList(),
            order = 0
        )

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = testItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        // We search for any node that matches the preview description pattern and ensure it's missing
        composeTestRule
            .onNodeWithContentDescription("Preview", substring = true)
            .assertDoesNotExist()
    }

    /**
     * Test: Click Interaction.
     * Verifies that clicking the Card triggers the callback with the correct [RTSPItem].
     */
    @Test
    fun streamListItem_onClick_triggersCallbackWithCorrectData() {
        val testItem = RTSPItem(
            id = 100L,
            name = "Backyard",
            uri = "rtsp://192.168.1.56",
            tags = listOf("Outdoor"),
            order = 1
        )

        // Using Mockito to verify the functional interface
        val mockOnClick: (RTSPItem) -> Unit = mock()

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = testItem,
                    preview = null,
                    onClick = mockOnClick
                )
            }
        }

        // Perform click on the item
        composeTestRule
            .onNodeWithText("Backyard")
            .performClick()

        // Assert that the callback was executed exactly once with the provided item
        verify(mockOnClick, times(1)).invoke(testItem)
    }
}

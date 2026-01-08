package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock Data
    private val mockItem = RTSPItem(
        id = 101L,
        name = "Front Door Camera",
        uri = "rtsp://192.168.1.1",
        tags = listOf("Outdoor", "Security"),
        order = 1
    )

    /**
     * Verifies that the StreamListItem correctly displays the
     * item's name and all associated tags.
     */
    @Test
    fun streamListItem_displaysNameAndTags() {
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        // Verify the stream name is visible
        composeTestRule.onNodeWithText("Front Door Camera").assertIsDisplayed()

        // Verify tags are visible using the testTag defined in StreamItem.kt
        composeTestRule.onNodeWithTag("Tag Outdoor", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("Tag Security", useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * Verifies that clicking the StreamListItem triggers the onClick callback
     * with the correct RTSPItem data.
     */
    @Test
    fun streamListItem_clickTriggersCallback() {
        var capturedItem: RTSPItem? = null

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = null,
                    onClick = { capturedItem = it }
                )
            }
        }

        // Perform click on the item
        composeTestRule.onNodeWithText("Front Door Camera").performClick()

        // Assert that the callback was received with the correct item
        assertEquals(mockItem, capturedItem)
    }

    /**
     * Verifies that when a preview bitmap is provided, the item renders correctly.
     */
    @Test
    fun streamListItem_withPreview_rendersCorrectly() {
        // Create a simple 1x1 dummy bitmap
        val dummyBitmap = createBitmap(100, 100).apply {
            eraseColor(Color.Red.toArgb())
        }

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = dummyBitmap,
                    onClick = {}
                )
            }
        }

        // The name should still be visible alongside the image
        composeTestRule.onNodeWithText("Front Door Camera").assertIsDisplayed()

        // Note: Testing actual bitmap content is complex in Compose tests,
        // but checking the layout nodes ensures the logic didn't crash.
    }
}

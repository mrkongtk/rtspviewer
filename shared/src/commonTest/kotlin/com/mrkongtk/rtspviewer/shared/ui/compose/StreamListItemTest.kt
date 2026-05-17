package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.createPlainImage
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class StreamListItemTest {

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
    fun streamListItem_displaysNameAndTags() = runComposeUiTest {
        setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        // Verify the stream name is visible
        onNodeWithText("Front Door Camera").assertIsDisplayed()

        // Verify tags are visible using the testTag defined in StreamItem.kt
        onNodeWithTag("Tag Outdoor", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("Tag Security", useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * Verifies that clicking the StreamListItem triggers the onClick callback
     * with the correct RTSPItem data.
     */
    @Test
    fun streamListItem_clickTriggersCallback() = runComposeUiTest {
        var capturedItem: RTSPItem? = null

        setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = null,
                    onClick = { capturedItem = it }
                )
            }
        }

        // Perform click on the item
        onNodeWithText("Front Door Camera").performClick()

        // Assert that the callback was received with the correct item
        assertEquals(capturedItem, mockItem)
    }

    /**
     * Verifies that when a preview bitmap is provided, the item renders the thumbnail.
     */
    @Test
    fun streamListItem_withPreview_rendersThumbnail() = runComposeUiTest {
        val dummyBitmap = ImageBitmap.createPlainImage(100, 100, Color.Red)

        setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = dummyBitmap,
                    onClick = {}
                )
            }
        }

        onNodeWithTag("thumbnail", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("Front Door Camera").assertIsDisplayed()
    }

    /**
     * Verifies that when no preview is provided, a placeholder is displayed.
     */
    @Test
    fun streamListItem_withoutPreview_displaysPlaceholder() = runComposeUiTest {
        setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        onNodeWithTag("no thumbnail", useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * Verifies that the tags container is not displayed when the item has no tags.
     */
    @Test
    fun streamListItem_withoutTags_hidesTagsContainer() = runComposeUiTest {
        val itemWithoutTags = mockItem.copy(tags = emptyList())

        setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = itemWithoutTags,
                    preview = null,
                    onClick = {}
                )
            }
        }

        onNodeWithTag("item tags").assertDoesNotExist()
    }

    /**
     * Verifies that the detail icon (chevron) is displayed.
     */
    @Test
    fun streamListItem_displaysDetailIcon() = runComposeUiTest {
        setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = null,
                    onClick = {}
                )
            }
        }

        onNodeWithTag("detail", useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * Verifies that clicking the detail icon triggers the onClick callback.
     */
    @Test
    fun streamListItem_clickDetailIcon_triggersCallback() = runComposeUiTest {
        var capturedItem: RTSPItem? = null

        setContent {
            RTSPViewerTheme {
                StreamListItem(
                    data = mockItem,
                    preview = null,
                    onClick = { capturedItem = it }
                )
            }
        }

        // Perform click on the detail icon
        onNodeWithTag("detail", useUnmergedTree = true).performClick()

        // Assert that the callback was received
        assertEquals(capturedItem, mockItem)
    }
}

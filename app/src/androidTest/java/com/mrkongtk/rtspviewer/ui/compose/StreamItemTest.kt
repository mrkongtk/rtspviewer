package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.createBitmap
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StreamItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockItem = RTSPItem(
        id = 1,
        name = "Front Door Camera",
        uri = "rtsp://example.com/live",
        tags = listOf("Outdoor", "Security"),
        order = 0
    )

    @Test
    fun streamItem_displaysNameAndTags() {
        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = null,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Play") },
                    onClick = {}
                )
            }
        }

        // Verify name is displayed
        composeTestRule.onNodeWithText("Front Door Camera").assertIsDisplayed()

        // Verify tags are displayed using the testTags defined in the component
        composeTestRule.onNodeWithTag("Tag Outdoor", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("Tag Security", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun streamItem_showsThumbnail_whenProvided() {
        val bitmap = createBitmap(100, 100).apply {
            eraseColor(Color.Red.toArgb())
        }

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = bitmap,
                    icon = {},
                    onClick = {}
                )
            }
        }

        // The content description in StreamItem uses a template: "Preview of %1"
        // Note: You should ideally use stringResource() to get the exact string,
        // but for this example, we assume the string exists.
        composeTestRule.onNodeWithContentDescription(
            "Preview for \"${mockItem.name}\"",
            substring = true,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun streamItem_click_triggersCallback() {
        var clickedItem: RTSPItem? = null

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = null,
                    icon = {},
                    onClick = { clickedItem = it }
                )
            }
        }

        // Perform click on the item
        composeTestRule.onNodeWithText("Front Door Camera").performClick()

        // Assert that the callback was received with the correct data
        assertEquals(mockItem, clickedItem)
    }

    @Test
    fun streamItem_hidesTags_whenListIsEmpty() {
        val itemNoTags = mockItem.copy(tags = emptyList())

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = itemNoTags,
                    preview = null,
                    icon = {},
                    onClick = {}
                )
            }
        }

        // Verify that tag nodes do not exist
        composeTestRule.onNodeWithTag("Tag Outdoor").assertDoesNotExist()
    }
}

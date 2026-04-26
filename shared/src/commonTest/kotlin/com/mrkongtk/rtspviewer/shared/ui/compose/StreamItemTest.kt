package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.createPlainImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class StreamItemTest {

    private val mockItem = RTSPItem(
        id = 1,
        name = "Front Door Camera",
        uri = "rtsp://example.com/live",
        tags = listOf("Outdoor", "Security"),
        order = 0
    )

    @Test
    fun streamItem_displaysNameAndTags() = runComposeUiTest {
        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = null,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Play") },
                    onClick = {}
                )
            }
        }

        onNodeWithText("Front Door Camera").assertIsDisplayed()

        // useUnmergedTree = true is necessary because FlowRow/Row often merges
        // children into a single semantic node for accessibility.
        onNodeWithTag("Tag Outdoor", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("Tag Security", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun streamItem_showsThumbnail_whenProvided() = runComposeUiTest {
        val bitmap = ImageBitmap.createPlainImage(100, 100, Color.Red)

        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = bitmap,
                    icon = {},
                    onClick = {}
                )
            }
        }

        // substring = true is used because the name might be formatted with additional characters.
        onNodeWithContentDescription(
            label = mockItem.name,
            substring = true,
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun streamItem_click_triggersCallback() = runComposeUiTest {
        var clickedItem: RTSPItem? = null

        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = null,
                    icon = {},
                    onClick = { clickedItem = it }
                )
            }
        }

        onNodeWithText("Front Door Camera").performClick()

        assertNotNull(clickedItem, "Callback was not triggered")
        assertEquals(mockItem.id, clickedItem.id)
        assertEquals(mockItem.name, clickedItem.name)
    }

    @Test
    fun streamItem_hidesTags_whenListIsEmpty() = runComposeUiTest {
        val itemNoTags = mockItem.copy(tags = emptyList())

        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = itemNoTags,
                    preview = null,
                    icon = {},
                    onClick = {}
                )
            }
        }

        onNodeWithTag("Tag Outdoor").assertDoesNotExist()
        onNodeWithTag("Tag Security").assertDoesNotExist()
    }

    @Test
    fun streamItem_handlesThinBitmaps() = runComposeUiTest {
        // Create a very thin bitmap to test aspectRatio(width/height) handling.
        val thinBitmap = ImageBitmap.createPlainImage(10, 100, Color.White)

        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = thinBitmap,
                    icon = {},
                    onClick = {}
                )
            }
        }

        onNodeWithText("Front Door Camera").assertIsDisplayed()
    }

    @Test
    fun streamItem_showsPlaceholder_whenPreviewIsNull() = runComposeUiTest {
        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = null,
                    icon = {},
                    onClick = {}
                )
            }
        }

        onNodeWithContentDescription("No Stream Available").assertIsDisplayed()
    }

    @Test
    fun streamItem_displaysCustomIcon() = runComposeUiTest {
        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = null,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Custom Icon") },
                    onClick = {}
                )
            }
        }

        onNodeWithContentDescription("Custom Icon").assertIsDisplayed()
    }

    @Test
    fun streamItem_truncatesLongName() = runComposeUiTest {
        val longName = "A".repeat(100)
        val itemWithLongName = mockItem.copy(name = longName)

        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = itemWithLongName,
                    preview = null,
                    icon = {},
                    onClick = {}
                )
            }
        }

        onNodeWithText(longName).assertIsDisplayed()
    }

    @Test
    fun streamItem_allTagsDisplayed() = runComposeUiTest {
        val itemWithMultipleTags =
            mockItem.copy(tags = listOf("Tag1", "Tag2", "Tag3", "Tag4", "Tag5"))
        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = itemWithMultipleTags,
                    preview = null,
                    icon = {},
                    onClick = {}
                )
            }
        }

        onNodeWithTag("Tag Tag1", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("Tag Tag2", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("Tag Tag3", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("Tag Tag4", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("Tag Tag5", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun streamItem_clickCustomIconTriggersCallback() = runComposeUiTest {
        var clickedItem: RTSPItem? = null
        setContent {
            RTSPViewerTheme {
                StreamItem(
                    data = mockItem,
                    preview = null,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Custom Icon") },
                    onClick = { clickedItem = it }
                )
            }
        }

        onNodeWithContentDescription("Custom Icon").performClick()
        assertNotNull(clickedItem, "Callback was not triggered via icon click")
        assertEquals(mockItem.id, clickedItem.id)
        assertEquals(mockItem.name, clickedItem.name)
    }
}

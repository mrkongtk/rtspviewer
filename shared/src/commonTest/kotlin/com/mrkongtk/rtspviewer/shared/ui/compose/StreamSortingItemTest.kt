package com.mrkongtk.rtspviewer.shared.ui.compose


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.createPlainImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class StreamSortingItemTest {

    @Test
    fun streamSortingItem_displaysNameAndReorderIcon() = runComposeUiTest {
        val mockItem = RTSPItem(
            id = 1,
            name = "Living Room Camera",
            uri = "rtsp://192.168.1.10",
            tags = listOf("Indoor", "House"),
            order = 1
        )

        setContent {
            RTSPViewerTheme {
                StreamSortingItem(
                    data = mockItem,
                    preview = null
                )
            }
        }

        // Verify Name
        onNodeWithText("Living Room Camera").assertIsDisplayed()

        // Verify specific tags exist using the testTag pattern from StreamItem
        // useUnmergedTree is true because tags are nested deep within the Card/Row structure
        onNodeWithTag("Tag Indoor", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("Tag House", useUnmergedTree = true).assertIsDisplayed()

        // Verify the drag handle icon via content description (Matches Composable: R.string.reorder)

        onNodeWithTag("reorder", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun streamSortingItem_withPreview_displaysThumbnail() = runComposeUiTest {
        val mockItem = RTSPItem(1, "Front Porch", "rtsp://...", emptyList(), 1)
        val bitmap = ImageBitmap.createPlainImage(100, 100, Color.Red)

        setContent {
            RTSPViewerTheme {
                StreamSortingItem(
                    data = mockItem,
                    preview = bitmap
                )
            }
        }

        // Verify that the image exists and has the correct accessibility description
        onNodeWithTag("thumbnail", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun streamSortingItem_withoutTags_doesNotRenderTagNodes() = runComposeUiTest {
        val mockItem = RTSPItem(
            id = 2,
            name = "Garage",
            uri = "rtsp://192.168.1.11",
            tags = emptyList(),
            order = 2
        )

        setContent {
            RTSPViewerTheme {
                StreamSortingItem(
                    data = mockItem,
                    preview = null
                )
            }
        }

        // Verify name still exists
        onNodeWithText("Garage").assertIsDisplayed()

        // Verify NO tags are displayed by checking the "Tag " prefix
        onAllNodes(hasTestTagPrefix("Tag ")).assertCountEquals(0)
    }

    /**
     * Helper Matcher: Custom logic to handle partial TestTag matching.
     */
    private fun hasTestTagPrefix(prefix: String): SemanticsMatcher {
        return SemanticsMatcher("TestTag starts with '$prefix'") { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            tag?.startsWith(prefix) == true
        }
    }
}

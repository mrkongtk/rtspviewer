package com.mrkongtk.rtspviewer.ui.compose

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.util.formatText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamSortingItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun streamSortingItem_displaysNameAndReorderIcon() {
        val mockItem = RTSPItem(
            id = 1,
            name = "Living Room Camera",
            uri = "rtsp://192.168.1.10",
            tags = listOf("Indoor", "House"),
            order = 1
        )

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamSortingItem(
                    data = mockItem,
                    preview = null
                )
            }
        }

        // Verify Name
        composeTestRule.onNodeWithText("Living Room Camera").assertIsDisplayed()

        // Verify specific tags exist using the testTag pattern from StreamItem
        // useUnmergedTree is true because tags are nested deep within the Card/Row structure
        composeTestRule.onNodeWithTag("Tag Indoor", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("Tag House", useUnmergedTree = true).assertIsDisplayed()

        // Verify the drag handle icon via content description (Matches Composable: R.string.reorder)
        val reorderDescription = context.getString(R.string.reorder)
        composeTestRule.onNodeWithContentDescription(reorderDescription).assertIsDisplayed()
    }

    @Test
    fun streamSortingItem_withPreview_displaysThumbnail() {
        val mockItem = RTSPItem(1, "Front Porch", "rtsp://...", emptyList(), 1)
        val bitmap = createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Construct the expected formatted content description used in StreamItem
        val expectedDescription = context.getString(R.string.rtsp_item_preview_description)
            .formatText(mockItem.name)

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamSortingItem(
                    data = mockItem,
                    preview = bitmap
                )
            }
        }

        // Verify that the image exists and has the correct accessibility description
        composeTestRule.onNodeWithContentDescription(expectedDescription).assertIsDisplayed()
    }

    @Test
    fun streamSortingItem_withoutTags_doesNotRenderTagNodes() {
        val mockItem = RTSPItem(
            id = 2,
            name = "Garage",
            uri = "rtsp://192.168.1.11",
            tags = emptyList(),
            order = 2
        )

        composeTestRule.setContent {
            RTSPViewerTheme {
                StreamSortingItem(
                    data = mockItem,
                    preview = null
                )
            }
        }

        // Verify name still exists
        composeTestRule.onNodeWithText("Garage").assertIsDisplayed()

        // Verify NO tags are displayed by checking the "Tag " prefix
        composeTestRule.onAllNodes(hasTestTagPrefix("Tag ")).assertCountEquals(0)
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

package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreamSortingItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun streamSortingItem_displaysNameAndSortIcon() {
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

        // Verify specific tags exist using the exact tag generated in StreamItem
        composeTestRule.onNodeWithTag("Tag Indoor", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("Tag House", useUnmergedTree = true).assertIsDisplayed()

        // Verify the drag handle icon via content description
        val sortDescription = context.getString(R.string.sort)
        composeTestRule.onNodeWithContentDescription(sortDescription).assertIsDisplayed()
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

        // To verify NO tags are displayed without knowing the specific tag names,
        // we use a custom matcher to find any node whose testTag starts with "Tag "
        composeTestRule.onAllNodes(hasTestTagPrefix("Tag ")).assertCountEquals(0)
    }

    /**
     * Helper Matcher: onNodeWithTag doesn't support partial matches.
     * This matcher finds semantics nodes where the TestTag property starts with [prefix].
     */
    private fun hasTestTagPrefix(prefix: String): SemanticsMatcher {
        return SemanticsMatcher("TestTag starts with '$prefix'") { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            tag?.startsWith(prefix) == true
        }
    }
}

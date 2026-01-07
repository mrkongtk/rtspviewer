package com.mrkongtk.rtspviewer.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * UI Test Suite for [StreamListScreen].
 *
 * Updated to support tag filtering and the latest Composable signature.
 */
class StreamListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Helper interface for Mockito verification.
     */
    interface ScreenActions {
        fun onItemSelected(item: RTSPItem)
        fun onAddItemSelected()
        fun onItemsReordered(items: List<RTSPItem>)
        fun onTagSelected(tag: String?)
    }

    private val actions: ScreenActions = mock()

    // -------------------------------------------------------------------------
    // Sample Data
    // -------------------------------------------------------------------------

    private val sampleItem1 = RTSPItem(
        id = 1L,
        name = "Front Door",
        uri = "rtsp://192.168.1.50",
        tags = listOf("Outdoor"),
        order = 0,
        forceTcp = false
    )

    private val sampleItem2 = RTSPItem(
        id = 2L,
        name = "Kitchen",
        uri = "rtsp://192.168.1.51",
        tags = listOf("Indoor"),
        order = 1,
        forceTcp = true
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun streamListScreen_whenListIsEmpty_showsEmptyStateMessage() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = emptyList(),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                onItemSelected = actions::onItemSelected,
                onAddItemSelected = actions::onAddItemSelected,
                onItemsReordered = actions::onItemsReordered,
                onTagSelected = actions::onTagSelected
            )
        }

        // Verify empty state text
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText")
            .assertIsDisplayed()

        // Verify list container does not exist
        composeTestRule.onNodeWithTag("LazyColumn")
            .assertDoesNotExist()
    }

    @Test
    fun streamListScreen_whenListHasItems_showsListAndTags() {
        val items = listOf(sampleItem1, sampleItem2)
        val tags = listOf("Outdoor", "Indoor")

        composeTestRule.setContent {
            StreamListScreen(
                itemList = items,
                previews = emptyMap(),
                tags = tags,
                selectedTag = null,
                onItemSelected = actions::onItemSelected,
                onAddItemSelected = actions::onAddItemSelected,
                onItemsReordered = actions::onItemsReordered,
                onTagSelected = actions::onTagSelected
            )
        }

        // Empty text should be gone
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText")
            .assertDoesNotExist()

        // Tag row should be visible
        composeTestRule.onNodeWithTag("Tags")
            .assertIsDisplayed()

        // List should be visible
        composeTestRule.onNodeWithTag("LazyColumn")
            .assertIsDisplayed()

        // Verify specific items
        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem1.id}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleItem1.name)
            .assertIsDisplayed()
    }

    @Test
    fun streamListScreen_whenTagIsClicked_triggersCallback() {
        val tags = listOf("Outdoor")

        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem1),
                previews = emptyMap(),
                tags = tags,
                selectedTag = null,
                onItemSelected = {},
                onAddItemSelected = {},
                onItemsReordered = {},
                onTagSelected = actions::onTagSelected
            )
        }

        // Click the specific tag (Note: The UI implementation uses "Tag $tag" as testTag)
        composeTestRule.onNodeWithTag("Tag Outdoor")
            .performClick()

        verify(actions).onTagSelected("Outdoor")
    }

    @Test
    fun streamListScreen_whenAddButtonIsClicked_triggersCallback() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = emptyList(),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                onItemSelected = {},
                onAddItemSelected = actions::onAddItemSelected,
                onItemsReordered = {},
                onTagSelected = {}
            )
        }

        composeTestRule.onNodeWithTag("AddButton")
            .performClick()

        verify(actions).onAddItemSelected()
    }

    @Test
    fun streamListScreen_whenItemIsClicked_triggersItemSelectedCallback() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem1),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                onItemSelected = actions::onItemSelected,
                onAddItemSelected = {},
                onItemsReordered = {},
                onTagSelected = {}
            )
        }

        composeTestRule.onNodeWithTag("StreamListItem: ${sampleItem1.id}")
            .performClick()

        verify(actions).onItemSelected(sampleItem1)
    }

    @Test
    fun streamListScreen_alwaysShowsAddButton() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem1),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                onItemSelected = {},
                onAddItemSelected = {},
                onItemsReordered = {},
                onTagSelected = {}
            )
        }

        composeTestRule.onNodeWithTag("AddButton")
            .assertIsDisplayed()
    }
}

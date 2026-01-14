package com.mrkongtk.rtspviewer.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.screen.action.StreamListScreenActions
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * UI Test Suite for [StreamListScreen].
 *
 * Verifies empty states, content rendering, tag filtering, and
 * the transition between navigation mode and sorting mode.
 */
class StreamListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val actions: StreamListScreenActions = mock()

    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Front Door",
        uri = "rtsp://192.168.1.50",
        tags = listOf("Outdoor"),
        order = 0,
        forceTcp = false
    )

    @Test
    fun emptyState_isDisplayed_whenListIsEmpty() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = emptyList(),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        // Verify the placeholder text is visible and the list is not
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()
        composeTestRule.onNodeWithTag("LazyColumn").assertDoesNotExist()
    }

    @Test
    fun content_isDisplayed_whenListHasItems() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = listOf("Outdoor"),
                selectedTag = null,
                screenActions = actions
            )
        }

        // Verify basic list infrastructure and the item itself
        composeTestRule.onNodeWithTag("LazyColumn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    @Test
    fun tagSelection_triggersCallback_includingAllTag() {
        val allLabel = composeTestRule.activity.getString(R.string.tags_all)

        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = listOf("Outdoor"),
                selectedTag = null,
                screenActions = actions
            )
        }

        // 1. Test clicking a specific custom tag
        composeTestRule.onNodeWithTag("Tag Outdoor").performClick()
        verify(actions).onTagSelected("Outdoor")

        // 2. Test clicking the "All" tag (index 0)
        // Note: The implementation uses "Tag $tag", so we match that pattern
        composeTestRule.onNodeWithTag("Tag $allLabel").performClick()
        verify(actions).onTagSelected(null)
    }

    @Test
    fun addButton_isAccessible_viaMoreMenu() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        // Open the dropdown menu
        composeTestRule.onNodeWithTag("MoreButton").performClick()

        // Click Add in the menu
        composeTestRule.onNodeWithTag("AddButton").assertIsDisplayed().performClick()

        verify(actions).onAddItemSelected()
    }

    @Test
    fun sortingMode_toggleLogic() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        // 1. Enter Sorting Mode via Menu
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("SortButton").performClick()

        // 2. Verify UI state change: Draggable list replaces standard list
        composeTestRule.onNodeWithTag("DraggableLazyColumn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("LazyColumn").assertDoesNotExist()
        composeTestRule.onNodeWithTag("StreamSortingItem: 1").assertIsDisplayed()

        // 3. Verify the FAB changed its identity to "EndSortingButton"
        composeTestRule.onNodeWithTag("EndSortingButton").assertIsDisplayed()

        // 4. Exit Sorting Mode
        composeTestRule.onNodeWithTag("EndSortingButton").performClick()

        // 5. Verify UI reverted to normal listing
        composeTestRule.onNodeWithTag("LazyColumn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("DraggableLazyColumn").assertDoesNotExist()
    }

    @Test
    fun itemClick_triggersNavigation() {
        composeTestRule.setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        // Click the card and verify the action is called
        composeTestRule.onNodeWithTag("StreamListItem: 1").performClick()
        verify(actions).onItemSelected(sampleItem)
    }
}

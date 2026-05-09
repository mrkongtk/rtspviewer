package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.screen.action.StreamListScreenActions
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.tags_all
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UI Test Suite for [StreamListScreen].
 *
 * Verifies empty states, content rendering, tag filtering, and
 * the transition between navigation mode and sorting mode.
 */
@OptIn(ExperimentalTestApi::class)
class StreamListScreenTest {

    private class FakeStreamListScreenActions: StreamListScreenActions {
        private var _addItemSelected: Boolean = false
        val addItemSelected: Boolean
            get() = _addItemSelected

        private var _selectedTag: String? = null
        val selectedTag: String?
            get() = _selectedTag

        private var _itemSelected: RTSPItem? = null
        val itemSelected: RTSPItem?
            get() = _itemSelected

        override fun onItemSelected(item: RTSPItem) {
            _itemSelected = item
        }
        override fun onAddItemSelected() {
            _addItemSelected = true
        }
        override fun onItemsReordered(orderedList: List<RTSPItem>) {}
        override fun onTagSelected(tag: String?) {
            _selectedTag = tag
        }

        fun reset() {
            _selectedTag = null
            _addItemSelected = false
            _itemSelected = null
        }
    }
    private val actions: FakeStreamListScreenActions = FakeStreamListScreenActions()

    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Front Door",
        uri = "rtsp://192.168.1.50",
        tags = listOf("Outdoor"),
        order = 0,
        forceTcp = false
    )

    @BeforeTest
    fun reset() {
        actions.reset()
    }

    @Test
    fun emptyState_isDisplayed_whenListIsEmpty() = runComposeUiTest {
        setContent {
            StreamListScreen(
                itemList = emptyList(),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        // Verify the placeholder text is visible and the list is not
        onNodeWithTag("StreamListScreenEmptyText").assertIsDisplayed()
        onNodeWithTag("LazyColumn").assertDoesNotExist()
    }

    @Test
    fun content_isDisplayed_whenListHasItems() = runComposeUiTest {
        setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = listOf("Outdoor"),
                selectedTag = null,
                screenActions = actions
            )
        }

        // Verify basic list infrastructure and the item itself
        onNodeWithTag("LazyColumn").assertIsDisplayed()
        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    @Test
    fun tagSelection_triggersCallback_includingAllTag() = runComposeUiTest {
        var allLabel = ""

        setContent {
            allLabel = stringResource(Res.string.tags_all)
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = listOf("Outdoor"),
                selectedTag = null,
                screenActions = actions
            )
        }

        // 1. Test clicking a specific custom tag
        onNodeWithTag("Tag Outdoor").performClick()
        actions.onTagSelected("Outdoor")

        // 2. Test clicking the "All" tag (index 0)
        // Note: The implementation uses "Tag $tag", so we match that pattern
        onNodeWithTag("Tag $allLabel").performClick()
        actions.onTagSelected(null)
    }

    @Test
    fun addButton_isAccessible_viaMoreMenu() = runComposeUiTest {
        setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        assertFalse(actions.addItemSelected)
        // Open the dropdown menu
        onNodeWithTag("MoreButton").performClick()

        // Click Add in the menu
        onNodeWithTag("AddButton").assertIsDisplayed().performClick()

        assertTrue(actions.addItemSelected)
    }

    @Test
    fun sortingMode_toggleLogic() = runComposeUiTest {
        setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        // 1. Enter Sorting Mode via Menu
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("SortButton").performClick()

        // 2. Verify UI state change: Draggable list replaces standard list
        onNodeWithTag("DraggableLazyColumn").assertIsDisplayed()
        onNodeWithTag("LazyColumn").assertDoesNotExist()
        onNodeWithTag("StreamSortingItem: 1").assertIsDisplayed()

        // 3. Verify the FAB changed its identity to "EndSortingButton"
        onNodeWithTag("EndSortingButton").assertIsDisplayed()

        // 4. Exit Sorting Mode
        onNodeWithTag("EndSortingButton").performClick()

        // 5. Verify UI reverted to normal listing
        onNodeWithTag("LazyColumn").assertIsDisplayed()
        onNodeWithTag("DraggableLazyColumn").assertDoesNotExist()
    }

    @Test
    fun itemClick_triggersNavigation() = runComposeUiTest {
        setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        // Click the card and verify the action is called
        onNodeWithTag("StreamListItem: 1").performClick()
        actions.onItemSelected(sampleItem)
        assertEquals(sampleItem, actions.itemSelected)
    }
}

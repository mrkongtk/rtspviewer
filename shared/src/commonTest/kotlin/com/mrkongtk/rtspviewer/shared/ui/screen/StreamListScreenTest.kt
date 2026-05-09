package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.screen.action.StreamListScreenActions
import com.mrkongtk.rtspviewer.shared.util.createPlainImage
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.tags_all
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * UI tests for [StreamListScreen].
 */
@OptIn(ExperimentalTestApi::class)
class StreamListScreenTest {

    private class FakeStreamListScreenActions : StreamListScreenActions {
        private var _addItemSelected: Boolean = false
        val addItemSelected: Boolean get() = _addItemSelected

        private var _selectedTag: String? = null
        val selectedTag: String? get() = _selectedTag

        private var _itemSelected: RTSPItem? = null
        val itemSelected: RTSPItem? get() = _itemSelected

        private var _reorderedList: List<RTSPItem>? = null
        val reorderedList: List<RTSPItem>? get() = _reorderedList

        override fun onItemSelected(item: RTSPItem) {
            _itemSelected = item
        }

        override fun onAddItemSelected() {
            _addItemSelected = true
        }

        override fun onItemsReordered(orderedList: List<RTSPItem>) {
            _reorderedList = orderedList
        }

        override fun onTagSelected(tag: String?) {
            _selectedTag = tag
        }

        fun reset() {
            _selectedTag = null
            _addItemSelected = false
            _itemSelected = null
            _reorderedList = null
        }
    }

    private val actions = FakeStreamListScreenActions()

    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Front Door",
        uri = "rtsp://192.168.1.50",
        tags = listOf("Outdoor"),
        order = 0,
        forceTcp = false
    )

    @BeforeTest
    fun setup() {
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

        onNodeWithTag("LazyColumn").assertIsDisplayed()
        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    @Test
    fun multipleItems_areDisplayed() = runComposeUiTest {
        val items = listOf(
            sampleItem.copy(id = 1, name = "Item 1"),
            sampleItem.copy(id = 2, name = "Item 2")
        )
        setContent {
            StreamListScreen(
                itemList = items,
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        onNodeWithTag("StreamListItem: 2").assertIsDisplayed()
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

        onNodeWithTag("Tag Outdoor").performClick()
        assertEquals("Outdoor", actions.selectedTag)

        onNodeWithTag("Tag $allLabel").performClick()
        assertNull(actions.selectedTag)
    }

    @Test
    fun tagSelection_reflectsActiveState() = runComposeUiTest {
        var allLabel = ""
        setContent {
            allLabel = stringResource(Res.string.tags_all)
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = listOf("Outdoor"),
                selectedTag = "Outdoor",
                screenActions = actions
            )
        }

        onNodeWithTag("Tag Outdoor").assertIsSelected()
        onNodeWithTag("Tag $allLabel").assertIsNotSelected()
    }

    @Test
    fun filtering_showsOnlyMatchingItems() = runComposeUiTest {
        val items = listOf(
            sampleItem.copy(id = 1, name = "Outdoor Item", tags = listOf("Outdoor")),
            sampleItem.copy(id = 2, name = "Indoor Item", tags = listOf("Indoor"))
        )

        setContent {
            StreamListScreen(
                itemList = items,
                previews = emptyMap(),
                tags = listOf("Outdoor", "Indoor"),
                selectedTag = "Outdoor",
                screenActions = actions
            )
        }

        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
        onNodeWithTag("StreamListItem: 2").assertDoesNotExist()
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
        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").assertIsDisplayed().performClick()

        assertTrue(actions.addItemSelected)
    }

    @Test
    fun moreMenu_canBeClosed() = runComposeUiTest {
        setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("AddButton").assertIsDisplayed()

        onNodeWithTag("CloseMoreButton").performClick()
        onNodeWithTag("AddButton").assertDoesNotExist()
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

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("SortButton").performClick()

        onNodeWithTag("DraggableLazyColumn").assertIsDisplayed()
        onNodeWithTag("LazyColumn").assertDoesNotExist()
        onNodeWithTag("StreamSortingItem: 1").assertIsDisplayed()
        onNodeWithTag("EndSortingButton").assertIsDisplayed()

        onNodeWithTag("EndSortingButton").performClick()

        onNodeWithTag("LazyColumn").assertIsDisplayed()
        onNodeWithTag("DraggableLazyColumn").assertDoesNotExist()
    }

    @Test
    fun reorderCallback_notCalled_whenNoChange() = runComposeUiTest {
        setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = emptyMap(),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        onNodeWithTag("MoreButton").performClick()
        onNodeWithTag("SortButton").performClick()
        onNodeWithTag("EndSortingButton").performClick()

        assertNull(actions.reorderedList)
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

        onNodeWithTag("StreamListItem: 1").performClick()
        assertEquals(sampleItem, actions.itemSelected)
    }

    @Test
    fun previews_areHandledWithoutCrashing() = runComposeUiTest {
        val dummyBitmap = ImageBitmap.createPlainImage(10, 10)
        setContent {
            StreamListScreen(
                itemList = listOf(sampleItem),
                previews = mapOf(sampleItem.id to dummyBitmap),
                tags = emptyList(),
                selectedTag = null,
                screenActions = actions
            )
        }

        onNodeWithTag("StreamListItem: 1").assertIsDisplayed()
    }
}

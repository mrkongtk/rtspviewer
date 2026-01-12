package com.mrkongtk.rtspviewer.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.screen.action.StreamListScreenActions
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * UI Test Suite for [StreamListScreen].
 *
 * This test verifies the Unidirectional Data Flow (UDF) by ensuring UI interactions
 * correctly trigger methods in the [StreamListScreenActions] interface.
 */
class StreamListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Mock the actions interface
    private val actions: StreamListScreenActions = mock()

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
                screenActions = actions
            )
        }

        // Verify empty state text defined in the Composable
        composeTestRule.onNodeWithTag("StreamListScreenEmptyText")
            .assertIsDisplayed()

        // Verify list container does not exist when empty
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
                screenActions = actions
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

        // Verify specific item is rendered
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
                screenActions = actions
            )
        }

        // The UI prepends "All" to the tags list.
        // We verify that clicking the "Outdoor" tag chip triggers the action.
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
                screenActions = actions
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
                screenActions = actions
            )
        }

        // Click the specific item card
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
                screenActions = actions
            )
        }

        // The FAB (Add Button) should be visible regardless of list content
        composeTestRule.onNodeWithTag("AddButton")
            .assertIsDisplayed()
    }
}

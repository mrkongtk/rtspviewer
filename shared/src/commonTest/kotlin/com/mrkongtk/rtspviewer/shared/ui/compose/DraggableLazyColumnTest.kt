package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * UI tests for [DraggableLazyColumn].
 *
 * These tests validate the drag-and-drop reordering logic by simulating long-press
 * gestures and vertical drags.
 *
 * **Note on Implementation:** To make touch coordinate calculations predictable
 * and robust across different screen densities/sizes in tests, we use a fixed
 * [itemHeightDp] for all items.
 */
@OptIn(ExperimentalTestApi::class)
class DraggableLazyColumnTest {

    private val itemHeightDp = 50.dp

    @Test
    fun testInitialRendering() = runComposeUiTest {
        val items = listOf("Item A", "Item B", "Item C")

        setContent {
            DraggableLazyColumn(
                items = items,
                onReordered = {},
                itemContent = { _, item ->
                    Text(text = item)
                }
            )
        }

        // Verify that all items are initially composed and visible
        items.forEach { item ->
            onNodeWithText(item).assertIsDisplayed()
        }
    }

    /**
     * Verifies that dragging an item down past its neighbor correctly triggers the reorder callback.
     */
    @Test
    fun testDragToReorder_Down() = runComposeUiTest {
        val initialItems = listOf("1", "2", "3")
        var reorderedItems = listOf<String>()

        setContent {
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { reorderedItems = it },
                itemKey = { _, item -> item },
                itemContent = { modifier, item ->
                    Text(
                        text = item,
                        modifier = modifier.height(itemHeightDp)
                    )
                }
            )
        }

        // Action: Drag Item "1" (index 0) down past Item "2" (index 1)
        onNodeWithText("1").performTouchInput {
            // 1. Long press to initiate the drag state
            down(center)
            advanceEventTime(1000L)

            // 2. Drag down.
            // We move 1.5x the height + a small buffer to ensure the center of the
            // dragged item passes the threshold of the next item.
            val dragDistance = height * 1.5f + 2.dp.toPx()
            moveBy(Offset(0f, dragDistance))

            // 3. Release to commit the move
            up()
        }

        waitForIdle()

        // Expected: ["2", "1", "3"]
        assertEquals(listOf("2", "1", "3"), reorderedItems)
    }

    /**
     * Verifies that dragging an item across multiple positions downwards works correctly.
     */
    @Test
    fun testDragToReorder_MultipleDown() = runComposeUiTest {
        val initialItems = listOf("1", "2", "3", "4")
        var reorderedItems = listOf<String>()

        setContent {
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { reorderedItems = it },
                itemKey = { _, item -> item },
                itemContent = { modifier, item ->
                    Text(text = item, modifier = modifier.height(itemHeightDp))
                }
            )
        }

        // Action: Drag Item "1" (index 0) to index 2 (past "2" and "3")
        onNodeWithText("1").performTouchInput {
            down(center)
            advanceEventTime(1000L)

            val currentHeight = height.toFloat()

            // Perform drag in increments to simulate natural movement across multiple items
            moveBy(Offset(0f, currentHeight)) // Past "2"
            moveBy(Offset(0f, currentHeight)) // Past "3"
            moveBy(Offset(0f, currentHeight * 0.6f)) // Buffer past the target center

            up()
        }

        waitForIdle()

        // Expected result: ["2", "3", "1", "4"]
        assertEquals(listOf("2", "3", "1", "4"), reorderedItems)
    }

    /**
     * Verifies that dragging an item up past its neighbor correctly triggers the reorder callback.
     */
    @Test
    fun testDragToReorder_Up() = runComposeUiTest {
        val initialItems = listOf("A", "B", "C")
        var reorderedItems = listOf<String>()

        setContent {
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { reorderedItems = it },
                itemKey = { _, item -> item },
                itemContent = { modifier, item ->
                    Text(
                        text = item,
                        modifier = modifier
                            .height(itemHeightDp)
                            .background(Color.White)
                    )
                }
            )
        }

        // Action: Drag Item "C" (index 2) up past Item "B" (index 1)
        onNodeWithText("C").performTouchInput {
            down(center)
            advanceEventTime(1000L)

            // Drag UP (Negative Y offset)
            val dragDistance = -(height * 1.5f + 2.dp.toPx())
            moveBy(Offset(0f, dragDistance))

            up()
        }

        waitForIdle()

        // Expected result: ["A", "C", "B"]
        assertEquals(listOf("A", "C", "B"), reorderedItems)
    }

    /**
     * Verifies that dragging an item across multiple positions upwards works correctly.
     */
    @Test
    fun testDragToReorder_MultipleUp() = runComposeUiTest {
        val initialItems = listOf("A", "B", "C", "D")
        var reorderedItems = listOf<String>()

        setContent {
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { reorderedItems = it },
                itemKey = { _, item -> item },
                itemContent = { modifier, item ->
                    Text(
                        text = item,
                        modifier = modifier.height(itemHeightDp)
                    )
                }
            )
        }

        // Action: Drag Item "D" (index 3) to index 1 (past "C" and "B")
        onNodeWithText("D").performTouchInput {
            down(center)
            advanceEventTime(1000L)

            val currentHeight = height.toFloat()

            // Move upwards in increments
            moveBy(Offset(0f, -currentHeight)) // Past "C"
            moveBy(Offset(0f, -currentHeight)) // Past "B"
            moveBy(Offset(0f, -currentHeight * 0.6f))

            up()
        }

        waitForIdle()

        // Expected result: ["A", "D", "B", "C"]
        assertEquals(listOf("A", "D", "B", "C"), reorderedItems)
    }

    /**
     * Verifies that if a drag gesture is cancelled (e.g., by system interruption),
     * the list order remains unchanged and the callback is not triggered.
     */
    @Test
    fun testDragAndCancel_ShouldNotReorder() = runComposeUiTest {
        val initialItems = listOf("X", "Y", "Z")
        var callbackCalled = false

        setContent {
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { callbackCalled = true },
                itemContent = { modifier, item ->
                    Text(item, modifier.height(itemHeightDp))
                }
            )
        }

        onNodeWithText("X").performTouchInput {
            down(center)
            advanceEventTime(1000L)

            // Drag it so it *visually* overlaps with neighbors
            moveBy(Offset(0f, height * 1.5f + 2.dp.toPx()))

            // Simulate a gesture cancellation rather than a normal release (up)
            cancel()
        }

        waitForIdle()

        // Ensure the reordering logic was aborted
        assertEquals(false, callbackCalled, "Callback should not be called on drag cancel")
    }
}

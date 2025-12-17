package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class DraggableLazyColumnTest {

    @get:Rule
    val rule = createComposeRule()

    // We use a fixed height for items to make drag-distance calculations predictable in tests
    private val itemHeightDp = 50.dp

    @Test
    fun testInitialRendering() {
        val items = listOf("Item A", "Item B", "Item C")

        rule.setContent {
            DraggableLazyColumn(
                items = items,
                onReordered = {},
                itemContent = { _, item ->
                    Text(text = item)
                }
            )
        }

        // Assert all items are displayed
        items.forEach { item ->
            rule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun testDragToReorder_Down() {
        val initialItems = listOf("1", "2", "3")
        var reorderedItems = listOf<String>()

        rule.setContent {
            // Get density for pixel conversions if needed, though usually automatic in touch input
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { reorderedItems = it },
                // Add a test tag to find the specific rows easily
                itemKey = { _, item -> item },
                itemContent = { modifier, item ->
                    Box(
                        modifier = modifier
                            .height(itemHeightDp) // Fixed height is crucial for test math
                            .background(Color.LightGray)
                    ) {
                        Text(text = item)
                    }
                }
            )
        }

        // Logic:
        // 1. Find Item "1" (Index 0).
        // 2. Drag it DOWN past Item "2" (Index 1).
        // 3. Item "1" should end up at Index 1.
        // 4. Expected result: ["2", "1", "3"]

        rule.onNodeWithText("1").performTouchInput {
            // 1. Long press to activate drag
            down(center)
            advanceEventTime(1000L) // Wait longer than long-press timeout

            // 2. Drag down
            // We need to move enough pixels to cross the middle of the item below.
            // Since item height is 50.dp, moving ~1.6x height ensures a swap.
            // Note: coordinates in performTouchInput are in pixels.
            val dragDistance = height * 1.5f + 2.dp.toPx()
            moveBy(Offset(0f, dragDistance))

            // 3. Release
            up()
        }

        // Wait for Compose to settle (animations etc)
        rule.waitForIdle()

        // Assert the callback was called with the new order
        assertEquals(listOf("2", "1", "3"), reorderedItems)
    }

    @Test
    fun testDragToReorder_Up() {
        val initialItems = listOf("A", "B", "C")
        var reorderedItems = listOf<String>()

        rule.setContent {
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { reorderedItems = it },
                itemKey = { _, item -> item },
                itemContent = { modifier, item ->
                    Box(
                        modifier = modifier
                            .height(itemHeightDp)
                            .background(Color.White)
                    ) {
                        Text(text = item)
                    }
                }
            )
        }

        // Logic:
        // 1. Find Item "C" (Index 2).
        // 2. Drag it UP past Item "B" (Index 1).
        // 3. Expected result: ["A", "C", "B"]

        rule.onNodeWithText("C").performTouchInput {
            down(center)
            advanceEventTime(1000L) // Trigger Long Press

            // Drag UP (Negative Y)
            val dragDistance = -(height * 1.5f + 2.dp.toPx())
            moveBy(Offset(0f, dragDistance))

            up()
        }

        rule.waitForIdle()

        assertEquals(listOf("A", "C", "B"), reorderedItems)
    }

    @Test
    fun testDragAndCancel_ShouldNotReorder() {
        val initialItems = listOf("X", "Y", "Z")
        var callbackCalled = false

        rule.setContent {
            DraggableLazyColumn(
                items = initialItems,
                onReordered = { callbackCalled = true },
                itemContent = { modifier, item ->
                    Box(modifier.height(itemHeightDp)) { Text(item) }
                }
            )
        }

        rule.onNodeWithText("X").performTouchInput {
            down(center)
            advanceEventTime(1000L)
            moveBy(Offset(0f, height * 1.5f + 2.dp.toPx())) // Drag it so it *visually* swaps
            cancel() // Simulating the system cancelling the touch (e.g., incoming call or parent scroll takeover)
        }

        rule.waitForIdle()

        // The logic in your code resets draggingItemIndex onCancel,
        // BUT it does NOT trigger onReordered.
        assertEquals(false, callbackCalled)
    }
}

package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A wrapper around [LazyColumn] that provides drag-and-drop reordering functionality.
 *
 * @param items The list of data items to display.
 * @param onReordered Callback invoked when the user finishes dragging. Returns the new ordered list.
 * @param itemKey Unique key for each item. Crucial for correct animation and state preservation during reordering.
 * @param itemContent The composable content for each list item.
 */
@Composable
fun <T> DraggableLazyColumn(
    modifier: Modifier = Modifier,
    items: List<T>,
    itemKey: ((index: Int, item: T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    onReordered: (List<T>) -> Unit,
    itemContent: @Composable (Modifier, T) -> Unit,
) {
    // create a local mutable copy of the items to allow immediate UI updates
    // while dragging, before syncing back to the source of truth.
    val items = remember(items) { items.toMutableStateList() }

    val listState = rememberLazyListState()

    // Tracks the index of the item currently being dragged (-1 means no drag active)
    var draggingItemIndex by remember { mutableIntStateOf(-1) }

    // Tracks the vertical pixel displacement of the dragged item relative to its original slot
    var draggingItemOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect
    ) {

        itemsIndexed(
            items = items,
            // Optimization: Using a stable ID helps Compose identify items
            // strictly for smoother reordering animations and less flickering.
            key = itemKey
        ) { index, item ->

            // Check if this specific item is the one currently being held
            val isDragging = index == draggingItemIndex

            // Animate elevation to give the "lifted" visual effect
            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

            val modifier: Modifier = Modifier
                // 1. Placement Animation:
                // This modifier makes NON-dragged items slide smoothly into their new positions
                // when the list order changes.
                .animateItem()

                // 2. Visual Transformations (The Dragged Item):
                // We apply translation and scaling here. Note that translationY moves the *pixels*
                // but does not change the item's physical layout position in the column.
                .graphicsLayer {
                    translationY = if (isDragging) draggingItemOffset else 0f
                    scaleX = if (isDragging) 1.05f else 1f // Slight pop effect
                    scaleY = if (isDragging) 1.05f else 1f
                    shadowElevation = elevation.toPx()
                    // Increase Z-Index to ensure the dragged item floats above others
                }
                .zIndex(if (isDragging) 1f else 0f)

                // 3. Gesture Detection:
                // Handles the long-press and subsequent drag movements.
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingItemIndex = index
                            draggingItemOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggingItemOffset += dragAmount.y

                            // --- REORDERING LOGIC --- //

                            // Find the LayoutInfo for the item currently being dragged
                            val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == draggingItemIndex }

                            if (currentItemInfo != null) {
                                // Calculate the absolute visual bounds of the dragged item
                                val currentBottom =
                                    currentItemInfo.offset + currentItemInfo.size + draggingItemOffset
                                val currentTop = currentItemInfo.offset + draggingItemOffset

                                // Find the adjacent items in the visible viewport
                                val itemBelow = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == draggingItemIndex + 1 }

                                val itemAbove = listState.layoutInfo.visibleItemsInfo
                                    .lastOrNull { it.index == draggingItemIndex - 1 }

                                // Check if we should swap DOWN
                                if (itemBelow != null && currentBottom > itemBelow.offset.toFloat() + (itemBelow.size.toFloat() / 2f)) {
                                    // Move data in the list
                                    items.add(
                                        draggingItemIndex + 1,
                                        items.removeAt(draggingItemIndex)
                                    )
                                    draggingItemIndex += 1

                                    // Offset Correction:
                                    // When the item physically moves down one slot in the list, its
                                    // layout position changes. We must subtract that distance from the
                                    // visual offset so the item stays under the user's finger.
                                    draggingItemOffset -= itemBelow.size
                                }
                                // Check if we should swap UP
                                else if (itemAbove != null && currentTop < itemAbove.offset.toFloat() + (itemAbove.size.toFloat() / 2f)) {
                                    // Move data in the list
                                    items.add(
                                        draggingItemIndex - 1,
                                        items.removeAt(draggingItemIndex)
                                    )
                                    draggingItemIndex -= 1

                                    // Offset Correction:
                                    // When moving up, we add the size of the item we just jumped over.
                                    draggingItemOffset += itemAbove.size
                                }
                            }
                        },
                        onDragEnd = {
                            draggingItemIndex = -1
                            draggingItemOffset = 0f
                            // Persist the final order via callback
                            onReordered(items.toList())
                        },
                        onDragCancel = {
                            draggingItemIndex = -1
                            draggingItemOffset = 0f
                        }
                    )
                }

            itemContent(modifier, item)
        }
    }
}

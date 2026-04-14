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
import androidx.compose.runtime.rememberUpdatedState
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
 * @param T The type of items in the list.
 * @param modifier The modifier to be applied to the layout.
 * @param items The list of data items to display.
 * @param itemKey Unique key for each item. Crucial for correct animation and state preservation during reordering.
 * @param contentPadding A padding around the whole content.
 * @param reverseLayout Reverse the direction of scrolling and layout.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalAlignment The horizontal alignment of the layout's children.
 * @param flingBehavior Logic describing fling behavior.
 * @param userScrollEnabled Whether the scrolling via the user gestures or accessibility actions is enabled.
 * @param overscrollEffect The overscroll effect to use.
 * @param onReordered Callback invoked when the user finishes dragging. Returns the new ordered list.
 * @param itemContent The composable content for each list item. Receives a [Modifier] that must be
 * applied to the root element of the item to enable drag-and-drop.
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
    // Maintain a local mutable copy of the items to allow immediate UI updates
    // while dragging, before syncing back to the source of truth via onReordered.
    val internalItems = remember(items) { items.toMutableStateList() }

    val listState = rememberLazyListState()

    // Index of the item currently being dragged. -1 indicates no active drag.
    var draggingItemIndex by remember { mutableIntStateOf(-1) }

    // Vertical displacement of the dragged item relative to its original position.
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
            items = internalItems,
            key = itemKey
        ) { index, item ->
            // Use rememberUpdatedState to ensure the gesture handler uses the most recent index.
            val currentIndex by rememberUpdatedState(index)
            val isDragging = index == draggingItemIndex

            // Animate elevation to provide visual feedback when an item is "lifted".
            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

            val itemModifier = Modifier
                // 1. Reordering Animation:
                // Smoothly animates non-dragged items to their new positions.
                .animateItem()
                // 2. Visual Feedback for the Dragged Item:
                // Applies translation, scaling, and elevation.
                .graphicsLayer {
                    translationY = if (isDragging) draggingItemOffset else 0f
                    scaleX = if (isDragging) 1.05f else 1f
                    scaleY = if (isDragging) 1.05f else 1f
                    shadowElevation = elevation.toPx()
                }
                // Ensure the dragged item stays on top of others.
                .zIndex(if (isDragging) 1f else 0f)
                // 3. Gesture Detection:
                // Handles long-press to start dragging and subsequent movement.
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingItemIndex = currentIndex
                            draggingItemOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggingItemOffset += dragAmount.y

                            // Dynamic reordering logic:
                            // Determines if the dragged item has moved far enough to swap with a neighbor.
                            val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == draggingItemIndex }

                            currentItemInfo?.let { info ->
                                if (draggingItemOffset > 0) { // Dragging down
                                    val currentOffset = info.offset + info.size + draggingItemOffset
                                    val targetIndex =
                                        listState.layoutInfo.visibleItemsInfo.indexOfLast {
                                            it.offset + it.size / 2f < currentOffset
                                        }
                                    if (draggingItemIndex != targetIndex && targetIndex >= 0) {
                                        draggingItemOffset -= listState.layoutInfo.visibleItemsInfo[targetIndex].size
                                        internalItems.add(
                                            targetIndex,
                                            internalItems.removeAt(draggingItemIndex)
                                        )
                                        draggingItemIndex = targetIndex
                                    }
                                } else if (draggingItemOffset < 0) { // Dragging up
                                    val currentOffset = info.offset + draggingItemOffset
                                    val targetIndex =
                                        listState.layoutInfo.visibleItemsInfo.indexOfFirst {
                                            it.offset + it.size / 2f > currentOffset
                                        }
                                    if (draggingItemIndex != targetIndex && targetIndex >= 0) {
                                        draggingItemOffset += listState.layoutInfo.visibleItemsInfo[targetIndex].size
                                        internalItems.add(
                                            targetIndex,
                                            internalItems.removeAt(draggingItemIndex)
                                        )
                                        draggingItemIndex = targetIndex
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            draggingItemIndex = -1
                            draggingItemOffset = 0f
                            onReordered(internalItems.toList())
                        },
                        onDragCancel = {
                            draggingItemIndex = -1
                            draggingItemOffset = 0f
                        }
                    )
                }

            itemContent(itemModifier, item)
        }
    }
}

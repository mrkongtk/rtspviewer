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

            // Capture current index in a state to avoid closure capture issues during reorders
            val currentIndex by rememberUpdatedState(index)

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
                            draggingItemIndex = currentIndex
                            draggingItemOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggingItemOffset += dragAmount.y

                            // --- REORDERING LOGIC --- //

                            val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == draggingItemIndex }

                            currentItemInfo?.let {
                                if (draggingItemOffset > 0) {
                                    val currentOffset =
                                        it.offset + it.size.toFloat() + draggingItemOffset
                                    val lastIndex =
                                        listState.layoutInfo.visibleItemsInfo.indexOfLast { item ->
                                            item.offset + item.size.toFloat() / 2f < currentOffset
                                        }
                                    if (draggingItemIndex != lastIndex && lastIndex >= 0) {
                                        draggingItemOffset -= listState.layoutInfo.visibleItemsInfo[lastIndex].size
                                        items.add(lastIndex, items.removeAt(draggingItemIndex))
                                        draggingItemIndex = lastIndex
                                    }
                                } else if (draggingItemOffset < 0) {
                                    val currentOffset = it.offset + draggingItemOffset
                                    val firstIndex =
                                        listState.layoutInfo.visibleItemsInfo.indexOfFirst { item ->
                                            item.offset + item.size.toFloat() / 2f > currentOffset
                                        }
                                    if (draggingItemIndex != firstIndex && firstIndex >= 0) {
                                        draggingItemOffset += listState.layoutInfo.visibleItemsInfo[firstIndex].size
                                        items.add(firstIndex, items.removeAt(draggingItemIndex))
                                        draggingItemIndex = firstIndex
                                    }
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

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
 * A custom [LazyColumn] that enables manual reordering of items via long-press and drag.
 *
 * This component handles the complex gesture logic and visual state changes required for
 * an intuitive drag-and-drop experience. It provides real-time visual feedback by scaling
 * and elevating the dragged item while automatically animating the displacement of other items.
 *
 * @param T The type of data items.
 * @param modifier The [Modifier] to be applied to the underlying [LazyColumn].
 * @param items The initial list of items to display.
 * @param itemKey A factory to provide a stable and unique key for each item. **Highly recommended**
 * for correct reordering animations and scroll state preservation.
 * @param contentPadding Padding around the whole content of the list.
 * @param reverseLayout When true, items are laid out in reverse order.
 * @param verticalArrangement The vertical arrangement of the layout's children.
 * @param horizontalAlignment The horizontal alignment of the layout's children.
 * @param flingBehavior The fling behavior to be used for scrolling.
 * @param userScrollEnabled Whether scrolling via gestures is enabled.
 * @param overscrollEffect The overscroll effect to use.
 * @param onReordered Callback triggered when a drag gesture completes and the list order has changed.
 * @param itemContent The composable UI for each item. It receives a [Modifier] that **must**
 * be applied to the root element of the item to enable reordering functionality.
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
    // Keep a local mutable state of items to provide immediate visual feedback during the drag process.
    val internalItems = remember(items) { items.toMutableStateList() }
    val listState = rememberLazyListState()

    // State to track the index and the current vertical displacement of the item being dragged.
    var draggingItemIndex by remember { mutableIntStateOf(-1) }
    var draggingItemOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled && draggingItemIndex == -1,
        overscrollEffect = overscrollEffect
    ) {
        itemsIndexed(
            items = internalItems,
            key = itemKey
        ) { index, item ->
            val currentIndex by rememberUpdatedState(index)
            val isDragging = index == draggingItemIndex

            // Smoothly animate elevation when the item is picked up or dropped.
            val elevation by animateDpAsState(
                targetValue = if (isDragging) 8.dp else 0.dp,
                label = "DraggableItemElevation"
            )

            val itemModifier = Modifier
                // 1. Position Animation: Handles the smooth sliding of items not being dragged.
                .animateItem()
                // 2. Drag Transformation: Applies visual offsets and scale to the active item.
                .graphicsLayer {
                    translationY = if (isDragging) draggingItemOffset else 0f
                    scaleX = if (isDragging) 1.05f else 1f
                    scaleY = if (isDragging) 1.05f else 1f
                    shadowElevation = elevation.toPx()
                }
                // 3. Layering: Ensure the dragged item is rendered above all other items.
                .zIndex(if (isDragging) 1f else 0f)
                // 4. Gesture Interaction: Detects long-press to initiate and track the drag.
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingItemIndex = currentIndex
                            draggingItemOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggingItemOffset += dragAmount.y
                        },
                        onDragEnd = {
                            // Calculate the new position based on the final gesture offset.
                            val currentItemInfo = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == draggingItemIndex }

                            currentItemInfo?.let { info ->
                                val currentOffset = info.offset + draggingItemOffset

                                // Determine the target index by checking where the item was dropped
                                // relative to the current layout positions of visible items.
                                val targetIndex = if (draggingItemOffset > 0) {
                                    listState.layoutInfo.visibleItemsInfo.indexOfLast {
                                        it.offset < currentOffset && it.index > draggingItemIndex
                                    }
                                } else if (draggingItemOffset < 0) {
                                    listState.layoutInfo.visibleItemsInfo.indexOfFirst {
                                        it.offset > currentOffset && it.index < draggingItemIndex
                                    }
                                } else {
                                    -1
                                }

                                // Update the internal state if the item was moved to a different position.
                                if (targetIndex >= 0 && draggingItemIndex != targetIndex) {
                                    internalItems.add(
                                        targetIndex,
                                        internalItems.removeAt(draggingItemIndex)
                                    )
                                }
                            }

                            // Cleanup state and notify the parent of the final reordered list.
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

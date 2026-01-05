package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.createBitmap
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.DraggableLazyColumn
import com.mrkongtk.rtspviewer.ui.compose.StreamListItem
import com.mrkongtk.rtspviewer.ui.theme.ErrorColor
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * Screen responsible for displaying and managing the list of RTSP streams.
 *
 * This composable handles three main UI states/layers:
 * 1. **Empty State:** Shows a placeholder text when [itemList] is empty.
 * 2. **List State:** Shows a reorderable list of streams via [DraggableLazyColumn].
 * 3. **FAB Overlay:** An overlay button to trigger the stream creation flow.
 *
 * @param itemList The current list of [RTSPItem]s to be rendered.
 * @param previews A map associating [RTSPItem.id] (Long) with its snapshot [Bitmap].
 *                 Used to display thumbnails within the list items.
 * @param onItemSelected Callback triggered when a list item is clicked.
 * @param onAddItemSelected Callback triggered when the Floating Action Button is clicked.
 * @param onItemsReordered Callback triggered when items are dragged and dropped.
 *                         Returns a new list where [RTSPItem.order] has been updated
 *                         to match the new list index.
 * @param modifier Modifier to be applied to the root layout.
 */
@Composable
fun StreamListScreen(
    itemList: List<RTSPItem>,
    previews: Map<Long, Bitmap>,
    onItemSelected: (RTSPItem) -> Unit,
    onAddItemSelected: () -> Unit,
    onItemsReordered: (List<RTSPItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 1. Content Layer: Switch between Empty State and List State
    if (itemList.isEmpty()) {
        // Render Empty State: Centered placeholder text
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.no_streaming_items),
                    modifier = Modifier.testTag("StreamListScreenEmptyText")
                )
            }
        }
    } else {
        // Render List State: Reorderable list
        DraggableLazyColumn(
            modifier = modifier
                .testTag("StreamListScreenListRoot")
                .fillMaxSize()
                .padding(vertical = PaddingM),
            verticalArrangement = Arrangement.spacedBy(PaddingM),
            items = itemList,
            onReordered = { reorderedList ->
                // Normalization Logic:
                // When items are dropped, we assume the list order has changed physically.
                // We map through the new list and update the 'order' property of each item
                // to match its new index, ensuring persistence consistency.
                val updatedOrderList = reorderedList.mapIndexed { index, item ->
                    item.copy(order = index)
                }
                onItemsReordered(updatedOrderList)
            }
        ) { modifier, item ->
            StreamListItem(
                data = item,
                preview = previews[item.id],
                modifier = modifier
                    .testTag("StreamListItem: ${item.id}")
                    .fillMaxWidth()
                    .padding(horizontal = PaddingM),
                onClick = { data -> onItemSelected(data) }
            )
        }
    }

    // 2. Overlay Layer: Floating Action Button (FAB)
    // We use a Box here to ensure the FAB floats on top (z-index) of either
    // the Empty State or the List State, regardless of scroll position.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingM),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            IconButton(
                modifier = Modifier.testTag("AddButton"),
                onClick = { onAddItemSelected() },
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_rtsp_button)
                )
            }
        }
    }
}

/**
 * Preview provider for [StreamListScreen].
 *
 * Validates the layout in both Light and Dark modes with mock data to ensure
 * text visibility and correct item spacing.
 */
@Preview(
    name = "Day",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun StreamListScreenPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Mock data representing a typical populated database state
            val mockItems = listOf(
                RTSPItem(
                    id = 1,
                    name = "Living Room Camera",
                    uri = "rtsp://192.168.1.10",
                    tags = emptyList(),
                    order = 1
                ),
                RTSPItem(
                    id = 2,
                    name = "Backyard",
                    uri = "rtsp://192.168.1.11",
                    tags = emptyList(),
                    order = 2
                )
            )

            val w = 1920
            val h = 1080
            val bmp = createBitmap(w, h).let {
                val canvas = Canvas(it)
                // Draw a solid color (ErrorColor) onto the canvas to visualize the bitmap
                canvas.drawColor(ErrorColor.toArgb())
                it
            }
            val mockPreviews = mapOf(
                Pair(1L, bmp)
            )

            StreamListScreen(
                itemList = mockItems,
                previews = mockPreviews,
                onItemSelected = {},
                onAddItemSelected = {},
                onItemsReordered = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

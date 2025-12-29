package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.DraggableLazyColumn
import com.mrkongtk.rtspviewer.ui.compose.StreamListItem
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * Screen responsible for displaying and managing the list of RTSP streams.
 *
 * This composable handles three main states/layers:
 * 1. **Empty State:** Shows a placeholder when [itemList] is empty.
 * 2. **List State:** Shows a reorderable list of streams.
 * 3. **FAB Overlay:** An overlay button to trigger stream creation.
 *
 * @param itemList The current list of [RTSPItem]s to be rendered.
 * @param onItemSelected Callback triggered when a list item is clicked.
 * @param onAddItemSelected Callback triggered when the Floating Action Button is clicked.
 * @param onItemsReordered Callback triggered when items are dragged and dropped.
 *                         Returns a new list with updated [RTSPItem.order] values.
 * @param modifier Modifier to be applied to the root layout.
 */
@Composable
fun StreamListScreen(
    itemList: List<RTSPItem>,
    onItemSelected: (RTSPItem) -> Unit,
    onAddItemSelected: () -> Unit,
    onItemsReordered: (List<RTSPItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Determine which content to show based on data availability
    if (itemList.isEmpty()) {
        // Empty State: Centered placeholder text
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
        // List State: Reorderable list
        DraggableLazyColumn(
            modifier = modifier
                .testTag("StreamListScreenListRoot")
                .fillMaxSize()
                .padding(vertical = PaddingM),
            verticalArrangement = Arrangement.spacedBy(PaddingM),
            items = itemList,
            onReordered = { reorderedList ->
                // When items are dropped, regenerate the list with updated 'order' properties
                // based on their new index in the list.
                val updatedOrderList = reorderedList.mapIndexed { index, item ->
                    item.copy(order = index)
                }
                onItemsReordered(updatedOrderList)
            }
        ) { modifier, item ->
            StreamListItem(
                data = item,
                modifier = modifier
                    .testTag("StreamListItem: ${item.id}")
                    .fillMaxWidth()
                    .padding(horizontal = PaddingM),
                onClick = { data -> onItemSelected(data) }
            )
        }
    }

    // Overlay: Floating Action Button (FAB)
    // Uses a Box to ensure the FAB floats above the list/empty content regardless of scroll state.
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
 * Validates the layout in both Light and Dark modes with mock data.
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
            // Mock data to simulate a populated list
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

            StreamListScreen(
                itemList = mockItems,
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

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.StreamListItem
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * The main screen responsible for displaying a list of configured RTSP streams.
 *
 * This Composable is stateless (UI only) and renders based on the provided [itemList].
 * It handles the following UI states:
 * 1. **Empty State**: Displays a placeholder message when the list is empty.
 * 2. **Content State**: Displays a scrollable list of streams.
 * 3. **Action Overlay**: Always displays a floating button to add new items.
 *
 * @param itemList The current list of RTSP items to display.
 * @param onItemSelected Callback triggered when a user taps on a specific [RTSPItem].
 * @param onAddItemSelected Callback triggered when the "Add" (Floating Action) button is clicked.
 * @param modifier The modifier to apply to the container of this screen.
 */
@Composable
fun StreamListScreen(
    itemList: List<RTSPItem>,
    onItemSelected: (RTSPItem) -> Unit,
    onAddItemSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // -------------------------------------------------------------
    // Content Layer: Logic to switch between Empty View and List View
    // -------------------------------------------------------------
    if (itemList.isEmpty()) {
        // --- Empty State UI ---
        // Rendered when the data source returns no items to guide the user.
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = stringResource(R.string.no_streaming_items))
            }
        }
    } else {
        // --- List Content UI ---
        // Uses LazyColumn for efficient memory usage (recycling views) during scrolling.
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = PaddingM),
            verticalArrangement = Arrangement.spacedBy(PaddingM)
        ) {
            items(
                items = itemList,
                // key optimization: using ID helps Compose strictly identify items for smoother reordering/deletions
                key = { item -> item.id }
            ) { item ->
                StreamListItem(
                    data = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingM),
                    onClick = { data ->
                        onItemSelected(data)
                    }
                )
            }
        }
    }

    // -------------------------------------------------------------
    // Overlay Layer: Floating Action Button (FAB)
    // -------------------------------------------------------------
    // We use a Box to overlay the button on top of the list content.
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingM),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            IconButton(
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
 * Defines previews for both Light and Dark themes (Day/Night) to ensure
 * text contrast and background colors are correct.
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
        // Scaffold acts as a container to mimic the actual screen structure,
        // including handling system bar insets (status bar/navigation bar).
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // Generate mock data specifically for the preview environment
            // to visualize how the list looks with content.
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
                onItemSelected = {}, // No-op for preview
                itemList = mockItems,
                onAddItemSelected = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

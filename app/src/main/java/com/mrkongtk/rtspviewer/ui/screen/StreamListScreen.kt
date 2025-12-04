package com.mrkongtk.rtspviewer.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rtspviewer.data.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.StreamListItem
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * The main screen responsible for displaying a list of configured RTSP streams.
 *
 * This Composable is stateless (UI only) and renders based on the provided [itemList].
 * It handles two primary UI states:
 * 1. **Empty State**: Displays a placeholder message when the list is empty.
 * 2. **Content State**: Displays a scrollable list of streams.
 *
 * @param onItemSelected Callback triggered when a user taps on a specific [RTSPItem].
 * @param modifier The modifier to apply to the container of this screen.
 * @param itemList The current list of RTSP items to display.
 */
@Composable
fun StreamListScreen(
    onItemSelected: (RTSPItem) -> Unit,
    modifier: Modifier = Modifier,
    itemList: List<RTSPItem>,
) {
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
                Text(text = "No item here.")
            }
        }
    } else {
        // --- List Content UI ---
        // Uses LazyColumn for efficient recycling of views during scrolling.
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = PaddingM),
            verticalArrangement = Arrangement.spacedBy(PaddingM)
        ) {
            itemsIndexed(
                items = itemList,
                // Optimization: Providing a unique key helps Compose efficiently
                // reorder or update items without redrawing the whole list.
                key = { _, item -> item.order }
            ) { index, item ->
                StreamListItem(
                    index = index,
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
}

/**
 * Preview provider for [StreamListScreen].
 *
 * Defines previews for both Light and Dark themes (Day/Night) to ensure
 * text contrast and background colors are correct.
 */
@SuppressLint("ViewModelConstructorInComposable")
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

package com.mrkongtk.rstpviewer.ui.screen

import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrkongtk.rstpviewer.model.RSTPItem
import com.mrkongtk.rstpviewer.ui.compose.StreamListItem
import com.mrkongtk.rstpviewer.ui.theme.PaddingM
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme
import com.mrkongtk.rstpviewer.viewmode.StreamListScreenViewModel

/**
 * The main screen responsible for displaying a list of configured RTSP streams.
 *
 * This Composable observes the state from [StreamListScreenViewModel] and handles
 * two primary UI states:
 * 1. **Empty State**: Displays a placeholder message when no streams are saved.
 * 2. **Content State**: Displays a scrollable list of streams.
 *
 * @param viewModel The view model responsible for providing the list of RTSP items.
 *                  Defaults to an instance provided by Hilt navigation graph.
 * @param modifier  The modifier to apply to the container of this screen.
 */
@Composable
fun StreamListScreen(
    viewModel: StreamListScreenViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Observes the stream list flow in a lifecycle-aware manner.
    // This ensures collection stops when the app goes to the background to save resources.
    val rstpItems: List<RSTPItem> by viewModel.rstpItems.collectAsStateWithLifecycle()

    if (rstpItems.isEmpty()) {
        // --- Empty State UI ---
        // Rendered when the data source returns no items.
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
                items = rstpItems,
                key = { _, item -> item.order } // Optimization: Helps Compose identify items on updates
            ) { index, item ->
                StreamListItem(
                    index = index,
                    data = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingM),
                    onClick = { data ->
                        // Handle item click (e.g., navigate to player)
                        Log.d("StreamListScreen", "Clicked item: $data")
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
fun StreamListScreenPreview() {
    val context = LocalContext.current

    RSTPViewerTheme {
        // Scaffold acts as a container to mimic the actual screen structure,
        // including handling system bar insets (status bar/navigation bar).
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // Generate mock data for the preview
            val mockItems = listOf(
                RSTPItem(id = 1, name = "Living Room Camera", link = "rtsp://192.168.1.10", tags = emptyList(), order = 1),
                RSTPItem(id = 2, name = "Backyard", link = "rtsp://192.168.1.11", tags = emptyList(), order = 2)
            )

            // Manually initialize ViewModel with mock data.
            // Note: In production code, prefer decoupling the UI from the ViewModel class
            // by creating a 'StreamListContent' composable that takes a raw List<RSTPItem>,
            // but this approach works for quick prototyping.
            val mockViewModel = StreamListScreenViewModel(context).apply {
                updateData(mockItems, immediate = true)
            }

            StreamListScreen(
                viewModel = mockViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

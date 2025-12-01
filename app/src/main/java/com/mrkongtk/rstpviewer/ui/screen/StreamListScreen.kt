package com.mrkongtk.rstpviewer.ui.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrkongtk.rstpviewer.model.RSTPItem
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme
import com.mrkongtk.rstpviewer.viewmode.StreamListScreenViewModel

/**
 * A Composable screen responsible for displaying the list of saved RTSP streams.
 *
 * This screen follows the Unidirectional Data Flow pattern:
 * 1. It observes state (`rstpItems`) from the [StreamListScreenViewModel].
 * 2. It reacts to state changes (Empty vs. Populated list).
 * 3. It renders the appropriate UI layout.
 *
 * @param viewModel The ViewModel injected via Hilt to handle business logic and state holding.
 *                  Defaults to `hiltViewModel()` for production use.
 * @param modifier The modifier to apply to the root layout of this screen.
 */
@Composable
fun StreamListScreen(
    viewModel: StreamListScreenViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Collect the flow of RSTP items as a Compose State.
    // 'collectAsStateWithLifecycle' is used to ensure the flow collection stops
    // when the app goes into the background, saving resources.
    val rstpItems: List<RSTPItem> by viewModel.rstpItems.collectAsStateWithLifecycle()

    // Determine which UI state to show based on the data
    if (rstpItems.isEmpty()) {
        // --- Empty State ---
        // Displays a centered message when no streams are available.
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center // Vertically center the content
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center // Horizontally center the text
            ) {
                Text("No item here.")
            }
        }

    } else {
        // --- Content State ---
        // Displays the list of items using a LazyColumn for performance efficiency.
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // Iterate through the items. 'itemsIndexed' is used here in case
            // the index is needed for specific logic (e.g., alternating background colors)
            // in the future.
            itemsIndexed(rstpItems) { index, item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    // TODO: Replace this basic Text with a detailed Row/Card component
                    // for the actual stream item (e.g., thumbnail, status, edit buttons).
                    Text(item.name)
                }
            }
        }
    }
}

/**
 * Preview configurations for [StreamListScreen].
 * Generates interactive previews for both Day (Light) and Night (Dark) modes
 * to verify layout responsiveness and theme adherence.
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

    // Apply the application's custom theme to the preview environment
    RSTPViewerTheme {
        // Scaffold acts as the root container, handling system insets (statusBar/navigationBar)
        // so the preview closely matches the actual device rendering.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // --- Mock Data Setup ---
            // Create dummy data to visualize how the list looks when populated.
            val items = listOf(
                RSTPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                RSTPItem(2, "Backyard", "rtsp://192.168.1.11", emptyList(), 2)
            )

            // Manually instantiate the ViewModel for the Preview.
            // Note: In a real Hilt environment, we usually avoid manually new-ing ViewModels,
            // but for simple UI previews, passing a mocked instance or setting initial data works.
            val viewModel = StreamListScreenViewModel(context)
            viewModel.updateData(items, true)

            // Render the screen with the scaffold padding applied
            StreamListScreen(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme
import com.mrkongtk.rstpviewer.viewmode.StreamListScreenViewModel

/**
 * A Composable screen responsible for displaying the list of saved RTSP streams.
 *
 * This screen interacts with [StreamListScreenViewModel] to fetch data.
 * currently, it displays a placeholder message if no streams are found.
 *
 * @param viewModel The ViewModel injected via Hilt to handle business logic and state.
 * @param modifier The modifier to apply to the root layout of this screen.
 */
@Composable
fun StreamListScreen(
    viewModel: StreamListScreenViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Root container to center content vertically
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        // TODO: Observe viewModel state here to toggle between the list and the empty state.

        // Wrapper Row to center the text horizontally across the full width
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("No item here.")
        }
    }
}

/**
 * Preview configurations for [StreamListScreen].
 * Generates previews for both Day (Light) and Night (Dark) modes
 * to ensure the UI looks correct in both system themes.
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
    // Apply the application's custom theme to the preview
    RSTPViewerTheme {
        // Scaffold provides the basic material design visual layout structure
        // and handles window insets (like the status bar) for the preview.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Render the screen with padding provided by the Scaffold
            StreamListScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

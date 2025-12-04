package com.mrkongtk.rtspviewer.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mrkongtk.rtspviewer.data.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.RTSPVideoPlayerViewModel

/**
 * A Composable screen responsible for displaying the playback view of a specific RTSP stream.
 *
 * This screen acts as a container that instantiates the necessary ViewModel and
 * initializes the video player component with the provided [RTSPItem] configuration.
 *
 * @param modifier The modifier to be applied to the root layout of this screen.
 * @param item The data model containing stream details (URI, TCP preferences, etc.).
 */
@Composable
fun StreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
) {
    // Root container: Using a Column to stack elements vertically.
    // It is configured to align content to the top-center of the screen.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hilt Dependency Injection:
        // Retrieves the scoped ViewModel for the video player logic.
        // This handles the lifecycle of the player separately from the UI.
        val rtspViewModel: RTSPVideoPlayerViewModel = hiltViewModel()

        // Render the actual Video Player component.
        // We pass the URI and settings from the 'item' object and inject the ViewModel.
        RTSPVideoPlayer(
            uri = item.uri,
            forceTcp = item.forceTcp,
            viewModel = rtspViewModel,
            modifier = Modifier.fillMaxWidth() // Player takes full width of the screen
        )
    }
}

/**
 * Preview function for [StreamItemScreen].
 *
 * This renders the UI in both "Day" (Light) and "Night" (Dark) modes to verify
 * theming and contrast. It wraps the component in a full [Scaffold] to simulate
 * system insets (status bar/navigation bar) behavior.
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
private fun StreamItemScreenPreview() {
    RTSPViewerTheme {
        // Scaffold acts as the top-level container to mimic the actual application structure.
        // It applies window insets so the preview respects the status bar and navigation bar areas.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // Create a mock RTSPItem object with dummy data to populate the UI for the preview.
            // This isolates the UI test from the actual database or network.
            val mockItem = RTSPItem(
                id = 1,
                name = "Living Room Camera",
                uri = "rtsp://192.168.1.10",
                tags = emptyList(),
                order = 1
            )

            // Render the screen with the mock data and apply the Scaffold's content padding.
            StreamItemScreen(
                item = mockItem,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

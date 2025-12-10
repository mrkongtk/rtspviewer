package com.mrkongtk.rtspviewer.ui.screen

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
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.RTSPVideoPlayerViewModel

/**
 * Displays the playback screen for a specific RTSP stream.
 *
 * This Composable initializes the [RTSPVideoPlayerViewModel] using assisted injection
 * to pass specific stream configuration (URI and TCP preference) and renders the
 * video player component.
 *
 * @param item The [RTSPItem] containing the stream configuration details.
 * @param modifier The [Modifier] to be applied to the layout.
 */
@Composable
fun StreamItemScreen(
    item: RTSPItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Initialize the ViewModel using Assisted Injection.
        // This allows us to pass runtime arguments (URI, forceTcp) to the ViewModel factory.
        val rtspViewModel: RTSPVideoPlayerViewModel =
            hiltViewModel<RTSPVideoPlayerViewModel, RTSPVideoPlayerViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(item.uri, item.forceTcp)
                }
            )

        // Render the player, delegating logic to the ViewModel.
        RTSPVideoPlayer(
            viewModel = rtspViewModel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Preview for [StreamItemScreen].
 *
 * Renders the screen in both Light and Dark modes with a mock data item.
 * Includes a [Scaffold] to simulate system window insets.
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
private fun StreamItemScreenPreview() {
    RTSPViewerTheme {
        // Scaffold allows us to apply window insets to visualize how the app
        // handles the status bar and navigation bar.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Mock data for preview purposes
            val mockItem = RTSPItem(
                id = 1,
                name = "Living Room Camera",
                uri = "rtsp://192.168.1.10",
                tags = emptyList(),
                order = 1
            )

            StreamItemScreen(
                item = mockItem,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

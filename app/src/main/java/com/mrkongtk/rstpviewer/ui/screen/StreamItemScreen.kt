package com.mrkongtk.rstpviewer.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rstpviewer.data.RTSPItem
import com.mrkongtk.rstpviewer.ui.theme.RTSPViewerTheme

/**
 * A Composable screen responsible for displaying the details of a specific RTSP stream.
 *
 * Currently, this screen displays the name of the stream centered in the view.
 *
 * @param modifier The modifier to be applied to the root layout of this screen.
 * @param item The [RTSPItem] data object containing stream details (name, url, etc.).
 */
@Composable
fun StreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
) {
    // Root container: Centers the content vertically and horizontally
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the name of the stream
        Text(text = item.name)
    }
}

/**
 * Preview function for [StreamItemScreen].
 *
 * This renders the UI in both "Day" (Light) and "Night" (Dark) modes to ensure
 * visibility and theming correctness. It uses a [Scaffold] and mock data to
 * simulate a real runtime environment.
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
        // Scaffold acts as the top-level container to mimic the actual screen structure.
        // It handles window insets (status bar/navigation bar) to ensure the preview
        // looks exactly like it will on a real device.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // Create a mock RTSPItem to populate the UI for the preview
            val mockItem = RTSPItem(
                id = 1,
                name = "Living Room Camera",
                url = "rtsp://192.168.1.10",
                tags = emptyList(),
                order = 1
            )

            // Render the screen with the mock data and apply the Scaffold's padding
            StreamItemScreen(
                item = mockItem,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

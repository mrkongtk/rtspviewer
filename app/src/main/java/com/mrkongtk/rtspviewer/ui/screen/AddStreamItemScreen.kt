package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * A Composable screen designed specifically for adding a new RTSP stream.
 *
 * This function acts as a wrapper around [EditStreamItemScreen]. It instantiates the
 * underlying edit form with a default, empty [RTSPItem] object (where `id = 0`),
 * allowing the user to input details from scratch.
 *
 * @param modifier The modifier to be applied to the layout container.
 * @param onSave Callback triggered when the valid form is saved. Passes the newly configured [RTSPItem].
 */
@Composable
fun AddStreamItemScreen(
    modifier: Modifier = Modifier,
    onSave: (RTSPItem) -> Unit,
) {
    // Delegate the UI logic to EditStreamItemScreen.
    // We provide a default "blank" item here. The ID is set to 0 to indicate
    // to the database (Room) that this is a new entry to be inserted, not updated.
    EditStreamItemScreen(
        modifier = modifier,
        item = RTSPItem(
            id = 0,
            name = "",
            uri = "",
            tags = emptyList(),
            order = -1,
            forceTcp = false
        ),
        onSave = onSave
    )
}

// --- Previews ---

/**
 * Preview for the Add Stream screen.
 *
 * Displays the UI in both Day (Light) and Night (Dark) modes to ensure
 * theme consistency. It wraps the content in a [Scaffold] to simulate
 * system window insets (status bar/navigation bar) handling.
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
        // Scaffold acts as the top-level container to mimic the actual application structure.
        // It applies window insets so the preview visually respects the status bar
        // and navigation bar areas.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Render the screen with the mock data and apply the Scaffold's content padding
            // to prevent content from being drawn behind system bars.
            AddStreamItemScreen(
                onSave = {}, // No-op for preview
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

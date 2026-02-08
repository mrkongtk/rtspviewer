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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.screen.action.EditStreamItemScreenActions
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * A specialized entry-point screen for creating new RTSP stream configurations.
 *
 * This Composable serves as a functional wrapper around [EditStreamItemScreen]. While it
 * shares the same underlying form UI, it is distinct in its intent: providing a
 * blank state for fresh data entry rather than modifying existing persistence.
 *
 * **Key Logic:**
 * - **State Initialization:** It instantiates a "template" [RTSPItem] with default values.
 * - **Room Integration:** By setting [RTSPItem.id] to `0`, it signals to the Room DAO
 *   (via the Repository) that this is a new record. This ensures the database performs an
 *   `INSERT` with an auto-generated primary key instead of an `UPDATE`.
 * - **Unidirectional Data Flow:** User inputs are managed locally within the form,
 *   and the final "Save" event is bubbled up through [screenActions].
 *
 * @param modifier The modifier to be applied to the layout container.
 * @param screenActions A collection of lambdas (defined in [EditStreamItemScreenActions])
 * to handle persistence and navigation logic, keeping this UI component decoupled
 * from business logic.
 */
@Composable
fun AddStreamItemScreen(
    modifier: Modifier = Modifier,
    screenActions: EditStreamItemScreenActions,
) {
    // Delegate the UI logic to EditStreamItemScreen.
    // We provide a default "blank" item here. The ID is set to 0 to indicate
    // to the database (Room) that this is a new entry to be inserted, not updated.
    EditStreamItemScreen(
        modifier = modifier.testTag("AddStreamItemScreenRoot"),
        item = RTSPItem(
            id = 0,
            name = "",
            uri = "",
            tags = emptyList(),
            order = -1, // Indicates it hasn't been assigned a specific position yet
            forceTcp = false
        ),
        screenActions = screenActions,
    )
}

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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                screenActions = object : EditStreamItemScreenActions {
                    override fun onSaveItem(newItem: RTSPItem) {}
                }
            )
        }
    }
}

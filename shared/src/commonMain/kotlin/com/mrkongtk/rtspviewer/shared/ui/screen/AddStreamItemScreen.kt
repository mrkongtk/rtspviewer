package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.screen.action.EditStreamItemScreenActions
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme

/**
 * Screen for creating a new RTSP stream configuration.
 *
 * This is a wrapper around [EditStreamItemScreen] that initializes a blank [RTSPItem].
 * Setting [RTSPItem.id] to 0 ensures the database performs an insert operation.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param screenActions Actions for handling persistence and navigation.
 */
@Composable
fun AddStreamItemScreen(
    modifier: Modifier = Modifier,
    screenActions: EditStreamItemScreenActions,
) {
    EditStreamItemScreen(
        modifier = modifier.testTag("AddStreamItemScreenRoot"),
        item = RTSPItem(
            id = 0,
            name = "",
            uri = "",
            tags = emptyList(),
            order = -1,
            forceTcp = false
        ),
        screenActions = screenActions,
    )
}

/**
 * Preview for [AddStreamItemScreen] in light and dark modes.
 */
@Preview(
    name = "Day",
    showSystemUi = true,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun StreamItemScreenPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
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

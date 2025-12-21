package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.ui.screen.MoreOptionState.CLOSED
import com.mrkongtk.rtspviewer.ui.screen.MoreOptionState.OPEN
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmodel.RTSPVideoPlayerViewModel

/**
 * Represents the visibility state of the "More Options" dropdown menu.
 */
private enum class MoreOptionState(val value: Boolean) {
    /** Menu is expanded/visible. */
    OPEN(true),

    /** Menu is collapsed/hidden. */
    CLOSED(false);

    companion object {
        /**
         * Returns the [MoreOptionState] corresponding to the given boolean value.
         */
        fun fromValue(value: Boolean): MoreOptionState {
            return MoreOptionState.entries.first { it.value == value }
        }
    }
}

/**
 * Toggles the state between [OPEN] and [CLOSED].
 */
private operator fun MoreOptionState.not(): MoreOptionState {
    return when (this) {
        OPEN -> CLOSED
        CLOSED -> OPEN
    }
}

/**
 * Displays the detailed screen for a specific RTSP stream item.
 *
 * This screen includes:
 * 1. The Video Player (using [VideoPlayerCompose]).
 * 2. Metadata rows (Name, URI, Tags, Force TCP setting).
 * 3. A floating action button that opens a menu for Edit and Delete actions.
 *
 * @param modifier The modifier to be applied to the root layout.
 * @param item The [RTSPItem] data object containing stream details.
 * @param moreOption The initial state of the options menu (default is closed).
 * @param onEditItemSelected Callback triggered when the "Edit" menu item is clicked.
 * @param onDeleteItemSelected Callback triggered when the "Delete" menu item is clicked.
 */
@Composable
fun StreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    moreOption: Boolean = false,
    onEditItemSelected: () -> Unit,
    onDeleteItemSelected: () -> Unit,
) {
    // Local state to control the visibility of the delete confirmation dialog
    var showDeleteConfirmationPrompt by remember { mutableStateOf(false) }

    // Root container: Uses a Box to layer the FAB/Menu on top of the content
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Main Content Layer: Video + Info
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. Top section: The Video Player
            VideoPlayerCompose(Modifier.fillMaxWidth(), item)

            // 2. Definition of UI Rows for Metadata
            // Defined as lambdas to keep the main Column composition clean and repetitive logic isolated

            // Row for the RTSP Stream Name
            val nameRow: @Composable (Modifier) -> Unit = { modifier ->
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_name),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        item.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Row for the URI (Credentials are hidden via helper function)
            val uriRow: @Composable (Modifier) -> Unit = { modifier ->
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_uri),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        hideUriCredential(item.uri),
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Row for Tags (Uses FlowRow to wrap tags to the next line if space is limited)
            val tagsRow: @Composable (Modifier) -> Unit = { modifier ->
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_tags),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            PaddingS,
                            alignment = Alignment.End
                        )
                    ) {
                        item.tags.fastForEach { tag ->
                            // Individual Tag Chip styling
                            val modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(PaddingXs)
                            )
                            Text(
                                modifier = modifier.padding(horizontal = PaddingS),
                                text = tag,
                                color = MaterialTheme.colorScheme.onSecondary,
                            )
                        }
                    }
                }
            }

            // Row for the Force TCP setting
            val forceTcpRow: @Composable (Modifier) -> Unit = { modifier ->
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_force_tcp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    // Read-only checkbox to show the setting state (user cannot toggle here)
                    Checkbox(item.forceTcp, onCheckedChange = null, enabled = false)
                }
            }

            // 3. Render the metadata rows dynamically
            val rows = listOf(nameRow, uriRow, forceTcpRow, tagsRow)
            rows.fastForEach { rowComposable ->
                rowComposable(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingM)
                )
            }
        }

        // Overlay Layer: Floating Action Button & Dropdown Menu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingM),
            contentAlignment = Alignment.BottomEnd
        ) {
            // State to manage the visibility of the "More Options" dropdown
            var moreState by remember { mutableStateOf(MoreOptionState.fromValue(moreOption)) }

            Column {
                // The dropdown menu containing Edit/Delete actions
                DropdownMenu(
                    expanded = moreState.value,
                    onDismissRequest = { moreState = CLOSED }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit)
                            )
                        },
                        onClick = {
                            moreState = CLOSED
                            onEditItemSelected()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete)
                            )
                        },
                        onClick = {
                            moreState = CLOSED
                            showDeleteConfirmationPrompt = true
                        }
                    )
                }

                // The FAB/Icon button that toggles the menu
                IconButton(
                    onClick = {
                        moreState = !moreState // Uses the custom operator 'not()'
                    },
                    colors = IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(
                        imageVector = when (moreState) {
                            CLOSED -> Icons.Default.MoreVert
                            OPEN -> Icons.Default.Close
                        },
                        contentDescription = stringResource(R.string.add_rtsp_button)
                    )
                }
            }
        }

        // Dialog Layer: Confirmation when deleting an item
        if (showDeleteConfirmationPrompt) {
            DeleteConfirmDialogCompose(
                rtspItem = item,
                onDismissRequest = {
                    showDeleteConfirmationPrompt = false
                },
                onConfirmation = {
                    showDeleteConfirmationPrompt = false
                    onDeleteItemSelected()
                }
            )
        }
    }
}

/**
 * A wrapper composable for the Video Player.
 *
 * This handles the distinction between running in a UI Preview (where Hilt is unavailable)
 * and running in the actual application.
 *
 * @param modifier Modifier for the player container.
 * @param item The RTSP item containing connection details.
 */
@Composable
private fun VideoPlayerCompose(
    modifier: Modifier = Modifier,
    item: RTSPItem
) {
    // Check if we are running in Android Studio Preview mode
    if (LocalInspectionMode.current) {
        // Show a placeholder to prevent crashing, as Hilt cannot inject ViewModels in preview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Video Player Here",
                textAlign = TextAlign.Center
            )
        }
    } else {
        // Initialize the ViewModel using Assisted Injection.
        // The 'creationCallback' allows us to pass runtime arguments (URI, forceTcp)
        // directly to the ViewModel's @AssistedFactory.
        val rtspViewModel: RTSPVideoPlayerViewModel =
            hiltViewModel<RTSPVideoPlayerViewModel, RTSPVideoPlayerViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(item.uri, item.forceTcp)
                }
            )

        // Render the actual player, delegating logic to the ViewModel
        RTSPVideoPlayer(
            viewModel = rtspViewModel,
            modifier = modifier,
        )
    }
}

@Composable
private fun DeleteConfirmDialogCompose(
    rtspItem: RTSPItem,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.delete))
        },
        title = {
            Text(text = stringResource(R.string.delete_dialog_title).replace("%1", rtspItem.name))
        },
        text = {
            Text(text = stringResource(R.string.delete_dialog_message))
        },
        containerColor = MaterialTheme.colorScheme.background,
        iconContentColor = MaterialTheme.colorScheme.onBackground,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}


/**
 * Utility function to mask credentials in an RTSP URI for display purposes.
 *
 * If the URI contains a username and password (format: `rtsp://user:pass@ip...`),
 * they are replaced with `***`.
 *
 * @param uri The raw RTSP URI string.
 * @return The sanitized URI string suitable for UI display.
 */
private fun hideUriCredential(uri: String): String {
    // Regex Groups:
    // 1: protocol (rtsp://)
    // 2: username
    // 3: separator (:)
    // 4: password
    // 5: separator (@)
    val regex = "(rtsp://)(.+)(:)(.+)(@)".toRegex()

    if (regex.containsMatchIn(uri)) {
        // Replace groups 2 and 4 with asterisks
        val replacement = "\$1***\$3***\$5"
        return regex.replace(uri, replacement)
    } else {
        return uri
    }
}

/**
 * Provides sample data for the [StreamItemScreenPreview].
 *
 * Generates combinations of:
 * 1. [RTSPItem] (one with tags/credentials, one without).
 * 2. [MoreOptionState] (Open vs Closed menu).
 */
private class StreamItemScreenPreviewParameterProvider :
    PreviewParameterProvider<Pair<RTSPItem, MoreOptionState>> {

    private val items = sequenceOf(
        RTSPItem(
            id = 1,
            name = "Living Room Camera",
            uri = "rtsp://123:456@192.168.1.10.fdsafds.fdsafdsaf.ghfdsgfdsgfd.",
            tags = listOf("tag1", "tag 2"),
            order = 1
        ),
        RTSPItem(
            id = 1,
            name = "Living Room Camera",
            uri = "rtsp://192.168.1.10.fdsafds.fdsafdsaf.ghfdsgfdsgfd.",
            tags = listOf(),
            order = 1
        )
    )

    private val states = MoreOptionState.entries.asSequence()

    // Creates a Cartesian product of Items x States for comprehensive previewing
    override val values = items.flatMap { item ->
        states.map {
            Pair(item, it)
        }
    }
}

/**
 * Preview for [StreamItemScreen].
 *
 * Renders the screen in both Light and Dark modes using data provided by
 * [StreamItemScreenPreviewParameterProvider].
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
private fun StreamItemScreenPreview(
    @PreviewParameter(StreamItemScreenPreviewParameterProvider::class) parameters: Pair<RTSPItem, MoreOptionState>,
) {
    RTSPViewerTheme {
        // Scaffold allows us to apply window insets to visualize how the app
        // handles the status bar and navigation bar.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            StreamItemScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                item = parameters.first,
                moreOption = parameters.second.value,
                onEditItemSelected = {},
                onDeleteItemSelected = {},
            )
        }
    }
}

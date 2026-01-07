package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.compose.ui.platform.testTag
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
 * Internal state enum to manage the visibility of the Options Dropdown Menu.
 *
 * @property value Boolean mapping: true for visible, false for hidden.
 */
private enum class MoreOptionState(val value: Boolean) {
    OPEN(true),
    CLOSED(false);

    companion object {
        /** Maps a boolean visibility state back to the Enum. */
        fun fromValue(value: Boolean): MoreOptionState {
            return MoreOptionState.entries.first { it.value == value }
        }
    }
}

/**
 * Extension operator to allow '!' syntax (e.g., !moreState) to toggle the menu state.
 */
private operator fun MoreOptionState.not(): MoreOptionState {
    return when (this) {
        OPEN -> CLOSED
        CLOSED -> OPEN
    }
}

/**
 * Main Screen Composable for viewing a single RTSP stream and its metadata.
 *
 * This screen utilizes a layered approach:
 * 1. **Content Layer**: Video player at the top followed by a scrollable/form-like list of details.
 * 2. **Overlay Layer**: A Floating Action Button (FAB) that triggers an Edit/Delete menu.
 * 3. **Dialog Layer**: Conditional confirmation prompts.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param item The RTSP data entity to display.
 * @param moreOption Initial visibility state of the options menu (used primarily for testing/previews).
 * @param onEditItemSelected Invoked when the user selects 'Edit'.
 * @param onDeleteItemSelected Invoked after the user confirms deletion.
 * @param onImageAvailable Callback providing a [Bitmap] frame from the video stream for thumbnails.
 */
@Composable
fun StreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    moreOption: Boolean = false,
    onEditItemSelected: () -> Unit,
    onDeleteItemSelected: () -> Unit,
    onImageAvailable: (RTSPItem, Bitmap) -> Unit,
) {
    // UI-only state: tracks if the user has clicked "Delete" but not yet confirmed.
    var showDeleteConfirmationPrompt by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.testTag("StreamItemScreenRoot"),
        contentAlignment = Alignment.Center
    ) {
        // --- CONTENT LAYER ---
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // RTSP Video Player Section
            VideoPlayerCompose(
                Modifier
                    .testTag("VideoPlayer")
                    .fillMaxWidth(),
                item,
                onImageAvailable
            )

            // UI Components for Metadata display
            // We define these as local lambdas to maintain clean code and avoid repetitive Row styling.

            val nameRow: @Composable (Modifier) -> Unit = { mod ->
                Row(
                    modifier = mod,
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
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("Name")
                    )
                }
            }

            val uriRow: @Composable (Modifier) -> Unit = { mod ->
                Row(
                    modifier = mod,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_uri),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        hideUriCredential(item.uri), // Security: Mask credentials for UI display
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("Uri")
                    )
                }
            }

            val tagsRow: @Composable (Modifier) -> Unit = { mod ->
                Row(
                    modifier = mod,
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_tags),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    // FlowRow allows tags to wrap naturally if they exceed screen width
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            PaddingS,
                            alignment = Alignment.End
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaddingXs)
                    ) {
                        item.tags.fastForEach { tag ->
                            Text(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondary,
                                        RoundedCornerShape(PaddingXs)
                                    )
                                    .padding(horizontal = PaddingS)
                                    .testTag("Tag $tag"),
                                text = tag,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            val forceTcpRow: @Composable (Modifier) -> Unit = { mod ->
                Row(
                    modifier = mod,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.label_force_tcp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Checkbox(
                        item.forceTcp,
                        onCheckedChange = {},
                        enabled = false,
                        modifier = Modifier.testTag("ForceTCP")
                    )
                }
            }

            // Iterate through row definitions and apply consistent horizontal padding
            val rows = listOf(nameRow, uriRow, forceTcpRow, tagsRow)
            rows.fastForEach { rowComposable ->
                rowComposable(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingM)
                )
            }
        }

        // --- OVERLAY LAYER (Floating Menu) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingM),
            contentAlignment = Alignment.BottomEnd
        ) {
            var moreState by remember { mutableStateOf(MoreOptionState.fromValue(moreOption)) }

            Column {
                DropdownMenu(
                    expanded = moreState.value,
                    onDismissRequest = { moreState = CLOSED }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            moreState = CLOSED
                            onEditItemSelected()
                        },
                        modifier = Modifier.testTag("EditButton")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            moreState = CLOSED
                            showDeleteConfirmationPrompt = true
                        },
                        modifier = Modifier.testTag("DeleteButton")
                    )
                }

                IconButton(
                    onClick = { moreState = !moreState },
                    colors = IconButtonDefaults.filledIconButtonColors(),
                    modifier = Modifier.testTag("MoreButton")
                ) {
                    Icon(
                        imageVector = if (moreState == CLOSED) Icons.Default.MoreVert else Icons.Default.Close,
                        contentDescription = stringResource(R.string.add_rtsp_button)
                    )
                }
            }
        }

        // --- DIALOG LAYER ---
        if (showDeleteConfirmationPrompt) {
            DeleteConfirmDialogCompose(
                rtspItem = item,
                onDismissRequest = { showDeleteConfirmationPrompt = false },
                onConfirmation = {
                    showDeleteConfirmationPrompt = false
                    onDeleteItemSelected()
                }
            )
        }
    }
}

/**
 * Handles the initialization and lifecycle of the RTSP Video Player.
 *
 * **Note on Architecture:** Uses Hilt Assisted Injection to pass the stream URI
 * and TCP preference to the ViewModel at runtime.
 */
@Composable
private fun VideoPlayerCompose(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    onImageAvailable: (RTSPItem, Bitmap) -> Unit,
) {
    // If in Edit Mode / Android Studio Preview, don't attempt to load Hilt/FFmpeg.
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentAlignment = Alignment.Center
        ) {
            Text(modifier = Modifier.testTag("EmptyVideo"), text = "Video Player Preview")
        }
    } else {
        // Assisted Injection: Creates the ViewModel with specific stream parameters
        val rtspViewModel: RTSPVideoPlayerViewModel =
            hiltViewModel<RTSPVideoPlayerViewModel, RTSPVideoPlayerViewModel.Factory>(
                creationCallback = { factory ->
                    factory.create(item.uri, item.forceTcp, { onImageAvailable(item, it) })
                }
            )

        RTSPVideoPlayer(
            viewModel = rtspViewModel,
            modifier = modifier,
        )
    }
}

/**
 * A standard Material 3 Alert Dialog for confirming destructive actions.
 */
@Composable
private fun DeleteConfirmDialogCompose(
    rtspItem: RTSPItem,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = {
            Text(text = stringResource(R.string.delete_dialog_title).replace("%1", rtspItem.name))
        },
        text = { Text(text = stringResource(R.string.delete_dialog_message)) },
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onConfirmation,
                modifier = Modifier.testTag("DeleteConfirmButton")
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("DeleteCancelButton")
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.testTag("DeleteConfirmDialog")
    )
}

/**
 * Sanitizes a URI by replacing user credentials with asterisks.
 * If the URI contains a username and password (format: `rtsp://user:pass@ip...`),
 * they are replaced with `***`.
 *
 * @param uri The raw RTSP connection string.
 * @return A string safe for display in the UI.
 */
private fun hideUriCredential(uri: String): String {
// Regex identifies: [1: protocol][2: user][3: :][4: pass][5: @]
    val regex = "(rtsp://)(.+)(:)(.+)(@)".toRegex()

    return if (regex.containsMatchIn(uri)) {
// Keeps separators (1, 3, 5) but replaces user/pass (2, 4)
        regex.replace(uri, "\$1***\$3***\$5")
    } else {
        uri
    }
}

/**
 * Generates test data combinations for Previewing the screen.
 */
private class StreamItemScreenPreviewParameterProvider :
    PreviewParameterProvider<Pair<RTSPItem, MoreOptionState>> {

    private val items = sequenceOf(
        RTSPItem(1, "Front Porch", "rtsp://user:pass@10.0.0.1/live", listOf("Outdoor", "HD"), 1),
        RTSPItem(2, "Baby Monitor", "rtsp://192.168.1.50/stream", listOf(), 2)
    )

    override val values = items.flatMap { item ->
        MoreOptionState.entries.asSequence().map { state -> item to state }
    }
}

@Preview(name = "Day Mode", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Night Mode", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StreamItemScreenPreview(
    @PreviewParameter(StreamItemScreenPreviewParameterProvider::class) params: Pair<RTSPItem, MoreOptionState>,
) {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            StreamItemScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                item = params.first,
                moreOption = params.second.value,
                onEditItemSelected = {},
                onDeleteItemSelected = {},
                onImageAvailable = { _, _ -> },
            )
        }
    }
}
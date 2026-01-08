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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
 * Internal state management for the options dropdown menu.
 * Using an Enum instead of a raw Boolean improves readability and scalability.
 */
private enum class MoreOptionState(val value: Boolean) {
    OPEN(true),
    CLOSED(false);

    companion object {
        /** Creates a state from a boolean value (useful for initial state from previews/testing). */
        fun fromValue(value: Boolean): MoreOptionState = if (value) OPEN else CLOSED
    }
}

/**
 * Toggles the [MoreOptionState] state using the logical 'not' operator.
 */
private operator fun MoreOptionState.not(): MoreOptionState = if (this == OPEN) CLOSED else OPEN

/**
 * Main Screen for displaying a specific RTSP stream.
 *
 * Features:
 * - Adaptive Layout: Fullscreen video in landscape; Video + Metadata in portrait.
 * - Credential Masking: Sanitizes RTSP URIs for display.
 * - Contextual Actions: Edit and Delete options via a Floating Action Button (FAB) menu.
 *
 * @param modifier Applied to the root layout.
 * @param item The [RTSPItem] entity containing stream details.
 * @param moreOption Initial visibility of the dropdown menu (defaults to false).
 * @param onEditItemSelected Callback triggered when the 'Edit' action is selected.
 * @param onDeleteItemSelected Callback triggered after the user confirms deletion.
 * @param onImageAvailable Callback providing a frame [Bitmap] for thumbnail generation.
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
    val configuration = LocalConfiguration.current
    var orientation by remember { mutableIntStateOf(configuration.orientation) }

    // State for managing the "Are you sure?" delete dialog
    var showDeleteConfirmationPrompt by remember { mutableStateOf(false) }

    // Sync orientation state with configuration changes
    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect { orientation = it }
    }

    Box(
        modifier = modifier.testTag("StreamItemScreenRoot"),
        contentAlignment = Alignment.Center
    ) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // --- LANDSCAPE MODE: Immersive Video Player ---
            VideoPlayerCompose(
                Modifier
                    .testTag("VideoPlayerLandscape")
                    .fillMaxSize(),
                item,
                onImageAvailable
            )
        } else {
            // --- PORTRAIT MODE: Scrollable Content + Details ---
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VideoPlayerCompose(
                    Modifier
                        .testTag("VideoPlayer")
                        .fillMaxWidth(),
                    item,
                    onImageAvailable
                )

                /*
                 * UI Rows for Stream Metadata
                 * Defined as lambdas to maintain consistency in styling across different data points.
                 */

                val nameRow: @Composable (Modifier) -> Unit = { mod ->
                    Row(
                        mod,
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
                        mod,
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
                            textAlign = TextAlign.End,
                            modifier = Modifier.testTag("Uri")
                        )
                    }
                }

                val tagsRow: @Composable (Modifier) -> Unit = { mod ->
                    Row(
                        mod,
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
                        mod,
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

                // Render all metadata rows with standard horizontal padding
                val rows = listOf(nameRow, uriRow, forceTcpRow, tagsRow)
                rows.fastForEach { rowComposable ->
                    rowComposable(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PaddingM)
                    )
                }
            }
        }

        // --- OVERLAY: Action Menu (Visible only in Portrait) ---
        if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
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
        }

        // --- DIALOGS ---
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
 * Handles the logic for initializing the RTSP Video Player.
 * Uses Hilt Assisted Injection to bridge the gap between static DI and runtime stream parameters.
 */
@Composable
private fun VideoPlayerCompose(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    onImageAvailable: (RTSPItem, Bitmap) -> Unit,
) {
    // Prevent ViewModel initialization during Android Studio Preview to avoid crashes
    if (LocalInspectionMode.current) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(modifier = Modifier.testTag("EmptyVideo"), text = "Video Player Preview")
        }
    } else {
        // Create ViewModel using Hilt Assisted Factory
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
 * Material 3 Confirmation Dialog for stream deletion.
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
            // Dynamic title replacing placeholder with item name
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
 * Masks RTSP credentials for UI security.
 * If the URI contains a username and password (format: `rtsp://user:pass@ip...`),
 * they are replaced with `***`.
 *
 * Regex Breakdown:
 * 1. `(rtsp://)` - The protocol prefix.
 * 2. `(.+)` - The username.
 * 3. `(:)` - The separator between user/pass.
 * 4. `(.+)` - The password.
 * 5. `(@)` - The separator before the IP.
 *
 * @return Sanitized URI string or original if no match.
 */
private fun hideUriCredential(uri: String): String {
    val regex = "(rtsp://)(.+)(:)(.+)(@)".toRegex()

    return if (regex.containsMatchIn(uri)) {
// Replaces captures 2 (user) and 4 (pass) with asterisks while keeping separators
        regex.replace(uri, "\$1***\$3***\$5")
    } else {
        uri
    }
}

/**
 * Provider for generating diverse UI states (Night/Day, Portrait/Landscape, Menu Open/Closed)
 * for the Compose Preview tool.
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

@Preview(
    name = "Day Portrait",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:orientation=portrait,width=411dp,height=891dp"
)
@Preview(
    name = "Day Landscape",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:orientation=landscape,width=411dp,height=891dp"
)
@Preview(
    name = "Night Portrait",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:orientation=portrait,width=411dp,height=891dp"
)
@Preview(
    name = "Night Landscape",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:orientation=landscape,width=411dp,height=891dp"
)
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

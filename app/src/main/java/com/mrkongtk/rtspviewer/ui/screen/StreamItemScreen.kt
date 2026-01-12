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
import com.mrkongtk.rtspviewer.data.database.entity.hideCredentialUri
import com.mrkongtk.rtspviewer.ui.compose.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.ui.screen.MoreOptionState.CLOSED
import com.mrkongtk.rtspviewer.ui.screen.MoreOptionState.OPEN
import com.mrkongtk.rtspviewer.ui.screen.action.StreamItemScreenActions
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.util.formatText
import com.mrkongtk.rtspviewer.viewmodel.RTSPVideoPlayerViewModel

/**
 * Encapsulates the visual state of the "More Options" menu.
 * Using an Enum instead of a Boolean makes state transitions more explicit.
 */
private enum class MoreOptionState(val value: Boolean) {
    OPEN(true),
    CLOSED(false);

    companion object {
        fun fromValue(value: Boolean): MoreOptionState = if (value) OPEN else CLOSED
    }
}

/**
 * Extension operator to toggle the menu state using [!state] syntax.
 */
private operator fun MoreOptionState.not(): MoreOptionState = if (this == OPEN) CLOSED else OPEN

/**
 * The main screen for viewing a specific RTSP stream and its metadata.
 *
 * Behavior:
 * - **Landscape**: Fullscreen video player for an immersive experience.
 * - **Portrait**: Split view with the video player on top and stream details below.
 * - **Security**: Uses `hideCredentialUri` to ensure sensitive RTSP credentials aren't visible in plain text.
 *
 * @param item The data entity representing the stream configuration.
 * @param moreOption Initial visibility state of the action menu (useful for deep-linking/testing).
 * @param screenActions Interface to handle navigation or data logic (edit, delete, snapshot saving).
 * @param playerViewModel Optional ViewModel instance, primarily used for Preview or manual injection.
 */
@Composable
fun StreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    moreOption: Boolean = false,
    screenActions: StreamItemScreenActions,
    playerViewModel: RTSPVideoPlayerViewModel? = null,
) {
    val configuration = LocalConfiguration.current
    var orientation by remember { mutableStateOf(configuration.orientation) }

    // Logic for the deletion confirmation dialog
    var showDeleteConfirmationPrompt by remember { mutableStateOf(false) }

    // Reactively track orientation changes to update the UI layout dynamically
    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect { orientation = it }
    }

    Box(
        modifier = modifier.testTag("StreamItemScreenRoot"),
        contentAlignment = Alignment.Center
    ) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // --- LANDSCAPE UI: Focus purely on the video feed ---
            VideoPlayer(
                modifier = Modifier
                    .testTag("VideoPlayerLandscape")
                    .fillMaxSize(),
                item = item,
                viewModel = playerViewModel,
            ) { item, bitmap ->
                screenActions.onImageAvailable(item, bitmap)
            }
        } else {
            // --- PORTRAIT UI: Video + Info List ---
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VideoPlayer(
                    Modifier
                        .testTag("VideoPlayer")
                        .fillMaxWidth(),
                    item,
                    viewModel = playerViewModel,
                ) { item, bitmap ->
                    screenActions.onImageAvailable(item, bitmap)
                }

                /*
                 * Define row components for metadata.
                 * Extracting these as lambdas keeps the layout code clean.
                 */

                // 1. Name Display
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

                // 2. Masked URI Display
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
                            item.hideCredentialUri,
                            color = MaterialTheme.colorScheme.onSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.testTag("Uri")
                        )
                    }
                }

                // 3. Tags (using FlowRow to handle wrapping)
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

                // 4. Transport Protocol Checkbox
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

                // Iterate and render all rows with standard padding
                // Note: fastForEach is used for performance optimization (avoids iterator allocation)
                val rows = listOf(nameRow, uriRow, forceTcpRow, tagsRow)
                rows.fastForEach { rowComposable ->
                    rowComposable(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaddingM))
                }
            }
        }

        // --- FLOATING ACTION MENU: Actions available in Portrait only ---
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
                                screenActions.onEditItemSelected()
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

        // --- MODALS ---
        if (showDeleteConfirmationPrompt) {
            DeleteConfirmDialogCompose(
                rtspItem = item,
                onDismissRequest = { showDeleteConfirmationPrompt = false },
                onConfirmation = {
                    showDeleteConfirmationPrompt = false
                    screenActions.onDeleteItemSelected()
                }
            )
        }
    }
}

/**
 * A wrapper for [RTSPVideoPlayer] that handles Hilt ViewModel creation.
 *
 * Uses **Assisted Injection** via [RTSPVideoPlayerViewModel.Factory] to pass runtime
 * stream parameters (URI, TCP preference) into the ViewModel.
 */
@Composable
private fun VideoPlayer(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    viewModel: RTSPVideoPlayerViewModel? = null,
    onImageAvailable: (RTSPItem, Bitmap) -> Unit,
) {
    // Check if we are in Android Studio Preview mode to avoid Hilt/Native errors
    if (LocalInspectionMode.current) {
        viewModel?.let {
            RTSPVideoPlayer(viewModel = it, modifier = modifier)
        } ?: run {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(modifier = Modifier.testTag("EmptyVideo"), text = "Video Player Preview")
            }
        }
    } else {
        // Resolve ViewModel: use the provided one or create a new one via Hilt factory
        val rtspViewModel: RTSPVideoPlayerViewModel =
            viewModel ?: hiltViewModel<RTSPVideoPlayerViewModel, RTSPVideoPlayerViewModel.Factory>(
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
 * Standard Material 3 confirmation dialog for deleting an RTSP stream.
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
            // Replaces placeholders in the string (e.g., "Delete %s?") with the item name
            Text(text = stringResource(R.string.delete_dialog_title).formatText(rtspItem.name))
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
 * Provides a set of dummy data to the Preview tool to visualize multiple states
 * (different items, menu open vs. closed).
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

// --- PREVIEWS ---

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
                screenActions = object : StreamItemScreenActions {
                    override fun onEditItemSelected() {}
                    override fun onDeleteItemSelected() {}
                    override fun onImageAvailable(item: RTSPItem, bitmap: Bitmap) {}
                },
            )
        }
    }
}

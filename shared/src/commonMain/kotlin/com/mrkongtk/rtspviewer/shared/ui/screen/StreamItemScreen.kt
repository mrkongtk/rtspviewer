package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.hideCredentialUri
import com.mrkongtk.rtspviewer.shared.ui.compose.KeepScreenOn
import com.mrkongtk.rtspviewer.shared.ui.compose.RTSPVideoPlayer
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerPlaybackState
import com.mrkongtk.rtspviewer.shared.ui.player.RTSPVideoPlayerState
import com.mrkongtk.rtspviewer.shared.ui.screen.action.StreamItemScreenActions
import com.mrkongtk.rtspviewer.shared.ui.state.MoreOptionState
import com.mrkongtk.rtspviewer.shared.ui.state.not
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.formatText
import com.mrkongtk.rtspviewer.shared.viewmodel.RTSPVideoPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.add_rtsp_button
import rtspviewer.shared.generated.resources.cancel
import rtspviewer.shared.generated.resources.confirm
import rtspviewer.shared.generated.resources.delete
import rtspviewer.shared.generated.resources.delete_dialog_message
import rtspviewer.shared.generated.resources.delete_dialog_title
import rtspviewer.shared.generated.resources.edit
import rtspviewer.shared.generated.resources.label_force_tcp
import rtspviewer.shared.generated.resources.label_name
import rtspviewer.shared.generated.resources.label_tags
import rtspviewer.shared.generated.resources.label_uri


/**
 * The main screen for viewing a specific RTSP stream and its metadata.
 *
 * Provides a fullscreen experience in landscape and a detailed split view in portrait.
 *
 * @param item The data entity representing the stream configuration.
 * @param moreOption Initial visibility state of the action menu.
 * @param screenActions Actions for handling navigation and stream operations.
 */
@Composable
fun StreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    moreOption: Boolean = false,
    screenActions: StreamItemScreenActions,
    playerViewModel: RTSPVideoPlayerViewModel? = null
) {
    BoxWithConstraints(
        modifier = modifier.testTag("StreamItemScreenRoot"),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight
        var showDeleteConfirmationPrompt by remember { mutableStateOf(false) }

        if (isLandscape) {
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
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VideoPlayer(
                    modifier = Modifier
                        .testTag("VideoPlayer")
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    item = item,
                    viewModel = playerViewModel,
                ) { item, bitmap ->
                    screenActions.onImageAvailable(item, bitmap)
                }

                val nameRow: @Composable (Modifier) -> Unit = { mod ->
                    Row(
                        mod,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(Res.string.label_name),
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
                            stringResource(Res.string.label_uri),
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

                val tagsRow: @Composable (Modifier) -> Unit = { mod ->
                    Row(
                        mod,
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(Res.string.label_tags),
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
                            stringResource(Res.string.label_force_tcp),
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

        if (!isLandscape) {
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
                        onDismissRequest = { moreState = MoreOptionState.CLOSED }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                moreState = MoreOptionState.CLOSED
                                screenActions.onEditItemSelected()
                            },
                            modifier = Modifier.testTag("EditButton")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                moreState = MoreOptionState.CLOSED
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
                            imageVector = if (moreState == MoreOptionState.CLOSED) Icons.Default.MoreVert else Icons.Default.Close,
                            contentDescription = stringResource(Res.string.add_rtsp_button)
                        )
                    }
                }
            }
        }

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
 * A wrapper for [RTSPVideoPlayer] that handles Koin ViewModel creation.
 */
@Composable
private fun VideoPlayer(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    viewModel: RTSPVideoPlayerViewModel? = null,
    onImageAvailable: (RTSPItem, ImageBitmap) -> Unit,
) {
    val rtspViewModel = viewModel ?: koinViewModel<RTSPVideoPlayerViewModel> {
        parametersOf(
            item.uri,
            item.forceTcp,
            { bitmap: ImageBitmap -> onImageAvailable(item, bitmap) }
        )
    }

    val state by rtspViewModel.state.collectAsStateWithLifecycle(
        RTSPVideoPlayerState(
            RTSPVideoPlayerPlaybackState.Idle,
            null
        )
    )
    KeepScreenOn(state.playback == RTSPVideoPlayerPlaybackState.Playing)

    RTSPVideoPlayer(
        viewModel = rtspViewModel,
        modifier = modifier,
    )
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
            Text(text = stringResource(Res.string.delete_dialog_title).formatText(rtspItem.name))
        },
        text = { Text(text = stringResource(Res.string.delete_dialog_message)) },
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onConfirmation,
                modifier = Modifier.testTag("DeleteConfirmButton")
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("DeleteCancelButton")
            ) {
                Text(stringResource(Res.string.cancel))
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
    uiMode = UI_MODE_NIGHT_NO,
    device = "spec:orientation=portrait,width=411dp,height=891dp"
)
@Preview(
    name = "Day Landscape",
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_NO,
    device = "spec:orientation=landscape,width=411dp,height=891dp"
)
@Preview(
    name = "Night Portrait",
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:orientation=portrait,width=411dp,height=891dp"
)
@Preview(
    name = "Night Landscape",
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:orientation=landscape,width=411dp,height=891dp"
)
@Composable
private fun StreamItemScreenPreview(
    @PreviewParameter(StreamItemScreenPreviewParameterProvider::class) params: Pair<RTSPItem, MoreOptionState>,
) {
    val mockVideoPlayer = remember {
        object : com.mrkongtk.rtspviewer.shared.player.RTSPVideoPlayer {
            override val currentState: StateFlow<RTSPVideoPlayerPlaybackState> =
                MutableStateFlow(RTSPVideoPlayerPlaybackState.Idle)
            override val videoAspectRatio: StateFlow<Float> = MutableStateFlow(16f / 9f)
            override val error: StateFlow<Throwable?> = MutableStateFlow(null)
            override fun prepare(uri: String, forceTcp: Boolean) {}
            override fun play() {}
            override fun stop() {}
            override fun release() {}
            override fun <T> getPlayer(): T? = null
        }
    }

    val mockViewModel = remember(params.first) {
        RTSPVideoPlayerViewModel(
            videoPlayer = mockVideoPlayer,
            initialUri = params.first.uri,
            initialForceTcp = params.first.forceTcp,
            onImageAvailable = null
        )
    }

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
                    override fun onImageAvailable(item: RTSPItem, bitmap: ImageBitmap) {}
                },
                playerViewModel = mockViewModel
            )
        }
    }
}

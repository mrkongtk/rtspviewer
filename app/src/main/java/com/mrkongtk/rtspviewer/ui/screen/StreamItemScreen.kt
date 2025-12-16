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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.mrkongtk.rtspviewer.viewmode.RTSPVideoPlayerViewModel

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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // State to manage the visibility of the "More Options" dropdown
        var moreState by remember { mutableStateOf(MoreOptionState.fromValue(moreOption)) }

        // Top section: The Video Player
        VideoPlayerCompose(Modifier.fillMaxWidth(), item)

        // -- Definition of UI Rows for Metadata --

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
                // Read-only checkbox to show the setting state
                Checkbox(item.forceTcp, onCheckedChange = null, enabled = false)
            }
        }

        // Render the metadata rows
        val rows = listOf(nameRow, uriRow, forceTcpRow, tagsRow)
        rows.fastForEach {
            it(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingM)
            )
        }

        // Floating Action Button / Menu Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingM),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column {
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
                            onDeleteItemSelected()
                        }
                    )
                }
                IconButton(
                    onClick = {
                        moreState = !moreState
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
    }
}

/**
 * A wrapper composable for the Video Player.
 *
 * This handles the distinction between running in a UI Preview (where Hilt is unavailable)
 * and running in the actual application.
 *
 * - In **Preview Mode**: Renders a placeholder box.
 * - In **App Mode**: Initializes the [RTSPVideoPlayerViewModel] using Assisted Injection
 *   to pass the URI and TCP preferences, then renders the [RTSPVideoPlayer].
 *
 * @param modifier Modifier for the player container.
 * @param item The RTSP item containing connection details.
 */
@Composable
private fun VideoPlayerCompose(
    modifier: Modifier = Modifier,
    item: RTSPItem
) {
    if (LocalInspectionMode.current) {
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
            modifier = modifier,
        )
    }
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
    val regex = "(rtsp://)(.+)(:)(.+)(@)".toRegex()
    if (regex.containsMatchIn(uri)) {
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

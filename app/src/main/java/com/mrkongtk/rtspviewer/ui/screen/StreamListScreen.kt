package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.graphics.createBitmap
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.MoreOptionState
import com.mrkongtk.rtspviewer.data.not
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.DraggableLazyColumn
import com.mrkongtk.rtspviewer.ui.compose.StreamListItem
import com.mrkongtk.rtspviewer.ui.compose.StreamSortingItem
import com.mrkongtk.rtspviewer.ui.screen.action.StreamListScreenActions
import com.mrkongtk.rtspviewer.ui.theme.ErrorColor
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * The main dashboard screen for viewing and managing RTSP streams.
 *
 * This screen handles three primary UI states:
 * 1. **Empty State:** Shows a placeholder when no streams exist.
 * 2. **Viewing State:** Displays a filterable list of streams using a [LazyColumn].
 * 3. **Reordering State:** A dedicated drag-and-drop mode for manual list sorting.
 *
 * @param modifier The [Modifier] for the root container.
 * @param itemList The source list of [RTSPItem] entities from the database.
 * @param previews A map of stream IDs to their latest [Bitmap] snapshots.
 * @param tags Unique categories derived from the available streams.
 * @param selectedTag The active filter; `null` represents the "All" view.
 * @param screenActions Interface for handling user interactions like selection, addition, and sorting.
 */
@Composable
fun StreamListScreen(
    modifier: Modifier = Modifier,
    itemList: List<RTSPItem>,
    previews: Map<Long, ImageBitmap>,
    tags: List<String>,
    selectedTag: String?,
    screenActions: StreamListScreenActions,
) {
    var isSorting by remember { mutableStateOf(false) }
    var isOptionOpening by remember { mutableStateOf(MoreOptionState.CLOSED) }

    if (itemList.isEmpty()) {
        // State 1: Empty - Guidance for new users
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_streaming_items),
                    modifier = Modifier.testTag("StreamListScreenEmptyText")
                )
            }
        }
    } else if (isSorting) {
        // State 2: Sorting Mode - Drag-and-drop reordering
        ReorderItemList(
            modifier = modifier,
            itemList = itemList,
            previews = previews,
            onItemsReordered = { screenActions.onItemsReordered(it) },
        )
    } else {
        // State 3: Viewing Mode - Content list with category filtering
        val allTags = listOf(stringResource(R.string.tags_all)) + tags
        val selectedTagIndex = selectedTag?.let { allTags.indexOf(it) } ?: 0

        StreamItemList(
            modifier = modifier,
            allTags = allTags,
            selectedTagIndex = selectedTagIndex,
            itemList = itemList,
            previews = previews,
            onTagSelected = { index ->
                // Map index 0 back to null (All) for the business logic
                if (index == 0) {
                    screenActions.onTagSelected(null)
                } else {
                    screenActions.onTagSelected(allTags.getOrNull(index))
                }
            },
            onItemsReordered = { screenActions.onItemsReordered(it) },
            onItemSelected = { screenActions.onItemSelected(it) },
        )
    }

    // Floating Action Menu: Handles both "Add" and "Enter Sorting Mode"
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(PaddingM),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column {
            DropdownMenu(
                expanded = isOptionOpening.value,
                onDismissRequest = { isOptionOpening = MoreOptionState.CLOSED }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_rtsp_button)) },
                    leadingIcon = { Icon(Icons.Default.Add, null) },
                    onClick = {
                        isOptionOpening = MoreOptionState.CLOSED
                        screenActions.onAddItemSelected()
                    },
                    modifier = Modifier.testTag("AddButton")
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_rtsp_button)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
                    onClick = {
                        isOptionOpening = MoreOptionState.CLOSED
                        isSorting = true
                    },
                    modifier = Modifier.testTag("SortButton")
                )
            }

            IconButton(
                onClick = {
                    if (isSorting) {
                        isSorting = false // Exit sorting mode
                    } else {
                        isOptionOpening = !isOptionOpening // Toggle more options
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(),
                modifier = Modifier.testTag(
                    if (isSorting) "EndSortingButton"
                    else if (isOptionOpening == MoreOptionState.OPEN) "CloseMoreButton"
                    else "MoreButton"
                )
            ) {
                // Icon switches between 'Close' (in sorting/open menu) and 'More' (idle)
                val isActive = isSorting || isOptionOpening == MoreOptionState.OPEN
                Icon(
                    imageVector = if (isActive) Icons.Default.Close else Icons.Default.MoreVert,
                    contentDescription = stringResource(
                        if (isSorting) R.string.description_end_sorting_button
                        else if (isOptionOpening == MoreOptionState.OPEN) R.string.description_close_button
                        else R.string.description_more_button
                    )
                )
            }
        }
    }
}

/**
 * Displays the list of RTSP streams with a horizontal tag filter at the top.
 */
@Composable
internal fun StreamItemList(
    modifier: Modifier,
    allTags: List<String>,
    selectedTagIndex: Int,
    itemList: List<RTSPItem>,
    previews: Map<Long, ImageBitmap>,
    onTagSelected: (Int) -> Unit,
    onItemsReordered: (List<RTSPItem>) -> Unit,
    onItemSelected: (RTSPItem) -> Unit
) {
    Column(
        modifier = modifier.padding(vertical = PaddingM),
        verticalArrangement = Arrangement.spacedBy(PaddingM)
    ) {
        // Tag Cloud: Wraps to multiple lines if tags exceed screen width
        FlowRow(
            modifier = Modifier
                .testTag("Tags")
                .fillMaxWidth()
                .padding(horizontal = PaddingM),
            horizontalArrangement = Arrangement.spacedBy(PaddingS),
            verticalArrangement = Arrangement.spacedBy(PaddingXs),
        ) {
            allTags.fastForEachIndexed { index, tag ->
                val selected = index == selectedTagIndex
                val backgroundColour =
                    if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondary
                val textColour =
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondary
                val borderColour = if (selected) textColour else Color.Transparent
                val shape = RoundedCornerShape(PaddingXs)

                Text(
                    modifier = Modifier
                        .background(color = backgroundColour, shape = shape)
                        .border(1.dp, borderColour, shape)
                        .padding(horizontal = PaddingS)
                        .testTag("Tag $tag")
                        .selectable(selected, onClick = { onTagSelected(index) }),
                    text = tag,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColour,
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .testTag("LazyColumn")
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(PaddingM),
        ) {
            // Filter logic: Show all if 'All' is selected, otherwise filter by tag string
            val filteredList = if (selectedTagIndex == 0) {
                itemList
            } else {
                allTags.getOrNull(selectedTagIndex)?.let { tag ->
                    itemList.fastFilter { it.tags.contains(tag) }
                } ?: itemList
            }

            items(filteredList) { item ->
                StreamListItem(
                    data = item,
                    preview = previews[item.id],
                    modifier = Modifier
                        .testTag("StreamListItem: ${item.id}")
                        .fillMaxWidth()
                        .padding(horizontal = PaddingM),
                    onClick = { data -> onItemSelected(data) }
                )
            }
        }
    }
}

/**
 * A dedicated view for manual stream reordering.
 * Uses [DraggableLazyColumn] to allow users to change the sequence of items.
 */
@Composable
internal fun ReorderItemList(
    modifier: Modifier,
    itemList: List<RTSPItem>,
    previews: Map<Long, ImageBitmap>,
    onItemsReordered: (List<RTSPItem>) -> Unit,
) {
    Column(
        modifier = modifier.padding(vertical = PaddingM),
        verticalArrangement = Arrangement.spacedBy(PaddingM)
    ) {
        DraggableLazyColumn(
            modifier = Modifier
                .testTag("DraggableLazyColumn")
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(PaddingM),
            items = itemList,
            onReordered = { reorderedList ->
                // Normalize the 'order' property based on new visual indices before saving to DB
                val updatedOrderList = reorderedList.mapIndexed { index, item ->
                    item.copy(order = index)
                }
                onItemsReordered(updatedOrderList)
            }
        ) { itemModifier, item ->
            StreamSortingItem(
                data = item,
                preview = previews[item.id],
                modifier = itemModifier
                    .testTag("StreamSortingItem: ${item.id}")
                    .fillMaxWidth()
                    .padding(horizontal = PaddingM),
            )
        }
    }
}

/**
 * Preview provider to test Empty, Populated, and Filtered states in the IDE.
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
private fun StreamListScreenPreview(@PreviewParameter(StreamListScreenPreviewParameterProvider::class) mockData: ProviderData) {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Generate a dummy bitmap placeholder for preview purposes
            val bmp = createBitmap(1920, 1080).apply {
                Canvas(this).drawColor(ErrorColor.toArgb())
            }.asImageBitmap()
            val mockPreviews = mapOf(1L to bmp, 4L to bmp)

            StreamListScreen(
                itemList = mockData.items,
                previews = mockPreviews,
                tags = mockData.tags,
                selectedTag = mockData.selectedTag,
                screenActions = object : StreamListScreenActions {
                    override fun onItemSelected(item: RTSPItem) {}
                    override fun onAddItemSelected() {}
                    override fun onItemsReordered(orderedList: List<RTSPItem>) {}
                    override fun onTagSelected(tag: String?) {}
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

/**
 * Provides variations of data to the Preview:
 * 1. Empty State
 * 2. Populated list with tags (nothing selected)
 * 3. Populated list with a specific tag filtered
 */
private class StreamListScreenPreviewParameterProvider :
    PreviewParameterProvider<ProviderData> {
    override val values: Sequence<ProviderData>
        get() {
            // Clean up LoremIpsum for use as tag labels
            val lorem = (LoremIpsum(100).values.toList().firstOrNull() ?: "").replace(
                "[.\n\r]".toRegex(),
                ""
            ).split(" ").mapNotNull { it.trim().ifEmpty { null } }

            return sequenceOf(
                ProviderData(), // Case 1: Empty
                ProviderData( // Case 2: Populated, No selection
                    items = listOf(
                        RTSPItem(1, "Living Room Camera", "rtsp://10.0.0.1", emptyList(), 1),
                        RTSPItem(2, "Backyard", "rtsp://10.0.0.2", lorem.slice(2..10), 2)
                    ),
                    tags = lorem.slice(10..<20),
                    selectedTag = null
                ),
                ProviderData( // Case 3: Populated, Filter active
                    items = listOf(
                        RTSPItem(1, "Living Room Camera", "rtsp://10.0.0.1", emptyList(), 1),
                        RTSPItem(2, "Backyard", "rtsp://10.0.0.2", lorem.slice(2..10), 2)
                    ),
                    tags = lorem.slice(10..<20),
                    selectedTag = lorem[10]
                )
            )
        }
}

/**
 * Internal helper class for [StreamListScreenPreviewParameterProvider].
 */
private data class ProviderData(
    val items: List<RTSPItem> = emptyList(),
    val tags: List<String> = emptyList(),
    val selectedTag: String? = null,
)

package com.mrkongtk.rtspviewer.shared.ui.screen

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.compose.DraggableLazyColumn
import com.mrkongtk.rtspviewer.shared.ui.compose.StreamListItem
import com.mrkongtk.rtspviewer.shared.ui.compose.StreamSortingItem
import com.mrkongtk.rtspviewer.shared.ui.screen.action.StreamListScreenActions
import com.mrkongtk.rtspviewer.shared.ui.state.MoreOptionState
import com.mrkongtk.rtspviewer.shared.ui.state.not
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.createPlainImage
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.add_rtsp_button
import rtspviewer.shared.generated.resources.description_close_button
import rtspviewer.shared.generated.resources.description_end_sorting_button
import rtspviewer.shared.generated.resources.description_more_button
import rtspviewer.shared.generated.resources.no_streaming_items
import rtspviewer.shared.generated.resources.sort_rtsp_button
import rtspviewer.shared.generated.resources.tags_all

/**
 * Main dashboard screen for viewing and managing RTSP streams.
 *
 * Supports three primary modes:
 * 1. Empty: Placeholder when no streams are configured.
 * 2. Viewing: Filterable list of available streams.
 * 3. Sorting: Manual drag-and-drop reordering of the stream list.
 *
 * @param modifier Root container modifier.
 * @param itemList List of [RTSPItem] streams from the data source.
 * @param previews Map of stream IDs to their cached preview thumbnails.
 * @param tags List of available stream categories for filtering.
 * @param selectedTag Currently active filter tag; null represents "All".
 * @param screenActions Callbacks for handling user interactions.
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
    var reorderedItemList by remember(itemList) { mutableStateOf(itemList) }

    if (itemList.isEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.no_streaming_items),
                    modifier = Modifier.testTag("StreamListScreenEmptyText")
                )
            }
        }
    } else if (isSorting) {
        ReorderItemList(
            modifier = modifier,
            itemList = itemList,
            previews = previews,
            onItemsReordered = { reorderedItemList = it },
        )
    } else {
        val allTags = listOf(stringResource(Res.string.tags_all)) + tags
        val selectedTagIndex = selectedTag?.let { allTags.indexOf(it) } ?: 0

        StreamItemList(
            modifier = modifier,
            allTags = allTags,
            selectedTagIndex = selectedTagIndex,
            itemList = itemList,
            previews = previews,
            onTagSelected = { index ->
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
                    text = { Text(stringResource(Res.string.add_rtsp_button)) },
                    leadingIcon = { Icon(Icons.Default.Add, null) },
                    onClick = {
                        isOptionOpening = MoreOptionState.CLOSED
                        screenActions.onAddItemSelected()
                    },
                    modifier = Modifier.testTag("AddButton")
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.sort_rtsp_button)) },
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
                        isSorting = false
                        val hasOrderChanged = itemList.size != reorderedItemList.size ||
                                itemList.indices.any { i -> itemList[i].id != reorderedItemList[i].id }
                        if (hasOrderChanged) {
                            screenActions.onItemsReordered(reorderedItemList)
                        }
                    } else {
                        isOptionOpening = !isOptionOpening
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(),
                modifier = Modifier.testTag(
                    if (isSorting) "EndSortingButton"
                    else if (isOptionOpening == MoreOptionState.OPEN) "CloseMoreButton"
                    else "MoreButton"
                )
            ) {
                val isActive = isSorting || isOptionOpening == MoreOptionState.OPEN
                Icon(
                    imageVector = if (isActive) Icons.Default.Close else Icons.Default.MoreVert,
                    contentDescription = stringResource(
                        if (isSorting) Res.string.description_end_sorting_button
                        else if (isOptionOpening == MoreOptionState.OPEN) Res.string.description_close_button
                        else Res.string.description_more_button
                    )
                )
            }
        }
    }
}

/**
 * Displays the list of RTSP streams with category filtering.
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
 * View for manual stream reordering using drag-and-drop.
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
private fun StreamListScreenPreview(@PreviewParameter(StreamListScreenPreviewParameterProvider::class) mockData: ProviderData) {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            val bmp = ImageBitmap.createPlainImage(1920, 1080)
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

private class StreamListScreenPreviewParameterProvider :
    PreviewParameterProvider<ProviderData> {
    override val values: Sequence<ProviderData>
        get() {
            val lorem = (LoremIpsum(100).values.toList().firstOrNull() ?: "").replace(
                "[.\n\r]".toRegex(),
                ""
            ).split(" ").mapNotNull { it.trim().ifEmpty { null } }

            return sequenceOf(
                ProviderData(),
                ProviderData(
                    items = listOf(
                        RTSPItem(1, "Living Room Camera", "rtsp://10.0.0.1", emptyList(), 1),
                        RTSPItem(2, "Backyard", "rtsp://10.0.0.2", lorem.slice(2..10), 2)
                    ),
                    tags = lorem.slice(10..<20),
                    selectedTag = null
                ),
                ProviderData(
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

private data class ProviderData(
    val items: List<RTSPItem> = emptyList(),
    val tags: List<String> = emptyList(),
    val selectedTag: String? = null,
)

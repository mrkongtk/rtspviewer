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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.DraggableLazyColumn
import com.mrkongtk.rtspviewer.ui.compose.StreamListItem
import com.mrkongtk.rtspviewer.ui.theme.ErrorColor
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * High-level screen for managing and viewing RTSP streams.
 *
 * It manages three primary UI states:
 * 1. **Empty State:** Displays a placeholder when no streams are configured.
 * 2. **Populated State:** Displays a filterable tag row and a reorderable list of streams.
 * 3. **Action Layer:** Provides a Floating Action Button (FAB) for adding new streams.
 *
 * @param itemList The source list of [RTSPItem] entities from the database.
 * @param previews A mapping of item IDs to their latest [Bitmap] snapshots.
 * @param tags The unique list of tags available across all streams.
 * @param selectedTag The currently active filter tag (null if 'All' is selected).
 * @param onItemSelected Invoked when a user taps a specific stream card.
 * @param onAddItemSelected Invoked when the 'Add' FAB is clicked.
 * @param onItemsReordered Invoked after a drag-and-drop gesture; provides the full list
 *                         with updated [RTSPItem.order] values for persistence.
 * @param onTagSelected Invoked when a filter chip is tapped.
 * @param modifier Root modifier for the screen container.
 */
@Composable
fun StreamListScreen(
    itemList: List<RTSPItem>,
    previews: Map<Long, Bitmap>,
    tags: List<String>,
    selectedTag: String?,
    onItemSelected: (RTSPItem) -> Unit,
    onAddItemSelected: () -> Unit,
    onItemsReordered: (List<RTSPItem>) -> Unit,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (itemList.isEmpty()) {
        // State 1: Empty - Inform the user there is nothing to show
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
    } else {
        // State 2: Content - Show tags and the list
        // Prepend the "All" category to the tag list for the UI filter row
        val allTags = listOf(stringResource(R.string.tags_all)) + tags

        // Determine the UI index based on the selectedTag string
        val selectedTagIndex = selectedTag?.let { allTags.indexOf(it) } ?: 0

        StreamItemList(
            modifier = modifier,
            allTags = allTags,
            selectedTagIndex = selectedTagIndex,
            itemList = itemList,
            previews = previews,
            onTagSelected = { index ->
                // If index 0 is selected, it represents 'All' (null)
                if (index == 0) {
                    onTagSelected(null)
                } else {
                    onTagSelected(allTags.getOrNull(index))
                }
            },
            onItemsReordered = onItemsReordered,
            onItemSelected = onItemSelected
        )
    }

    // State 3: Overlay - Floating Add Button
    // Wrapped in a Box to ensure it stays on top of the list content
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingM),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            IconButton(
                modifier = Modifier.testTag("AddButton"),
                onClick = { onAddItemSelected() },
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_rtsp_button)
                )
            }
        }
    }
}

/**
 * Internal layout responsible for rendering the horizontal tag filter and the reorderable list.
 *
 * @param allTags Combined list of tags including the static "All" entry.
 * @param selectedTagIndex The integer index of the currently active filter.
 */
@Composable
internal fun StreamItemList(
    modifier: Modifier,
    allTags: List<String>,
    selectedTagIndex: Int,
    itemList: List<RTSPItem>,
    previews: Map<Long, Bitmap>,
    onTagSelected: (Int) -> Unit,
    onItemsReordered: (List<RTSPItem>) -> Unit,
    onItemSelected: (RTSPItem) -> Unit
) {
    Column(
        modifier = modifier.padding(vertical = PaddingM),
        verticalArrangement = Arrangement.spacedBy(PaddingM)
    ) {
        // Filter Section: Displays tags in a multi-line flow if they exceed width
        FlowRow(
            modifier = Modifier
                .testTag("Tags")
                .fillMaxWidth()
                .padding(horizontal = PaddingM),
            horizontalArrangement = Arrangement.spacedBy(
                space = PaddingS,
                alignment = Alignment.Start
            ),
            verticalArrangement = Arrangement.spacedBy(PaddingXs),
        ) {
            // Use fastForEachIndexed (Compose util) for lower overhead in the UI loop
            allTags.fastForEachIndexed { index, tag ->
                val selected = index == selectedTagIndex

                // Determine styling based on selection state
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

        // List Section: Draggable items using custom DraggableLazyColumn
        DraggableLazyColumn(
            modifier = Modifier
                .testTag("LazyColumn")
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(PaddingM),
            // Logic: Show all items if 'All' (index 0) is selected,
            // otherwise perform a fast filter based on the tag string.
            items = if (selectedTagIndex == 0) {
                itemList
            } else {
                allTags.getOrNull(selectedTagIndex)?.let { selectedTag ->
                    itemList.fastFilter { it.tags.contains(selectedTag) }
                } ?: itemList
            },
            onReordered = { reorderedList ->
                // Normalization: Map the new UI positions back to the 'order' property.
                // This ensures that the DB reflects exactly what the user sees.
                val updatedOrderList = reorderedList.mapIndexed { index, item ->
                    item.copy(order = index)
                }
                onItemsReordered(updatedOrderList)
            }
        ) { itemModifier, item ->
            // Custom item renderer for individual RTSP streams
            StreamListItem(
                data = item,
                preview = previews[item.id],
                modifier = itemModifier
                    .testTag("StreamListItem: ${item.id}")
                    .fillMaxWidth()
                    .padding(horizontal = PaddingM),
                onClick = { data -> onItemSelected(data) }
            )
        }
    }
}

/**
 * Preview definitions for IDE design-time support.
 * Covers both Light and Dark modes to ensure visibility of tags and text.
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
            }
            val mockPreviews = mapOf(1L to bmp, 4L to bmp)

            StreamListScreen(
                itemList = mockData.items,
                previews = mockPreviews,
                tags = mockData.tags,
                selectedTag = mockData.selectedTag,
                onItemSelected = {},
                onAddItemSelected = {},
                onItemsReordered = {},
                onTagSelected = {},
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

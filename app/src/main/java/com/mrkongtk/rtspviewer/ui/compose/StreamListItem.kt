package com.mrkongtk.rtspviewer.ui.compose

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.createBitmap
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.ErrorColor
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.ui.theme.PreviewWidth
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.ui.theme.RoundedCornerSize

/**
 * A list item component representing a single RTSP stream configuration.
 *
 * This composable renders a clickable [ElevatedCard] designed to display:
 * 1. An optional live snapshot/thumbnail of the stream.
 * 2. The stream's display name.
 * 3. A collection of tags associated with the stream (wrapped automatically).
 * 4. A navigation indicator.
 *
 * @param modifier The [Modifier] to be applied to the outer Card layout.
 * @param data The [RTSPItem] domain entity containing stream details (name, URL, tags, etc.).
 * @param preview An optional [Bitmap] representing the latest snapshot of the stream.
 *                If `null`, the image section is completely hidden.
 * @param onClick A callback lambda triggered when the card is tapped; passes the associated [data] item.
 */
@Composable
fun StreamListItem(
    modifier: Modifier = Modifier,
    data: RTSPItem,
    preview: Bitmap?,
    onClick: (RTSPItem) -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = PaddingS,
        ),
        shape = RoundedCornerShape(RoundedCornerSize),
        modifier = modifier,
        onClick = { onClick(data) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingM),
            // Distribute space: Content aligned to start, Icon aligned to end
            horizontalArrangement = Arrangement.SpaceBetween,
            // Vertically center all elements (Thumbnail, Text block, Icon)
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Thumbnail Section ---
            // Only render the image component if a valid bitmap is provided.
            preview?.let {
                Image(
                    modifier = Modifier
                        .width(PreviewWidth)
                        // Calculate aspect ratio dynamically based on the bitmap dimensions
                        .aspectRatio(it.width.toFloat() / it.height.toFloat())
                        .clip(RoundedCornerShape(RoundedCornerSize)),
                    bitmap = it.asImageBitmap(),
                    // Accessibility: injects the stream name into the description for screen readers
                    contentDescription = stringResource(R.string.rtsp_item_preview_description).replace(
                        "%1",
                        data.name
                    )
                )
                // Spacing between Thumbnail and Text
                Spacer(modifier = Modifier.width(PaddingM))
            }

            // --- Details Section (Name & Tags) ---
            // Using .weight(1f) ensures this column takes up all remaining space between
            // the image and the arrow icon, preventing overlaps.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleMedium
                )

                // Render tags in a flexible row that wraps to new lines if space runs out
                if (data.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = PaddingS,
                            alignment = Alignment.Start
                        ),
                        // verticalArrangement can be added here if spacing between rows is needed
                    ) {
                        data.tags.fastForEach { tag ->
                            // Individual Tag Chip styling
                            Text(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(PaddingXs)
                                    )
                                    .padding(horizontal = PaddingS)
                                    .testTag("Tag $tag"),
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            // --- Navigation Indicator ---
            // Uses 'AutoMirrored' to ensure the arrow points correctly in RTL (Right-to-Left) locales.
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.detail),
            )
        }
    }
}

/**
 * Preview provider for [StreamListItem].
 *
 * Demonstrates the component in the following scenarios:
 * 1. **Day Mode**: Standard light theme.
 * 2. **Night Mode**: Dark theme.
 * 3. **Variations**: Items with/without previews and items with/without tags.
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
private fun StreamListItemPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // --- Mock Data Generation ---
            // Create a dummy Bitmap to simulate a camera snapshot
            val w = 1920
            val h = 1080
            val bmp = createBitmap(w, h).let {
                val canvas = Canvas(it)
                // Draw a placeholder color
                canvas.drawColor(ErrorColor.toArgb())
                it
            }

            // Define various RTSP items to test different UI states
            val mockItems = listOf(
                // Case 1: Standard item with no tags
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                // Case 2: Item without preview image
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 2),
                // Case 3: Item with tags
                RTSPItem(
                    3,
                    "Kitchen Camera",
                    "rtsp://192.168.1.10",
                    listOf("indoor", "ground-floor"),
                    1
                ),
                // Case 4: Item with tags but no preview
                RTSPItem(
                    4,
                    "Garage Camera",
                    "rtsp://192.168.1.11",
                    listOf("outdoor", "security"),
                    3
                ),
            )

            // Map specific items to the mock bitmap (Items 1 and 3 get images)
            val mockPreviews = mapOf(
                Pair(1L, bmp),
                Pair(3L, bmp)
            )

            // --- Preview Layout ---
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(PaddingS) // External margin for the list
            ) {
                mockItems.fastForEach { item ->
                    StreamListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = PaddingS),
                        data = item,
                        preview = mockPreviews[item.id],
                        onClick = {}
                    )
                }
            }
        }
    }
}

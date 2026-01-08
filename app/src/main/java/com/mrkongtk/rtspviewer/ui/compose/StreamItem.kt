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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
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
 * A UI component representing a single RTSP stream entry in a list.
 *
 * This component displays a summary of the stream, including a thumbnail (if available),
 * the stream name, and any associated tags. It is designed to be used within a [LazyColumn]
 * or a standard [Column].
 *
 * @param modifier [Modifier] to be applied to the [ElevatedCard] container.
 * @param data The [RTSPItem] entity containing the stream metadata (name, url, tags, etc.).
 * @param preview An optional [Bitmap] snapshot of the live stream. If null, the preview area is hidden.
 * @param icon A trailing composable (e.g., a Chevron or Edit button) to be placed at the end of the row.
 * @param onClick Lambda invoked when the user taps anywhere on the card.
 */
@Composable
fun StreamItem(
    modifier: Modifier = Modifier,
    data: RTSPItem,
    preview: Bitmap?,
    icon: @Composable () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Thumbnail Section ---
            // We only show the Image and the following Spacer if a preview bitmap exists.
            preview?.let {
                Image(
                    modifier = Modifier
                        .width(PreviewWidth)
                        // Uses the bitmap's intrinsic dimensions to maintain aspect ratio
                        // and prevent "squashing" or "stretching" regardless of camera resolution.
                        .aspectRatio(it.width.toFloat() / it.height.toFloat())
                        .clip(RoundedCornerShape(RoundedCornerSize)),
                    bitmap = it.asImageBitmap(),
                    contentDescription = stringResource(R.string.rtsp_item_preview_description)
                        .replace("%1", data.name)
                )
                Spacer(modifier = Modifier.width(PaddingM))
            }

            // --- Content Section (Title & Tags) ---
            Column(
                // .weight(1f) is critical: it tells this column to take up all remaining horizontal
                // space between the thumbnail (start) and the icon (end).
                // This ensures the trailing icon is pushed to the far right.
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PaddingS)
            ) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1 // Prevents UI breakage if the camera name is exceptionally long
                )

                if (data.tags.isNotEmpty()) {
                    // FlowRow is used instead of a standard Row to allow tags to wrap
                    // onto multiple lines if they don't fit the screen width.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = PaddingS,
                            alignment = Alignment.Start
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaddingXs)
                    ) {
                        // fastForEach (from androidx.compose.ui.util) is an optimized loop
                        // for Lists that avoids iterator allocation, improving scroll performance.
                        data.tags.fastForEach { tag ->
                            Text(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(PaddingXs)
                                    )
                                    .padding(horizontal = PaddingS)
                                    // testTag provides a specific handle for UI Automator or Compose tests
                                    .testTag("Tag $tag"),
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                }
            }

            // --- Trailing Icon ---
            // This container holds whatever action component (Chevron/Edit/Delete) is passed in.
            icon()
        }
    }
}

/**
 * Visual Preview for [StreamItem] within Android Studio.
 *
 * Tests multiple scenarios:
 * 1. Default state (No image/tags)
 * 2. Active state (With image)
 * 3. Tagged state (Short list of tags)
 * 4. Stress test (Many tags causing wrapping via FlowRow)
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
private fun StreamItemPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // --- Mock Data Generation ---

            // Create a dummy bitmap (16:9) to simulate a camera snapshot
            val w = 1920
            val h = 1080
            val bmp = createBitmap(w, h).let {
                val canvas = Canvas(it)
                canvas.drawColor(ErrorColor.toArgb()) // Use a distinct color for visual confirmation
                it
            }

            // Generate a list of random strings to simulate camera tags
            val lorem = (LoremIpsum(100).values.toList().firstOrNull() ?: "")
                .replace("[.\n\r]".toRegex(), "")
                .split(" ")
                .mapNotNull { it.trim().ifEmpty { null } }

            val mockItems = listOf(
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 2),
                RTSPItem(
                    3,
                    "Kitchen Camera",
                    "rtsp://192.168.1.10",
                    lorem.slice(0..2),
                    1
                ),
                RTSPItem(
                    4,
                    "Garage Camera",
                    "rtsp://192.168.1.11",
                    lorem.slice(3..10), // Stress test for FlowRow wrapping
                    3
                ),
            )

            // Map item IDs to the dummy bitmap for conditional rendering
            val mockPreviews = mapOf(
                Pair(1L, bmp),
                Pair(3L, bmp)
            )

            // --- Preview Layout ---
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(PaddingS)
            ) {
                mockItems.fastForEach { item ->
                    StreamItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = PaddingS),
                        data = item,
                        preview = mockPreviews[item.id],
                        icon = { /* Leave empty for preview or add a Chevron icon here */ },
                        onClick = {}
                    )
                }
            }
        }
    }
}

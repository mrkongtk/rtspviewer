package com.mrkongtk.rtspviewer.ui.compose

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.createBitmap
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.ErrorColor
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * A specialized version of [StreamItem] designed for the sorting/reordering screen.
 *
 * It displays a drag-handle icon (Dehaze) to indicate to the user that the item
 * can be moved within a list.
 *
 * @param modifier Layout modifiers for the item container.
 * @param data The [RTSPItem] entity containing stream details (name, url, tags).
 * @param preview An optional [Bitmap] representing the last captured frame of the stream.
 */
@Composable
fun StreamSortingItem(
    modifier: Modifier = Modifier,
    data: RTSPItem,
    preview: Bitmap?,
) {
    StreamItem(
        modifier = modifier,
        data = data,
        preview = preview,
        // The 'trailing' icon used as a visual cue for drag-and-drop sorting
        icon = {
            Icon(
                imageVector = Icons.Default.Dehaze,
                contentDescription = stringResource(R.string.reorder),
            )
        },
        // onClick is disabled here because interaction is usually handled by
        // a drag-and-drop listener in the parent lazy list.
        onClick = {}
    )
}

/**
 * Previews the [StreamSortingItem] in various states and themes.
 *
 * This preview covers:
 * 1. Day and Night modes.
 * 2. Items with and without preview thumbnails.
 * 3. Items with no tags, a few tags, and many tags (to test text wrapping).
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
private fun StreamSortingItemPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // --- Mock Data Generation ---

            // 1. Create a dummy solid-color bitmap to simulate a camera thumbnail
            val w = 1920
            val h = 1080
            val bmp = createBitmap(w, h).let {
                val canvas = Canvas(it)
                canvas.drawColor(ErrorColor.toArgb()) // Using ErrorColor as a placeholder fill
                it
            }

            // 2. Generate a list of clean strings from LoremIpsum to simulate stream tags
            val lorem = (LoremIpsum(100).values.toList().firstOrNull() ?: "")
                .replace("[.\n\r]".toRegex(), "")
                .split(" ")
                .mapNotNull { it.trim().ifEmpty { null } }

            // 3. Create mock items representing different UI scenarios
            val mockItems = listOf(
                // Scenario: Basic item, no tags, no image
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                // Scenario: Basic item, no tags, has image
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 2),
                // Scenario: Item with a few tags
                RTSPItem(
                    3,
                    "Kitchen Camera",
                    "rtsp://192.168.1.10",
                    lorem.slice(0..2),
                    1
                ),
                // Scenario: Item with many tags (tests FlowRow/wrapping logic)
                RTSPItem(
                    4,
                    "Garage Camera",
                    "rtsp://192.168.1.11",
                    lorem.slice(3..10),
                    3
                ),
            )

            // Map specific item IDs to the dummy bitmap to test the "no image" fallback UI
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
                // fastForEach is used for better performance in Compose previews/lists
                mockItems.fastForEach { item ->
                    StreamSortingItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = PaddingS),
                        data = item,
                        preview = mockPreviews[item.id],
                    )
                }
            }
        }
    }
}

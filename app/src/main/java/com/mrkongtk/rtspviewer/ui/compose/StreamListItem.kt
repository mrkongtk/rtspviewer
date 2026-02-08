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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
 * A specialized list item for displaying RTSP stream information.
 * This component acts as a high-level wrapper around the generic [StreamItem],
 * pre-configuring it with a chevron icon to indicate navigation/detail actions.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param data The [RTSPItem] entity containing stream details (name, url, tags).
 * @param preview A nullable [Bitmap] representing the video snapshot/thumbnail.
 * @param onClick Callback triggered when the item is tapped.
 */
@Composable
fun StreamListItem(
    modifier: Modifier = Modifier,
    data: RTSPItem,
    preview: Bitmap?,
    onClick: (RTSPItem) -> Unit,
) {
    StreamItem(
        modifier = modifier,
        data = data,
        preview = preview,
        icon = {
            Icon(
                // AutoMirrored ensures the arrow points correctly in Right-to-Left (RTL) locales
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.detail),
            )
        },
        onClick = onClick
    )
}

/**
 * Previews the [StreamListItem] in both Light and Dark themes.
 *
 * This preview covers multiple UI states:
 * 1. Items with and without thumbnails.
 * 2. Items with few tags vs. many tags (testing text wrapping/flow).
 * 3. Handling of system bar insets.
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

            // --- Mock Setup: Bitmap ---
            // Create a dummy solid-color bitmap to simulate a camera snapshot
            val w = 1920
            val h = 1080
            val bmp = createBitmap(w, h).let {
                val canvas = Canvas(it)
                canvas.drawColor(ErrorColor.toArgb())
                it
            }

            // --- Mock Setup: Tags ---
            // Generate a list of clean strings from LoremIpsum for tag testing
            val lorem = (LoremIpsum(100).values.toList().firstOrNull() ?: "")
                .replace("[.\n\r]".toRegex(), "")
                .split(" ")
                .mapNotNull { it.trim().ifEmpty { null } }

            // --- Mock Setup: Entities ---
            // Create a variety of items to test different UI constraints
            val mockItems = listOf(
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 2),
                RTSPItem(
                    3,
                    "Kitchen Camera",
                    "rtsp://192.168.1.10",
                    lorem.slice(0..2), // Typical number of tags
                    1
                ),
                RTSPItem(
                    4,
                    "Garage Camera",
                    "rtsp://192.168.1.11",
                    lorem.slice(3..10), // Extreme case: Many tags to test multi-line FlowRow
                    3
                ),
            )

            // Map specific item IDs to the dummy bitmap to test "No Preview" states
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
                // fastForEach is used here as a performance optimization for Compose
                // which avoids iterator allocation during recomposition.
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

package com.mrkongtk.rtspviewer.shared.ui.compose

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.util.fastForEach
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.util.createPlainImage
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.reorder

/**
 * A specialized [StreamItem] for sorting and reordering.
 *
 * Displays a drag-handle icon to indicate that the item can be moved.
 *
 * @param modifier The modifier to be applied to the item.
 * @param data The [RTSPItem] containing stream details.
 * @param preview An optional [ImageBitmap] of the stream's last captured frame.
 */
@Composable
fun StreamSortingItem(
    modifier: Modifier = Modifier,
    data: RTSPItem,
    preview: ImageBitmap?,
) {
    StreamItem(
        modifier = modifier,
        data = data,
        preview = preview,
        icon = {
            Icon(
                modifier = Modifier.testTag("reorder"),
                imageVector = Icons.Default.Dehaze,
                contentDescription = stringResource(Res.string.reorder),
            )
        },
        onClick = {} // Disabled for sorting screen
    )
}

/**
 * Preview for [StreamSortingItem] demonstrating various states and themes.
 */
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
private fun StreamSortingItemPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            val bmp = ImageBitmap.createPlainImage(1920, 1080, Color.Cyan)

            val lorem = (LoremIpsum(100).values.toList().firstOrNull() ?: "")
                .replace("[.\n\r]".toRegex(), "")
                .split(" ")
                .mapNotNull { it.trim().ifEmpty { null } }

            val mockItems = listOf(
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 2),
                RTSPItem(3, "Kitchen Camera", "rtsp://192.168.1.10", lorem.slice(0..2), 1),
                RTSPItem(4, "Garage Camera", "rtsp://192.168.1.11", lorem.slice(3..10), 3),
            )

            val mockPreviews = mapOf(
                1L to bmp,
                3L to bmp
            )

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(PaddingS)
            ) {
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

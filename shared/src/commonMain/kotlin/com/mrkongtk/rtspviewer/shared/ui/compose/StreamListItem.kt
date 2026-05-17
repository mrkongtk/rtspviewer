package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.chevron_forward_24px
import rtspviewer.shared.generated.resources.detail

/**
 * A specialized list item for displaying [RTSPItem] information.
 *
 * It wraps the generic [StreamItem] and adds a chevron icon to indicate navigation.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param data The [RTSPItem] containing stream details.
 * @param preview An optional [ImageBitmap] representing the stream snapshot.
 * @param onClick Callback triggered when the item is tapped.
 */
@Composable
fun StreamListItem(
    modifier: Modifier = Modifier,
    data: RTSPItem,
    preview: ImageBitmap?,
    onClick: (RTSPItem) -> Unit,
) {
    StreamItem(
        modifier = modifier,
        data = data,
        preview = preview,
        icon = {
            Icon(
                modifier = Modifier.testTag("detail"),
                painter = painterResource(Res.drawable.chevron_forward_24px),
                contentDescription = stringResource(Res.string.detail),
            )
        },
        onClick = onClick
    )
}

/**
 * Previews [StreamListItem] in both Light and Dark themes, covering multiple UI states
 * such as presence/absence of thumbnails and varying tag lengths.
 */
@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun StreamListItemPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            val w = 1920
            val h = 1080
            val bmp = ImageBitmap.createPlainImage(w, h, Color.Blue)

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
                    lorem.slice(3..10),
                    3
                ),
            )

            val mockPreviews = mapOf(
                Pair(1L, bmp),
                Pair(3L, bmp)
            )

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(PaddingS)
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

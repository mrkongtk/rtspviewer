package com.mrkongtk.rtspviewer.shared.ui.compose

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
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingXs
import com.mrkongtk.rtspviewer.shared.ui.theme.PreviewWidth
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.ui.theme.RoundedCornerSize
import com.mrkongtk.rtspviewer.shared.util.createPlainImage
import com.mrkongtk.rtspviewer.shared.util.formatText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.rtsp_item_preview_description
import rtspviewer.shared.generated.resources.video_label_24px

/**
 * A UI component representing a single RTSP stream entry.
 *
 * @param modifier [Modifier] to be applied to the card.
 * @param data The [RTSPItem] metadata for the stream.
 * @param preview Optional [ImageBitmap] snapshot of the stream.
 * @param icon Trailing action composable (e.g., Play, Edit).
 * @param onClick Callback invoked when the card is clicked.
 */
@Composable
fun StreamItem(
    modifier: Modifier = Modifier,
    data: RTSPItem,
    preview: ImageBitmap?,
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
            preview?.let {
                Image(
                    modifier = Modifier
                        .width(PreviewWidth)
                        .aspectRatio(it.width.toFloat() / it.height.toFloat())
                        .clip(RoundedCornerShape(RoundedCornerSize))
                        .testTag("thumbnail"),
                    bitmap = it,
                    contentDescription = stringResource(
                        Res.string.rtsp_item_preview_description
                    ).formatText(data.name),
                )
            } ?: run {
                Row(
                    modifier = Modifier
                        .width(PreviewWidth)
                        .aspectRatio(16.0f / 9.0f)
                        .clip(RoundedCornerShape(RoundedCornerSize))
                        .background(MaterialTheme.colorScheme.secondary)
                        .testTag("no thumbnail"),
                ) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize(),
                        painter = painterResource(Res.drawable.video_label_24px),
                        contentDescription = "No Stream Available",
                        tint = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(PaddingM))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PaddingS)
            ) {
                Text(
                    modifier = Modifier.testTag("item name"),
                    text = data.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                if (data.tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().testTag("item tags"),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = PaddingS,
                            alignment = Alignment.Start
                        ),
                        verticalArrangement = Arrangement.spacedBy(PaddingXs)
                    ) {
                        data.tags.fastForEach { tag ->
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

            icon()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun StreamItemPreview() {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(PaddingM),
                verticalArrangement = Arrangement.spacedBy(PaddingS)
            ) {
                val mockTags =
                    listOf("Living Room", "Outdoor", "Security", "Night Vision", "HD", "1080p")

                val mockItems = listOf(
                    RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                    RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 2),
                    RTSPItem(3, "Backyard Camera", "rtsp://192.168.1.11", mockTags.take(2), 3),
                    RTSPItem(
                        4,
                        "Garage Camera",
                        "rtsp://192.168.1.12",
                        mockTags,
                        4
                    ),
                )

                val mockImages = mapOf(
                    1L to null,
                    2L to ImageBitmap.createPlainImage(160, 90, Color.Red),
                    3L to null,
                    4L to ImageBitmap.createPlainImage(160, 90, Color.Blue),
                )

                mockItems.fastForEach { item ->
                    StreamItem(
                        modifier = Modifier.fillMaxWidth(),
                        data = item,
                        preview = mockImages[item.id],
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {}
                    )
                }
            }
        }
    }
}

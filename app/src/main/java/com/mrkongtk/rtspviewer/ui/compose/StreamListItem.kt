package com.mrkongtk.rtspviewer.ui.compose

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.createBitmap
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.ErrorColor
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.PreviewWidth
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.ui.theme.RoundedCornerSize

/**
 * A list item component representing a single RTSP stream configuration.
 *
 * This composable renders a clickable [ElevatedCard] containing:
 * - An optional snapshot/thumbnail of the stream.
 * - The stream's name.
 * - A navigation indicator arrow.
 *
 * @param modifier The [Modifier] to be applied to the outer Card layout.
 * @param data The [RTSPItem] domain object containing stream details (name, URL, etc.).
 * @param preview An optional [Bitmap] representing the latest snapshot of the stream.
 *                If null, the image section is hidden.
 * @param onClick A callback lambda triggered when the card is tapped; passes the associated [data].
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
            // Pushes content to the edges: Content starts at left, Icon is pushed to right
            horizontalArrangement = Arrangement.SpaceBetween,
            // Vertically centers the thumbnail, text, and icon within the row height
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Conditional rendering: Only show the image block if a valid bitmap exists
            preview?.let {
                Image(
                    modifier = Modifier
                        .width(PreviewWidth)
                        // maintain the aspect ratio based on the actual bitmap dimensions
                        .aspectRatio(it.width.toFloat() / it.height.toFloat())
                        .clip(RoundedCornerShape(RoundedCornerSize)),
                    bitmap = it.asImageBitmap(),
                    // Accessibility: Formatting the string resource to include the stream name
                    contentDescription = stringResource(R.string.rtsp_item_preview_description).replace(
                        "%1",
                        data.name
                    )
                )
                // Add spacing between the image and the text
                Spacer(modifier = Modifier.width(PaddingM))
            }

            // Stream Name Display
            Text(
                text = data.name,
                // Weight 1f ensures the text takes up all available remaining space,
                // pushing the icon to the edge and truncating the text if it becomes too long.
                modifier = Modifier.weight(1f),
            )

            // Navigation Icon
            // Uses 'AutoMirrored' to ensure the arrow points correctly in RTL (Right-to-Left) layouts.
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
 * Renders the component in both Light (Day) and Dark (Night) modes.
 * It demonstrates two states:
 * 1. An item with a mocked snapshot (generated programmatically).
 * 2. An item without a snapshot.
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

            // Create a Mock Bitmap to simulate a camera preview
            val w = 1920
            val h = 1080
            val bmp = createBitmap(w, h).let {
                val canvas = Canvas(it)
                // Draw a solid color (ErrorColor) onto the canvas to visualize the bitmap
                canvas.drawColor(ErrorColor.toArgb())
                it
            }

            // Mock data items
            val mockItems = listOf(
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 1)
            )

            // Map IDs to previews (only item 1 has a preview)
            val mockPreviews = mapOf(
                Pair(1L, bmp)
            )

            Column(
                modifier = Modifier
                    .padding(innerPadding)
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

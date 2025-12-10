package com.mrkongtk.rtspviewer.ui.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.ui.theme.RoundedCornerSize

/**
 * A list item component representing a single RTSP stream configuration.
 *
 * This composable renders a clickable [ElevatedCard] displaying the stream's name
 * and a navigation indicator.
 *
 * @param data The [RTSPItem] domain object containing stream details (name, URL, etc.).
 * @param modifier The [Modifier] to be applied to the outer Card layout.
 * @param onClick A callback lambda triggered when the card is tapped; passes the associated [data].
 */
@Composable
fun StreamListItem(
    data: RTSPItem,
    modifier: Modifier = Modifier,
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
            // Pushes content to the edges: Text to start, Icon to end
            horizontalArrangement = Arrangement.SpaceBetween,
            // Vertically centers the text and icon within the row height
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stream Name Display
            Text(
                text = data.name,
                // Weight 1f ensures the text takes up all available space, pushing the icon to the edge
                // and truncating the text if it becomes too long, rather than pushing the icon off-screen.
                modifier = Modifier.weight(1f),
            )

            // Navigation Icon
            // Uses 'AutoMirrored' to ensure the arrow points correctly in RTL (Right-to-Left) layouts.
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.detail), // Semantic description for accessibility
            )
        }
    }
}

/**
 * Preview provider for [StreamListItem].
 *
 * Renders the component in both Light (Day) and Dark (Night) modes to verify
 * theme adaptability and layout correctness.
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

            // Mock data for preview purposes
            val mockItems = listOf(
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 1)
            )

            Column(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                mockItems.fastForEach { item ->
                    StreamListItem(
                        data = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = PaddingS),
                        onClick = {}
                    )
                }
            }
        }
    }
}

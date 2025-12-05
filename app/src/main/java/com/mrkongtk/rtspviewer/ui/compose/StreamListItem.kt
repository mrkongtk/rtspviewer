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
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * A list item component representing a single RTSP stream.
 *
 * This composable renders stream details within an [ElevatedCard]. It implements
 * a "zebra-striping" pattern (alternating background colors) to improve readability
 * in long lists.
 *
 * @param index The index of the item in the list, used to calculate the background color.
 * @param data The [RTSPItem] data object containing stream details (name, URL, etc.).
 * @param modifier The modifier to be applied to the outer Card layout.
 * @param onClick Callback function invoked when the card is clicked. Passes the [data] item.
 */
@Composable
fun StreamListItem(
    index: Int,
    data: RTSPItem,
    modifier: Modifier = Modifier,
    onClick: (RTSPItem) -> Unit,
) {
    // Determine colors based on index for the zebra-striping effect.
    // Even indexes use Surface color, Odd indexes use SurfaceVariant.
    val isEven = index % 2 == 0
    val containerColor = if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isEven) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = PaddingS,
        ),
        modifier = modifier,
        onClick = { onClick(data) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingM),
            // Pushes the Text to the start and Icon to the end
            horizontalArrangement = Arrangement.SpaceBetween,
            // Vertically centers content within the row
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display the Stream Name
            Text(
                text = data.name,
                color = contentColor,
                modifier = Modifier.weight(1f) // Text takes up remaining space, preventing overlap with icon
            )

            // Navigation Indicator
            // Uses AutoMirrored icon to point the correct direction in RTL (Right-to-Left) languages
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.detail), // Accessibility description
                tint = contentColor
            )
        }
    }
}

/**
 * Preview provider for [StreamListItem].
 *
 * Renders the component in both Light (Day) and Dark (Night) modes to ensure
 * theme consistency and verify the zebra-striping visual logic.
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
        // Scaffold provides the standard app layout structure including system bar handling
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // Mock data designed to test alternating colors (Index 0 and 1)
            val mockItems = listOf(
                RTSPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1),
                RTSPItem(2, "Backyard Camera", "rtsp://192.168.1.11", emptyList(), 1)
            )

            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                // fastForEach is a performance-optimized loop for Compose lists
                mockItems.forEachIndexed { index, item ->
                    StreamListItem(
                        index = index,
                        data = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = PaddingS), // Add slight spacing between items in preview
                        onClick = {}
                    )
                }
            }
        }
    }
}

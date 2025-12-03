package com.mrkongtk.rstpviewer.ui.compose

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
import androidx.compose.ui.util.fastForEach
import com.mrkongtk.rstpviewer.R
import com.mrkongtk.rstpviewer.model.RSTPItem
import com.mrkongtk.rstpviewer.ui.screen.StreamListScreen
import com.mrkongtk.rstpviewer.ui.theme.PaddingM
import com.mrkongtk.rstpviewer.ui.theme.PaddingS
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme

/**
 * A composable that renders a single row representing an RTSP stream in a list.
 *
 * This component uses an [ElevatedCard] to display the stream information.
 * It implements a "zebra-striping" visual effect where the background color
 * changes based on whether the item index is even or odd.
 *
 * @param index The position of this item in the list. Used to determine the background color.
 * @param data The [RSTPItem] model containing the stream details (name, url, etc.).
 * @param modifier Modifier to be applied to the card layout.
 * @param onClick Lambda callback triggered when the user taps on the card.
 */
@Composable
fun StreamListItem(
    index: Int,
    data: RSTPItem,
    modifier: Modifier = Modifier,
    onClick: (RSTPItem) -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.cardColors(
            // Apply alternating background colors (Surface vs SurfaceVariant) for visual distinction
            containerColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = PaddingS,
        ),
        modifier = modifier,
        onClick = {
            onClick(data)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stream Name
            Text(
                text = data.name,
                // Ensure text contrast matches the alternating background color
                color = if (index % 2 == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Navigation Indicator (Arrow)
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = stringResource(R.string.detail)
            )
        }
    }
}

/**
 * Preview configurations for [StreamListItem].
 *
 * Generates interactive previews for both Day (Light) and Night (Dark) modes
 * to verify layout responsiveness, zebra-striping logic, and theme adherence.
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
fun StreamListItemPreview() {

    // Apply the application's custom theme to the preview environment
    RSTPViewerTheme {
        // Scaffold acts as the root container, handling system insets (statusBar/navigationBar)
        // so the preview closely matches the actual device rendering.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->

            // --- Mock Data Setup ---
            // Create dummy data to visualize how the list looks when populated.
            val itemList = listOf(
                Pair(0, RSTPItem(1, "Living Room Camera", "rtsp://192.168.1.10", emptyList(), 1)),
                Pair(1, RSTPItem(2, "Living Room Camera2", "rtsp://192.168.1.10", emptyList(), 1))
            )

            // Render a column of items to demonstrate the alternating colors
            Column {
                itemList.fastForEach { (index, data) ->
                    StreamListItem(
                        index = index,
                        data = data,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(innerPadding)
                    ) {
                        // Empty click action for preview
                    }
                }
            }
        }
    }
}

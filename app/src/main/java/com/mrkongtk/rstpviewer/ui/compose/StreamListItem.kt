package com.mrkongtk.rstpviewer.ui.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.mrkongtk.rstpviewer.model.RSTPItem
import com.mrkongtk.rstpviewer.ui.screen.StreamListScreen
import com.mrkongtk.rstpviewer.ui.theme.PaddingM
import com.mrkongtk.rstpviewer.ui.theme.PaddingS
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme

@Composable
fun StreamListItem(
    index: Int,
    data: RSTPItem,
    modifier: Modifier = Modifier,
    onClick: (RSTPItem) -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = PaddingS
        ),
        modifier = modifier,
        onClick = {
            onClick(data)
        }
    ) {
        Text(
            text = data.name,
            color = if (index % 2 == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(PaddingM),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Preview configurations for [StreamListScreen].
 * Generates interactive previews for both Day (Light) and Night (Dark) modes
 * to verify layout responsiveness and theme adherence.
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
            Column {
                itemList.fastForEach { (index, data) ->
                    StreamListItem(
                        index, data, Modifier
                            .fillMaxWidth()
                            .padding(innerPadding)
                    ) {
                    }
                }
            }
        }
    }
}
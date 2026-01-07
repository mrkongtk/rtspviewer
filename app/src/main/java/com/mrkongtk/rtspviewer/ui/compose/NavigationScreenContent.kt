package com.mrkongtk.rtspviewer.ui.compose

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.data.AppUiState
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.screen.AddStreamItemScreen
import com.mrkongtk.rtspviewer.ui.screen.EditStreamItemScreen
import com.mrkongtk.rtspviewer.ui.screen.StreamItemScreen
import com.mrkongtk.rtspviewer.ui.screen.StreamListScreen
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * The root navigation graph for the RTSP Viewer application.
 *
 * This composable manages the transitions between different screens using Jetpack Navigation.
 * It observes the [AppUiState] to provide data to child screens and forwards user actions
 * via lambda callbacks to the caller (typically the MainActivity or a higher-level ViewModel).
 *
 * @param modifier The modifier to be applied to the [NavHost].
 * @param navController The controller managing app navigation.
 * @param uiState The state holder containing the list of streams, tags, and selection state.
 * @param onListingItemSelected Called when a stream is clicked in the list.
 * @param onAddItemRequested Called when a new RTSP stream is submitted for persistence.
 * @param onItemsReordered Called when the manual sort order of streams is changed.
 * @param onEditItemRequested Called when an existing stream's details are updated.
 * @param onDeleteItemRequested Called when a stream is removed from the database.
 * @param onImageAvailable Called when the player captures a frame (used for generating thumbnails).
 * @param onTagSelected Called when a filter tag is clicked or cleared.
 */
@Composable
fun NavigationScreenContent(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    uiState: AppUiState,
    onListingItemSelected: (RTSPItem) -> Unit,
    onAddItemRequested: (RTSPItem) -> Unit,
    onItemsReordered: (List<RTSPItem>) -> Unit,
    onEditItemRequested: (RTSPItem) -> Unit,
    onDeleteItemRequested: (RTSPItem) -> Unit,
    onImageAvailable: (RTSPItem, Bitmap) -> Unit,
    onTagSelected: (String?) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = modifier.testTag("NavHost")
    ) {

        // --- 1. Main List Screen ---
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                itemList = uiState.items,
                previews = uiState.cachedPreviews,
                tags = uiState.tags,
                selectedTag = uiState.selectedTag,
                onItemSelected = { rtspItem ->
                    onListingItemSelected(rtspItem)
                    navController.navigate(AppScreen.RTSPDisplay.name)
                },
                onAddItemSelected = {
                    navController.navigate(AppScreen.AddRTSPItem.name)
                },
                onItemsReordered = onItemsReordered,
                onTagSelected = onTagSelected,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 2. Full-Screen Player Screen ---
        composable(route = AppScreen.RTSPDisplay.name) {
            // Guard clause: Only render if a selection exists in the state.
            // This prevents crashes during rapid navigation or state resets.
            uiState.selectedItem?.let { item ->
                StreamItemScreen(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                    onEditItemSelected = {
                        navController.navigate(AppScreen.EditRTSPItem.name)
                    },
                    onDeleteItemSelected = {
                        onDeleteItemRequested(item)
                        // Pop back to list after deletion to avoid viewing a non-existent item.
                        navController.popBackStack()
                    },
                    onImageAvailable = onImageAvailable
                )
            }
        }

        // --- 3. Stream Creation Form ---
        composable(route = AppScreen.AddRTSPItem.name) {
            AddStreamItemScreen(
                onSave = { item ->
                    onAddItemRequested(item)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 4. Stream Modification Form ---
        composable(route = AppScreen.EditRTSPItem.name) {
            // Uses the same selectedItem as the display screen.
            uiState.selectedItem?.let { selectedItem ->
                EditStreamItemScreen(
                    item = selectedItem,
                    onSave = { updatedItem ->
                        onEditItemRequested(updatedItem)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Provides mock data sets for Android Studio Previews.
 */
private class NavigationScreenContentPreviewParameterProvider :
    PreviewParameterProvider<AppUiState> {
    override val values = sequenceOf(
        // Empty State: Verifies UI when no streams are configured.
        AppUiState(),

        // Populated State: Verifies list rendering with data.
        AppUiState(
            items = listOf(
                RTSPItem(
                    id = 1,
                    name = "Living Room Camera",
                    uri = "rtsp://192.168.1.10",
                    tags = emptyList(),
                    order = 1
                ),
                RTSPItem(
                    id = 2,
                    name = "Backyard",
                    uri = "rtsp://192.168.1.11",
                    tags = emptyList(),
                    order = 2
                )
            )
        )
    )
}

/**
 * Preview function to visualize the navigation flow in the IDE.
 *
 * Includes both Light and Dark mode configurations.
 */
@Preview(
    name = "Day",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun NavigationScreenContentPreview(
    @PreviewParameter(NavigationScreenContentPreviewParameterProvider::class) state: AppUiState
) {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                // windowInsetsPadding ensures the content doesn't overlap with status/navigation bars.
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            NavigationScreenContent(
                navController = rememberNavController(),
                uiState = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onListingItemSelected = {},
                onAddItemRequested = {},
                onItemsReordered = {},
                onEditItemRequested = {},
                onDeleteItemRequested = {},
                onImageAvailable = { _, _ -> },
                onTagSelected = {}
            )
        }
    }
}

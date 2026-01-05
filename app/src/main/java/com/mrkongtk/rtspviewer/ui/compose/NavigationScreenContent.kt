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
 * The root composable responsible for handling the application's navigation graph.
 *
 * This function hosts the [NavHost] and defines the mapping between navigation routes
 * (defined in [AppScreen]) and their corresponding screen composables. It acts as the
 * central hub connecting the UI Layer, Navigation Controller, and ViewModel events.
 *
 * @param modifier The modifier to be applied to the NavHost container.
 * @param navController The [NavHostController] that manages app navigation. Defaults to [rememberNavController].
 * @param uiState The current UI state of the application, containing the list of RTSP items and the currently selected item.
 * @param onListingItemSelected Callback triggered when a user taps on a specific stream in the list to view it.
 * @param onAddItemRequested Callback triggered when the user submits the form to add a new RTSP stream.
 * @param onItemsReordered Callback triggered when the user reorders the list of streams (e.g., drag-and-drop).
 * @param onEditItemRequested Callback triggered when the user submits changes to an existing RTSP stream.
 * @param onDeleteItemRequested Callback triggered when the user confirms deletion of a specific stream.
 * @param onImageAvailable Callback triggered when the video player generates a snapshot/preview of the stream.
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
) {
    // NavHost connects the NavController to the navigation graph.
    // The startDestination is the first screen shown when the graph is loaded.
    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = modifier.testTag("NavHost")
    ) {
        // =====================================================================
        // Route: Start (Home/Stream List Screen)
        // =====================================================================
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                itemList = uiState.items,
                previews = uiState.cachedPreviews,
                onItemSelected = { rtspItem ->
                    // 1. Notify parent/ViewModel to update "selected item" state
                    onListingItemSelected(rtspItem)
                    // 2. Navigate to the detail/player screen
                    navController.navigate(AppScreen.RTSPDisplay.name)
                },
                onAddItemSelected = {
                    // Navigate to the creation form
                    navController.navigate(AppScreen.AddRTSPItem.name)
                },
                onItemsReordered = { itemsOrdered ->
                    // Delegate reordering logic to the parent (ViewModel)
                    onItemsReordered(itemsOrdered)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // =====================================================================
        // Route: RTSP Display (Video Player)
        // =====================================================================
        composable(route = AppScreen.RTSPDisplay.name) {
            // Guard clause: Ensure we actually have a selected item before trying to render the player.
            // If selectedItem is null (e.g., deep linking edge cases), nothing renders or we could redirect.
            uiState.selectedItem?.let { item ->
                StreamItemScreen(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                    onEditItemSelected = {
                        // Navigate to the edit form for this specific item
                        navController.navigate(AppScreen.EditRTSPItem.name)
                    },
                    onDeleteItemSelected = {
                        // 1. Perform the delete operation via callback
                        onDeleteItemRequested(item)
                        // 2. Navigate back to the list since the item no longer exists
                        navController.popBackStack()
                    },
                    onImageAvailable = onImageAvailable
                )
            }
        }

        // =====================================================================
        // Route: Add RTSP Item (Creation Form)
        // =====================================================================
        composable(route = AppScreen.AddRTSPItem.name) {
            AddStreamItemScreen(
                onSave = { item ->
                    // 1. Persist the new item via the parent callback
                    onAddItemRequested(item)
                    // 2. Return to the list screen after saving
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // =====================================================================
        // Route: Edit RTSP Item (Modification Form)
        // =====================================================================
        composable(route = AppScreen.EditRTSPItem.name) {
            // Ensure the item still exists in state before rendering the edit screen
            uiState.selectedItem?.let { selectedItem ->
                EditStreamItemScreen(
                    item = selectedItem,
                    onSave = { updatedItem ->
                        // 1. Persist the changes via the parent callback
                        onEditItemRequested(updatedItem)
                        // 2. Return to the previous screen (usually the Display screen)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * A [PreviewParameterProvider] that generates mock data for the [NavigationScreenContent] preview.
 *
 * It provides two scenarios to ensure the UI handles different data states correctly:
 * 1. **Empty State:** Checks how the list screen behaves with no items.
 * 2. **Populated State:** Checks how the list renders with mock RTSP items.
 */
private class NavigationScreenContentPreviewParameterProvider :
    PreviewParameterProvider<AppUiState> {
    override val values = sequenceOf(
        // Case 1: Empty State
        AppUiState(),

        // Case 2: Populated State
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
 * Preview for [NavigationScreenContent].
 *
 * This renders the navigation component in both Light (Day) and Dark (Night) themes.
 * It uses a [Scaffold] to simulate actual device bounds and proper system bar insets.
 *
 * @param state The mock UI state provided by [NavigationScreenContentPreviewParameterProvider].
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
        // Scaffold provides the basic material design visual layout structure
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Render the navigation content with dummy callbacks for preview purposes
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
                onImageAvailable = { _, _ -> }
            )
        }
    }
}

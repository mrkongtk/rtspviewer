package com.mrkongtk.rtspviewer.ui.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.mrkongtk.rtspviewer.ui.screen.StreamItemScreen
import com.mrkongtk.rtspviewer.ui.screen.StreamListScreen
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * The root composable responsible for handling the application's navigation graph.
 *
 * This function hosts the [NavHost] and defines the mapping between navigation routes
 * (defined in [AppScreen]) and their corresponding screen composables. It acts as the
 * wiring between the UI Layer and the Navigation Controller.
 *
 * @param modifier The modifier to be applied to the NavHost container.
 * @param navController The [NavHostController] that manages app navigation. Defaults to [rememberNavController].
 * @param uiState The current UI state of the application, containing the list of RTSP items and the currently selected item.
 * @param onListingItemSelected Callback triggered when a user taps on a specific stream in the list.
 * @param onAddItemRequested Callback triggered when the user submits the form to add a new RTSP stream.
 * @param onItemsReordered Callback triggered when the user reorders the list of streams (e.g., drag-and-drop).
 */
@Composable
fun NavigationScreenContent(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    uiState: AppUiState,
    onListingItemSelected: (RTSPItem) -> Unit,
    onAddItemRequested: (RTSPItem) -> Unit,
    onItemsReordered: (List<RTSPItem>) -> Unit,
) {
    // NavHost connects the NavController to the navigation graph
    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = modifier
    ) {
        // =====================================================================
        // Route: Start (Home/List Screen)
        // =====================================================================
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                itemList = uiState.items,
                onItemSelected = { rtspItem ->
                    // 1. Update the UI state to reflect the currently selected item
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
            // If selectedItem is null, nothing is rendered for this frame.
            uiState.selectedItem?.let { item ->
                StreamItemScreen(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // =====================================================================
        // Route: Add RTSP Item (Form)
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
    }
}

/**
 * A [PreviewParameterProvider] that generates mock data for the [NavigationScreenContent] preview.
 *
 * It provides two states:
 * 1. An empty state (to verify empty list handling).
 * 2. A populated state (to verify list rendering with data).
 */
private class NavigationScreenContentPreviewParameterProvider :
    PreviewParameterProvider<AppUiState> {
    override val values = sequenceOf(
        // Case 1: Empty State
        AppUiState(),

        // Case 2: Populated State
        AppUiState(
            listOf(
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
 * Renders the UI in both Light and Dark themes within a [Scaffold] to simulate
 * actual device bounds and system bar insets.
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
            )
        }
    }
}

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
 * The main entry point for the app's UI navigation logic.
 * This Composable sets up the [NavHost] and defines the navigation graph, linking
 * specific routes (Screens) to their respective Composables.
 *
 * @param navController The controller managing app navigation. Defaults to [rememberNavController].
 * @param uiState The current state of the application (contains the list of streams and the active selection).
 * @param modifier Modifier to be applied to the NavHost container.
 * @param onListingItemSelected Callback triggered when a user taps a stream in the list.
 * @param onAddItemRequested Callback triggered when a user completes the "Add Item" form.
 */
@Composable
fun NavigationScreenContent(
    navController: NavHostController = rememberNavController(),
    uiState: AppUiState,
    modifier: Modifier = Modifier,
    onListingItemSelected: (RTSPItem) -> Unit,
    onAddItemRequested: (RTSPItem) -> Unit,
) {
    // Defines the navigation graph with the Start screen as the initial destination
    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = modifier
    ) {
        // ---------------------------------------------------------
        // Route: Start (Stream List Screen)
        // Displays the list of available RTSP streams.
        // ---------------------------------------------------------
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                itemList = uiState.items,
                onItemSelected = { rtspItem ->
                    // 1. Notify the ViewModel (via callback) to update the selected item
                    onListingItemSelected(rtspItem)
                    // 2. Navigate to the player screen
                    navController.navigate(AppScreen.RTSPDisplay.name)
                },
                onAddItemSelected = {
                    // Navigate to the form to add a new stream
                    navController.navigate(AppScreen.AddRTSPItem.name)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ---------------------------------------------------------
        // Route: RTSP Display (Video Player Screen)
        // Plays the selected RTSP stream.
        // ---------------------------------------------------------
        composable(route = AppScreen.RTSPDisplay.name) {
            // Null-check: Only render the player if a valid item is currently selected in the state
            uiState.selectedItem?.let { item ->
                StreamItemScreen(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ---------------------------------------------------------
        // Route: Add RTSP Item (Input Form Screen)
        // Allows the user to input details for a new camera stream.
        // ---------------------------------------------------------
        composable(route = AppScreen.AddRTSPItem.name) {
            AddStreamItemScreen(
                onSave = { item ->
                    // 1. Pass the new item back up to the ViewModel to be saved to the database
                    onAddItemRequested(item)
                    // 2. Return to the previous screen (Stream List)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * A Data Provider for the Android Studio Preview.
 * Generates mock [AppUiState] objects to visualize different scenarios (Empty state vs Populated state).
 */
private class NavigationScreenContentPreviewParameterProvider :
    PreviewParameterProvider<AppUiState> {
    override val values = sequenceOf(
        // Scenario 1: Empty State (No items)
        AppUiState(),

        // Scenario 2: Populated State (Two mock cameras)
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
 * Preview composable to view the screen layout in Android Studio Design view.
 * Renders both Light Mode and Dark Mode versions.
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
        // Scaffold is used here to mimic the actual app structure (handling system bars, etc.)
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Note: We pass empty lambdas {} for callbacks since this is just a UI preview
            NavigationScreenContent(
                navController = rememberNavController(),
                uiState = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onListingItemSelected = {},
                onAddItemRequested = {}
            )
        }
    }
}

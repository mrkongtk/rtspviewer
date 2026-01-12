package com.mrkongtk.rtspviewer.ui.screen

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
import com.mrkongtk.rtspviewer.ui.screen.action.EditStreamItemScreenActions
import com.mrkongtk.rtspviewer.ui.screen.action.NavigationScreenActions
import com.mrkongtk.rtspviewer.ui.screen.action.StreamItemScreenActions
import com.mrkongtk.rtspviewer.ui.screen.action.StreamListScreenActions
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * The central navigation graph and coordinator for the RTSP Viewer application.
 *
 * This Composable defines the app's routing structure using Jetpack Navigation. It acts as the
 * "glue" between the global application state and the individual functional screens.
 *
 * **Key Responsibilities:**
 * - **Routing:** Defines the mapping between [AppScreen] destinations and their Composable implementations.
 * - **State Distribution:** Passes relevant slices of the [AppUiState] down to child screens.
 * - **Action Delegation:** Consolidates UI events (clicks, saves, deletes) into the [NavigationScreenActions]
 *   interface, facilitating a clean separation between UI navigation and business logic.
 *
 * **Main Destinations:**
 * 1. [AppScreen.Start]: The entry point showing the filterable and reorderable list of streams.
 * 2. [AppScreen.RTSPDisplay]: The dedicated playback screen for a selected RTSP stream.
 * 3. [AppScreen.AddRTSPItem]: The form interface for adding new stream configurations.
 * 4. [AppScreen.EditRTSPItem]: The form interface for updating existing stream metadata.
 *
 * @param modifier The modifier to be applied to the [NavHost] container.
 * @param navController The controller managing the app's navigation backstack.
 * @param uiState The current snapshot of the application's data (streams, tags, previews, and selections).
 * @param screenActions A consolidated interface providing handlers for high-level user actions that
 *                      require interaction with the data layer or ViewModel.
 */
@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    uiState: AppUiState,
    screenActions: NavigationScreenActions,
) {
    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = modifier.testTag("NavHost")
    ) {

        // --- 1. Main List Screen ---
        composable(route = AppScreen.Start.name) {

            val actions = object : StreamListScreenActions {
                override fun onItemSelected(item: RTSPItem) {
                    screenActions.onListingItemSelected(item)
                    navController.navigate(AppScreen.RTSPDisplay.name)
                }

                override fun onAddItemSelected() {
                    navController.navigate(AppScreen.AddRTSPItem.name)
                }

                override fun onItemsReordered(orderedList: List<RTSPItem>) {
                    screenActions.onItemsReordered(orderedList)
                }

                override fun onTagSelected(tag: String?) {
                    screenActions.onTagSelected(tag)
                }
            }

            StreamListScreen(
                itemList = uiState.items,
                previews = uiState.cachedPreviews,
                tags = uiState.tags,
                selectedTag = uiState.selectedTag,
                screenActions = actions,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 2. Full-Screen Player Screen ---
        composable(route = AppScreen.RTSPDisplay.name) {

            val actions = object : StreamItemScreenActions {
                override fun onEditItemSelected() {
                    navController.navigate(AppScreen.EditRTSPItem.name)
                }

                override fun onDeleteItemSelected() {
                    uiState.selectedItem?.let { item ->
                        screenActions.onDeleteItemRequested(item)
                        // Pop back to list after deletion to avoid viewing a non-existent item.
                        navController.popBackStack()
                    }
                }

                override fun onImageAvailable(
                    item: RTSPItem,
                    bitmap: Bitmap
                ) {
                    screenActions.onImageAvailable(item, bitmap)
                }
            }

            // Guard clause: Only render if a selection exists in the state.
            // This prevents crashes during rapid navigation or state resets.
            uiState.selectedItem?.let { item ->
                StreamItemScreen(
                    modifier = Modifier.fillMaxSize(),
                    item = item,
                    screenActions = actions,
                )
            }
        }

        // --- 3. Stream Creation Form ---
        composable(route = AppScreen.AddRTSPItem.name) {
            val action = object : EditStreamItemScreenActions {
                override fun onSaveItem(newItem: RTSPItem) {
                    screenActions.onAddItemRequested(newItem)
                    navController.popBackStack()
                }

            }

            AddStreamItemScreen(
                modifier = Modifier.fillMaxSize(),
                screenActions = action
            )
        }

        // --- 4. Stream Modification Form ---
        composable(route = AppScreen.EditRTSPItem.name) {
            val action = object : EditStreamItemScreenActions {
                override fun onSaveItem(newItem: RTSPItem) {
                    screenActions.onEditItemRequested(newItem)
                    navController.popBackStack()
                }

            }

            // Uses the same selectedItem as the display screen.
            uiState.selectedItem?.let { selectedItem ->
                EditStreamItemScreen(
                    modifier = Modifier.fillMaxSize(),
                    item = selectedItem,
                    screenActions = action,
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
private fun NavigationScreenPreview(
    @PreviewParameter(NavigationScreenContentPreviewParameterProvider::class) state: AppUiState
) {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                // windowInsetsPadding ensures the content doesn't overlap with status/navigation bars.
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            NavigationScreen(
                navController = rememberNavController(),
                uiState = state,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                screenActions = object : NavigationScreenActions {
                    override fun onListingItemSelected(item: RTSPItem) {
                    }

                    override fun onAddItemRequested(newItem: RTSPItem) {
                    }

                    override fun onItemsReordered(newOrderedList: List<RTSPItem>) {
                    }

                    override fun onEditItemRequested(updatedItem: RTSPItem) {
                    }

                    override fun onDeleteItemRequested(deleteItem: RTSPItem) {
                    }

                    override fun onImageAvailable(
                        item: RTSPItem,
                        snapshot: Bitmap
                    ) {
                    }

                    override fun onTagSelected(tag: String?) {
                    }

                },
            )
        }
    }
}

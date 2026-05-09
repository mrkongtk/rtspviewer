package com.mrkongtk.rtspviewer.shared.ui.screen


import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.navigation.AppScreen
import com.mrkongtk.rtspviewer.shared.ui.screen.action.EditStreamItemScreenActions
import com.mrkongtk.rtspviewer.shared.ui.screen.action.NavigationScreenActions
import com.mrkongtk.rtspviewer.shared.ui.screen.action.StreamItemScreenActions
import com.mrkongtk.rtspviewer.shared.ui.screen.action.StreamListScreenActions
import com.mrkongtk.rtspviewer.shared.ui.state.AppUiState
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme

/**
 * Application navigation host.
 *
 * Maps [AppScreen] destinations, passes [AppUiState] to each screen,
 * and routes user events through [NavigationScreenActions].
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

        composable(route = AppScreen.RTSPDisplay.name) {

            val actions = object : StreamItemScreenActions {
                override fun onEditItemSelected() {
                    navController.navigate(AppScreen.EditRTSPItem.name)
                }

                override fun onDeleteItemSelected() {
                    uiState.selectedItem?.let { item ->
                        screenActions.onDeleteItemRequested(item)
                        navController.popBackStack()
                    }
                }

                override fun onImageAvailable(
                    item: RTSPItem,
                    bitmap: ImageBitmap
                ) {
                    screenActions.onImageAvailable(item, bitmap)
                }
            }

            uiState.selectedItem?.let { item ->

                StreamItemScreen(
                    modifier = Modifier.fillMaxSize(),
                    item = item,
                    screenActions = actions,
                )
            }
        }

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

        composable(route = AppScreen.EditRTSPItem.name) {
            val action = object : EditStreamItemScreenActions {
                override fun onSaveItem(newItem: RTSPItem) {
                    screenActions.onEditItemRequested(newItem)
                    navController.popBackStack()
                }

            }

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

/** Preview data for [NavigationScreenPreview]. */
private class NavigationScreenContentPreviewParameterProvider :
    PreviewParameterProvider<AppUiState> {
    override val values = sequenceOf(
        AppUiState(),
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

/** IDE preview for [NavigationScreen]. */
@Preview(
    name = "Day",
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun NavigationScreenPreview(
    @PreviewParameter(NavigationScreenContentPreviewParameterProvider::class) state: AppUiState
) {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
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
                        snapshot: ImageBitmap
                    ) {
                    }

                    override fun onTagSelected(tag: String?) {
                    }

                },
            )
        }
    }
}

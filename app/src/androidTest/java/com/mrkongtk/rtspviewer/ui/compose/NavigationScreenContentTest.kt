package com.mrkongtk.rtspviewer.ui.compose

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.graphics.createBitmap
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
 * It connects the UI Layer, Navigation Controller, and ViewModel events,
 * featuring smooth transitions and state-guarding for detail views.
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
        modifier = modifier.testTag("NavHost"),
        // --- Navigation Transitions ---
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Home Screen: Stream List
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

        // Detail Screen: Video Player
        composable(route = AppScreen.RTSPDisplay.name) {
            val selectedItem = uiState.selectedItem
            if (selectedItem == null) {
                // Safety: Redirect to list if selection state is lost
                LaunchedEffect(Unit) { navController.popBackStack(AppScreen.Start.name, false) }
            } else {
                StreamItemScreen(
                    item = selectedItem,
                    modifier = Modifier.fillMaxSize(),
                    onEditItemSelected = {
                        navController.navigate(AppScreen.EditRTSPItem.name)
                    },
                    onDeleteItemSelected = {
                        onDeleteItemRequested(selectedItem)
                        navController.popBackStack()
                    },
                    onImageAvailable = onImageAvailable
                )
            }
        }

        // Create Screen: New RTSP Source
        composable(route = AppScreen.AddRTSPItem.name) {
            AddStreamItemScreen(
                onSave = { item ->
                    onAddItemRequested(item)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Edit Screen: Modify existing Source
        composable(route = AppScreen.EditRTSPItem.name) {
            val selectedItem = uiState.selectedItem
            if (selectedItem == null) {
                LaunchedEffect(Unit) { navController.popBackStack(AppScreen.Start.name, false) }
            } else {
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
 * Enhanced Preview provider with realistic mock data (Tags & Thumbnails).
 */
private class NavigationScreenContentPreviewParameterProvider :
    PreviewParameterProvider<AppUiState> {

    private val mockBmp = createBitmap(100, 100).apply {
        Canvas(this).drawColor(Color.DKGRAY)
    }

    override val values = sequenceOf(
        // Case 1: Empty State
        AppUiState(),

        // Case 2: Populated State with Tags and Previews
        AppUiState(
            items = listOf(
                RTSPItem(1, "Living Room", "rtsp://192.168.1.10", listOf("Home"), 0),
                RTSPItem(2, "Backyard", "rtsp://192.168.1.11", listOf("Outdoor"), 1),
                RTSPItem(3, "Driveway", "rtsp://192.168.1.12", listOf("Outdoor", "Security"), 2)
            ),
            tags = listOf("Home", "Outdoor", "Security"),
            cachedPreviews = mapOf(1L to mockBmp, 3L to mockBmp),
            selectedTag = null
        )
    )
}

@Preview(name = "Day", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Night", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NavigationScreenContentPreview(
    @PreviewParameter(NavigationScreenContentPreviewParameterProvider::class) state: AppUiState
) {
    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
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

package com.mrkongtk.rtspviewer.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.AppUiState
import com.mrkongtk.rtspviewer.data.database.MockAppDatabase
import com.mrkongtk.rtspviewer.data.repository.FileRepositoryImpl
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepositoryImpl
import com.mrkongtk.rtspviewer.ui.compose.NavigationScreenContent
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmodel.AppViewModel
import kotlinx.coroutines.flow.map

/**
 * A custom Top App Bar component that dynamically updates its title based on the current screen.
 *
 * This component utilizes a "Template-Replacement" strategy for titles. If a screen title
 * contains placeholders (e.g., "Editing %1"), it fetches the active item name from the
 * [AppViewModel] and performs a string replacement.
 *
 * @param currentScreen The enum representing the active screen destination.
 * @param canNavigateBack Whether to show the back arrow icon.
 * @param navigateUp Action to perform when the back arrow is clicked.
 * @param modifier Modifier for layout adjustments.
 * @param viewModel Injected ViewModel used to observe the name of the currently selected RTSP stream.
 */
@Composable
fun AppBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel()
) {
    // 1. Observe the selected item's name.
    // We map the state to a List<String> to support multiple potential placeholders in the future.
    val args: List<String> by viewModel.uiState.map { state ->
        state.selectedItem?.name?.let { listOf(it) } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            val baseTitle = stringResource(currentScreen.title)

            // 2. Perform dynamic string formatting.
            // This takes the base string (e.g., "Stream: %1") and replaces "%1" with the first argument.
            // reduceIndexed treats index 0 as the accumulator (the template).
            val formattedTitle = (listOf(baseTitle) + args).reduceIndexed { index, acc, nextValue ->
                acc.replace("%$index", nextValue)
            }

            Text(text = formattedTitle)
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        }
    )
}

/**
 * The primary scaffold/root container for the application UI.
 *
 * This composable manages the relationship between the [NavHostController], the [AppViewModel],
 * and the [AppBar]. It listens to backstack changes to update the Top Bar's title and
 * navigation state automatically.
 *
 * @param modifier Modifier for the root layout.
 * @param navController The controller managing the app's navigation stack.
 * @param viewModel The Hilt-injected business logic coordinator.
 */
@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Observe the current navigation route to update UI components like the AppBar
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Safely map the current route string back to our AppScreen enum.
    // Falls back to AppScreen.Start if the route is null or unrecognized.
    val currentScreen = backStackEntry?.destination?.route?.let { route ->
        try {
            AppScreen.valueOf(route)
        } catch (_: IllegalArgumentException) {
            AppScreen.Start
        }
    } ?: AppScreen.Start

    Scaffold(
        modifier = modifier
            .testTag("NavigationScreenRoot")
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // The back button is visible if there's a screen to return to
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // lifecycle-aware collection of the UI state flow
        val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

        // Pass event lambdas down to the Content layer.
        // This follows the "UDF (Unidirectional Data Flow)" pattern where
        // events flow up to the ViewModel and state flows down to the UI.
        NavigationScreenContent(
            navController = navController,
            uiState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Ensure content doesn't overlap the AppBar
            onListingItemSelected = { item ->
                viewModel.select(item)
            },
            onAddItemRequested = { item ->
                viewModel.addItem(item)
            },
            onItemsReordered = { itemsOrdered ->
                viewModel.reorderItems(itemsOrdered)
            },
            onEditItemRequested = { editedItem ->
                viewModel.editItem(editedItem)
            },
            onDeleteItemRequested = { item ->
                viewModel.deleteItem(item)
            },
            onImageAvailable = { item, bitmap ->
                viewModel.savePreview(item, bitmap)
            },
            onTagSelected = { tag ->
                viewModel.select(tag)
            }
        )
    }
}

/**
 * Visual preview for Android Studio Layout Editor.
 *
 * We suppress ViewModelConstructorInComposable because we are manually creating
 * the ViewModel with Mock dependencies to avoid requiring a real Database context
 * during the preview rendering process.
 */
@SuppressLint("ViewModelConstructorInComposable")
@Preview(
    name = "Day Mode",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night Mode",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun NavigationScreenPreview() {
    RTSPViewerTheme {
        val navController = rememberNavController()

        // Manual Dependency Injection for Preview stability.
        // This simulates the data layer without hitting the real Android SQLite system.
        val mockViewModel = AppViewModel(
            RTSPItemRepositoryImpl(
                LocalContext.current,
                MockAppDatabase()
            ),
            FileRepositoryImpl(LocalContext.current),
        )

        NavigationScreen(
            navController = navController,
            viewModel = mockViewModel
        )
    }
}

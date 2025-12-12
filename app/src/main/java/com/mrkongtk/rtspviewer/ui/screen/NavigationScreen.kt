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
import com.mrkongtk.rtspviewer.data.repository.RTSPItemWithSampleInitRepositoryImpl
import com.mrkongtk.rtspviewer.ui.compose.NavigationScreenContent
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.AppViewModel
import kotlinx.coroutines.flow.map

/**
 * A custom Top App Bar composable that manages the screen title and back navigation.
 *
 * This component handles dynamic title generation. It combines the static title resource
 * defined in [AppScreen] with dynamic data (like the selected RTSP stream name)
 * observed from the [AppViewModel].
 *
 * @param currentScreen The current navigation destination, used to determine the base title string.
 * @param canNavigateBack Whether the back arrow button should be visible.
 * @param navigateUp Callback invoked when the back button is clicked.
 * @param modifier Modifier to apply to the TopAppBar.
 * @param viewModel The ViewModel used to observe the currently selected item for title formatting.
 */
@Composable
fun AppBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel()
) {
    // Collect the selected item's name from the UI state to use as a title argument.
    // If a name exists, wrap it in a list; otherwise, use an empty list.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Dynamic Title Formatting:
            // 1. Get the template string from resources (e.g., "Watching %1").
            // 2. Combine the template with the dynamic args (e.g., ["Watching %1", "Camera 1"]).
            // 3. Use reduceIndexed to replace placeholders (like "%1") with the actual argument.
            Text(stringResource(currentScreen.title).let { titleTemplate ->
                (listOf(titleTemplate) + args).reduceIndexed { index, acc, new ->
                    acc.replace("%$index", new)
                }
            })
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
 * The root screen composable that hosts the Scaffold and Navigation logic.
 *
 * This component acts as the main container. It observes the navigation controller's
 * back stack to update the TopBar title and manages the global UI state flow.
 *
 * @param navController The central Navigation Controller.
 * @param viewModel The global AppViewModel (injected via Hilt).
 * @param modifier Modifier to apply to the Scaffold.
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    // Observe the current back stack entry to react to navigation changes.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Determine the current screen enum based on the route string.
    // Falls back to AppScreen.Start if the route is null or unknown.
    val currentScreen = backStackEntry?.destination?.route?.let { route ->
        try {
            AppScreen.valueOf(route)
        } catch (_: IllegalArgumentException) {
            AppScreen.Start
        }
    } ?: AppScreen.Start

    Scaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Only show the back button if there is a previous screen in the stack.
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // Collect the main UI state (list of RTSP items, loading state, etc.)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

        // Render the content area, passing state and event callbacks.
        // innerPadding ensures content isn't obscured by the TopBar.
        NavigationScreenContent(
            navController = navController,
            uiState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onListingItemSelected = { item ->
                viewModel.select(item)
            },
            onAddItemRequested = { item ->
                viewModel.addItem(item)
            },
            onItemsReordered = { itemsOrdered ->
                viewModel.reorderItems(itemsOrdered)
            },
        )
    }
}

/**
 * Preview for [NavigationScreen].
 *
 * Renders the UI in both Day and Night modes.
 *
 * @SuppressLint("ViewModelConstructorInComposable") is required here because we are
 * manually instantiating the ViewModel with mock dependencies. Hilt injection
 * does not function within @Preview methods.
 */
@SuppressLint("ViewModelConstructorInComposable")
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
private fun NavigationScreenPreview() {
    RTSPViewerTheme {
        val navController = rememberNavController()

        // Manual dependency injection for Preview purposes.
        // We use a Mock database and a repository populated with sample data.
        val viewModel = AppViewModel(
            RTSPItemWithSampleInitRepositoryImpl(
                LocalContext.current,
                MockAppDatabase()
            )
        )

        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

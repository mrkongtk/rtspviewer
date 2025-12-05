package com.mrkongtk.rtspviewer.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.AppScreen
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.AppUiState
import com.mrkongtk.rtspviewer.data.database.MockAppDatabase
import com.mrkongtk.rtspviewer.data.repository.RTSPItemWithSampleInitRepositoryImpl
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.AppViewModel
import kotlinx.coroutines.flow.map

/**
 * A custom Top App Bar that displays the screen title and handles back navigation.
 *
 * It dynamically constructs the title by combining a string resource template (from [AppScreen])
 * with dynamic arguments (e.g., the selected stream name) observed from the [AppViewModel].
 *
 * @param currentScreen The current destination in the navigation graph, used to determine the title template.
 * @param canNavigateBack If true, displays the back arrow icon.
 * @param navigateUp Callback invoked when the back arrow is clicked.
 * @param modifier Modifier to be applied to the TopAppBar.
 * @param viewModel The view model used to observe the currently selected RTSP item for title formatting.
 */
@Composable
fun AppBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel()
) {
    // Transform the UI state flow to extract the name of the selected item.
    // If an item is selected, we wrap its name in a list to be used as a format argument.
    // If null, we return an empty list.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Dynamic Title Logic:
            // 1. Get the raw string resource (e.g., "Watching %0")
            // 2. Create a list containing the template + any dynamic args.
            // 3. Use reduceIndexed to replace placeholders (like "%0") with the actual values from args.
            Text(stringResource(currentScreen.title).let { titleTemplate ->
                (listOf(titleTemplate) + args).reduceIndexed { index, acc, new ->
                    acc.replace("%$index", new)
                }
            })
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
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
 * The main container for the application's UI structure.
 *
 * This composable implements the [Scaffold] pattern, coordinating the [AppBar] and the
 * [NavigationScreenContent]. It monitors the navigation back stack to update the
 * current screen state automatically.
 *
 * @param navController The controller managing app navigation. Defaults to [rememberNavController].
 * @param viewModel The [AppViewModel] instance, injected via Hilt.
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Observe the back stack to determine which screen is currently visible.
    // This triggers a recomposition of the AppBar title when the route changes.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Parse the current route string into an AppScreen enum.
    // Fallback to AppScreen.Start if the route is null or invalid.
    val currentScreen = backStackEntry?.destination?.route?.let { route ->
        try {
            AppScreen.valueOf(route)
        } catch (e: IllegalArgumentException) {
            AppScreen.Start
        }
    } ?: AppScreen.Start

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            // Add padding for system bars (status bar, navigation bar) to prevent content overlap
            .windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Show back button only if there is a previous entry in the back stack
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // Pass the scaffold's inner padding (calculated based on TopBar height)
        // to the content to ensure the top of the content isn't hidden behind the bar.
        NavigationScreenContent(navController, innerPadding, viewModel)
    }
}

/**
 * Hosts the Navigation Graph and defines the composables for each route.
 *
 * @param navController The navigation controller passed down from the parent.
 * @param innerPadding Padding values provided by the Scaffold to respect UI boundaries.
 * @param viewModel The shared view model for managing app state.
 */
@Composable
fun NavigationScreenContent(
    navController: NavHostController = rememberNavController(),
    innerPadding: PaddingValues,
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Collect UI state using lifecycle-aware collection (pauses when app is backgrounded)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding) // Apply the padding from Scaffold
    ) {
        // Route: The list of available RTSP streams
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                onItemSelected = { rtspItem ->
                    // Update the selected item in ViewModel and navigate to detail view
                    viewModel.select(rtspItem)
                    navController.navigate(AppScreen.RTSPDisplay.name)
                },
                itemList = uiState.items,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Route: The player/detail view for a specific RTSP stream
        composable(route = AppScreen.RTSPDisplay.name) {
            // Ensure we have a valid selected item before rendering the player
            uiState.selectedItem?.let { item ->
                StreamItemScreen(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Preview for the [NavigationScreen].
 *
 * Displays the UI in both Day and Night themes.
 * Uses a [MockAppDatabase] and a sample repository to render the UI without
 * requiring real database connections or network dependencies.
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

        // Initialize ViewModel with mock data for preview purposes
        val viewModel = AppViewModel(
            RTSPItemWithSampleInitRepositoryImpl(
                LocalContext.current,
                MockAppDatabase()
            )
        )

        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

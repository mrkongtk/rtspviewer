package com.mrkongtk.rtspviewer.ui.screen

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
import com.mrkongtk.rtspviewer.data.repository.RTSPItemWithSampleInitRepositoryImpl
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.AppViewModel
import kotlinx.coroutines.flow.map

/**
 * A reusable Top App Bar composable used throughout the application.
 *
 * This component renders a [CenterAlignedTopAppBar]. It features dynamic title formatting
 * by combining the static title from [AppScreen] with dynamic data (e.g., selected item name)
 * observed from the [AppViewModel].
 *
 * @param currentScreen The [AppScreen] enum representing the current destination, used to fetch the title resource.
 * @param canNavigateBack If true, displays a back arrow icon.
 * @param navigateUp Callback function invoked when the back button is clicked.
 * @param modifier Modifier to be applied to the TopAppBar.
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
    // Transform UI State to extract dynamic arguments for the title.
    // If an item is selected, its name is used as the first argument; otherwise, an empty list.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Dynamic Title Logic:
            // 1. Get the base string resource from currentScreen.title.
            // 2. Combine the base string with the dynamic args list.
            // 3. Use 'reduceIndexed' to replace placeholders (e.g., "%0") with the actual values.
            // Example: "Watching: %0" + ["Camera 1"] -> "Watching: Camera 1"
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
 * The main scaffolding container for the application.
 *
 * This composable sets up the [Scaffold], handles system insets for edge-to-edge display,
 * and manages the [AppBar] state based on the current navigation destination.
 *
 * @param navController The [NavHostController] that manages app navigation.
 * @param viewModel The shared [AppViewModel] instance.
 * @param content The content composable that receives the navigation controller and padding values.
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
    content: @Composable (NavHostController, PaddingValues, AppViewModel) -> Unit
) {
    // Observe the current backstack entry to determine which screen is currently visible.
    // This triggers a recomposition of the AppBar title whenever navigation occurs.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Determine the current AppScreen from the route string.
    // Falls back to AppScreen.Start if the route is null or invalid.
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
            // Padding ensures the app content doesn't draw strictly behind the system bars
            // (status bar/navigation bar) unless intended.
            .windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Show back button only if there is a previous entry in the navigation stack
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // Pass innerPadding to content to prevent content from being hidden behind the TopAppBar
        content(navController, innerPadding, viewModel)
    }
}

/**
 * Defines the Navigation Graph and hosts the application screens.
 *
 * This component maps [AppScreen] routes to their respective composables and
 * handles navigation actions (e.g., navigating to details when an item is clicked).
 *
 * @param navController The navigation controller provided by the parent.
 * @param innerPadding Padding values provided by the Scaffold to avoid UI obstruction.
 * @param viewModel The shared ViewModel for state management.
 */
@Composable
fun NavigationScreenContent(
    navController: NavHostController = rememberNavController(),
    innerPadding: PaddingValues,
    viewModel: AppViewModel
) {
    // Collect the UI state in a lifecycle-aware manner
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding) // Apply Scaffold padding to the NavHost
    ) {
        // Destination: List of RTSP Streams
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                onItemSelected = { rtspItem ->
                    // Select item in ViewModel and navigate to display screen
                    viewModel.select(rtspItem)
                    navController.navigate(AppScreen.RTSPDisplay.name)
                },
                itemList = uiState.items,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Destination: Individual Stream Display
        composable(route = AppScreen.RTSPDisplay.name) {
            // Only render if there is a valid selected item
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
 * Preview for the Navigation Screen structure.
 *
 * Renders the UI in both Day and Night modes using a dummy repository
 * to simulate data.
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
private fun NavigationScreenPreview() {
    RTSPViewerTheme {
        val navController = rememberNavController()
        // Initialize ViewModel with a repository implementation suitable for preview context
        val viewModel = AppViewModel(RTSPItemWithSampleInitRepositoryImpl(LocalContext.current))

        NavigationScreen(
            navController = navController,
            viewModel = viewModel
        ) { _, innerPadding, _ ->
            // Dummy content to visualize the content area within the Scaffold
            Text(
                text = "Preview Content Area",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

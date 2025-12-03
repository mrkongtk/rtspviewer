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
 * by combining the static title from the current [AppScreen] with dynamic data (e.g., the
 * name of a selected RTSP stream) observed from the [AppViewModel].
 *
 * @param currentScreen The [AppScreen] enum representing the current navigation destination.
 *                      Used to fetch the base title string resource.
 * @param canNavigateBack Boolean indicating if the back button should be visible.
 * @param navigateUp Callback function invoked when the back button is clicked.
 * @param modifier Modifier to be applied to the TopAppBar.
 * @param viewModel The [AppViewModel] used to observe the currently selected item for
 *                  populating dynamic title arguments.
 */
@Composable
fun AppBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel()
) {
    // Observes the UI State to extract dynamic arguments for the title string.
    // If an item is selected, its name is wrapped in a list; otherwise, an empty list is returned.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Logic to format the title dynamically:
            // 1. Fetch the base string resource associated with the current screen.
            // 2. Combine the base string (template) with the dynamic arguments list.
            // 3. Use 'reduceIndexed' to replace placeholders (e.g., "%0") in the template with actual values.
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
 * The root container (Scaffold) for the application screens.
 *
 * This composable manages the global UI structure, including the [AppBar] and system window insets.
 * It observes the navigation back stack to update the Top Bar title automatically.
 *
 * @param navController The [NavHostController] that manages app navigation. Defaults to [rememberNavController].
 * @param viewModel The shared [AppViewModel] instance. Defaults to [hiltViewModel].
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Observe the current back stack entry to determine the active screen.
    // This allows the AppBar to update its title when the route changes.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Resolve the current AppScreen from the route string.
    // Defaults to AppScreen.Start if the route is null or invalid.
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
            // Applies padding to avoid drawing behind system bars (status bar/navigation bar)
            .windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Display the back arrow only if there is a previous screen in the stack
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // innerPadding contains the offsets calculated by the Scaffold (e.g., height of TopBar).
        // This is passed to the content to ensure nothing is obscured.
        NavigationScreenContent(navController, innerPadding, viewModel)
    }
}

/**
 * Defines the Navigation Graph and hosts the specific screen composables.
 *
 * This component maps [AppScreen] routes to their corresponding UI screens and handles
 * high-level navigation logic, such as moving from the list view to the detail view.
 *
 * @param navController The navigation controller provided by the parent Scaffold.
 * @param innerPadding Padding values provided by the Scaffold to be applied to the content.
 * @param viewModel The shared ViewModel for state management.
 */
@Composable
fun NavigationScreenContent(
    navController: NavHostController = rememberNavController(),
    innerPadding: PaddingValues,
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Collect the UI state in a lifecycle-aware manner to ensure updates stop when the view is not active.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding) // Apply the TopBar/SystemBar padding here
    ) {
        // Destination: The main list of RTSP Streams
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                onItemSelected = { rtspItem ->
                    // Update ViewModel selection and navigate to the display screen
                    viewModel.select(rtspItem)
                    navController.navigate(AppScreen.RTSPDisplay.name)
                },
                itemList = uiState.items,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Destination: The detailed display of a specific RTSP Stream
        composable(route = AppScreen.RTSPDisplay.name) {
            // Only render the screen if a valid item is currently selected in the state
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
 * Preview provider for [NavigationScreen].
 *
 * Renders the UI structure in both Day and Night modes.
 * Uses a mock repository ([RTSPItemWithSampleInitRepositoryImpl]) to provide sample data without networking.
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
        // Inject a dummy repository for the preview environment
        val viewModel = AppViewModel(RTSPItemWithSampleInitRepositoryImpl(LocalContext.current))

        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

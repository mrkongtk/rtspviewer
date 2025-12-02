package com.mrkongtk.rstpviewer.ui.screen

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rstpviewer.AppScreen
import com.mrkongtk.rstpviewer.R
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme
import com.mrkongtk.rstpviewer.viewmode.StreamListScreenViewModel

/**
 * A reusable Top App Bar composable used throughout the application.
 *
 * This component renders a [CenterAlignedTopAppBar] that dynamically changes its title
 * based on the current screen and optionally shows a back navigation arrow.
 *
 * @param currentScreen The [AppScreen] enum representing the current destination. Used to fetch the title string resource.
 * @param canNavigateBack Boolean flag indicating if the back stack is not empty (true = show back arrow).
 * @param navigateUp Callback function to handle the back button click event.
 * @param modifier Modifier to be applied to the TopAppBar container.
 */
@Composable
fun AppBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier,
        navigationIcon = {
            // Only display the back button if there is a previous screen in the back stack
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
 * The root container (Scaffold) for the application's UI.
 *
 * This composable handles the high-level structure of the app, including:
 * 1. Observing navigation state to update the AppBar.
 * 2. Providing the [AppBar] (TopBar).
 * 3. Handling System Bar insets (Edge-to-Edge display).
 *
 * @param navController The central [NavHostController] managing app navigation.
 * @param content A composable lambda that receives the `navController` and `innerPadding`.
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    content: @Composable (NavHostController, PaddingValues) -> Unit
) {
    // Observe the back stack. This forces a recomposition whenever the user navigates,
    // allowing us to update the AppBar title and back-button state dynamically.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Parse the current route from the back stack entry.
    // We use a try-catch block to safely convert the route String back to an AppScreen Enum.
    // If the route is null or invalid, we default to the 'Start' screen.
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
            // Apply padding to avoid drawing content behind the system status/navigation bars
            .windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Check if there is a previous entry in the stack to determine if "Back" is possible
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        // Pass the Scaffold's innerPadding to the content so child screens don't overlap the TopAppBar
        content(navController, innerPadding)
    }
}

/**
 * Defines the Navigation Graph and hosts the screen destinations.
 *
 * This component maps [AppScreen] routes to specific Composable screens and manages
 * dependency injection for ViewModels.
 *
 * @param navController The navigation controller provided by the parent [NavigationScreen].
 * @param innerPadding Padding values from the parent Scaffold to ensure content respects UI boundaries.
 */
@Composable
fun NavigationScreenContent(
    navController: NavHostController = rememberNavController(),
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // --- Destination: Start Screen (Stream List) ---
        composable(route = AppScreen.Start.name) {
            // Hilt automatically provides the correctly scoped ViewModel for this navigation entry
            val viewModel: StreamListScreenViewModel = hiltViewModel()

            StreamListScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Add more composable() entries here for other screens in the future
    }
}

/**
 * Preview for the Navigation Shell.
 *
 * Renders the UI shell in both Day (Light) and Night (Dark) modes to verify
 * layout, theming, and system bar handling.
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
fun NavigationScreenPreview() {
    RSTPViewerTheme {
        val navController = rememberNavController()

        // Render the NavigationScreen with dummy content for preview purposes
        NavigationScreen(navController = navController) { _, innerPadding ->
            Text(
                text = "Preview Content Area",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

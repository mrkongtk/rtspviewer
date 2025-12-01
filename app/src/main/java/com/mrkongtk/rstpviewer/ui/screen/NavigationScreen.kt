package com.mrkongtk.rstpviewer.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * It renders a [CenterAlignedTopAppBar] that dynamically displays the title based on the
 * current screen and conditionally shows a back navigation arrow.
 *
 * @param currentScreen The [AppScreen] enum representing the active destination. Used to resolve the title string resource.
 * @param canNavigateBack If true, displays the back arrow icon.
 * @param navigateUp A callback lambda triggered when the back button is pressed.
 * @param modifier The [Modifier] to be applied to the TopAppBar.
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
 * The primary layout wrapper for the application.
 *
 * This composable sets up the [Scaffold], which provides the basic material design visual layout structure.
 * It observes the Navigation Controller's state to automatically update the [AppBar] title
 * and back-button visibility as the user navigates through the app.
 *
 * @param navController The [NavHostController] that manages app navigation. Defaults to [rememberNavController].
 * @param content A composable lambda that accepts the controller and padding values. Usually contains the [NavHost].
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    content: @Composable (NavHostController, PaddingValues) -> Unit
) {
    // Observe the back stack to determine which screen is currently visible.
    // This allows the AppBar to update its title reactively.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Parse the current route string back into an AppScreen enum.
    // Falls back to AppScreen.Start if the route is null or unrecognized.
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
            // Applies padding for system bars (status bar, navigation bar) to support edge-to-edge display.
            .windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Only show the back button if there is a screen "behind" the current one in the stack.
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        // innerPadding must be passed to the content to prevent the TopAppBar from obscuring the UI.
        content(navController, innerPadding)
    }
}

/**
 * Defines the navigation graph (routes) for the application.
 *
 * This component hosts the specific screen implementations and manages dependency injection
 * (via Hilt) for their ViewModels.
 *
 * @param navController The navigation controller passed down from the parent [NavigationScreen].
 * @param innerPadding Padding values provided by the parent Scaffold, ensuring content sits below the TopAppBar.
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
            // Note: Applying verticalScroll here makes the entire container scrollable.
            // If individual screens (like LazyColumn) handle their own scrolling, this might need to be removed.
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
    ) {
        // Route definition for the Start Screen
        composable(route = AppScreen.Start.name) {
            // Hilt automatically provides the scoped ViewModel for this screen
            val viewModel: StreamListScreenViewModel = hiltViewModel()

            StreamListScreen(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }

        // Add more composable() destinations here as the app grows
    }
}

/**
 * Preview provider for the [NavigationScreen].
 * Renders the UI in both Day (Light) and Night (Dark) modes to ensure theme compatibility.
 */
@Preview(
    name = "Day Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun NavigationScreenPreview() {
    RSTPViewerTheme {
        val navController: NavHostController = rememberNavController()

        // Renders the shell with dummy content to visualize the Scaffold structure
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

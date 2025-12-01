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
 * This component renders a [CenterAlignedTopAppBar] that reacts to the navigation state.
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            // Only display the back button if navigation history exists
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
 * This composable handles the top-level structure, including the [AppBar] and the
 * [Scaffold] padding. It observes the [NavController] to dynamically update the
 * AppBar title and back button state.
 *
 * @param navController The central [NavHostController] managing app navigation.
 * @param content The content composable to render inside the Scaffold (usually the [NavHost]).
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    content: @Composable (NavHostController, PaddingValues) -> Unit
) {
    // 1. Observe the current back stack entry as State.
    // This ensures the UI recomposes whenever the user navigates to a new screen.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // 2. Resolve the current screen from the route string.
    // We use a try-catch block to safely convert the route string back to an Enum.
    // If the route is null or invalid, we fallback to the Start screen.
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
            // 3. Handle System Bars (Edge-to-Edge).
            // This padding prevents the content from drawing behind the status/navigation bars.
            .windowInsetsPadding(WindowInsets.systemBars),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Show back arrow only if there is a previous entry in the stack
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        // Pass innerPadding to the content to ensure it respects the TopAppBar's height
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
 * @param innerPadding Padding values from the parent Scaffold to prevent UI overlap.
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
        // --- Destination: Start Screen ---
        composable(route = AppScreen.Start.name) {
            // Hilt automatically scopes this ViewModel to the lifecycle of this navigation entry
            val viewModel: StreamListScreenViewModel = hiltViewModel()

            StreamListScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

    }
}

/**
 * Preview for the Navigation Shell.
 *
 * Renders the UI in both Light and Dark themes to verify layout and color adaptation.
 */
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
fun NavigationScreenPreview() {
    RSTPViewerTheme {
        val navController = rememberNavController()

        // Render the shell with dummy content for preview purposes
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

package com.mrkongtk.rtspviewer.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
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
 * A custom Top App Bar composable that manages the screen title and back navigation.
 *
 * This component dynamically generates the title by combining a static string resource template
 * (defined in [AppScreen]) with dynamic arguments observed from the [AppViewModel] (e.g., the name of the currently viewing stream).
 *
 * @param currentScreen The current destination in the navigation graph; used to fetch the title resource ID.
 * @param canNavigateBack Boolean flag indicating if the back arrow should be visible.
 * @param navigateUp Callback function invoked when the back arrow is clicked.
 * @param modifier Modifier to be applied to the layout.
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
    // Collect the selected item's name from the ViewModel state.
    // map transform: If an item is selected, wrap the name in a list (e.g., ["Camera 1"]).
    // If null, return an empty list. This list is used for string formatting later.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Dynamic Title Generation Logic:
            // 1. Fetch the raw string template (e.g., "Watching %0") based on the current screen.
            // 2. Prepend the template to the args list.
            // 3. Use reduceIndexed to iterate through. It effectively replaces placeholders like "%0"
            //    in the template (acc) with the values from the subsequent items in the list (new).
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
 * The root composable for the application's navigation structure.
 *
 * This component sets up the [Scaffold], which provides the standard Material Design layout structure
 * (TopBar, Content Area). It observes the navigation controller to update the TopBar title
 * as the user navigates between screens.
 *
 * @param navController The central navigation controller. Defaults to [rememberNavController].
 * @param viewModel The shared [AppViewModel] instance, injected via Hilt.
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Observe the current back stack entry to reactively update the UI when the route changes.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Determine the current AppScreen enum based on the route string.
    // We use a try-catch block to safely handle cases where the route might be null
    // or not map to a valid enum (defaulting to AppScreen.Start).
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
            // Apply padding to avoid drawing behind system bars (status/navigation bars).
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Only show the back button if the back stack has a previous entry.
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // The Scaffold provides 'innerPadding' which accounts for the height of the TopAppBar.
        // We pass this to the content to ensure the top of the content isn't obscured.
        NavigationScreenContent(navController, innerPadding, viewModel)
    }
}

/**
 * Contains the [NavHost] and defines the navigation graph for the application.
 *
 * This composable maps specific routes (defined in [AppScreen]) to their respective
 * composable screens. It also handles the passing of state and event callbacks
 * (like `onItemSelected`) between the ViewModel and the UI.
 *
 * @param navController The navigation controller used to navigate between screens.
 * @param innerPadding Padding values provided by the parent Scaffold.
 * @param viewModel The shared ViewModel containing the app state.
 */
@Composable
fun NavigationScreenContent(
    navController: NavHostController = rememberNavController(),
    innerPadding: PaddingValues,
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Collect the UI state in a lifecycle-aware manner.
    // This ensures flow collection stops when the app goes to the background to save resources.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

    NavHost(
        navController = navController,
        startDestination = AppScreen.Start.name,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding) // Apply the Scaffold padding here
    ) {
        // --- Route: Start (Stream List) ---
        composable(route = AppScreen.Start.name) {
            StreamListScreen(
                itemList = uiState.items,
                onItemSelected = { rtspItem ->
                    // Set the active item in the VM and navigate to the player view
                    viewModel.select(rtspItem)
                    navController.navigate(AppScreen.RTSPDisplay.name)
                },
                onAddItemSelected = {
                    navController.navigate(AppScreen.AddRTSPItem.name)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- Route: RTSP Display (Video Player) ---
        composable(route = AppScreen.RTSPDisplay.name) {
            // Only render the screen if a valid item is selected to avoid null pointer exceptions
            uiState.selectedItem?.let { item ->
                StreamItemScreen(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // --- Route: Add RTSP Item (Form) ---
        composable(route = AppScreen.AddRTSPItem.name) {
            AddStreamItemScreen(
                onSave = { item ->
                    // Save the new item to the database/state and return to the previous screen
                    viewModel.addItem(item)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Preview for [NavigationScreen].
 *
 * Renders the UI in both Day and Night modes.
 * Note: Since ViewModels usually require Hilt injection, we manually construct
 * the ViewModel here using a [MockAppDatabase] and a sample repository.
 * This allows the preview to render without crashing due to missing dependencies.
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

        // Manually inject dependencies for the Preview environment
        val viewModel = AppViewModel(
            RTSPItemWithSampleInitRepositoryImpl(
                LocalContext.current,
                MockAppDatabase()
            )
        )

        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

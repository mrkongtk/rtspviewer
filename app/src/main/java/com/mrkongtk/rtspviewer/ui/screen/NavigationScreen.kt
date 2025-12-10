package com.mrkongtk.rtspviewer.ui.screen

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
 * This component dynamically generates the title by combining a static string resource template
 * (defined in [AppScreen]) with dynamic arguments observed from the [AppViewModel]
 * (e.g., the name of the currently viewing stream).
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
    // Transform the UI state to extract the selected item's name.
    // If a stream is selected, it returns a list containing that name.
    // If null, it returns an empty list. This list is used for string template replacement below.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Title Formatting Logic:
            // 1. Get the base string resource associated with the current screen (e.g., "Watching %1").
            // 2. Create a list starting with the template, followed by dynamic arguments (e.g., ["Watching %1", "Camera 1"]).
            // 3. Use reduceIndexed to iterate. The 'acc' is the string being built, 'new' is the replacement value.
            //    It replaces placeholders (like "%1") with the corresponding argument.
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
 * @param modifier Modifier to apply to the root layout.
 */
@Composable
fun NavigationScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    // Observe the back stack to determine the current route.
    // This triggers a recomposition whenever the user navigates.
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Resolve the current route string to an AppScreen enum.
    // If the route is null or invalid (not found in enum), default to AppScreen.Start.
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
                // Show back button only if there is a previous entry in the back stack.
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // Collect the main UI state (list of items, selected item, etc.)
        // in a lifecycle-aware manner.
        val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

        // Render the main content navigation graph.
        // We pass 'innerPadding' to ensure content is not obscured by the TopAppBar.
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
            }
        )
    }
}

/**
 * Preview for [NavigationScreen].
 *
 * Renders the UI in both Day and Night modes.
 *
 * Note on Dependency Injection:
 * Since standard Hilt injection does not work within @Preview composables,
 * we manually instantiate the [AppViewModel] using a [MockAppDatabase] and
 * a repository implementation initialized with sample data.
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

        // Manually construct ViewModel with mock dependencies for preview purposes.
        val viewModel = AppViewModel(
            RTSPItemWithSampleInitRepositoryImpl(
                LocalContext.current,
                MockAppDatabase()
            )
        )

        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

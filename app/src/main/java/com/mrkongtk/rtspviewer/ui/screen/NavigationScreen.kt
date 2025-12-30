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
import com.mrkongtk.rtspviewer.data.repository.RTSPItemWithSampleInitRepositoryImpl
import com.mrkongtk.rtspviewer.ui.compose.NavigationScreenContent
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmodel.AppViewModel
import kotlinx.coroutines.flow.map

/**
 * A custom Top App Bar composable that handles screen titles and back navigation.
 *
 * This component supports dynamic title formatting by observing the [AppViewModel].
 * It fetches the current selected item's name to populate placeholders in the
 * screen title string (e.g., changing "Stream" to "Stream: Camera 1").
 *
 * @param currentScreen The current destination in the navigation graph.
 * @param canNavigateBack Boolean indicating if the back arrow should be displayed.
 * @param navigateUp Callback function invoked when the back arrow is clicked.
 * @param modifier Modifier to be applied to the TopAppBar.
 * @param viewModel The ViewModel used to observe UI state for title generation.
 */
@Composable
fun AppBar(
    currentScreen: AppScreen,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel()
) {
    // Extract the selected item's name from the UI state to use as a title argument.
    // Maps the StateFlow<AppUiState> to a Flow<List<String>>.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Dynamic Title Logic:
            // 1. Get the base string resource (e.g., "Watching %1").
            // 2. Append the dynamic arguments to a list containing the template.
            // 3. Use reduceIndexed to replace placeholders ("%1", "%2") with the actual values.
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
 * The main container screen for the application.
 *
 * This composable sets up the high-level UI structure using a [Scaffold].
 * It manages the [AppBar], the [NavHostController], and connects the [AppViewModel]
 * events to the content logic.
 *
 * @param modifier Modifier to be applied to the Scaffold.
 * @param navController The central controller for navigation.
 * @param viewModel The Hilt-injected ViewModel for managing app data.
 */
@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Observe the back stack to determine which screen is currently active
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Parse the current route into an AppScreen enum.
    // Defaults to AppScreen.Start if the route is null or invalid.
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
                // Only show back button if there is a previous entry in the stack
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // Collect the UI state in a lifecycle-aware manner
        val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

        // Render the navigation content and bind ViewModel actions to UI events
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
            onEditItemRequested = { editedItem ->
                viewModel.editItem(editedItem)
            },
            onDeleteItemRequested = { item ->
                viewModel.deleteItem(item)
            }
        )
    }
}

/**
 * Preview for [NavigationScreen].
 *
 * Displays the screen in both Day and Night modes using a Mock Database.
 *
 * Note: @SuppressLint("ViewModelConstructorInComposable") is used because we are
 * manually instantiating the ViewModel with mock dependencies for the preview,
 * which bypasses Hilt dependency injection.
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

        // Manually construct ViewModel with a mock repository for UI Preview
        val viewModel = AppViewModel(
            RTSPItemWithSampleInitRepositoryImpl(
                LocalContext.current,
                MockAppDatabase()
            )
        )

        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

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
import com.mrkongtk.rtspviewer.data.repository.FileRepositoryImpl
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepositoryImpl
import com.mrkongtk.rtspviewer.ui.compose.NavigationScreenContent
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmodel.AppViewModel
import kotlinx.coroutines.flow.map

/**
 * A custom Top App Bar composable that handles screen titles and back navigation.
 *
 * This component supports dynamic title formatting by observing the [AppViewModel].
 * It fetches the current selected item's name to populate placeholders in the
 * screen title string (e.g., changing "Stream %1" to "Stream: Camera 1").
 *
 * @param currentScreen The current destination in the navigation graph, used to determine the base title resource.
 * @param canNavigateBack Boolean indicating if the back arrow should be displayed (true if backstack is not empty).
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
    // Transform the UI State Flow to extract only the selected item's name.
    // We map this to a list because the dynamic string formatter expects a list of arguments.
    val args: List<String> by viewModel.uiState.map {
        it.selectedItem?.name?.let { name ->
            listOf(name)
        } ?: emptyList()
    }.collectAsStateWithLifecycle(emptyList())

    CenterAlignedTopAppBar(
        title = {
            // Logic for Dynamic Title Replacement:
            // 1. Load the resource string for the current screen (e.g., "Watching %1").
            // 2. Create a list starting with the template and followed by any arguments.
            // 3. Use `reduceIndexed` to iterate. It treats the first element as the accumulator (the template).
            //    It replaces "%1" with the item at index 1, "%2" with index 2, etc.
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
            // Only render the back button if the navigation controller indicates we can go back.
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
 * It acts as the bridge between the Navigation Controller, the ViewModel,
 * and the content screens.
 *
 * @param modifier Modifier to be applied to the Scaffold.
 * @param navController The central controller for navigation. Defaults to `rememberNavController()`.
 * @param viewModel The Hilt-injected ViewModel for managing app data and business logic.
 */
@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    // Observe the back stack to determine which screen is currently active (for AppBar title logic).
    val backStackEntry by navController.currentBackStackEntryAsState()

    // Determine the current AppScreen enum from the route string.
    // We use a try-catch block to safely handle cases where the route might be null
    // or not match a valid enum value (fallback to AppScreen.Start).
    val currentScreen = backStackEntry?.destination?.route?.let { route ->
        try {
            AppScreen.valueOf(route)
        } catch (_: IllegalArgumentException) {
            AppScreen.Start
        }
    } ?: AppScreen.Start

    Scaffold(
        modifier = modifier
            .testTag("NavigationScreenRoot") // Tag for UI testing
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(
                currentScreen = currentScreen,
                // Check if there is a previous entry in the backstack to decide if the back arrow appears.
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        // Collect the full UI state using a lifecycle-aware collector.
        val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

        // NavigationScreenContent handles the actual display of lists or details.
        // We pass lambdas here to delegate user actions (clicks, swipes, etc.) back to the ViewModel.
        NavigationScreenContent(
            navController = navController,
            uiState = uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Respect the padding provided by Scaffold (AppBar height)
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
            },
            onImageAvailable = { item, bitmap ->
                viewModel.savePreview(item, bitmap)
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
 * manually instantiating the ViewModel with mock dependencies for the preview.
 * In production code, Hilt handles this injection automatically.
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

        // Manually construct ViewModel with a mock repository to allow the Preview to render
        // without crashing due to missing Hilt bindings or Database context.
        val viewModel = AppViewModel(
            RTSPItemRepositoryImpl(
                LocalContext.current,
                MockAppDatabase()
            ),
            FileRepositoryImpl(LocalContext.current),
        )

        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

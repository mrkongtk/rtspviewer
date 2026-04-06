package com.mrkongtk.rtspviewer.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.RTSPItemRepository
import com.mrkongtk.rtspviewer.shared.ui.compose.AppBar
import com.mrkongtk.rtspviewer.shared.ui.navigation.AppScreen
import com.mrkongtk.rtspviewer.shared.ui.state.AppUiState
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.viewmodel.AppBarViewModel
import com.mrkongtk.rtspviewer.shared.viewmodel.AppViewModel
import com.mrkongtk.rtspviewer.ui.screen.action.NavigationScreenActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okio.Path


/**
 * The top-level UI orchestrator and root container for the application.
 *
 * This Composable serves as the "Glue Layer" between the navigation system, the business
 * logic (ViewModels), and the global layout. It is responsible for the following
 * key architectural concerns:
 *
 * 1. **Scaffold Management**: Hosts the [Scaffold] which provides the global [AppBar]
 *    and ensures proper content padding across all screens.
 * 2. **Navigation Synchronization**: Observes the [navController] backstack entries
 *    in a [LaunchedEffect] to automatically update the [AppBarViewModel] with the
 *    correct [AppScreen] and "back-nav" status.
 * 3. **Dynamic Title Routing**: Synchronizes the currently selected [RTSPItem] from
 *    the [AppViewModel] to the [AppBarViewModel], allowing the Top Bar to display
 *    context-aware titles (e.g., the name of the camera being viewed).
 * 4. **Action Mapping (UDF)**: Implements [NavigationScreenActions] to bridge user
 *    interactions from the UI layer back to the [AppViewModel]. This maintains
 *    Unidirectional Data Flow by keeping event handling centralized.
 * 5. **Lifecycle-Aware State Collection**: Collects the [AppUiState] using
 *    `collectAsStateWithLifecycle` to ensure resources are only consumed when
 *    the UI is active.
 *
 * @param modifier [Modifier] to be applied to the root [Scaffold].
 * @param navController The [NavHostController] managing the application's navigation stack.
 * @param viewModel The primary [AppViewModel] handling business logic, data persistence, and UI state.
 * @param appBarViewModel The [AppBarViewModel] dedicated to managing the state and
 * appearance of the Top App Bar.
 */
@Composable
fun AppInitialScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel,
    appBarViewModel: AppBarViewModel,
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(navBackStackEntry) {
        val currentEntry = navController.currentBackStackEntry
        val appScreen = currentEntry?.destination?.route?.let { route ->
            AppScreen.Start
            try {
                AppScreen.valueOf(route)
            } catch (_: IllegalArgumentException) {
                AppScreen.Start
            }
        } ?: AppScreen.Start

        val previousEntry = navController.previousBackStackEntry
        val canNavigateBack = previousEntry != null

        appBarViewModel.update(appScreen, canNavigateBack)
    }

    LaunchedEffect(viewModel.uiState) {
        viewModel.uiState.map { it.selectedItem }
            .collect { selectedItem -> appBarViewModel.update(selectedItem) }
    }

    Scaffold(
        modifier = modifier
            .testTag("NavigationScreenRoot")
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            AppBar(
                navigateUp = { navController.navigateUp() },
                viewModel = appBarViewModel
            )
        }
    ) { innerPadding ->
        // lifecycle-aware collection of the UI state flow
        val uiState by viewModel.uiState.collectAsStateWithLifecycle(AppUiState())

        val action = object : NavigationScreenActions {
            override fun onListingItemSelected(item: RTSPItem) {
                viewModel.select(item)
            }

            override fun onAddItemRequested(newItem: RTSPItem) {
                viewModel.addItem(newItem)
            }

            override fun onItemsReordered(newOrderedList: List<RTSPItem>) {
                viewModel.reorderItems(newOrderedList)
            }

            override fun onEditItemRequested(updatedItem: RTSPItem) {
                viewModel.editItem(updatedItem)
            }

            override fun onDeleteItemRequested(deleteItem: RTSPItem) {
                viewModel.deleteItem(deleteItem)
            }

            override fun onImageAvailable(
                item: RTSPItem,
                snapshot: ImageBitmap
            ) {
                viewModel.savePreview(item, snapshot)
            }

            override fun onTagSelected(tag: String?) {
                viewModel.select(tag)
            }

        }

        // Pass event lambdas down to the Content layer.
        // This follows the "UDF (Unidirectional Data Flow)" pattern where
        // events flow up to the ViewModel and state flows down to the UI.
        NavigationScreen(
            navController = navController,
            uiState = uiState,
            screenActions = action,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding), // Ensure content doesn't overlap the AppBar
        )
    }
}

/**
 * Visual preview for Android Studio Layout Editor.
 *
 * We suppress ViewModelConstructorInComposable because we are manually creating
 * the ViewModel with Mock dependencies to avoid requiring a real Database context
 * during the preview rendering process.
 */
@SuppressLint("ViewModelConstructorInComposable")
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
private fun AppInitialScreenPreview() {
    RTSPViewerTheme {
        val navController = rememberNavController()
        val context = LocalContext.current

        val fileRepository = object : FileRepository {
            override suspend fun writeJPEG(
                file: Path,
                bitmap: ImageBitmap
            ): Boolean {
                return true
            }

            override suspend fun readJPEG(file: Path): ImageBitmap? {
                return null
            }

            override fun getCacheDir(): Path {
                return okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY
            }
        }

        val rtspItemRepository = object : RTSPItemRepository {
            override val items = MutableStateFlow<List<RTSPItem>>(emptyList())
            override val cachedPreviews = MutableStateFlow<Map<Long, ImageBitmap>>(emptyMap())

            override suspend fun addItem(item: RTSPItem): Long {
                return 1
            }

            override suspend fun reorderItems(items: List<RTSPItem>): Int {
                return 1
            }

            override suspend fun updateItem(item: RTSPItem): Int {
                return 1
            }

            override suspend fun deleteItem(item: RTSPItem): Int {
                return 1
            }

            override fun previewPathFor(item: RTSPItem): Path {
                return okio.FileSystem.SYSTEM_TEMPORARY_DIRECTORY
            }

            override fun cachePreviewFor(
                item: RTSPItem,
                bitmap: ImageBitmap
            ) {
            }

            override fun removeCachedPreviews() {
            }

        }

        // Manual Dependency Injection for Preview stability.
        // This simulates the data layer without hitting the real Android SQLite system.
        val mockViewModel = AppViewModel(fileRepository, rtspItemRepository)

        val mockAppBarViewModel = AppBarViewModel()

        AppInitialScreen(
            navController = navController,
            viewModel = mockViewModel,
            appBarViewModel = mockAppBarViewModel,
        )
    }
}

package com.mrkongtk.rtspviewer.shared.ui.screen


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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
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
import com.mrkongtk.rtspviewer.shared.ui.screen.action.NavigationScreenActions
import com.mrkongtk.rtspviewer.shared.ui.state.AppUiState
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.viewmodel.AppBarViewModel
import com.mrkongtk.rtspviewer.shared.viewmodel.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okio.Path


/**
 * Root screen for the app.
 *
 * It wires navigation, shared UI state, and the top app bar together, while
 * keeping screen events routed back into [AppViewModel].
 *
 * @param modifier modifier applied to the root [Scaffold]
 * @param navController navigation controller used to observe and drive routes
 * @param viewModel source of app-wide state and actions
 * @param appBarViewModel state holder for the top app bar
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

        NavigationScreen(
            navController = navController,
            uiState = uiState,
            screenActions = action,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

/**
 * Preview for Android Studio.
 *
 * Uses in-memory stand-ins so the screen can render without app infrastructure.
 */
@Preview(
    name = "Day Mode",
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night Mode",
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun AppInitialScreenPreview() {
    RTSPViewerTheme {
        val navController = rememberNavController()

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

        val mockViewModel = AppViewModel(fileRepository, rtspItemRepository)

        val mockAppBarViewModel = AppBarViewModel()

        AppInitialScreen(
            navController = navController,
            viewModel = mockViewModel,
            appBarViewModel = mockAppBarViewModel,
        )
    }
}

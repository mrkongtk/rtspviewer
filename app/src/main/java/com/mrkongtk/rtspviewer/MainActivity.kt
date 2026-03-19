package com.mrkongtk.rtspviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.shared.viewmodel.AppBarViewModel
import com.mrkongtk.rtspviewer.shared.viewmodel.AppViewModel
import com.mrkongtk.rtspviewer.ui.screen.AppInitialScreen
import dagger.hilt.android.AndroidEntryPoint
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * The main entry point for the RTSP Viewer application.
 *
 * This Activity hosts the Jetpack Compose UI content. It is annotated with [AndroidEntryPoint]
 * to enable Hilt dependency injection, allowing view models and other dependencies to be
 * injected into the Compose hierarchy.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Apply the application's design system/theme to the widget tree
            RTSPViewerTheme {

                // Initialize the central navigation controller for screen transitions
                val navController: NavHostController = rememberNavController()

                // Inject the main AppViewModel. The lifecycle of this ViewModel is scoped
                // to this Activity (or the navigation graph if used within a NavHost).
                val viewModel: AppViewModel = getViewModel()

                val appBarViewModel: AppBarViewModel = getViewModel()

                // Render the root screen of the application.
                // We apply windowInsetsPadding(WindowInsets.systemBars) here to ensure
                // the root content respects the safe areas defined by the edge-to-edge configuration.
                AppInitialScreen(
                    navController = navController,
                    viewModel = viewModel,
                    appBarViewModel = appBarViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}

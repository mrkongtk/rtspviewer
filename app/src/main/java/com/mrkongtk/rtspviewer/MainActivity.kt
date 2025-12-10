package com.mrkongtk.rtspviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.ui.screen.NavigationScreen
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

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

        // Enables edge-to-edge display, allowing the app to draw behind the system bars
        // (status bar and navigation bar). Note: We must manually handle insets in the
        // UI to prevent content from overlapping these bars.
        enableEdgeToEdge()

        setContent {
            // Apply the application's design system/theme to the widget tree
            RTSPViewerTheme {

                // Initialize the central navigation controller for screen transitions
                val navController: NavHostController = rememberNavController()

                // Inject the main AppViewModel. The lifecycle of this ViewModel is scoped
                // to this Activity (or the navigation graph if used within a NavHost).
                val viewModel: AppViewModel = hiltViewModel()

                // Render the root screen of the application.
                // We apply windowInsetsPadding(WindowInsets.systemBars) here to ensure
                // the root content respects the safe areas defined by the edge-to-edge configuration.
                NavigationScreen(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                )
            }
        }
    }
}

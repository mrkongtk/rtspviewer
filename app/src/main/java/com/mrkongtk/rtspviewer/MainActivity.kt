package com.mrkongtk.rtspviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.ui.screen.NavigationScreen
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
import com.mrkongtk.rtspviewer.viewmode.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * The primary entry point for the RTSP Viewer application.
 *
 * This Activity serves as the container for the Jetpack Compose UI.
 * It is annotated with [AndroidEntryPoint] to allow Hilt to inject dependencies
 * into the Activity and the Composables hosted within it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display to allow content to be drawn behind system bars
        // (status bar and navigation bar) for a modern, immersive experience.
        enableEdgeToEdge()

        setContent {
            // Wrap the content in the application's custom theme
            RTSPViewerTheme {

                // Initialize the NavController to handle navigation between screens
                val navController: NavHostController = rememberNavController()

                // Obtain the AppViewModel instance via Hilt injection
                val viewModel: AppViewModel = hiltViewModel()

                // Render the main navigation structure (Scaffold, NavHost)
                NavigationScreen(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}

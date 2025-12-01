package com.mrkongtk.rstpviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rstpviewer.ui.screen.NavigationScreen
import com.mrkongtk.rstpviewer.ui.screen.NavigationScreenContent
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The main entry point of the RTSP Viewer application.
 *
 * Annotated with @AndroidEntryPoint to enable Hilt for dependency injection
 * within the Activity and its attached Composables.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configures the app to draw behind the system bars (status and navigation)
        enableEdgeToEdge()

        setContent {
            // Apply the custom application theme
            RSTPViewerTheme {
                // Initialize the NavController to manage app navigation state
                val navController: NavHostController = rememberNavController()

                // Set up the root navigation structure (Scaffold, BottomBar, etc.)
                NavigationScreen(navController = navController) { navController, innerPadding ->
                    // Render the specific screen content based on the current route
                    NavigationScreenContent(navController, innerPadding)
                }
            }
        }
    }
}
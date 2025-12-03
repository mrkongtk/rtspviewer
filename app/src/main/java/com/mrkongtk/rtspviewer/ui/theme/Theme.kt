package com.mrkongtk.rtspviewer.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.data.repository.RTSPItemWithSampleInitRepositoryImpl
import com.mrkongtk.rtspviewer.ui.screen.NavigationScreen
import com.mrkongtk.rtspviewer.viewmode.AppViewModel

/**
 * Definition of the Dark Mode color palette.
 * Maps specific color variables (e.g., PrimaryDark) to Material Design 3 color slots.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = OnPrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = OnSecondaryDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark
)

/**
 * Definition of the Light Mode color palette.
 * Maps specific color variables (e.g., PrimaryLight) to Material Design 3 color slots.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = OnSecondaryLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight
)

/**
 * The main Theme composable for the RTSPViewer application.
 *
 * This wrapper applies the Material Design 3 theme, handles Dynamic Color (Material You) logic,
 * and configures the System UI (Status Bar and Navigation Bar) colors.
 *
 * @param darkTheme Whether to use the dark color scheme. Defaults to the system's current setting.
 * @param dynamicColor Whether to use dynamic colors (wallpaper-based) on Android 12+ (S). Defaults to false.
 * @param content The composable content to be rendered within this theme.
 */
@Composable
fun RTSPViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determine which color scheme to use based on inputs and API level
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Access the current Activity window to modify System UI bars
    LocalActivity.current?.window?.let { window ->
        // Set the Status Bar color to match the theme's background
        window.statusBarColor = colorScheme.background.toArgb()
        // Set the Navigation Bar color to match the theme's background
        window.navigationBarColor = colorScheme.background.toArgb()
    }

    // Apply the MaterialTheme with the selected colors and typography
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Preview composable to visualize the Theme in Android Studio.
 * Generates two previews: one for Day mode and one for Night mode.
 *
 * It mocks a NavHostController and the NavigationScreen shell to show
 * how the basic UI structure looks under the applied theme.
 */
@Preview(
    name = "Day",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun RTSPViewerThemePreview() {
    RTSPViewerTheme {
        val navController: NavHostController = rememberNavController()
        val context = LocalContext.current
        val viewModel = AppViewModel(RTSPItemWithSampleInitRepositoryImpl(context))

        // Renders the shell with dummy content to visualize the Scaffold structure
        NavigationScreen(
            navController = navController,
            viewModel = viewModel
        ) { _, innerPadding, _ ->
            Text(
                text = "Preview Content Area",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

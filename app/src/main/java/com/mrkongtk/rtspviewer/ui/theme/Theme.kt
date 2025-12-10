package com.mrkongtk.rtspviewer.ui.theme

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mrkongtk.rtspviewer.data.database.MockAppDatabase
import com.mrkongtk.rtspviewer.data.repository.RTSPItemWithSampleInitRepositoryImpl
import com.mrkongtk.rtspviewer.ui.screen.NavigationScreen
import com.mrkongtk.rtspviewer.viewmode.AppViewModel

/**
 * Material Design 3 Dark Color Scheme.
 *
 * This property maps the project's specific dark color definitions (e.g., [PrimaryDark])
 * to the standard Material 3 color slots.
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurface, // Often used for card backgrounds in M3
    onPrimaryContainer = DarkTextPrimary,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    secondary = DarkSecondary,
    onSecondary = DarkTextSecondary, // Used for icons/text on secondary backgrounds

    error = ErrorColor,
    onError = OnErrorColor,
)

/**
 * Material Design 3 Light Color Scheme.
 *
 * This property maps the project's specific light color definitions (e.g., [PrimaryLight])
 * to the standard Material 3 color slots.
 */
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightSurface,
    onPrimaryContainer = LightTextPrimary,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,

    secondary = LightSecondary,
    onSecondary = LightTextSecondary,

    error = ErrorColor,
    onError = OnErrorColor
)

/**
 * The core Theme Composable for the RTSPViewer application.
 *
 * This wrapper performs three main functions:
 * 1. Determines the appropriate [colorScheme] based on system settings and OS version.
 * 2. Updates the System UI (Status Bar and Navigation Bar) colors to match the theme.
 * 3. Provides the [MaterialTheme] to the [content] hierarchy.
 *
 * @param darkTheme Whether the dark mode color scheme should be applied.
 *                  Defaults to [isSystemInDarkTheme] to match the device setting.
 * @param dynamicColor Whether to enable Material You (Dynamic Colors) derived from the user's wallpaper.
 *                     Only applicable on Android 12 (API 31) and above. Defaults to false.
 * @param content The Composable content to be displayed within the theme context.
 */
@Composable
fun RTSPViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 1. Resolve the Color Scheme
    val colorScheme = when {
        // Check if Dynamic Color is requested AND supported by the OS (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallback to static defined schemes
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 2. Update System UI Colors (Status Bar & Navigation Bar)
    // We access the current Activity window to modify the system bars directly.
    LocalActivity.current?.window?.let { window ->
        // Sets the status bar color to match the theme's background color
        window.statusBarColor = colorScheme.background.toArgb()
        // Sets the navigation bar color to match the theme's background color
        window.navigationBarColor = colorScheme.background.toArgb()
    }

    // 3. Apply the Material Theme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Theme Preview for Android Studio.
 *
 * This preview generates two configurations (Day Mode and Night Mode) to verify
 * how the [NavigationScreen] and the overall theme look in different environments.
 *
 * Note: This instantiates a [AppViewModel] with a repository implementation directly
 * for UI visualization purposes.
 */
@SuppressLint("ViewModelConstructorInComposable")
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
        // Setup required dependencies for the preview
        val navController: NavHostController = rememberNavController()
        val context = LocalContext.current

        // Initialize ViewModel with the repository for the preview context
        val viewModel =
            AppViewModel(RTSPItemWithSampleInitRepositoryImpl(context, MockAppDatabase()))

        // Render the main screen structure
        NavigationScreen(navController = navController, viewModel = viewModel)
    }
}

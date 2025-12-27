package com.mrkongtk.rtspviewer.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

/**
 * Material Design 3 Dark Color Scheme.
 *
 * This configuration maps the project's specific dark mode color definitions
 * (e.g., [DarkPrimary], [DarkSurface]) to the standard Material 3 semantic slots.
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurface,
    onPrimaryContainer = DarkTextPrimary,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    secondary = DarkSecondary,
    onSecondary = DarkTextSecondary,

    error = ErrorColor,
    onError = OnErrorColor,
)

/**
 * Material Design 3 Light Color Scheme.
 *
 * This configuration maps the project's specific light mode color definitions
 * (e.g., [LightPrimary], [LightSurface]) to the standard Material 3 semantic slots.
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
 * The main Theme Composable for the RTSPViewer application.
 *
 * This wrapper performs the following responsibilities:
 * 1. **Color Resolution:** Selects between Dynamic (Material You), Dark, or Light schemes.
 * 2. **System UI:** Updates the Status Bar and Navigation Bar colors to match the theme background.
 * 3. **Provider:** Applies the calculated [colorScheme] and [Typography] to the content hierarchy.
 *
 * @param darkTheme Whether to apply the dark color palette. Defaults to the system's global setting.
 * @param dynamicColor Whether to use Material You dynamic colors (derived from wallpaper).
 *                     Only available on Android 12+ (API 31+). Defaults to `false`.
 * @param content The Composable content to render within this theme.
 */
@Composable
fun RTSPViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 1. Determine the appropriate ColorScheme based on arguments and OS capabilities
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 2. Side Effect: Update System Bars (Status/Nav) to match the theme background
    LocalActivity.current?.window?.let { window ->
        SideEffect {
            // Use the background color of the scheme for system bars to ensure seamless UI
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    // 3. Provide the MaterialTheme to the subtree
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Theme Previews.
 *
 * Renders a dummy UI containing common components (Card, Text, Button, Icon)
 * to visualize how the [RTSPViewerTheme] handles both Light and Dark modes
 * within the Android Studio design view.
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
        Scaffold(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .background(MaterialTheme.colorScheme.background),
        ) { innerPadding ->
            Card(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Dummy content to test color contrast and mapping
                Text("Theme Test Title")
                Button(onClick = {}) {
                    Text("Primary Button")
                }
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = "Icon Test")
                }
            }
        }
    }
}

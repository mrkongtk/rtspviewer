package com.mrkongtk.rtspviewer.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat

/**
 * Custom Dark Color Scheme mapping local color tokens to Material 3 semantic slots.
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
 * The application's primary Theme wrapper.
 *
 * It manages:
 * 1. Dynamic color support (Android 12+).
 * 2. System UI styling (Status and Navigation bars).
 * 3. Edge-to-edge display configuration.
 *
 * @param darkTheme Forces dark or light mode. Defaults to system preference.
 * @param dynamicColor Enables Material You wallpaper-based colors (API 31+).
 * @param content The UI hierarchy to be themed.
 */
@Composable
fun RTSPViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Determine the color palette (Dynamic vs Static)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Configure System UI and Edge-to-Edge
    LocalActivity.current?.let { activity ->
        (activity as? ComponentActivity)?.let { componentActivity ->
            SideEffect {
                // Apply edge-to-edge styling with background-aware system bar colors
                componentActivity.enableEdgeToEdge(
                    statusBarStyle = if (!darkTheme) {
                        SystemBarStyle.light(
                            LightColorScheme.background.toArgb(),
                            LightColorScheme.background.toArgb()
                        )
                    } else {
                        SystemBarStyle.dark(DarkColorScheme.background.toArgb())
                    },
                    navigationBarStyle = if (!darkTheme) {
                        SystemBarStyle.light(
                            LightColorScheme.background.toArgb(),
                            LightColorScheme.background.toArgb()
                        )
                    } else {
                        SystemBarStyle.dark(DarkColorScheme.background.toArgb())
                    }
                )
            }
        }

        // Adjust system icon contrast (dark icons for light theme, vice-versa)
        val view = LocalView.current
        activity.window?.let { window ->
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

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

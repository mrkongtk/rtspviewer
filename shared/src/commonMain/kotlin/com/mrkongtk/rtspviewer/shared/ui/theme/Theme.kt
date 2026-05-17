package com.mrkongtk.rtspviewer.shared.ui.theme

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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Custom Dark Color Scheme mapping local color tokens to Material 3 semantic slots.
 */
internal val DarkColorScheme = darkColorScheme(
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
internal val LightColorScheme = lightColorScheme(
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

@Composable
expect fun RTSPViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
)

/**
 * Theme Previews.
 *
 * Renders a dummy UI containing common components (Card, Text, Button, Icon)
 * to visualize how the [RTSPViewerTheme] handles both Light and Dark modes
 * within the Android Studio design view.
 */
@Preview
@Composable
fun AndroidRTSPViewerThemePreview() {
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

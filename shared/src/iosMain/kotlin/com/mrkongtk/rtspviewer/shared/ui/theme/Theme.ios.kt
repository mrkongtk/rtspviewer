package com.mrkongtk.rtspviewer.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
actual fun RTSPViewerTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean, // Ignored on iOS
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // On iOS, status bar style is usually controlled by the root ViewController.
    // However, you can use SideEffects to communicate with a ComposeUIViewController.

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

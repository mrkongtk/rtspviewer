package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * A utility Composable that toggles the device's "Keep Screen On" flag.
 *
 * When [enable] is true, the screen will remain active and not dim or lock while
 * this Composable is part of the composition. The flag is automatically cleared
 * when this Composable leaves the composition or when [enable] is set to false.
 *
 * @param enable Whether the screen should be kept on.
 */
@Composable
fun KeepScreenOn(enable: Boolean) {
    val currentView = LocalView.current

    DisposableEffect(enable) {
        currentView.keepScreenOn = enable

        onDispose {
            // Ensure the screen-on flag is cleared when this effect is
            // disposed or the 'enable' state changes.
            currentView.keepScreenOn = false
        }
    }
}

package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
actual fun KeepScreenOn(enable: Boolean) {
    val currentView = LocalView.current

    DisposableEffect(enable) {
        currentView.keepScreenOn = enable

        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

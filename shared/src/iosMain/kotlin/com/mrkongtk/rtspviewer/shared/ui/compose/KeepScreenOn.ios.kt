package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOn(enable: Boolean) {
    val sharedApp = UIApplication.sharedApplication

    DisposableEffect(enable) {
        if (enable) {
            sharedApp.setIdleTimerDisabled(true)
        }

        onDispose {
            if (enable) {
                sharedApp.setIdleTimerDisabled(false)
            }
        }
    }
}

package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.runtime.Composable

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
expect fun KeepScreenOn(enable: Boolean)

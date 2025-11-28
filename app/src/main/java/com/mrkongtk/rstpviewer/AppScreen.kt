package com.mrkongtk.rstpviewer

import androidx.annotation.StringRes

/**
 * enum values that represent the screens in the app
 */
enum class AppScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
}

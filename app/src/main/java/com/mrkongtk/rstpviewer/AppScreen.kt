package com.mrkongtk.rstpviewer

import androidx.annotation.StringRes

/**
 * Defines the navigation destinations (routes) available in the RSTP Viewer application.
 *
 * @property title The string resource ID to display as the screen's title (e.g., in the TopAppBar).
 */
enum class AppScreen(@StringRes val title: Int) {
    /**
     * The initial start screen of the application.
     */
    Start(title = R.string.app_name),
}

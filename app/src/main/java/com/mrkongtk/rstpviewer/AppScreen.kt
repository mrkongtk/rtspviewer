package com.mrkongtk.rstpviewer

import androidx.annotation.StringRes

/**
 * Enumerates the navigation routes available in the RTSP Viewer application.
 *
 * Each entry represents a distinct screen and holds the metadata required
 * to display it (such as the title resource).
 *
 * @property title The string resource ID to display as the screen's title (e.g., in the TopAppBar).
 */
enum class AppScreen(@StringRes val title: Int) {
    /**
     * The landing or home screen of the application.
     */
    Start(title = R.string.app_name),

    /**
     * The video playback screen where the RTSP stream is rendered.
     */
    RSTPDisplay(title = R.string.screen_rtsp_display),
}

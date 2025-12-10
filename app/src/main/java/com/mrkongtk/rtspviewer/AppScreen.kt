package com.mrkongtk.rtspviewer

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
    RTSPDisplay(title = R.string.screen_rtsp_display),

    /**
     * The screen where the user can input details to add a new RTSP stream source.
     */
    AddRTSPItem(title = R.string.screen_add_rtsp_item),
}

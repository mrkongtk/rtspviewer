package com.mrkongtk.rtspviewer.shared.ui.navigation

import org.jetbrains.compose.resources.StringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.app_name
import rtspviewer.shared.generated.resources.screen_add_rtsp_item
import rtspviewer.shared.generated.resources.screen_edit_rtsp_item
import rtspviewer.shared.generated.resources.screen_rtsp_display

/**
 * Enumerates the navigation routes available in the RTSP Viewer application.
 *
 * Each entry represents a distinct screen and holds the metadata required
 * to display it (such as the title resource).
 *
 * @property title The string resource ID to display as the screen's title (e.g., in the TopAppBar).
 */
enum class AppScreen(val title: StringResource) {
    /**
     * The landing or home screen of the application.
     */
    Start(title = Res.string.app_name),

    /**
     * The video playback screen where the RTSP stream is rendered.
     */
    RTSPDisplay(title = Res.string.screen_rtsp_display),

    /**
     * The screen where the user can input details to add a new RTSP stream source.
     */
    AddRTSPItem(title = Res.string.screen_add_rtsp_item),

    /**
     * The screen where the user can modify the details of an existing RTSP stream source.
     */
    EditRTSPItem(title = Res.string.screen_edit_rtsp_item)
}

package com.mrkongtk.rtspviewer.data

import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem

/**
 * Represents the current state of the Application UI.
 * This class holds all data required to render the screen.
 *
 * @property items The list of available RTSP streams to display.
 * @property selectedItem The currently active or selected stream, or null if no selection has been made.
 */
data class AppUiState(
    val items: List<RTSPItem> = emptyList(),
    val selectedItem: RTSPItem? = null,
)

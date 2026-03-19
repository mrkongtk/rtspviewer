package com.mrkongtk.rtspviewer.shared.ui.state

import androidx.compose.ui.graphics.ImageBitmap
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem

/**
 * Represents the current state of the Application UI.
 * This class holds all data required to render the screen, manage stream selection,
 * and handle filtering logic.
 *
 * @property items The list of available RTSP streams to display.
 * @property selectedItem The currently active or selected stream for viewing or editing, or null if no selection has been made.
 * @property cachedPreviews A map storing cached preview images, where the key is the [com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem] ID and the value is the generated [androidx.compose.ui.graphics.ImageBitmap].
 * @property tags The list of all unique tags available to categorize and filter the RTSP streams.
 * @property selectedTag The currently active tag filter. If null, no filter is applied and all items are typically shown.
 */
data class AppUiState(
    val items: List<RTSPItem> = emptyList(),
    val selectedItem: RTSPItem? = null,
    val cachedPreviews: Map<Long, ImageBitmap> = emptyMap(),
    val tags: List<String> = emptyList(),
    val selectedTag: String? = null,
)
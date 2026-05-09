package com.mrkongtk.rtspviewer.shared.ui.screen.action

import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem

/**
 * Defines the set of user interactions available on the main Stream List screen.
 *
 * This interface facilitates the hoisting of UI events (clicks, drags, filters)
 * from the list components to the navigation and data coordinators.
 */
interface StreamListScreenActions {
    /**
     * Triggered when a user selects a specific stream from the list.
     * @param item The [RTSPItem] representing the chosen stream.
     */
    fun onItemSelected(item: RTSPItem)

    /**
     * Triggered when the user requests to create a new RTSP stream entry.
     */
    fun onAddItemSelected()

    /**
     * Triggered after a drag-and-drop reordering gesture is completed.
     * @param orderedList the complete list of items in their new display sequence.
     */
    fun onItemsReordered(orderedList: List<RTSPItem>)

    /**
     * Triggered when a user clicks a tag chip to filter the visible streams.
     * @param tag The tag string to filter by, or null to clear the filter and show "All".
     */
    fun onTagSelected(tag: String?)
}

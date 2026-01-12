package com.mrkongtk.rtspviewer.ui.screen.action

import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem

/**
 * A top-level delegate that aggregates all possible screen actions within the application.
 *
 * This interface is typically implemented by the root container (e.g., [AppInitialScreen])
 * to bridge the gap between individual screen events, the [AppViewModel]'s business logic,
 * and the [NavHostController]'s navigation state.
 */
interface NavigationScreenActions {
    /** Maps to selecting an item from the main list. */
    fun onListingItemSelected(item: RTSPItem)

    /** Maps to the persistence of a brand-new RTSP stream. */
    fun onAddItemRequested(newItem: RTSPItem)

    /** Maps to persisting a new manual sort order for the entire list. */
    fun onItemsReordered(newOrderedList: List<RTSPItem>)

    /** Maps to updating an existing stream's metadata in the database. */
    fun onEditItemRequested(updatedItem: RTSPItem)

    /** Maps to the removal of a stream and its associated preview files. */
    fun onDeleteItemRequested(deleteItem: RTSPItem)

    /** Maps to the file-system persistence of a captured stream thumbnail. */
    fun onImageAvailable(item: RTSPItem, snapshot: Bitmap)

    /** Maps to updating the global UI filter state. */
    fun onTagSelected(tag: String?)
}

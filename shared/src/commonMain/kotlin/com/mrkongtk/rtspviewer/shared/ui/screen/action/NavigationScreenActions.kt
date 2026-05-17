package com.mrkongtk.rtspviewer.shared.ui.screen.action

import androidx.compose.ui.graphics.ImageBitmap
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem

/**
 * A delegate interface that aggregates UI-triggered actions within the application.
 *
 * It bridges individual screen events with the application's business logic
 * and navigation state.
 */
interface NavigationScreenActions {
    fun onListingItemSelected(item: RTSPItem)

    fun onAddItemRequested(newItem: RTSPItem)

    fun onItemsReordered(newOrderedList: List<RTSPItem>)

    fun onEditItemRequested(updatedItem: RTSPItem)

    fun onDeleteItemRequested(deleteItem: RTSPItem)

    fun onImageAvailable(item: RTSPItem, snapshot: ImageBitmap)

    fun onTagSelected(tag: String?)
}

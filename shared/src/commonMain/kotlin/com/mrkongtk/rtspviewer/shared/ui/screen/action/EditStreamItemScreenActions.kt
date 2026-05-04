package com.mrkongtk.rtspviewer.shared.ui.screen.action

import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem

/**
 * Defines the completion actions for the stream configuration form.
 *
 * Used by both the "Add" and "Edit" screens to signal that the user has
 * finished inputting data and wishes to persist the changes.
 */
interface EditStreamItemScreenActions {
    /**
     * Triggered when the user submits a valid form to save or update an item.
     * @param newItem The [RTSPItem] containing the validated data from the form fields.
     */
    fun onSaveItem(newItem: RTSPItem)
}

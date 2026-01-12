package com.mrkongtk.rtspviewer.ui.screen.action

import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem

/**
 * Defines the set of actions available when viewing the details or playback of a specific stream.
 *
 * This interface decouples the player UI from the navigation logic and the
 * image persistence logic.
 */
interface StreamItemScreenActions {
    /**
     * Triggered when the user requests to enter the editing mode for the current stream.
     */
    fun onEditItemSelected()

    /**
     * Triggered when the user requests to permanently delete the current stream.
     */
    fun onDeleteItemSelected()

    /**
     * Triggered periodically by the player to provide a visual snapshot of the feed.
     * @param item The [RTSPItem] associated with the snapshot.
     * @param bitmap The captured video frame to be used for thumbnails or previews.
     */
    fun onImageAvailable(item: RTSPItem, bitmap: Bitmap)
}

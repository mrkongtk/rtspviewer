package com.mrkongtk.rtspviewer.data.repository

import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Defines the contract for the repository responsible for managing RTSP stream items.
 *
 * This repository acts as the single source of truth for the application's data layer,
 * abstracting the underlying data sources (local database, file system, and memory cache).
 */
interface RTSPItemRepository {

    /**
     * A state holder exposing the current list of [RTSPItem]s.
     *
     * This StateFlow allows observers (like ViewModels or UI components) to reactively
     * update whenever the data source changes. It always holds the latest state.
     */
    val items: StateFlow<List<RTSPItem>>

    /**
     * A state holder exposing an in-memory cache of preview images.
     *
     * The Map key represents the [RTSPItem.id], and the value is the generated [Bitmap] preview.
     * This allows the UI to display thumbnails immediately without reading from disk repeatedly.
     */
    val cachedPreviews: StateFlow<Map<Long, Bitmap>>

    /**
     * Asynchronously loads the RTSP items from the underlying data source.
     *
     * This function should be called to initiate data fetching or to refresh the content
     * contained in [items].
     */
    suspend fun loadData()

    /**
     * Asynchronously adds a new [RTSPItem] to the data source.
     *
     * @param item The RTSP stream item to be persisted.
     * @return The unique identifier (typically the row ID) of the newly inserted item.
     */
    suspend fun addItem(item: RTSPItem): Long

    /**
     * Asynchronously updates the order of the RTSP items in the data source.
     *
     * This is typically used to persist changes after a user has rearranged items
     * in the UI (e.g., via drag-and-drop). The order in the database will be updated
     * to match the sequence of the provided list.
     *
     * @param items The list of [RTSPItem]s sorted in the new desired order.
     * @return The number of items successfully updated (rows affected).
     */
    suspend fun reorderItems(items: List<RTSPItem>): Int

    /**
     * Asynchronously updates the details of an existing [RTSPItem].
     *
     * This should be called when the user modifies the properties of a stream
     * (e.g., renaming the stream or changing the RTSP URL).
     *
     * @param item The item with updated values. It must contain the correct ID to map to the existing record.
     * @return The number of rows affected (usually 1 if successful, 0 otherwise).
     */
    suspend fun updateItem(item: RTSPItem): Int

    /**
     * Asynchronously removes an [RTSPItem] from the data source.
     *
     * @param item The RTSP stream item to be deleted.
     * @return The number of rows affected (usually 1 if successful, 0 otherwise).
     */
    suspend fun deleteItem(item: RTSPItem): Int

    /**
     * Generates the file handle for the stored preview image of a specific item.
     *
     * This does not guarantee the file exists; it provides the path where the file
     * *should* reside on the local filesystem.
     *
     * @param item The RTSP item for which the preview path is needed.
     * @return A [File] object pointing to the expected location in the cache directory.
     */
    fun previewPathFor(item: RTSPItem): File

    /**
     * Updates the in-memory cache with a specific bitmap for an RTSP item.
     *
     * This method is responsible for managing memory usage, including the potential
     * recycling of old bitmaps associated with the same ID.
     *
     * @param item The RTSP item the bitmap belongs to.
     * @param bitmap The loaded bitmap image.
     */
    fun cachePreviewFor(item: RTSPItem, bitmap: Bitmap)

    /**
     * Clears all bitmaps from the in-memory cache.
     *
     * This should be called when the data is no longer needed (e.g., ViewModel cleared)
     * to free up native memory by recycling the bitmaps.
     */
    fun removeCachedPreviews()

}

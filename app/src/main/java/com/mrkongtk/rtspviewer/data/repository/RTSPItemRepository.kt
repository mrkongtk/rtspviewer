package com.mrkongtk.rtspviewer.data.repository

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Domain-level abstraction for managing RTSP stream metadata and their associated visual assets.
 *
 * This repository coordinates data between the persistent SQLite storage (Room),
 * the local filesystem for binary previews, and an in-memory cache for UI performance.
 *
 * It serves as the "Single Source of Truth" for the application's data layer.
 */
interface RTSPItemRepository {

    /**
     * A reactive stream of all configured RTSP items.
     *
     * Emits a new list whenever the underlying database table changes.
     * Items are typically sorted by their [RTSPItem.order] property.
     */
    val items: Flow<List<RTSPItem>>

    /**
     * An observable in-memory cache of decoded stream thumbnails.
     *
     * Key: [RTSPItem.id]
     * Value: The latest captured [Bitmap] for that stream.
     *
     * This allows the UI to render thumbnails instantly during list scrolling
     * without triggering expensive disk I/O.
     */
    val cachedPreviews: StateFlow<Map<Long, ImageBitmap>>

    /**
     * Persists a new RTSP stream configuration.
     *
     * Implementations should perform data sanitization (e.g., trimming tags)
     * before saving to the database.
     *
     * @param item The stream configuration to create.
     * @return The unique auto-generated ID of the new item.
     */
    suspend fun addItem(item: RTSPItem): Long

    /**
     * Updates the persistent sort order for a collection of items.
     *
     * This is an optimized operation used primarily after drag-and-drop reordering
     * to ensure the UI state matches the database on the next launch.
     *
     * @param items The list of items in their new sequential order.
     * @return The count of records successfully updated.
     */
    suspend fun reorderItems(items: List<RTSPItem>): Int

    /**
     * Updates the details of an existing RTSP configuration.
     *
     * @param item The item containing updated values (matched by ID).
     * @return The number of affected rows (1 if successful, 0 if the item no longer exists).
     */
    suspend fun updateItem(item: RTSPItem): Int

    /**
     * Removes an RTSP configuration and prepares for cleanup of associated files.
     *
     * @param item The item to delete.
     * @return The number of affected rows.
     */
    suspend fun deleteItem(item: RTSPItem): Int

    /**
     * Resolves the [File] handle where a stream's preview image is stored.
     *
     * Note: This method only calculates the path; it does not verify if the file
     * exists or perform any I/O.
     *
     * @param item The item whose preview path is being requested.
     * @return A [File] reference within the application's internal storage.
     */
    fun previewPathFor(item: RTSPItem): File

    /**
     * Updates the memory-resident cache with a new snapshot for a stream.
     *
     * Implementations should handle resource management, such as recycling
     * superseded bitmaps to prevent memory leaks.
     *
     * @param item The stream associated with the snapshot.
     * @param bitmap The new image data to cache.
     */
    fun cachePreviewFor(item: RTSPItem, bitmap: ImageBitmap)

    /**
     * Invalidates the memory cache and releases all held [Bitmap] resources.
     *
     * Should be called when the stream-viewing feature is disposed or
     * during low-memory conditions.
     */
    fun removeCachedPreviews()
}

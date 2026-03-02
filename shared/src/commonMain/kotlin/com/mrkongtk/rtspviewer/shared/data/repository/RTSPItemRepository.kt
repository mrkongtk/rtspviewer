package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.Path

/**
 * Domain-level abstraction for managing RTSP stream metadata and their associated visual assets.
 *
 * This repository coordinates data between persistent SQLite storage (Room),
 * the local filesystem for binary preview images, and an in-memory cache for UI performance.
 *
 * It serves as the "Single Source of Truth" for the application's data layer.
 */
interface RTSPItemRepository {

    /**
     * A reactive stream of all configured RTSP items.
     *
     * Emits a new list whenever the underlying database changes.
     * Items are typically sorted by their [RTSPItem.order] property.
     */
    val items: Flow<List<RTSPItem>>

    /**
     * An observable in-memory cache of decoded stream thumbnails.
     *
     * Key: [RTSPItem.id]
     * Value: The latest captured [ImageBitmap] for that stream.
     *
     * This allows the UI to render thumbnails instantly during list navigation
     * without triggering expensive disk I/O.
     */
    val cachedPreviews: StateFlow<Map<Long, ImageBitmap>>

    /**
     * Persists a new RTSP stream configuration.
     *
     * Implementations should perform data validation or sanitization
     * before saving to the database.
     *
     * @param item The stream configuration to create.
     * @return The unique auto-generated ID of the new item.
     */
    suspend fun addItem(item: RTSPItem): Long

    /**
     * Updates the persistent sort order for a collection of items.
     *
     * Optimized for use after UI drag-and-drop operations to ensure the
     * sequence is preserved across application launches.
     *
     * @param items The list of items in their new sequential order.
     * @return The count of records successfully updated.
     */
    suspend fun reorderItems(items: List<RTSPItem>): Int

    /**
     * Updates the details of an existing RTSP configuration.
     *
     * @param item The item containing updated values (matched by [RTSPItem.id]).
     * @return The number of affected rows (1 if successful, 0 if the item no longer exists).
     */
    suspend fun updateItem(item: RTSPItem): Int

    /**
     * Removes an RTSP configuration and triggers cleanup of associated local assets.
     *
     * @param item The item to delete.
     * @return The number of affected rows.
     */
    suspend fun deleteItem(item: RTSPItem): Int

    /**
     * Resolves the filesystem [Path] where a stream's preview image is stored.
     *
     * Note: This method only calculates the destination path; it does not perform
     * I/O or verify the file's existence.
     *
     * @param item The item whose preview path is being requested.
     * @return An [okio.Path] pointing to the file location in internal storage.
     */
    fun previewPathFor(item: RTSPItem): Path

    /**
     * Updates the memory-resident cache with a new snapshot for a stream.
     *
     * Implementations should handle resource management, ensuring that old
     * bitmaps are cleared to prevent memory leaks.
     *
     * @param item The stream associated with the snapshot.
     * @param bitmap The new [ImageBitmap] to cache.
     */
    fun cachePreviewFor(item: RTSPItem, bitmap: ImageBitmap)

    /**
     * Invalidates the memory cache and releases all held [ImageBitmap] resources.
     *
     * Should be called during low-memory conditions or when the
     * stream-viewing component is being disposed.
     */
    fun removeCachedPreviews()
}

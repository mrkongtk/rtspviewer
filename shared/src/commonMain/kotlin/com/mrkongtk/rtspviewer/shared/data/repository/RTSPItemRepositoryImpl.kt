package com.mrkongtk.rtspviewer.shared.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import com.mrkongtk.rtspviewer.shared.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItemOrderUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import okio.FileSystem
import okio.Path

/**
 * Single Source of Truth (SSOT) for RTSP stream metadata and preview assets.
 *
 * This repository coordinates data across three layers:
 * 1. **Persistence:** Room database for [RTSPItem] metadata.
 * 2. **Filesystem:** JPEG storage for stream snapshots using Okio.
 * 3. **Memory:** A reactive [ImageBitmap] cache to ensure smooth UI performance.
 *
 * @property fileRepository Helper for performing disk I/O operations for images.
 * @property db The Room database instance.
 */
class RTSPItemRepositoryImpl(
    private val fileRepository: FileRepository,
    private val db: AppDatabase
) : RTSPItemRepository {

    /**
     * A stream of all RTSP items.
     *
     * Side-effect: On each emission, this repository automatically attempts to hydrate
     * the in-memory cache by reading associated preview files from the filesystem.
     */
    override val items: Flow<List<RTSPItem>> = db.rtspItemDao().getAllItemsFlow()
        .onEach { itemList ->
            itemList.forEach { rtspItem ->
                val file = previewPathFor(rtspItem)
                fileRepository.readJPEG(file)?.let { bitmap ->
                    cachePreviewFor(rtspItem, bitmap)
                }
            }
        }

    /**
     * Internal state for the [ImageBitmap] cache to prevent repeated disk reads.
     */
    private val _cachedPreviews = MutableStateFlow<Map<Long, ImageBitmap>>(emptyMap())

    /**
     * Reactive map of cached bitmaps keyed by [RTSPItem.id].
     * Observe this to display stream thumbnails without blocking the UI thread.
     */
    override val cachedPreviews: StateFlow<Map<Long, ImageBitmap>> = _cachedPreviews

    /**
     * Persists a new [RTSPItem] after sanitizing tags.
     *
     * Sanitization includes trimming whitespace, removing empty strings, and
     * deduplicating the tag list.
     *
     * @param item The item to insert.
     * @return The row ID of the newly inserted item.
     */
    override suspend fun addItem(item: RTSPItem): Long {
        val sanitizedItem = item.copy(
            tags = item.tags.mapNotNull { it.trim().ifEmpty { null } }.distinct()
        )
        return db.rtspItemDao().insert(sanitizedItem)
    }

    /**
     * Updates the display order of multiple items.
     *
     * Uses a lightweight projection ([RTSPItemOrderUpdate]) to avoid
     * rewriting full entity rows.
     *
     * @param items List of items with updated [RTSPItem.order] values.
     * @return The number of rows updated.
     */
    override suspend fun reorderItems(items: List<RTSPItem>): Int {
        val updates = items.map { RTSPItemOrderUpdate(id = it.id, order = it.order) }
        return db.rtspItemDao().updateOrders(updates)
    }

    /**
     * Updates an existing [RTSPItem] with tag sanitization.
     *
     * @param item The item to update.
     * @return The number of rows affected.
     */
    override suspend fun updateItem(item: RTSPItem): Int {
        val sanitizedItem = item.copy(
            tags = item.tags.mapNotNull { it.trim().ifEmpty { null } }.distinct()
        )
        return db.rtspItemDao().update(sanitizedItem)
    }

    /**
     * Deletes an item from the database.
     * Note: Does not automatically delete the physical preview file from disk.
     *
     * @param item The item to delete.
     * @return The number of rows affected.
     */
    override suspend fun deleteItem(item: RTSPItem): Int {
        return db.rtspItemDao().delete(item)
    }

    /**
     * Returns the [Path] location for an item's preview thumbnail.
     * Files are stored in the system's temporary directory.
     *
     * @param item The item to resolve a path for.
     * @return The [Path] where the preview should be located.
     */
    override fun previewPathFor(item: RTSPItem): Path {
        return FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "preview_${item.id}.jpg"
    }

    /**
     * Updates the memory cache with a new [ImageBitmap].
     *
     * @param item The associated [RTSPItem].
     * @param bitmap The new snapshot to cache.
     */
    override fun cachePreviewFor(item: RTSPItem, bitmap: ImageBitmap) {
        _cachedPreviews.update { currentMap ->
            val newMap = currentMap.toMutableMap()
            newMap[item.id] = bitmap
            newMap.toMap()
        }
    }

    /**
     * Clears all cached bitmaps from memory.
     * Should be called during high-memory pressure or when the UI is backgrounded.
     */
    override fun removeCachedPreviews() {
        _cachedPreviews.update { _ ->
            emptyMap()
        }
    }
}

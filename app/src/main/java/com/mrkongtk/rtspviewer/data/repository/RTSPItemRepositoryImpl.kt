package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItemOrderUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [RTSPItemRepository] that coordinates data access.
 *
 * This repository acts as the Single Source of Truth (SSOT) for the application's RTSP stream
 * configurations. It synchronizes data between the Room database, the device's internal
 * cache for preview images, and the UI via reactive streams.
 *
 * @property context Used to resolve internal file system paths for image caching.
 * @property db The primary persistence layer for stream metadata.
 */
class RTSPItemRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) : RTSPItemRepository {

    /**
     * Backing property for the list of RTSP items currently in memory.
     */
    private val _items = MutableStateFlow<List<RTSPItem>>(emptyList())

    /**
     * An observable stream of the RTSP item list.
     * UI components should observe this to remain in sync with the database state.
     */
    override val items: StateFlow<List<RTSPItem>> = _items

    /**
     * Backing property for the in-memory bitmap cache to avoid redundant disk I/O.
     */
    private val _cachedPreviews = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    /**
     * An observable map of cached bitmaps where the key is the [RTSPItem.id].
     * Provides fast access to stream snapshots for the UI.
     */
    override val cachedPreviews: StateFlow<Map<Long, Bitmap>> = _cachedPreviews


    /**
     * Fetches the latest data from the local database and pushes it to the [items] flow.
     * This triggers a UI refresh for all active collectors.
     */
    override suspend fun loadData() {
        _items.update { _ ->
            // Fetches all items; range 0 to Max effectively returns the entire sorted collection.
            db.rtspItemDao().getItems(0, Long.MAX_VALUE)
        }
    }

    /**
     * Persists a new [RTSPItem].
     *
     * Logic includes sanitizing tags by trimming whitespace, removing empty strings,
     * and filtering out duplicates before insertion.
     *
     * @param item The model to save.
     * @return The unique database ID assigned to the new entry.
     */
    override suspend fun addItem(item: RTSPItem): Long {
        val sanitizedItem = item.copy(
            tags = item.tags.mapNotNull { it.trim().ifEmpty { null } }.distinct()
        )
        return db.rtspItemDao().insert(sanitizedItem)
    }

    /**
     * Batch updates the display order of RTSP items.
     *
     * Uses a lightweight projection ([RTSPItemOrderUpdate]) to minimize database
     * write overhead by only modifying the 'order' column.
     *
     * @param items List of items containing the updated sequence indices.
     * @return Total number of successfully updated rows.
     */
    override suspend fun reorderItems(items: List<RTSPItem>): Int {
        val updates = items.map { RTSPItemOrderUpdate(id = it.id, order = it.order) }
        return db.rtspItemDao().updateOrders(updates)
    }

    /**
     * Updates an existing record in the database.
     *
     * Similar to [addItem], this method cleanses the tag list before persistence.
     *
     * @param item The entity to update (matched by ID).
     * @return Number of rows affected (1 for success, 0 if ID not found).
     */
    override suspend fun updateItem(item: RTSPItem): Int {
        val sanitizedItem = item.copy(
            tags = item.tags.mapNotNull { it.trim().ifEmpty { null } }.distinct()
        )
        return db.rtspItemDao().update(sanitizedItem)
    }

    /**
     * Deletes an RTSP item from the local database.
     *
     * @param item The entity to remove.
     * @return Number of rows affected.
     */
    override suspend fun deleteItem(item: RTSPItem): Int {
        return db.rtspItemDao().delete(item)
    }

    /**
     * Generates the file system path for a specific stream's preview thumbnail.
     *
     * Files are stored in the application's internal cache directory to ensure they
     * are cleared if the OS needs space or the app is uninstalled.
     */
    override fun previewPathFor(item: RTSPItem): File {
        return File(context.cacheDir, "preview_${item.id}.jpg")
    }

    /**
     * Stores a [Bitmap] in the memory cache for immediate UI access.
     *
     * This method includes memory management logic: if an existing bitmap is
     * replaced, it is explicitly [Bitmap.recycle]'d to free up native memory
     * and prevent OOM (Out Of Memory) errors.
     */
    override fun cachePreviewFor(item: RTSPItem, bitmap: Bitmap) {
        _cachedPreviews.update { currentMap ->
            val newMap = currentMap.toMutableMap()
            // Explicitly recycle the old bitmap if it's no longer used
            newMap.put(item.id, bitmap)?.let { oldBitmap ->
                if (oldBitmap != bitmap && !oldBitmap.isRecycled) {
                    oldBitmap.recycle()
                }
            }
            newMap.toMap()
        }
    }

    /**
     * Clears all bitmaps from memory and invokes [Bitmap.recycle] on each.
     *
     * This should be called during high-memory pressure events or when the
     * feature visibility is lifecycle-stopped to ensure resources are returned to the system.
     */
    override fun removeCachedPreviews() {
        _cachedPreviews.update { currentMap ->
            currentMap.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            emptyMap()
        }
    }
}

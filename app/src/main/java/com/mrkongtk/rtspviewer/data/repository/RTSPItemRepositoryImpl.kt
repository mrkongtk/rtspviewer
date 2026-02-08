package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItemOrderUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

/**
 * Single Source of Truth (SSOT) for RTSP stream metadata and preview assets.
 *
 * This repository coordinates data across three layers:
 * 1. **Persistence:** Room database for [RTSPItem] metadata.
 * 2. **Filesystem:** JPEG storage for stream snapshots in the app's cache directory.
 * 3. **Memory:** A reactive [Bitmap] cache to ensure smooth UI performance.
 *
 * @property context Application context for accessing the internal cache directory.
 * @property db The Room database instance.
 * @property fileRepository Helper for performing disk I/O operations for images.
 */
class RTSPItemRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val fileRepository: FileRepository,
) : RTSPItemRepository {

    /**
     * A stream of all RTSP items.
     *
     * Side-effect: On each emission, this repository automatically attempts to hydrate
     * the in-memory cache by reading associated preview files from disk.
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
     * Internal state for the bitmap cache to prevent repeated disk reads.
     */
    private val _cachedPreviews = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    /**
     * Reactive map of cached bitmaps keyed by [RTSPItem.id].
     * Observe this to display stream thumbnails without blocking the UI thread.
     */
    override val cachedPreviews: StateFlow<Map<Long, Bitmap>> = _cachedPreviews

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
     */
    override suspend fun deleteItem(item: RTSPItem): Int {
        return db.rtspItemDao().delete(item)
    }

    /**
     * Returns the [File] location for an item's preview thumbnail.
     * Files are stored in the application cache and may be cleared by the OS.
     */
    override fun previewPathFor(item: RTSPItem): File {
        return File(context.cacheDir, "preview_${item.id}.jpg")
    }

    /**
     * Updates the memory cache with a new [Bitmap].
     *
     * If an old bitmap is replaced, it is explicitly [Bitmap.recycle]'d
     * to prevent native memory leaks.
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
     * Clears all cached bitmaps and releases their native memory.
     * Should be called during high-memory pressure or when the UI is backgrounded.
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

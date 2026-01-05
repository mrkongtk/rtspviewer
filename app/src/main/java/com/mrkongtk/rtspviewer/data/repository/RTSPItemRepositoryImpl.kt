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
 * Concrete implementation of [RTSPItemRepository].
 *
 * This repository manages data operations for RTSP items, acting as the single source
 * of truth by mediating between the local Room database, the file system (for images),
 * and the UI/Domain layer.
 *
 * @property context The application context, used to access the file system cache directory.
 * @property db The Room database instance injected via Dependency Injection.
 */
class RTSPItemRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) : RTSPItemRepository {

    /**
     * Internal mutable state flow to handle list updates.
     * Acts as the backing field for the public [items] flow.
     */
    private val _items = MutableStateFlow<List<RTSPItem>>(emptyList())

    /**
     * A read-only [StateFlow] observing the list of [RTSPItem]s.
     *
     * UI components should collect from this flow to receive real-time data updates
     * whenever [loadData] is called.
     */
    override val items: StateFlow<List<RTSPItem>> = _items

    /**
     * Internal mutable state flow for the bitmap cache.
     */
    private val _cachedPreviews = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())

    /**
     * A read-only [StateFlow] exposing the map of cached Bitmaps.
     * Key: RTSPItem ID, Value: Bitmap.
     */
    override val cachedPreviews: StateFlow<Map<Long, Bitmap>> = _cachedPreviews


    /**
     * Refreshes the local state by fetching the complete list of RTSP items from the database.
     *
     * The results are emitted to the [items] StateFlow.
     * Currently retrieves all items (from index 0 to [Long.MAX_VALUE]).
     */
    override suspend fun loadData() {
        _items.update { _ ->
            db.rtspItemDao().getItems(0, Long.MAX_VALUE)
        }
    }

    /**
     * Inserts a new [RTSPItem] into the database.
     *
     * @param item The RTSP item entity to be persisted.
     * @return The row ID of the newly inserted item.
     */
    override suspend fun addItem(item: RTSPItem): Long {
        val insertedId = db.rtspItemDao().insert(item)
        return insertedId
    }

    /**
     * Updates the ordering of a list of RTSP items in the database.
     *
     * This method transforms the provided list into [RTSPItemOrderUpdate] objects
     * to perform a partial update, modifying only the order field for the specific IDs.
     *
     * @param items The list of items containing the new order values.
     * @return The number of rows affected by the update.
     */
    override suspend fun reorderItems(items: List<RTSPItem>): Int {
        return items.map {
            RTSPItemOrderUpdate(id = it.id, order = it.order)
        }.let {
            db.rtspItemDao().updateOrders(it)
        }
    }

    /**
     * Updates an existing [RTSPItem] in the database.
     *
     * This replaces the existing entry with the data provided in the [item] parameter.
     *
     * @param item The item containing the updated data (must have a matching ID).
     * @return The number of rows affected (usually 1 if successful).
     */
    override suspend fun updateItem(item: RTSPItem): Int {
        return db.rtspItemDao().update(item)
    }

    /**
     * Permanently deletes an [RTSPItem] from the database.
     *
     * @param item The item to be removed.
     * @return The number of rows affected (usually 1 if successful).
     */
    override suspend fun deleteItem(item: RTSPItem): Int {
        return db.rtspItemDao().delete(item)
    }

    /**
     * Determines the cache file path for a given item's preview image.
     *
     * The file is named "preview_{id}.jpg" and is located in the application's cache directory.
     */
    override fun previewPathFor(item: RTSPItem): File {
        val cacheDir: File = context.cacheDir
        val filePath = File(cacheDir, "preview_${item.id}.jpg")
        return filePath
    }

    /**
     * Caches a bitmap for the specific item in memory.
     *
     * If a bitmap already exists for this ID, and it differs from the new one,
     * the old bitmap is explicitly recycled to prevent memory leaks.
     */
    override fun cachePreviewFor(item: RTSPItem, bitmap: Bitmap) {
        _cachedPreviews.update {
            val newMap = it.toMutableMap()
            newMap.put(item.id, bitmap)?.let { oldBitmap ->
                // Ensure we don't leak memory by holding onto replaced bitmaps
                if (oldBitmap != bitmap && !oldBitmap.isMutable) {
                    oldBitmap.recycle()
                }
            }
            newMap.toMap()
        }
    }

    /**
     * Clears the entire preview cache and recycles all contained bitmaps.
     *
     * This iterates through the current map values and calls [Bitmap.recycle]
     * on any bitmap that hasn't already been recycled.
     */
    override fun removeCachedPreviews() {
        _cachedPreviews.update {
            it.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            emptyMap()
        }
    }

}

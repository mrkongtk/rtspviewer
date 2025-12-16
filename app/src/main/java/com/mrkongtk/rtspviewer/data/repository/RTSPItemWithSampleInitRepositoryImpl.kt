package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItemOrderUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * A concrete implementation of [RTSPItemRepository] that orchestrates data initialization and synchronization.
 *
 * This repository employs a **"Hybrid Seed & Sync"** strategy to populate the UI:
 * 1. **Seed:** Loads default sample data from a local JSON asset file (`rtsp_sample_data.json`).
 * 2. **Fetch:** Retrieves existing user data from the local Room database.
 * 3. **Merge:** Combines the two sources. If an ID conflict occurs, the Database version takes precedence
 *    (preserving user edits over default values).
 * 4. **Persist & Emit:** Saves the merged list back to the database and updates the [items] StateFlow.
 *
 * @property context The application context injected via Hilt, required to access [AssetManager].
 * @property db The Room database instance used for persisting and retrieving user modifications.
 */
class RTSPItemWithSampleInitRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) : RTSPItemRepository {

    /**
     * A dynamic tag for logging, derived from the simple class name.
     */
    private val debugTag: String
        get() = this.javaClass.simpleName

    /**
     * Helper property to access the Android [AssetManager] for file I/O operations.
     */
    private val assetManager: AssetManager
        get() = context.assets

    /**
     * Internal mutable state flow acting as the "Single Source of Truth" for the UI data stream.
     */
    private val _items = MutableStateFlow<List<RTSPItem>>(emptyList())

    /**
     * A public, immutable [StateFlow] observable by the ViewModel or UI.
     *
     * This flow emits the current list of [RTSPItem]s and updates automatically whenever
     * [loadData] completes.
     */
    override val items: StateFlow<List<RTSPItem>> = _items.asStateFlow()

    /**
     * Orchestrates the full data synchronization cycle.
     *
     * This function executes the following logic atomically within a state update:
     * 1. **Load:** Reads `rtsp_sample_data.json` from assets.
     * 2. **Fetch:** Queries all existing items from the Room database.
     * 3. **Merge:** Maps items by ID. Database items overwrite asset items (ensuring user changes persist),
     *    while new items from assets are added.
     * 4. **Persist:** Writes the combined, sorted list back to the database (handling upserts).
     * 5. **Publish:** Updates the [_items] StateFlow with the final list.
     *
     * The final list is sorted first by [RTSPItem.order] (ascending) and then by [RTSPItem.name].
     */
    override suspend fun loadData() {
        _items.update { _ ->
            // 1. Load sample data and convert to a Map keyed by ID
            val sampleItems = loadSampleData() ?: emptyList()
            val mapping = sampleItems.associateBy { it.id }.toMutableMap()

            // 2. Fetch all current items from the DB
            val dbItems = db.rtspItemDao().getItems(0, Long.MAX_VALUE)

            // 3. Merge: Database items overwrite asset items with the same ID
            dbItems.forEach { item ->
                mapping[item.id] = item
            }

            // 4. Convert back to list, sort, and persist the merged state to DB
            val mergedList = mapping.values.toList()
                .sortedWith(compareBy<RTSPItem> { it.order }.thenBy { it.name })

            // Note: Assumes insertAll acts as an UPSERT (Update if exists, Insert if new)
            db.rtspItemDao().insertAll(mergedList)

            // 5. Return the result to update the StateFlow
            mergedList
        }
    }

    /**
     * Persists a new [RTSPItem] to the local database.
     *
     * @param item The RTSP item object to be inserted.
     * @return The row ID of the newly inserted item.
     */
    override suspend fun addItem(item: RTSPItem): Long {
        val insertedId = db.rtspItemDao().insert(item)
        return insertedId
    }

    /**
     * Reads and parses the `rtsp_sample_data.json` file from application assets.
     *
     * This operation is performed on the [Dispatchers.IO] thread to ensure the main thread
     * is never blocked by file I/O or JSON parsing.
     *
     * @return A list of [RTSPItem]s if parsing is successful, or `null` if an exception occurs.
     */
    private suspend fun loadSampleData(): List<RTSPItem>? {
        val jsonDecoder = Json { ignoreUnknownKeys = true }

        return withContext(Dispatchers.IO) {
            try {
                // Open the asset file and read its content
                val text = assetManager.open("rtsp_sample_data.json").use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }

                // Deserialize JSON string to objects
                jsonDecoder.decodeFromString<List<RTSPItem>>(text)
            } catch (e: Exception) {
                Log.e(debugTag, "Error reading or parsing sample data", e)
                null
            }
        }
    }

    /**
     * Updates the persistent order for a list of items.
     *
     * This method is typically called after a drag-and-drop reordering event in the UI.
     * It transforms the provided items into partial update entities ([RTSPItemOrderUpdate])
     * to efficiently update only the 'order' column in the database, rather than replacing entire rows.
     *
     * @param items The list of [RTSPItem]s in their new desired order.
     * @return The number of rows updated in the database.
     */
    override suspend fun reorderItems(items: List<RTSPItem>): Int {
        return items.map {
            RTSPItemOrderUpdate(id = it.id, order = it.order)
        }.let {
            db.rtspItemDao().updateOrders(it)
        }
    }

    /**
     * Updates the properties of an existing item in the database.
     *
     * @param item The [RTSPItem] containing the updated values.
     * @return The number of rows affected (should be 1 if the item exists).
     */
    override suspend fun updateItem(item: RTSPItem): Int {
        return db.rtspItemDao().update(item)
    }

    /**
     * Removes an item permanently from the database.
     *
     * @param item The [RTSPItem] to be deleted.
     * @return The number of rows affected (should be 1 if the item existed).
     */
    override suspend fun deleteItem(item: RTSPItem): Int {
        return db.rtspItemDao().delete(item)
    }
}

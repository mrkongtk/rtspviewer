package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.mrkongtk.rtspviewer.data.database.AppDatabase
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
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
 * A concrete implementation of [RTSPItemRepository] that handles data initialization and synchronization.
 *
 * This repository employs a hybrid strategy to populate the UI:
 * 1. Loads default sample data from a local JSON asset file (`rtsp_sample_data.json`).
 * 2. Fetches existing user data from the local Room database.
 * 3. Merges the two sources (giving the Database precedence for conflicting IDs).
 * 4. Persists the merged result back to the database and emits it via [items].
 *
 * @property context The application context injected via Hilt, used to access [AssetManager].
 * @property db The Room database instance used for persisting and retrieving user modifications.
 */
class RTSPItemWithSampleInitRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) : RTSPItemRepository {

    /**
     * A dynamic tag for logging, set to the simple class name (e.g., "RTSPItemWithSampleInitRepositoryImpl").
     */
    private val debugTag: String
        get() = this.javaClass.simpleName

    /**
     * Helper property to access the Android AssetManager for file operations.
     */
    private val assetManager: AssetManager
        get() = context.assets

    /**
     * Internal mutable state flow that holds the current list of RTSP items.
     * Acts as the single source of truth for the [items] stream.
     */
    private val _items = MutableStateFlow<List<RTSPItem>>(emptyList())

    /**
     * A public, immutable [StateFlow] observable by the ViewModel or UI.
     * Emits the latest list of [RTSPItem]s whenever the data is loaded or updated.
     */
    override val items: StateFlow<List<RTSPItem>> = _items.asStateFlow()

    /**
     * Orchestrates the data loading and synchronization process.
     *
     * This function performs the following steps atomically:
     * 1. Loads sample data from assets.
     * 2. Loads existing data from the database.
     * 3. **Merges** them: If an item exists in the DB, it overwrites the sample data (preserving user changes).
     * 4. **Persists** the combined list back to the database (`insertAll` typically handles upserts).
     * 5. Updates the [_items] StateFlow.
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

            // 4. Convert back to list and persist the merged state to DB
            val mergedList = mapping.values.toList()

            // Note: This assumes insertAll acts as an UPSERT (Update if exists, Insert if new)
            db.rtspItemDao().insertAll(mergedList)

            // 5. Return the result to update the StateFlow
            mergedList
        }
    }

    /**
     * Reads and parses the "rtsp_sample_data.json" file from the application assets.
     *
     * This function is strictly for reading the initial seed data. It operates on the
     * [Dispatchers.IO] thread to prevent blocking the main thread during file I/O.
     *
     * @return A list of [RTSPItem] if parsing is successful; otherwise `null`.
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
}

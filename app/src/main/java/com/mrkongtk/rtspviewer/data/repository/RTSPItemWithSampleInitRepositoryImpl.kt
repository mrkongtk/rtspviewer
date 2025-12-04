package com.mrkongtk.rtspviewer.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.mrkongtk.rtspviewer.data.RTSPItem
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
 * A concrete implementation of [RTSPItemRepository] that serves as a data source
 * by loading and parsing a local JSON file from the Android application assets.
 *
 * @property context The application context injected via Hilt, required to access the [AssetManager].
 */
class RTSPItemWithSampleInitRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RTSPItemRepository {

    /**
     * A tag used for logging, dynamically set to the class name.
     */
    private val debugTag: String
        get() = this.javaClass.simpleName

    /**
     * Helper property to access the Android AssetManager.
     */
    private val assetManager: AssetManager
        get() = context.assets

    /**
     * Internal mutable state flow that holds the current list of RTSP items.
     * This acts as the "backing field" for the public [items] property.
     */
    private val _items = MutableStateFlow<List<RTSPItem>>(emptyList())

    /**
     * Publicly exposed immutable [StateFlow] to be observed by the ViewModel or UI.
     * It emits updates whenever the data inside [_items] changes.
     */
    override val items: StateFlow<List<RTSPItem>> = _items.asStateFlow()

    /**
     * Configures the JSON parser.
     * [ignoreUnknownKeys] is set to true so that the app doesn't crash if the
     * JSON file contains fields that are not defined in the [RTSPItem] data class.
     */
    private val jsonDecoder = Json { ignoreUnknownKeys = true }

    /**
     * Triggers the asynchronous data loading process.
     * It calls [loadSampleData] and updates the [_items] StateFlow with the result.
     * If loading fails, an empty list is emitted to clear the state.
     */
    override suspend fun loadData() {
        _items.update { _ ->
            // Update the state atomically with the result of the load operation
            loadSampleData() ?: emptyList()
        }
    }

    /**
     * Reads the "rtsp_sample_data.json" file from the assets folder and deserializes it.
     *
     * This function is main-safe; it moves execution to the [Dispatchers.IO] thread
     * to perform the blocking file I/O operations.
     *
     * @return A list of [RTSPItem] if successful, or null if an IO or parsing error occurs.
     */
    private suspend fun loadSampleData(): List<RTSPItem>? {
        // Switch to the IO dispatcher for disk operations
        return withContext(Dispatchers.IO) {
            try {
                // Open the asset file. The .use block ensures the InputStream is
                // automatically closed after reading, preventing memory leaks.
                val text = assetManager.open("rtsp_sample_data.json").use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }

                // Deserialize the JSON string into a strongly-typed List
                val data = jsonDecoder.decodeFromString<List<RTSPItem>>(text)
                data
            } catch (e: Exception) {
                // Log the specific error for debugging purposes (e.g., FileNotFound, MalformedJson)
                Log.e(debugTag, "Error reading or parsing sample data: $e")
                null
            }
        }
    }
}

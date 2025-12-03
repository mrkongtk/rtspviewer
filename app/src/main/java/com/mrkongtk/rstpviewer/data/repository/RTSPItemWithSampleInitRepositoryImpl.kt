package com.mrkongtk.rstpviewer.data.repository

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.mrkongtk.rstpviewer.data.RTSPItem
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
 * An implementation of [RTSPItemRepository] that initializes its data source
 * by loading a sample JSON file from the Android application assets.
 *
 * @param context The application context, injected via Hilt, used to access [AssetManager].
 */
class RTSPItemWithSampleInitRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RTSPItemRepository {

    // simplified tag for logging
    private val debugTag: String
        get() = this.javaClass.simpleName

    private val assetManager: AssetManager
        get() = context.assets

    // Backing mutable state flow to hold the list of RTSP items
    private val _items = MutableStateFlow<List<RTSPItem>>(emptyList())

    // Publicly exposed immutable StateFlow to be observed by the UI/ViewModel
    override val items: StateFlow<List<RTSPItem>> = _items.asStateFlow()

    /**
     * Triggers the data loading process.
     * It fetches the sample data and updates the [_items] StateFlow.
     */
    override suspend fun loadData() {
        _items.update { _ ->
            // Load data from assets; return empty list if loading fails/returns null
            loadSampleData() ?: emptyList()
        }
    }

    /**
     * Reads "rtsp_sample_data.json" from the assets folder and deserializes it.
     * This operation is performed on the [Dispatchers.IO] thread to prevent blocking.
     *
     * @return A list of [RTSPItem] or null if an error occurs.
     */
    private suspend fun loadSampleData(): List<RTSPItem>? {
        return withContext(Dispatchers.IO) {
            try {
                // Open the asset file, buffer the input, and read the entire text content
                val text = assetManager.open("rtsp_sample_data.json").use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }

                // Decode the JSON string into a List of RTSPItem objects
                val data = Json.decodeFromString<List<RTSPItem>>(text)
                data
            } catch (e: Exception) {
                Log.e(debugTag, "read data error: $e")
                null
            }
        }
    }
}

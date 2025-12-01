package com.mrkongtk.rstpviewer.viewmode

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrkongtk.rstpviewer.model.RSTPItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * ViewModel responsible for managing and displaying the list of RTSP streams.
 *
 * This ViewModel handles:
 * 1. Loading initial sample data from the app's assets.
 * 2. Exposing the list of [RSTPItem]s as a StateFlow for the UI.
 * 3. Providing mechanisms to update the data source.
 */
@HiltViewModel
class StreamListScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Tag for logging purposes, uses the class name dynamically
    private val debugTag: String
        get() = this.javaClass.simpleName

    // Helper to access Android AssetManager
    private val assetManager: AssetManager
        get() = context.assets

    // Backing property: Mutable state only accessible inside the ViewModel
    private val _rstpItems = MutableStateFlow<List<RSTPItem>>(emptyList())

    /**
     * Public immutable StateFlow to be observed by the Compose UI.
     * Represents the current list of RTSP items.
     */
    val rstpItems: StateFlow<List<RSTPItem>> = _rstpItems.asStateFlow()

    init {
        // Automatically load data when the ViewModel is created
        loadData()
    }

    /**
     * Reads the "rstp_sample_data.json" file from assets, parses it, and sorts it.
     *
     * @return A sorted list of [RSTPItem] or null if an error occurs.
     */
    private suspend fun loadSampleData(): List<RSTPItem>? {
        // Switch to IO Dispatcher to perform file I/O operations off the main thread
        return withContext(Dispatchers.IO) {
            try {
                // Open and read the JSON file content
                val text = assetManager.open("rstp_sample_data.json").use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }

                // Deserialize JSON to data class and sort by the 'order' property
                val data = Json.decodeFromString<List<RSTPItem>>(text)
                data.sortedBy { it.order }
            } catch (e: Exception) {
                Log.e(debugTag, "read data error: $e")
                null
            }
        }
    }

    /**
     * Triggers the loading of sample data asynchronously.
     * Updates [rstpItems] upon success.
     */
    fun loadData() {
        viewModelScope.launch {
            loadSampleData()?.let { data ->
                updateData(data)
            }
        }
    }

    /**
     * Updates the current list of RTSP items.
     *
     * @param input The new list of items to display.
     * @param immediate If true, updates the value synchronously (non-blocking).
     *                  If false, emits the change via a coroutine.
     */
    fun updateData(input: List<RSTPItem>, immediate: Boolean = false) {
        if (immediate) {
            // Directly set the value (useful if not currently in a suspend function)
            _rstpItems.value = input
        } else {
            // Emit the value within the ViewModel scope (thread-safe, suspendable)
            viewModelScope.launch {
                _rstpItems.emit(input)
            }
        }
    }
}

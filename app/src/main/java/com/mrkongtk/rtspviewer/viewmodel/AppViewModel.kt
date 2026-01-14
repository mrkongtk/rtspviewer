package com.mrkongtk.rtspviewer.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrkongtk.rtspviewer.data.AppUiState
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.FileRepository
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The primary [ViewModel] for the application, acting as the state holder and coordinator
 * between the UI and data layers.
 *
 * It follows the Unidirectional Data Flow (UDF) pattern, exposing a single [uiState]
 * and handling events to modify data via repositories.
 *
 * **Key Responsibilities:**
 * - **Reactive State Management:** Combines database items, tags, and selection states into a single UI state.
 * - **Stream CRUD Operations:** Manages the lifecycle of [RTSPItem] entities.
 * - **Tag Management:** Aggregates and filters unique tags for the UI.
 * - **Media Persistence:** Handles saving and caching RTSP stream snapshots (thumbnails).
 *
 * @property rtspItemRepository Source of truth for RTSP metadata and in-memory preview caching.
 * @property fileRepository Handles filesystem I/O for persisting preview images.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val rtspItemRepository: RTSPItemRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _selectedItem = MutableStateFlow<RTSPItem?>(null)

    /**
     * The unified UI state for the application.
     *
     * Automatically reacts to changes in:
     * 1. The database [itemList].
     * 2. The in-memory bitmap cache [cachedPreviews].
     * 3. User selections ([_selectedItem], [_selectedTag]).
     *
     * Includes logic to validate that selections remain valid when the underlying data changes.
     */
    val uiState: StateFlow<AppUiState> = combine(
        rtspItemRepository.items,
        rtspItemRepository.cachedPreviews,
        _selectedItem,
        _selectedTag
    ) { itemList, cachedPreviews, selectedItem, selectedTag ->

        // Ensure the selected item still exists in the list (handle deletions)
        val validItem = selectedItem?.let { item ->
            itemList.firstOrNull { it.id == item.id }
        }

        // Extract and sort unique tags from all items for the filter UI
        val tags = itemList.flatMap { it.tags }.toSortedSet().toList()

        // Reset the tag filter if the selected tag no longer exists
        val validTag = selectedTag?.let {
            if (tags.contains(it)) it else null
        }

        AppUiState(
            items = itemList,
            selectedItem = validItem,
            tags = tags,
            selectedTag = validTag,
            cachedPreviews = cachedPreviews
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppUiState()
    )

    /**
     * Clears in-memory preview bitmaps when the ViewModel is destroyed to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        rtspItemRepository.removeCachedPreviews()
    }

    /**
     * Updates the currently selected RTSP stream.
     */
    fun select(rtspItem: RTSPItem) {
        viewModelScope.launch {
            _selectedItem.update { rtspItem }
        }
    }

    /**
     * Updates the active tag filter. Use `null` to clear the filter.
     */
    fun select(selectedTag: String?) {
        viewModelScope.launch {
            _selectedTag.update { selectedTag }
        }
    }

    /**
     * Persists a new RTSP item to the database.
     */
    fun addItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            rtspItemRepository.addItem(rtspItem)
        }
    }

    /**
     * Updates the sort order of items in the database.
     * Usually triggered by a drag-and-drop interaction in the UI.
     */
    fun reorderItems(items: List<RTSPItem>) {
        if (items.isNotEmpty()) {
            viewModelScope.launch {
                rtspItemRepository.reorderItems(items)
            }
        }
    }

    /**
     * Updates an existing RTSP item's metadata (e.g., name, URL, or tags).
     */
    fun editItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            rtspItemRepository.updateItem(rtspItem)
        }
    }

    /**
     * Deletes an RTSP item from the database.
     */
    fun deleteItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            rtspItemRepository.deleteItem(rtspItem)
        }
    }

    /**
     * Saves a snapshot of an RTSP stream to disk and updates the in-memory cache.
     *
     * @param rtspItem The item the preview belongs to.
     * @param bitmap The image data to persist.
     */
    fun savePreview(rtspItem: RTSPItem, bitmap: Bitmap) {
        viewModelScope.launch {
            val file = rtspItemRepository.previewPathFor(rtspItem)
            if (fileRepository.writeJPEG(file, bitmap)) {
                // Update memory cache only after a successful disk write
                rtspItemRepository.cachePreviewFor(rtspItem, bitmap)
            }
        }
    }
}

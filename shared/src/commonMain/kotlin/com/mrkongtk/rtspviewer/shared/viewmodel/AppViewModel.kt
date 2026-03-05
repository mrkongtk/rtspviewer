package com.mrkongtk.rtspviewer.shared.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrkongtk.rtspviewer.shared.data.AppUiState
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.data.repository.FileRepository
import com.mrkongtk.rtspviewer.shared.data.repository.RTSPItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The primary [ViewModel] for the application, acting as the state holder and coordinator
 * between the UI and data layers.
 *
 * It follows the Unidirectional Data Flow (UDF) pattern, exposing a single [uiState]
 * and handling events to modify data via repositories. Dependencies are provided via Koin.
 *
 * **Key Responsibilities:**
 * - **Reactive State Management:** Combines database items, tags, and selection states into a single UI state.
 * - **Stream CRUD Operations:** Manages the lifecycle of [RTSPItem] entities.
 * - **Tag Management:** Aggregates and filters unique tags for the UI.
 * - **Media Persistence:** Handles saving and caching RTSP stream snapshots (thumbnails).
 *
 * @property fileRepository Handles filesystem I/O for persisting preview images.
 * @property rtspItemRepository Source of truth for RTSP metadata and in-memory preview caching.
 */
class AppViewModel(
    private val fileRepository: FileRepository,
    private val rtspItemRepository: RTSPItemRepository
) : ViewModel() {

    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _selectedItem = MutableStateFlow<RTSPItem?>(null)

    /**
     * The unified UI state for the application, exposed as a [StateFlow].
     *
     * Automatically reacts to and combines changes from:
     * 1. The database stream ([rtspItemRepository.items]).
     * 2. The in-memory bitmap cache ([rtspItemRepository.cachedPreviews]).
     * 3. User UI selections ([_selectedItem] and [_selectedTag]).
     *
     * The logic ensures that if an item or tag is deleted from the database, the
     * selection state is invalidated to prevent UI inconsistencies.
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
        val tags = itemList.flatMap { it.tags }.toSet().sorted()

        // Reset the tag filter if the selected tag no longer exists in the current dataset
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
     * Updates the currently selected RTSP stream for viewing or editing.
     *
     * @param rtspItem The [RTSPItem] to be set as the current selection.
     */
    fun select(rtspItem: RTSPItem) {
        viewModelScope.launch {
            _selectedItem.update { rtspItem }
        }
    }

    /**
     * Updates the active tag filter used to narrow down the displayed list of streams.
     *
     * @param selectedTag The name of the tag to filter by, or `null` to show all items.
     */
    fun select(selectedTag: String?) {
        viewModelScope.launch {
            _selectedTag.update { selectedTag }
        }
    }

    /**
     * Persists a new RTSP item to the underlying database.
     *
     * @param rtspItem The new stream configuration to save.
     */
    fun addItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            rtspItemRepository.addItem(rtspItem)
        }
    }

    /**
     * Updates the sort order of items in the database.
     * Usually triggered by a drag-and-drop interaction in the UI list.
     *
     * @param items The full list of items in their new desired order.
     */
    fun reorderItems(items: List<RTSPItem>) {
        if (items.isNotEmpty()) {
            viewModelScope.launch {
                rtspItemRepository.reorderItems(items)
            }
        }
    }

    /**
     * Updates an existing RTSP item's metadata (e.g., name, URL, or tags) in the database.
     *
     * @param rtspItem The item containing updated information.
     */
    fun editItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            rtspItemRepository.updateItem(rtspItem)
        }
    }

    /**
     * Deletes a specific RTSP item from the database.
     *
     * @param rtspItem The item to be removed.
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
     * @param bitmap The [ImageBitmap] data to persist as a JPEG.
     */
    fun savePreview(rtspItem: RTSPItem, bitmap: ImageBitmap) {
        viewModelScope.launch {
            val file = rtspItemRepository.previewPathFor(rtspItem)
            if (fileRepository.writeJPEG(file, bitmap)) {
                // Update memory cache only after a successful disk write to ensure consistency
                rtspItemRepository.cachePreviewFor(rtspItem, bitmap)
            }
        }
    }
}

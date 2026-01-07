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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing the UI state of the main application screen.
 *
 * This class serves as the bridge between the UI layer and the data layer (Repositories).
 * It handles business logic such as fetching RTSP items, managing user selections,
 * persisting list reordering, and handling file I/O for stream preview snapshots.
 *
 * @property rtspItemRepository The injected repository used to perform CRUD operations on RTSP stream data.
 * @property fileRepository The injected repository used for handling file system operations (e.g., saving/loading images).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val rtspItemRepository: RTSPItemRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    /**
     * Internal mutable state flow used to track transient UI state.
     */
    private val _uiState = MutableStateFlow(AppUiState())

    /**
     * Public read-only StateFlow exposed to the UI (Compose/Views).
     * Any change in the underlying data or selection will trigger a UI recomposition.
     */
    val uiState: StateFlow<AppUiState> = _uiState

    init {
        // Observe the list of RTSP items from the repository
        viewModelScope.launch {
            rtspItemRepository.items.collect { itemList ->
                _uiState.update { currentState ->
                    // 1. Validate currently selected item:
                    // If the list changed, ensure the selected item still exists in the DB.
                    val selectedItem = currentState.selectedItem?.let { item ->
                        itemList.firstOrNull { it.id == item.id }
                    }

                    // 2. Extract and Sort Tags:
                    // Collect all unique tags from all items, sort them alphabetically.
                    val tags = itemList.flatMap { it.tags }.toSortedSet().toList()

                    // 3. Validate selected tag:
                    // If the current filter tag no longer exists in the new list, reset to null.
                    val selectedTag = currentState.selectedTag?.let {
                        if (tags.contains(it)) it else null
                    }

                    currentState.copy(
                        items = itemList,
                        selectedItem = selectedItem,
                        tags = tags,
                        selectedTag = selectedTag,
                    )
                }
            }
        }

        // Observe changes to the preview cache (bitmaps)
        viewModelScope.launch {
            rtspItemRepository.cachedPreviews.collect { cachedPreviews ->
                _uiState.update { currentState ->
                    currentState.copy(cachedPreviews = cachedPreviews)
                }
            }
        }

        // Initial Data Loading: Triggers the DB fetch and populates the memory cache from disk.
        viewModelScope.launch {
            rtspItemRepository.loadData()

            // Iterate through loaded items to hydrate the preview cache from the file system.
            // This prevents UI lag by loading images into memory once on startup.
            rtspItemRepository.items.value.forEach { rtspItem ->
                val file = rtspItemRepository.previewPathFor(rtspItem)
                fileRepository.readJPEG(file)?.let { bitmap ->
                    rtspItemRepository.cachePreviewFor(rtspItem, bitmap)
                }
            }
        }
    }

    /**
     * Cleanup method called when the ViewModel is destroyed.
     * Clears the in-memory bitmap cache to prevent memory leaks and free up resources.
     */
    override fun onCleared() {
        super.onCleared()
        rtspItemRepository.removeCachedPreviews()
    }

    /**
     * Updates the UI state to reflect the specific RTSP item selected by the user.
     *
     * @param rtspItem The [RTSPItem] clicked or selected by the user.
     */
    fun select(rtspItem: RTSPItem) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(selectedItem = rtspItem)
            }
        }
    }

    /**
     * Updates the UI state to filter the list by a specific tag.
     *
     * @param selectedTag The tag string to filter by. Passing `null` clears the filter.
     */
    fun select(selectedTag: String?) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                // Only update if the tag actually exists in the current list or if we are clearing it
                val validTag = selectedTag?.takeIf { currentState.tags.contains(it) }
                currentState.copy(selectedTag = validTag)
            }
        }
    }

    /**
     * Asynchronously adds a new RTSP item to the database.
     *
     * If the insertion is successful (returns a row ID > 0), it triggers a reload
     * of the repository data to refresh the UI list.
     *
     * @param rtspItem The new [RTSPItem] to be persisted.
     */
    fun addItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            if (rtspItemRepository.addItem(rtspItem) > 0) {
                rtspItemRepository.loadData()
            }
        }
    }

    /**
     * Updates the custom sort order of the RTSP items in the database.
     *
     * This is typically called when a user drags and drops items in the UI list.
     * If the update is successful, the data is reloaded to ensure the UI reflects
     * the persisted order.
     *
     * @param items The list of [RTSPItem]s in their new desired order.
     */
    fun reorderItems(items: List<RTSPItem>) {
        if (items.isNotEmpty()) {
            viewModelScope.launch {
                // Returns the number of rows updated; if > 0, refresh the data.
                if (rtspItemRepository.reorderItems(items) > 0) {
                    rtspItemRepository.loadData()
                }
            }
        }
    }

    /**
     * Asynchronously updates the details of an existing RTSP item in the database.
     *
     * This method is used when users edit properties like the name or URL.
     * If the update affects the database (result > 0), the repository data is reloaded.
     *
     * @param rtspItem The [RTSPItem] containing the updated values.
     */
    fun editItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            if (rtspItemRepository.updateItem(rtspItem) > 0) {
                rtspItemRepository.loadData()
            }
        }
    }

    /**
     * Asynchronously deletes a specific RTSP item from the database.
     *
     * Upon successful deletion, the repository data is reloaded to ensure
     * the item is removed from the UI list.
     *
     * @param rtspItem The [RTSPItem] to be deleted.
     */
    fun deleteItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            if (rtspItemRepository.deleteItem(rtspItem) > 0) {
                rtspItemRepository.loadData()
            }
        }
    }

    /**
     * Persists a snapshot (preview) of an RTSP stream to the file system and updates the cache.
     *
     * This method:
     * 1. Determines the correct file path for the item.
     * 2. Writes the bitmap to disk via the [FileRepository].
     * 3. If successful, updates the in-memory cache in [RTSPItemRepository] so the UI updates immediately.
     *
     * @param rtspItem The [RTSPItem] associated with the preview.
     * @param bitmap The image data to save.
     */
    fun savePreview(rtspItem: RTSPItem, bitmap: Bitmap) {
        viewModelScope.launch {
            val file = rtspItemRepository.previewPathFor(rtspItem)
            if (fileRepository.writeJPEG(file, bitmap)) {
                // Update memory cache only after successful file write
                rtspItemRepository.cachePreviewFor(rtspItem, bitmap)
            }
        }
    }
}

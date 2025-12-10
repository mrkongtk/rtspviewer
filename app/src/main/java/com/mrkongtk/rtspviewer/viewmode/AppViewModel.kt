package com.mrkongtk.rtspviewer.viewmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrkongtk.rtspviewer.data.AppUiState
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main application screen.
 *
 * This class manages the UI state, handles user interactions (like selecting or adding items),
 * and acts as a bridge between the UI and the RTSPItemRepository.
 *
 * @property rtspItemRepository The repository used to fetch and store RTSP stream data.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val rtspItemRepository: RTSPItemRepository,
) : ViewModel() {

    // Internal mutable state flow to track UI-specific state (like the currently selected item).
    private val _uiState = MutableStateFlow(AppUiState(items = emptyList(), selectedItem = null))

    /**
     * Public immutable Flow representing the current state of the UI.
     *
     * It uses [combine] to merge:
     * 1. The internal UI state (e.g., selection).
     * 2. The data stream from the repository (the list of RTSP items).
     *
     * This ensures the UI always displays the most up-to-date list from the database
     * while preserving the user's current selection.
     */
    val uiState = combine(_uiState, rtspItemRepository.items) { currentState, itemList ->
        currentState.copy(items = itemList)
    }

    init {
        // Trigger an initial data load from the repository when the ViewModel is created.
        viewModelScope.launch {
            rtspItemRepository.loadData()
        }
    }

    /**
     * Updates the UI state to reflect the specific RTSP item selected by the user.
     *
     * @param rtspItem The item clicked or selected by the user.
     */
    fun select(rtspItem: RTSPItem) {
        _uiState.update { currentState ->
            currentState.copy(selectedItem = rtspItem)
        }
    }

    /**
     * Adds a new RTSP item to the database.
     *
     * This runs asynchronously. If the insertion is successful (row ID > 0),
     * it triggers a reload of the data to refresh the list.
     *
     * @param rtspItem The new RTSP item to be added.
     */
    fun addItem(rtspItem: RTSPItem) {
        viewModelScope.launch {
            // Attempt to add the item and check if the operation was successful
            if (rtspItemRepository.addItem(rtspItem) > 0) {
                rtspItemRepository.loadData()
            }
        }
    }
}

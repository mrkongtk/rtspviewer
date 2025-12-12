package com.mrkongtk.rtspviewer.viewmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrkongtk.rtspviewer.data.AppUiState
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.data.repository.RTSPItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing the UI state of the main application screen.
 *
 * This class serves as the bridge between the UI layer and the [RTSPItemRepository].
 * It handles business logic such as fetching data, handling user selections, adding new items,
 * and persisting list reordering.
 *
 * @property rtspItemRepository The injected repository used to perform CRUD operations on RTSP stream data.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val rtspItemRepository: RTSPItemRepository,
) : ViewModel() {

    /**
     * Internal mutable state flow used to track transient UI state, such as the
     * currently selected item, independent of the database data.
     */
    private val _uiState = MutableStateFlow(AppUiState(items = emptyList(), selectedItem = null))

    /**
     * Public immutable [Flow] representing the distinct, combined state of the UI.
     *
     * This flow combines:
     * 1. The internal UI state (e.g., current selection).
     * 2. The persistent data stream from the repository (the list of RTSP items).
     *
     * By combining these streams, the UI automatically updates whenever the database changes
     * (Single Source of Truth) while maintaining the current user selection.
     */
    val uiState: Flow<AppUiState> =
        combine(_uiState, rtspItemRepository.items) { currentState, itemList ->
        currentState.copy(items = itemList)
    }

    init {
        // Triggers the initial data fetch from the repository when the ViewModel is instantiated.
        viewModelScope.launch {
            rtspItemRepository.loadData()
        }
    }

    /**
     * Updates the UI state to reflect the specific RTSP item selected by the user.
     *
     * @param rtspItem The [RTSPItem] clicked or selected by the user.
     */
    fun select(rtspItem: RTSPItem) {
        _uiState.update { currentState ->
            currentState.copy(selectedItem = rtspItem)
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
}

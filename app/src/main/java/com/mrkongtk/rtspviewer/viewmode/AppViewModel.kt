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

@HiltViewModel
class AppViewModel @Inject constructor(
    private val rtspItemRepository: RTSPItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState(items = emptyList(), selectedItem = null))

    val uiState = combine(_uiState, rtspItemRepository.items) { currentState, itemList ->
        currentState.copy(items = itemList)
    }

    init {
        viewModelScope.launch {
            rtspItemRepository.loadData()
        }
    }

    fun select(rtspItem: RTSPItem) {
        _uiState.update { currentState ->
            currentState.copy(selectedItem = rtspItem)
        }
    }
}
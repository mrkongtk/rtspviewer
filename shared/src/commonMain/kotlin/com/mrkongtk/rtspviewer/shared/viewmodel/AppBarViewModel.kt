package com.mrkongtk.rtspviewer.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrkongtk.rtspviewer.shared.AppScreen
import com.mrkongtk.rtspviewer.shared.data.AppBarState
import com.mrkongtk.rtspviewer.shared.data.AppBarTitle
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and content of the Top App Bar.
 *
 * This ViewModel centralizes the logic for the application's header, ensuring that
 * titles and navigation icons remain consistent across different screen transitions.
 * It transforms raw navigation and selection data into UI-ready state objects.
 *
 * **Key Responsibilities:**
 * - Resolving dynamic titles (e.g., "Main Screen" vs. "Editing: My Camera").
 * - Determining whether the 'Back' navigation button should be visible.
 * - Decoupling the UI's `Scaffold` components from individual screen logic.
 */

class AppBarViewModel : ViewModel() {

    /**
     * Internal state containing the current screen context and selected item data.
     */
    private val _appBarState = MutableStateFlow(AppBarState(appScreen = AppScreen.Start))

    /**
     * A reactive stream providing the formatted title for the Top Bar.
     *
     * It maps the current state into an [AppBarTitle] object. If a stream item is selected,
     * the item's name is passed as a formatting argument to the screen's title resource
     * (e.g., to replace %1 in "Viewing %1").
     */
    val title = _appBarState.map { state ->
        AppBarTitle(
            id = state.appScreen.title,
            args = state.selectedItem?.name?.let { listOf(it) } ?: emptyList()
        )
    }

    /**
     * A reactive stream indicating whether the UI should show a 'Back' or 'Up' arrow icon.
     */
    val canNavigateBack = _appBarState.map { it.canNavigateBack }

    /**
     * Updates the Top Bar context when the user navigates to a new screen.
     *
     * @param appScreen The metadata for the new destination (contains the title string resource).
     * @param canNavigateBack Set to true if there is a previous entry in the backstack.
     */
    fun update(appScreen: AppScreen, canNavigateBack: Boolean) {
        _appBarState.update {
            it.copy(appScreen = appScreen, canNavigateBack = canNavigateBack)
        }
    }

    /**
     * Updates the Top Bar with the currently selected RTSP stream.
     *
     * This allows the title to display specific details about the stream (like its name)
     * when on display or edit screens.
     *
     * @param item The [RTSPItem] currently in focus, or null to clear item-specific titles.
     */
    fun update(item: RTSPItem?) {
        _appBarState.update {
            it.copy(selectedItem = item)
        }
    }
}

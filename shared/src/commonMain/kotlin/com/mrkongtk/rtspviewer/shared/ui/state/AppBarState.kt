package com.mrkongtk.rtspviewer.shared.ui.state

import com.mrkongtk.rtspviewer.shared.AppScreen
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem

/**
 * Represents the configuration state of the Top App Bar across the application.
 *
 * This state tracks the current navigation context to determine which title to show
 * and whether the navigation (back) icon should be visible.
 *
 * @property appScreen The current [AppScreen] route, used to fetch the base title resource.
 * @property selectedItem The [RTSPItem] currently being viewed or edited. Its properties (like name)
 * are often used as dynamic arguments for the title.
 * @property canNavigateBack A flag indicating if the 'Back' arrow should be displayed in the UI.
 */
data class AppBarState(
    val appScreen: AppScreen,
    val selectedItem: RTSPItem? = null,
    val canNavigateBack: Boolean = false,
)

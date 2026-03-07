package com.mrkongtk.rtspviewer.shared.data

import com.mrkongtk.rtspviewer.shared.AppScreen
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import org.jetbrains.compose.resources.StringResource

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

/**
 * Encapsulates the data required to render a localized and potentially dynamic title.
 *
 * This class supports a "Template-Replacement" pattern. It holds a reference to a
 * string resource (the template) and a list of arguments to be injected into that
 * template (e.g., replacing %1 with a camera name).
 *
 * @property id The string resource ID  acting as the title template.
 * @property args A list of strings to be formatted into the resource template via
 * the `formatText` extension.
 */
data class AppBarTitle(
    val id: StringResource? = null,
    val args: List<String> = emptyList()
)

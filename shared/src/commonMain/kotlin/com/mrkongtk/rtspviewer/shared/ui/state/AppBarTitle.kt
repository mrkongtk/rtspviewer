package com.mrkongtk.rtspviewer.shared.ui.state

import org.jetbrains.compose.resources.StringResource

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
    val id: StringResource,
    val args: List<String> = emptyList()
)

package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.AppBarTitle
import com.mrkongtk.rtspviewer.util.formatText
import com.mrkongtk.rtspviewer.viewmodel.AppBarViewModel

/**
 * A Material 3 [CenterAlignedTopAppBar] that serves as the global navigation header.
 *
 * This component is "state-aware" but decoupled from specific screens. It observes the
 * [AppBarViewModel] to reactively update its title and navigation icon based on the
 * current destination in the [NavHost].
 *
 * ### Key Features:
 * - **Template-Based Titles:** Supports dynamic string formatting. It takes a base
 *   string resource ID (e.g., "Editing %1") and replaces placeholders with arguments
 *   provided by the state (e.g., the specific RTSP stream name).
 * - **Conditional Navigation:** Automatically displays a back arrow ([Icons.AutoMirrored.Filled.ArrowBack])
 *   only when the navigation stack permits a "navigate up" action.
 * - **RTL Support:** Uses [Icons.AutoMirrored] to ensure the back arrow points
 *   correctly in right-to-left locales.
 * - **Accessibility:** Includes semantic test tags and localized content descriptions.
 *
 * @param modifier [Modifier] to be applied to the top app bar container.
 * @param viewModel The Hilt-injected [AppBarViewModel] used to collect [AppBarTitle]
 * and navigation state with lifecycle awareness.
 * @param navigateUp Lambda callback triggered when the back button is clicked.
 */
@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    viewModel: AppBarViewModel = hiltViewModel(),
    navigateUp: () -> Unit,
) {

    val title by viewModel.title.collectAsStateWithLifecycle(AppBarTitle())

    val canNavigateBack by viewModel.canNavigateBack.collectAsStateWithLifecycle(false)

    CenterAlignedTopAppBar(
        title = {

            val formattedTitle = if (title.id > 0) {
                stringResource(title.id).formatText(title.args)
            } else {
                ""
            }

            Text(text = formattedTitle, modifier = Modifier.testTag("AppBarTitle"))
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        },
    )
}

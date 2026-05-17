package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mrkongtk.rtspviewer.shared.ui.state.AppBarTitle
import com.mrkongtk.rtspviewer.shared.util.formatText
import com.mrkongtk.rtspviewer.shared.viewmodel.AppBarViewModel
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.app_name
import rtspviewer.shared.generated.resources.back_button

/**
 * A Material 3 [CenterAlignedTopAppBar] that serves as the global navigation header for the application.
 *
 * This component is a shared Kotlin Multiplatform (KMP) composable. It is "state-aware" but
 * decoupled from specific screen logic, observing the [AppBarViewModel] to reactively update
 * its title and navigation icon based on the application's navigation state.
 *
 * ### Key Features:
 * - **Multiplatform Resource Integration:** Utilizes Compose Multiplatform's `Res` system
 *   to provide localized titles and accessibility descriptions across platforms.
 * - **Dynamic String Formatting:** Supports template-based titles by taking a base
 *   string resource ID and applying arguments (e.g., a specific camera name) via [formatText].
 * - **Reactive Navigation State:** Automatically displays a back arrow ([Icons.AutoMirrored.Filled.ArrowBack])
 *   when the [AppBarViewModel] signals that a "navigate up" action is available.
 * - **RTL Support:** Leverages [Icons.AutoMirrored] to ensure the back icon orientation
 *   is correct in right-to-left locales.
 * - **Lifecycle Awareness:** Uses [collectAsStateWithLifecycle] to efficiently observe
 *   ViewModel flows while respecting the UI lifecycle.
 *
 * @param modifier [Modifier] to be applied to the top app bar container.
 * @param viewModel The Koin-provided [AppBarViewModel] that drives the header's state.
 * @param navigateUp Lambda callback triggered when the back button is clicked,
 *   typically used to pop the navigation backstack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    viewModel: AppBarViewModel,
    navigateUp: () -> Unit,
) {

    val title by viewModel.title.collectAsStateWithLifecycle(AppBarTitle(Res.string.app_name))

    val canNavigateBack by viewModel.canNavigateBack.collectAsStateWithLifecycle(false)

    CenterAlignedTopAppBar(
        title = {

            val formattedTitle = stringResource(title.id).formatText(title.args)
            Text(text = formattedTitle, modifier = Modifier.testTag("AppBarTitle"))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_button)
                    )
                }
            }
        },
    )
}

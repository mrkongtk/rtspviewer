package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import com.mrkongtk.rtspviewer.R
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.compose.RTSPTextField
import com.mrkongtk.rtspviewer.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme

/**
 * Holds the temporary state for the stream edition/creation form.
 * This class handles validation logic and data conversion separate from the UI.
 *
 * @property name The display name of the stream.
 * @property uri The RTSP address.
 * @property tags A list of tags for categorization.
 * @property forceTcp Whether to force RTSP over TCP.
 */
private data class FieldsValue(
    val name: String = "",
    val uri: String = "",
    val tags: List<String> = emptyList(),
    val forceTcp: Boolean = false
) {
    /**
     * Converts the list of tags into a comma-separated string for display in the TextField.
     * Example: ["Camera", "Home"] -> "Camera,Home"
     */
    val tagsString: String
        get() {
            return tags.joinToString(",")
        }

    /**
     * Parses a comma-separated string back into a list of tags.
     * Used when the user types into the tags TextField.
     */
    fun toTags(tagString: String): List<String> {
        return tagString.split(",")
    }

    /**
     * Validates the name field.
     * @return A resource ID for the error message if invalid (empty/blank), null otherwise.
     */
    val nameError: Int?
        get() = if (name.isNotEmpty() && name.isNotBlank()) {
            null
        } else {
            R.string.field_name_error
        }

    /**
     * Validates the URI field.
     * Checks if the URI parses correctly, uses the 'rtsp' scheme, and contains a host.
     *
     * @return A [Result] containing the error resource ID if invalid, or null if valid/empty.
     */
    val uriError: Result<Int>?
        get() = try {
            if (uri.isEmpty()) {
                null
            } else {
                val uri = uri.toUri()
                // Ensure scheme is strictly rtsp://
                val isRtsp = uri.scheme?.equals("rtsp", ignoreCase = true) ?: false
                val hasHost = !uri.host.isNullOrEmpty()

                if (isRtsp && hasHost) {
                    null
                } else {
                    // Return failure resource if format is wrong
                    Result.success(R.string.field_uri_error)
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }

    /**
     * Checks if the entire form is valid.
     * Used to enable/disable the Save button.
     * Requires valid name, valid URI format, and URI must not be empty.
     */
    val isValid: Boolean
        get() = nameError == null && uriError == null && uri.isNotEmpty()

    /**
     * Secondary constructor to initialize the form fields from an existing database item.
     */
    constructor(rtspItem: RTSPItem) : this(
        name = rtspItem.name,
        uri = rtspItem.uri,
        tags = rtspItem.tags,
        forceTcp = rtspItem.forceTcp
    )

    /**
     * Merges the current form values into an existing RTSPItem.
     * Used when saving to update the original object.
     */
    fun mergeTo(rtspItem: RTSPItem): RTSPItem {
        return rtspItem.copy(name = name, uri = uri, tags = tags, forceTcp = forceTcp)
    }
}

/**
 * A Composable screen for editing or adding an RTSP stream item.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param item The [RTSPItem] to be edited. Pass a default object for creation mode.
 * @param onSave Callback triggered when the valid form is saved. Passes the updated [RTSPItem].
 */
@Composable
fun EditStreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    onSave: (RTSPItem) -> Unit,
) {
    // Focus manager used to handle keyboard actions (Next/Done)
    val focusManager = LocalFocusManager.current

    // Store the initial state to allow the "Restore/Reset" functionality
    val defaultValue = FieldsValue(rtspItem = item)

    // Mutable state holder for the form input fields
    var fieldsValue by remember {
        mutableStateOf(defaultValue)
    }

    // Main container: Scrollable column is essential for ensuring all fields
    // are accessible on small screens or in landscape mode/split-screen.
    Column(
        modifier = modifier
            .padding(PaddingM)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Form Input Section ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PaddingS, Alignment.Top),
        ) {

            // 1. Name Input
            RTSPTextField(
                value = fieldsValue.name,
                onValueChange = {
                    fieldsValue = fieldsValue.copy(name = it)
                },
                label = stringResource(R.string.field_name),
                placeholder = stringResource(R.string.field_placeholder_name),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("NameTextField"),
            )

            // 2. URI Input
            RTSPTextField(
                value = fieldsValue.uri,
                onValueChange = {
                    fieldsValue = fieldsValue.copy(uri = it)
                },
                label = stringResource(R.string.field_uri),
                placeholder = stringResource(R.string.field_placeholder_uri),
                // Extract error message from Result<Int> if it exists
                errorMessage = fieldsValue.uriError?.let { uriError ->
                    uriError.fold({ resId ->
                        stringResource(resId)
                    }, { throwable ->
                        throwable.localizedMessage
                    })
                },
                // Optimized keyboard for URL entry
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("UriTextField"),
            )

            // 3. Tags Input (Comma separated)
            RTSPTextField(
                value = fieldsValue.tagsString,
                onValueChange = {
                    // Convert string input back to List<String> immediately
                    fieldsValue = fieldsValue.copy(tags = fieldsValue.toTags(it))
                },
                label = stringResource(R.string.field_tags),
                placeholder = stringResource(R.string.field_placeholder_tags),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("TagsTextField"),
            )

            // 4. Force TCP Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.checkbox_force_tcp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("ForceTCPLabel")
                )
                Checkbox(checked = fieldsValue.forceTcp, onCheckedChange = {
                    fieldsValue = fieldsValue.copy(forceTcp = it)
                }, modifier = Modifier.testTag("ForceTCPCheckbox"))
            }
        }

        // --- Action Buttons Section (Clear / Save) ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

            // Clear Button: Resets the form to the original state (defaultValue)
            IconButton(
                onClick = {
                    fieldsValue = defaultValue
                },
                // Only enable if changes have been made
                enabled = defaultValue != fieldsValue,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("ClearButton")
            ) {
                Icon(imageVector = Icons.Default.Restore, stringResource(R.string.clear_button))
            }

            // Save Button: Persists data
            IconButton(
                onClick = {
                    onSave(fieldsValue.mergeTo(item))
                },
                // Only enable if validation passes
                enabled = fieldsValue.isValid,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("SaveButton")
            ) {
                Icon(imageVector = Icons.Default.Save, stringResource(R.string.save_button))
            }
        }
    }
}

// --- Previews ---

@Preview(
    name = "Day",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun StreamItemScreenPreview() {
    // Create a dummy item for preview purposes
    val mockItem = RTSPItem(
        id = 1,
        name = "Front Door",
        uri = "rtsp://192.168.1.100/stream",
        tags = listOf("Home"),
        forceTcp = false,
        order = -1
    )

    RTSPViewerTheme {
        // Scaffold acts as the top-level container to mimic the actual application structure.
        // It applies window insets so the preview respects the status bar and navigation bar areas.
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            // Render the screen with the mock data and apply the Scaffold's content padding.
            EditStreamItemScreen(
                item = mockItem,
                onSave = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

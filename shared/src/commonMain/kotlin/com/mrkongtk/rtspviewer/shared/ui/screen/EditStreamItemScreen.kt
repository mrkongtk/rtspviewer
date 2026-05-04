package com.mrkongtk.rtspviewer.shared.ui.screen

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.compose.RTSPTextField
import com.mrkongtk.rtspviewer.shared.ui.screen.action.EditStreamItemScreenActions
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingS
import com.mrkongtk.rtspviewer.shared.ui.theme.RTSPViewerTheme
import io.ktor.http.Url
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import rtspviewer.shared.generated.resources.Res
import rtspviewer.shared.generated.resources.checkbox_force_tcp
import rtspviewer.shared.generated.resources.clear_button
import rtspviewer.shared.generated.resources.field_name
import rtspviewer.shared.generated.resources.field_name_error
import rtspviewer.shared.generated.resources.field_placeholder_name
import rtspviewer.shared.generated.resources.field_placeholder_tags
import rtspviewer.shared.generated.resources.field_placeholder_uri
import rtspviewer.shared.generated.resources.field_tags
import rtspviewer.shared.generated.resources.field_uri
import rtspviewer.shared.generated.resources.field_uri_error
import rtspviewer.shared.generated.resources.save_button

/**
 * Temporary state for the stream edition/creation form.
 *
 * @property name Display name of the stream.
 * @property uri RTSP address.
 * @property tags Category tags.
 * @property forceTcp Flag to force RTSP over TCP.
 */
private data class FieldsValue(
    val name: String = "",
    val uri: String = "",
    val tags: List<String> = emptyList(),
    val forceTcp: Boolean = false
) {
    /**
     * Comma-separated representation of the tags for the UI.
     */
    val tagsString: String
        get() = tags.joinToString(",")

    /**
     * Parses a comma-separated string into a list of tags.
     */
    fun toTags(tagString: String): List<String> = tagString.split(",")

    /**
     * Returns an error resource if the name is blank.
     */
    val nameError: StringResource?
        get() = if (name.isNotBlank()) null else Res.string.field_name_error

    /**
     * Validates the URI format and scheme.
     * @return [Result] containing an error resource or null if valid.
     */
    val uriError: Result<StringResource>?
        get() = try {
            if (uri.isEmpty()) {
                null
            } else {
                val parsedUri = Url(uri)
                val isRtsp = parsedUri.protocolOrNull?.name.equals("rtsp", ignoreCase = true)
                val hasHost = parsedUri.host.isNotEmpty()

                if (isRtsp && hasHost) null else Result.success(Res.string.field_uri_error)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }

    /**
     * Returns true if all required fields are valid.
     */
    val isValid: Boolean
        get() = nameError == null && uriError == null && uri.isNotEmpty()

    constructor(rtspItem: RTSPItem) : this(
        name = rtspItem.name,
        uri = rtspItem.uri,
        tags = rtspItem.tags,
        forceTcp = rtspItem.forceTcp
    )

    /**
     * Updates an [RTSPItem] with the current form values.
     */
    fun mergeTo(rtspItem: RTSPItem): RTSPItem {
        return rtspItem.copy(name = name, uri = uri, tags = tags, forceTcp = forceTcp)
    }
}

/**
 * Unified input form for creating or modifying RTSP stream configurations.
 *
 * Features:
 * - Real-time validation for name and URI.
 * - Auto-parsing of comma-separated tags.
 * - Form reset (Restore) capability.
 * - Keyboard navigation (Next/Done actions).
 * - Scrollable layout for various screen sizes and orientations.
 *
 * @param modifier Root container modifier.
 * @param item Initial [RTSPItem] data.
 * @param screenActions Actions handler for persistence.
 */
@Composable
fun EditStreamItemScreen(
    modifier: Modifier = Modifier,
    item: RTSPItem,
    screenActions: EditStreamItemScreenActions,
) {
    val focusManager = LocalFocusManager.current
    val defaultValue = FieldsValue(rtspItem = item)
    var fieldsValue by remember { mutableStateOf(defaultValue) }

    Column(
        modifier = modifier
            .padding(PaddingM)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(PaddingS, Alignment.Top),
        ) {
            RTSPTextField(
                value = fieldsValue.name,
                onValueChange = { fieldsValue = fieldsValue.copy(name = it) },
                label = stringResource(Res.string.field_name),
                placeholder = stringResource(Res.string.field_placeholder_name),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("NameTextField"),
            )

            RTSPTextField(
                value = fieldsValue.uri,
                onValueChange = { fieldsValue = fieldsValue.copy(uri = it) },
                label = stringResource(Res.string.field_uri),
                placeholder = stringResource(Res.string.field_placeholder_uri),
                errorMessage = fieldsValue.uriError?.let { uriError ->
                    uriError.fold(
                        onSuccess = { resId -> stringResource(resId) },
                        onFailure = { it.message }
                    )
                },
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

            RTSPTextField(
                value = fieldsValue.tagsString,
                onValueChange = { fieldsValue = fieldsValue.copy(tags = fieldsValue.toTags(it)) },
                label = stringResource(Res.string.field_tags),
                placeholder = stringResource(Res.string.field_placeholder_tags),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("TagsTextField"),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.checkbox_force_tcp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("ForceTCPLabel")
                )
                Checkbox(
                    checked = fieldsValue.forceTcp,
                    onCheckedChange = { fieldsValue = fieldsValue.copy(forceTcp = it) },
                    modifier = Modifier.testTag("ForceTCPCheckbox")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { fieldsValue = defaultValue },
                enabled = defaultValue != fieldsValue,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("ClearButton")
            ) {
                Icon(imageVector = Icons.Default.Restore, stringResource(Res.string.clear_button))
            }

            IconButton(
                onClick = { screenActions.onSaveItem(fieldsValue.mergeTo(item)) },
                enabled = fieldsValue.isValid,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("SaveButton")
            ) {
                Icon(imageVector = Icons.Default.Save, stringResource(Res.string.save_button))
            }
        }
    }
}

@Preview(
    name = "Day",
    showSystemUi = true,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Night",
    showSystemUi = true,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun StreamItemScreenPreview() {
    val mockItem = RTSPItem(
        id = 1,
        name = "Front Door",
        uri = "rtsp://192.168.1.100/stream",
        tags = listOf("Home"),
        forceTcp = false,
        order = -1
    )

    RTSPViewerTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) { innerPadding ->
            EditStreamItemScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                item = mockItem,
                screenActions = object : EditStreamItemScreenActions {
                    override fun onSaveItem(newItem: RTSPItem) {}
                }
            )
        }
    }
}

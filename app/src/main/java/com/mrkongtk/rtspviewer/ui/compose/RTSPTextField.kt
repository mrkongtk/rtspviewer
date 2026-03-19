package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.mrkongtk.rtspviewer.shared.ui.theme.PaddingM
import com.mrkongtk.rtspviewer.shared.ui.theme.RoundedCornerSize

/**
 * A customized OutlinedTextField component designed for the RTSP Viewer application.
 *
 * This component wraps the standard Material3 [OutlinedTextField] with specific styling,
 * color overrides, and built-in support for displaying validation error messages below the field.
 *
 * @param value The input text to be shown in the text field.
 * @param onValueChange The callback that is triggered when the input service updates the text. An updated text comes as a parameter of the callback.
 * @param label The label to be displayed inside the text field container.
 * @param modifier The [Modifier] to be applied to the container column.
 * @param placeholder The placeholder text to be displayed when the text field is empty.
 * @param errorMessage An optional error message string. If not null, the field enters an error state and displays the message below the input.
 * @param keyboardOptions Software keyboard options (e.g., input type, IME action). Defaults to [KeyboardOptions.Default].
 * @param keyboardActions Software keyboard actions (e.g., what happens when 'Done' is pressed). Defaults to [KeyboardActions.Default].
 * @param singleLine When set to true, this text field becomes a single horizontally scrolling text field. Defaults to true.
 */
@Composable
fun RTSPTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    // Column is used to stack the Text Field and the Error Message vertically
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            placeholder = {
                Text(
                    text = placeholder,
                    // Reducing alpha for the placeholder to differentiate it from actual input
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                )
            },
            // Triggers the red border styling provided by Material3 if an error exists
            isError = errorMessage != null,
            singleLine = singleLine,
            shape = RoundedCornerShape(RoundedCornerSize),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("RTSPOutlinedTextField"),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,

            // Custom color overrides to match the app's specific theme requirements
            colors = OutlinedTextFieldDefaults.colors(
                // Container background colors
                focusedContainerColor = MaterialTheme.colorScheme.secondary,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondary,
                errorContainerColor = MaterialTheme.colorScheme.secondary,

                // Text colors
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,

                // Border colors (Transparent when unfocused gives a "filled" look until clicked)
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = MaterialTheme.colorScheme.error,

                // Label colors
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSecondary,
                errorLabelColor = MaterialTheme.colorScheme.error,

                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        // Conditionally display the error message below the text field if one is provided
        if (!errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(PaddingM)
            )
        }
    }
}

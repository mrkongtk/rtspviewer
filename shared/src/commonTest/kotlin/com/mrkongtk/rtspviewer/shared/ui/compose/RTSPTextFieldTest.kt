package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RTSPTextFieldTest {

    @Test
    fun rtspTextField_displaysLabelAndValue() = runComposeUiTest {
        val testLabel = "RTSP URL"
        val testValue = "rtsp://192.168.1.1"

        setContent {
            RTSPTextField(
                value = testValue,
                onValueChange = {},
                label = testLabel
            )
        }

        onNodeWithText(testLabel).assertIsDisplayed()
        onNodeWithText(testValue).assertIsDisplayed()
    }

    @Test
    fun rtspTextField_displaysPlaceholder_whenValueIsEmpty() = runComposeUiTest {
        val placeholderText = "Enter URL here"
        val labelText = "Label"

        setContent {
            RTSPTextField(
                value = "",
                onValueChange = {},
                label = labelText,
                placeholder = placeholderText
            )
        }

        onNodeWithText(labelText).performClick()
        onNodeWithText(placeholderText).assertIsDisplayed()
    }

    @Test
    fun rtspTextField_invokesOnValueChange_whenTextTyped() = runComposeUiTest {
        var capturedValue = ""
        val onValueChange: (String) -> Unit = { capturedValue = it }

        setContent {
            RTSPTextField(
                value = "",
                onValueChange = onValueChange,
                label = "Input"
            )
        }

        onNode(hasSetTextAction()).performTextInput("A")
        
        assertEquals("A", capturedValue)
    }

    @Test
    fun rtspTextField_displaysErrorMessage_whenErrorNotNull() = runComposeUiTest {
        val errorMsg = "Invalid IP Address"

        setContent {
            RTSPTextField(
                value = "Bad Input",
                onValueChange = {},
                label = "Label",
                errorMessage = errorMsg
            )
        }

        onNodeWithText(errorMsg).assertIsDisplayed()
    }

    @Test
    fun rtspTextField_doesNotDisplayError_whenErrorIsNull() = runComposeUiTest {
        val errorMsg = "Should Not Be Visible"

        setContent {
            RTSPTextField(
                value = "Good Input",
                onValueChange = {},
                label = "Label",
                errorMessage = null
            )
        }

        onNodeWithText(errorMsg).assertDoesNotExist()
    }
}

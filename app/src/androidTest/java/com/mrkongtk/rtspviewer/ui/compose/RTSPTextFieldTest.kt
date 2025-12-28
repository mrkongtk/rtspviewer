package com.mrkongtk.rtspviewer.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Instrumented test class for [RTSPTextField].
 *
 * This class verifies the visual state (rendering of labels, values, errors)
 * and the interaction logic (input callbacks) of the custom text field component.
 */
@RunWith(AndroidJUnit4::class)
class RTSPTextFieldTest {

    /**
     * The ComposeTestRule allows us to set the Compose content, find UI nodes,
     * and interact with them (click, type, assert) in a test environment.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Verifies that the RTSPTextField correctly renders the provided [value]
     * and the associated [label].
     */
    @Test
    fun rtspTextField_displaysLabelAndValue() {
        // Arrange
        val testLabel = "RTSP URL"
        val testValue = "rtsp://192.168.1.1"

        // Act: Render the component with specific data
        composeTestRule.setContent {
            RTSPTextField(
                value = testValue,
                onValueChange = {},
                label = testLabel
            )
        }

        // Assert
        // 1. Verify the label text is visible
        composeTestRule.onNodeWithText(testLabel).assertIsDisplayed()

        // 2. Verify the input value text is visible inside the field
        composeTestRule.onNodeWithText(testValue).assertIsDisplayed()
    }

    /**
     * Verifies that the placeholder text becomes visible when the field is empty
     * and gains focus (simulated by a click).
     *
     * Note: In Material Design, placeholders are often hidden until the field receives focus
     * if a label is also present (as the label floats up).
     */
    @Test
    fun rtspTextField_displaysPlaceholder_whenValueIsEmpty() {
        // Arrange
        val placeholderText = "Enter URL here"
        val labelText = "Label"

        composeTestRule.setContent {
            RTSPTextField(
                value = "",
                onValueChange = {},
                label = labelText,
                placeholder = placeholderText
            )
        }

        // Act: Click the text field (via the label text) to trigger focus
        composeTestRule.onNodeWithText(labelText).performClick()

        // Assert: Ensure the placeholder is now displayed
        composeTestRule.onNodeWithText(placeholderText).assertIsDisplayed()
    }

    /**
     * Verifies that typing text into the field triggers the [onValueChange] callback.
     * Uses a Mock object to verify the function call.
     */
    @Test
    fun rtspTextField_invokesOnValueChange_whenTextTyped() {
        // Arrange: Create a mock function to capture the callback
        val onValueChangeMock: (String) -> Unit = mock()

        composeTestRule.setContent {
            RTSPTextField(
                value = "",
                onValueChange = onValueChangeMock,
                label = "Input"
            )
        }

        // Act: Find the node capable of text input and type "A"
        composeTestRule.onNode(hasSetTextAction()).performTextInput("A")

        // Assert: Verify the mock was called exactly once with the string "A"
        verify(onValueChangeMock).invoke("A")
    }

    /**
     * Verifies that passing a non-null [errorMessage] causes the error text
     * to be displayed in the UI.
     */
    @Test
    fun rtspTextField_displaysErrorMessage_whenErrorNotNull() {
        // Arrange
        val errorMsg = "Invalid IP Address"

        // Act
        composeTestRule.setContent {
            RTSPTextField(
                value = "Bad Input",
                onValueChange = {},
                label = "Label",
                errorMessage = errorMsg
            )
        }

        // Assert: The specific error message should be visible to the user
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    }

    /**
     * Verifies that when [errorMessage] is null, no error text logic interferes
     * with the UI and no error message is displayed.
     */
    @Test
    fun rtspTextField_doesNotDisplayError_whenErrorIsNull() {
        // Arrange
        val errorMsg = "Should Not Be Visible"

        // Act
        composeTestRule.setContent {
            RTSPTextField(
                value = "Good Input",
                onValueChange = {},
                label = "Label",
                errorMessage = null
            )
        }

        // Assert: Confirm the text node does not exist in the hierarchy
        composeTestRule.onNodeWithText(errorMsg).assertDoesNotExist()
    }
}

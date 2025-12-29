package com.mrkongtk.rtspviewer.ui.screen

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never

/**
 * UI Instrumentation tests for the [AddStreamItemScreen].
 *
 * This class verifies the behavior of the "Add Stream" form, including:
 * - Initial state (empty fields, disabled buttons).
 * - Input validation (RTSP scheme checks, required fields).
 * - State updates (enabling/disabling buttons based on input).
 * - Interaction with the [SaveCallback] when valid data is submitted.
 */
@RunWith(AndroidJUnit4::class)
class AddStreamItemScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Helper interface to facilitate Mockito mocking of the Kotlin lambda function `onSave`.
     * Mocking Kotlin function types directly can sometimes be verbose or problematic.
     */
    interface SaveCallback {
        fun onSave(item: RTSPItem)
    }

    private lateinit var mockSaveCallback: SaveCallback

    /**
     * Sets up the test environment before every test execution.
     * Initializes the mock callback and mounts the [AddStreamItemScreen] composable.
     */
    @Before
    fun setup() {
        mockSaveCallback = mock(SaveCallback::class.java)

        composeTestRule.setContent {
            AddStreamItemScreen(
                onSave = mockSaveCallback::onSave
            )
        }
    }

    /**
     * Scenario: The user opens the screen for the first time.
     * Expected: All text fields are empty and the Save button is disabled.
     */
    @Test
    fun initialState_saveButtonIsDisabled() {
        // Verify Name and URI fields are empty
        onRTSPField("NameTextField")
            .assertTextContains("")

        onRTSPField("UriTextField")
            .assertTextContains("")

        // Verify Save button is disabled initially (preventing submission of empty data)
        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsNotEnabled()
    }

    /**
     * Scenario: Happy Path. The user enters valid data into all fields.
     * Expected: The Save button becomes enabled, and clicking it triggers the callback
     * with an RTSPItem object containing the exact data entered.
     */
    @Test
    fun enterValidData_saveButtonEnabled_andCallbackTriggered() {
        val testName = "Living Room"
        val testUri = "rtsp://192.168.1.55"
        val testTags = "Home,Security"

        // 1. Enter Valid Name
        onRTSPField("NameTextField")
            .performTextInput(testName)

        // 2. Enter Valid URI (Must start with rtsp://)
        onRTSPField("UriTextField")
            .performTextInput(testUri)

        // 3. Enter Tags (scrolling ensures visibility on smaller screens)
        onRTSPField("TagsTextField")
            .performScrollTo()
            .performTextInput(testTags)

        // 4. Check Force TCP
        composeTestRule.onNodeWithTag("ForceTCPCheckbox")
            .performScrollTo()
            .performClick()

        // 5. Verify Save Button is now enabled and Click it
        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsEnabled()
            .performClick()

        // 6. Verify Mockito callback captured the correct data structure
        verify(mockSaveCallback).onSave(check { item ->
            assert(item.name == testName)
            assert(item.uri == testUri)
            assert(item.tags == listOf("Home", "Security")) // Verifies comma-splitting logic
            assert(item.forceTcp)
            assert(item.id == 0L) // Add screen defaults ID to 0 for new items
        })
    }

    /**
     * Scenario: The user enters an invalid URI (e.g., HTTP).
     * Expected: The Save button remains disabled, and clicking it does not trigger the save callback.
     */
    @Test
    fun enterInvalidUri_saveButtonDisabled_andErrorShown() {
        // 1. Enter Valid Name
        onRTSPField("NameTextField")
            .performTextInput("Camera 1")

        // 2. Enter Invalid URI (Not RTSP scheme)
        onRTSPField("UriTextField")
            .performTextInput("http://192.168.1.1")

        // 3. Verify Save Button is disabled due to validation error
        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsNotEnabled()

        // 4. Attempt to click save anyway
        composeTestRule.onNodeWithTag("SaveButton").performClick()

        // 5. Verify that onSave was NEVER called
        verify(mockSaveCallback, never()).onSave(any())
    }

    /**
     * Scenario: The user enters data but then clicks the "Clear" button.
     * Expected: All fields return to empty and the Save button becomes disabled again.
     */
    @Test
    fun clearButton_resetsFields() {
        // 1. Enter some temporary data
        onRTSPField("NameTextField").performTextInput("Temp Name")
        onRTSPField("UriTextField").performTextInput("rtsp://temp")

        // 2. Click the Clear button
        composeTestRule.onNodeWithTag("ClearButton")
            .performClick()

        // 3. Verify fields are actually empty
        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")

        // 4. Verify Save is disabled again
        composeTestRule.onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    /**
     * Scenario: User provides Name but leaves URI empty.
     * Expected: Save button is disabled (both fields are required).
     */
    @Test
    fun enterIncompleteData_nameOnly_saveDisabled() {
        onRTSPField("NameTextField").performTextInput("Valid Name")

        // URI is left empty
        composeTestRule.onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    /**
     * Scenario: User provides URI but leaves Name empty.
     * Expected: Save button is disabled (both fields are required).
     */
    @Test
    fun enterIncompleteData_uriOnly_saveDisabled() {
        onRTSPField("UriTextField").performTextInput("rtsp://valid")

        // Name is left empty
        composeTestRule.onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    /**
     * Helper function to find a specific text field within the Compose hierarchy.
     *
     * Because the input fields are custom components (`RTSPOutlinedTextField`), we cannot
     * simply look for the tag "NameTextField". We must look for the actual input field
     * (`RTSPOutlinedTextField`) that acts as a child/descendant of the specific identifier tag.
     *
     * @param tag The TestTag of the container wrapping the text field.
     */
    private fun onRTSPField(tag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

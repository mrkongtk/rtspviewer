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
import com.mrkongtk.rtspviewer.ui.screen.action.EditStreamItemScreenActions
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
 */
@RunWith(AndroidJUnit4::class)
class AddStreamItemScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockActions: EditStreamItemScreenActions

    @Before
    fun setup() {
        mockActions = mock(EditStreamItemScreenActions::class.java)

        composeTestRule.setContent {
            AddStreamItemScreen(
                screenActions = mockActions
            )
        }
    }

    /**
     * Scenario: The user opens the screen for the first time.
     * Expected: Fields are empty and Save button is disabled.
     */
    @Test
    fun initialState_saveButtonIsDisabled() {
        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")

        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsNotEnabled()
    }

    /**
     * Scenario: Happy Path. User enters valid RTSP data.
     * Expected: Save button enabled, callback returns correct RTSPItem.
     */
    @Test
    fun enterValidData_saveButtonEnabled_andCallbackTriggered() {
        val testName = "Front Door"
        val testUri = "rtsp://192.168.1.100/live"
        val testTags = "Outdoor,Home"

        onRTSPField("NameTextField").performTextInput(testName)
        onRTSPField("UriTextField").performTextInput(testUri)

        onRTSPField("TagsTextField")
            .performScrollTo()
            .performTextInput(testTags)

        composeTestRule.onNodeWithTag("ForceTCPCheckbox")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsEnabled()
            .performClick()

        // Verify the item sent to the repository/viewmodel
        verify(mockActions).onSaveItem(check { item ->
            assert(item.name == testName)
            assert(item.uri == testUri)
            assert(item.tags == listOf("Outdoor", "Home"))
            assert(item.forceTcp)
            assert(item.id == 0L) // New item marker
            assert(item.order == -1) // Unassigned order marker
        })
    }

    /**
     * Scenario: User enters a non-RTSP URI (e.g., http).
     * Expected: Validation fails, Save button remains disabled.
     */
    @Test
    fun enterInvalidUri_saveButtonDisabled() {
        onRTSPField("NameTextField").performTextInput("Camera")

        // FieldsValue logic requires scheme to be 'rtsp'
        onRTSPField("UriTextField").performTextInput("http://192.168.1.1")

        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsNotEnabled()

        verify(mockActions, never()).onSaveItem(any())
    }

    /**
     * Scenario: User clears the form using the Restore/Clear button.
     * Expected: Fields return to default blank state.
     */
    @Test
    fun clearButton_resetsFields() {
        onRTSPField("NameTextField").performTextInput("Delete Me")
        onRTSPField("UriTextField").performTextInput("rtsp://valid")

        composeTestRule.onNodeWithTag("ClearButton")
            .performClick()

        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")
        composeTestRule.onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    /**
     * Scenario: Name is provided but URI is missing.
     * Expected: Save button disabled (isValid logic requires both).
     */
    @Test
    fun incompleteData_missingUri_saveDisabled() {
        onRTSPField("NameTextField").performTextInput("Living Room")

        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsNotEnabled()
    }

    /**
     * Helper function to find the inner OutlinedTextField within the RTSPTextField wrapper.
     */
    private fun onRTSPField(tag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

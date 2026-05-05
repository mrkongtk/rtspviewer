package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.screen.action.EditStreamItemScreenActions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * UI Instrumentation tests for the [AddStreamItemScreen].
 */
@OptIn(ExperimentalTestApi::class)
class AddStreamItemScreenTest {


    private val actionsNoOp = object : EditStreamItemScreenActions {
        override fun onSaveItem(newItem: RTSPItem) {}
    }

    /**
     * Scenario: The user opens the screen for the first time.
     * Expected: Fields are empty and Save button is disabled.
     */
    @Test
    fun initialState_saveButtonIsDisabled() = runComposeUiTest {

        setContent {
            AddStreamItemScreen(
                screenActions = actionsNoOp
            )
        }

        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")

        onNodeWithTag("SaveButton")
            .assertIsNotEnabled()
    }

    /**
     * Scenario: Happy Path. User enters valid RTSP data.
     * Expected: Save button enabled, callback returns correct RTSPItem.
     */
    @Test
    fun enterValidData_saveButtonEnabled_andCallbackTriggered() = runComposeUiTest {
        val testName = "Front Door"
        val testUri = "rtsp://192.168.1.100/live"
        val testTags = "Outdoor,Home"

        var capturedItem: RTSPItem? = null

        val actionsMock = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
                capturedItem = newItem
            }
        }

        setContent {
            AddStreamItemScreen(
                screenActions = actionsMock
            )
        }

        onRTSPField("NameTextField").performTextInput(testName)
        onRTSPField("UriTextField").performTextInput(testUri)

        onRTSPField("TagsTextField")
            .performScrollTo()
            .performTextInput(testTags)

        onNodeWithTag("ForceTCPCheckbox")
            .performScrollTo()
            .performClick()

        onNodeWithTag("SaveButton")
            .assertIsEnabled()
            .performClick()

        // Verify the item sent to the repository/viewmodel
        assertNotNull(capturedItem)
        assertEquals(testName,capturedItem.name)
        assertEquals(testUri, capturedItem.uri)
        assertEquals(listOf("Outdoor", "Home"), capturedItem.tags)
        assertTrue(capturedItem.forceTcp)
        assertEquals(0L, capturedItem.id) // New item marker
        assertEquals(-1, capturedItem.order) // Unassigned order marker
    }

    /**
     * Scenario: User enters a non-RTSP URI (e.g., http).
     * Expected: Validation fails, Save button remains disabled.
     */
    @Test
    fun enterInvalidUri_saveButtonDisabled() = runComposeUiTest {

        var capturedItem: RTSPItem? = null

        val actionsMock = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
                capturedItem = newItem
            }
        }

        setContent {
            AddStreamItemScreen(
                screenActions = actionsMock
            )
        }

        onRTSPField("NameTextField").performTextInput("Camera")

        // FieldsValue logic requires scheme to be 'rtsp'
        onRTSPField("UriTextField").performTextInput("http://192.168.1.1")

        onNodeWithTag("SaveButton").assertIsNotEnabled()

        assertNull(capturedItem)
    }

    /**
     * Scenario: User clears the form using the Restore/Clear button.
     * Expected: Fields return to default blank state.
     */
    @Test
    fun clearButton_resetsFields() = runComposeUiTest {

        setContent {
            AddStreamItemScreen(
                screenActions = actionsNoOp
            )
        }

        onRTSPField("NameTextField").performTextInput("Delete Me")
        onRTSPField("UriTextField").performTextInput("rtsp://valid")

        onNodeWithTag("ClearButton")
            .performClick()

        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    /**
     * Scenario: Name is provided but URI is missing.
     * Expected: Save button disabled (isValid logic requires both).
     */
    @Test
    fun incompleteData_missingUri_saveDisabled() = runComposeUiTest {

        setContent {
            AddStreamItemScreen(
                screenActions = actionsNoOp
            )
        }

        onRTSPField("NameTextField").performTextInput("Living Room")

        onNodeWithTag("SaveButton")
            .assertIsNotEnabled()
    }

    /**
     * Helper function to find the inner OutlinedTextField within the RTSPTextField wrapper.
     */
    private fun ComposeUiTest.onRTSPField(tag: String) = onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

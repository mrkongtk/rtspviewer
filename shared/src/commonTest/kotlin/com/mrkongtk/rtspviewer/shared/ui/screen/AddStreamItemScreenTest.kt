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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * UI Instrumentation tests for the [AddStreamItemScreen].
 */
@OptIn(ExperimentalTestApi::class)
class AddStreamItemScreenTest {

    private val actionsNoOp = object : EditStreamItemScreenActions {
        override fun onSaveItem(newItem: RTSPItem) {}
    }

    @Test
    fun initialState_saveButtonIsDisabled() = runComposeUiTest {
        setContent {
            AddStreamItemScreen(screenActions = actionsNoOp)
        }

        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")

        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

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
            AddStreamItemScreen(screenActions = actionsMock)
        }

        onRTSPField("NameTextField").performTextInput(testName)
        onRTSPField("UriTextField").performTextInput(testUri)
        onRTSPField("TagsTextField").performScrollTo().performTextInput(testTags)
        onNodeWithTag("ForceTCPCheckbox").performScrollTo().performClick()

        onNodeWithTag("SaveButton").assertIsEnabled().performClick()

        assertNotNull(capturedItem)
        assertEquals(testName, capturedItem.name)
        assertEquals(testUri, capturedItem.uri)
        assertEquals(listOf("Outdoor", "Home"), capturedItem.tags)
        assertTrue(capturedItem.forceTcp)
        assertEquals(0L, capturedItem.id)
    }

    @Test
    fun whitespaceName_saveButtonDisabled() = runComposeUiTest {
        setContent {
            AddStreamItemScreen(screenActions = actionsNoOp)
        }

        onRTSPField("NameTextField").performTextInput("   ")
        onRTSPField("UriTextField").performTextInput("rtsp://192.168.1.1")

        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun enterInvalidUri_saveButtonDisabled() = runComposeUiTest {
        setContent {
            AddStreamItemScreen(screenActions = actionsNoOp)
        }

        onRTSPField("NameTextField").performTextInput("Camera")
        onRTSPField("UriTextField").performTextInput("http://192.168.1.1")

        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun uriMissingHost_saveButtonDisabled() = runComposeUiTest {
        setContent {
            AddStreamItemScreen(screenActions = actionsNoOp)
        }

        onRTSPField("NameTextField").performTextInput("Camera")
        onRTSPField("UriTextField").performTextInput("rtsp://")

        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun clearButton_resetsFields() = runComposeUiTest {
        setContent {
            AddStreamItemScreen(screenActions = actionsNoOp)
        }

        onRTSPField("NameTextField").performTextInput("Delete Me")
        onRTSPField("UriTextField").performTextInput("rtsp://valid")

        onNodeWithTag("ClearButton").performClick()

        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun clearButton_enabledStateTransitions() = runComposeUiTest {
        setContent {
            AddStreamItemScreen(screenActions = actionsNoOp)
        }

        // Initially disabled because form is empty
        onNodeWithTag("ClearButton").assertIsNotEnabled()

        onRTSPField("NameTextField").performTextInput("New Name")
        onNodeWithTag("ClearButton").assertIsEnabled()

        onNodeWithTag("ClearButton").performClick()
        onNodeWithTag("ClearButton").assertIsNotEnabled()
    }

    @Test
    fun toggleForceTcp_capturedInCallback() = runComposeUiTest {
        var capturedItem: RTSPItem? = null
        val actionsMock = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
                capturedItem = newItem
            }
        }

        setContent {
            AddStreamItemScreen(screenActions = actionsMock)
        }

        onRTSPField("NameTextField").performTextInput("Camera")
        onRTSPField("UriTextField").performTextInput("rtsp://192.168.1.1")

        // Toggle true
        onNodeWithTag("ForceTCPCheckbox").performClick()
        onNodeWithTag("SaveButton").performClick()
        assertNotNull(capturedItem)
        assertTrue(capturedItem.forceTcp)

        // Toggle false
        onNodeWithTag("ForceTCPCheckbox").performClick()
        onNodeWithTag("SaveButton").performClick()
        assertFalse(capturedItem.forceTcp)
    }

    private fun ComposeUiTest.onRTSPField(tag: String) = onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

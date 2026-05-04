package com.mrkongtk.rtspviewer.shared.ui.screen

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.shared.ui.screen.action.EditStreamItemScreenActions
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * UI Test class for [EditStreamItemScreen].
 */
@OptIn(ExperimentalTestApi::class)
class EditStreamItemScreenTest {

    private val testItem = RTSPItem(
        id = 1,
        name = "Test Camera",
        uri = "rtsp://192.168.1.1",
        tags = listOf("Home", "Security"),
        order = 0,
        forceTcp = false
    )

    private val actionsNoOp = object : EditStreamItemScreenActions {
        override fun onSaveItem(newItem: RTSPItem) {}
    }

    @Test
    fun initialRendering_populatesFieldsCorrectly() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsNoOp
            )
        }

        onRTSPField("NameTextField").assertTextContains("Test Camera")
        onRTSPField("UriTextField").assertTextContains("rtsp://192.168.1.1")
        onRTSPField("TagsTextField").assertTextContains("Home,Security")

        onNodeWithTag("ForceTCPCheckbox").assertIsOff()
        onNodeWithTag("SaveButton").assertIsEnabled()
        onNodeWithTag("ClearButton").assertIsNotEnabled()
    }

    @Test
    fun initialRendering_emptyItem_fieldsAreEmptyAndSaveDisabled() = runComposeUiTest {
        val emptyItem = RTSPItem(id = 0, name = "", uri = "", tags = emptyList(), order = 0, forceTcp = false)
        setContent {
            EditStreamItemScreen(item = emptyItem, screenActions = actionsNoOp)
        }

        onRTSPField("NameTextField").assertTextContains("")
        onRTSPField("UriTextField").assertTextContains("")
        onRTSPField("TagsTextField").assertTextContains("")
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun validation_invalidUri_showsErrorAndDisablesSave() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(

                item = testItem.copy(uri = ""),
                screenActions = actionsNoOp
            )
        }

        // Enter invalid scheme
        onRTSPField("UriTextField").performTextInput("http://google.com")
        onNodeWithTag("SaveButton").assertIsNotEnabled()

        // Correct to valid RTSP
        onRTSPField("UriTextField").performTextClearance()
        onRTSPField("UriTextField").performTextInput("rtsp://valid.address")
        onNodeWithTag("SaveButton").assertIsEnabled()
    }

    @Test
    fun validation_uriWithoutHost_disablesSave() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(item = testItem, screenActions = actionsNoOp)
        }
        onRTSPField("UriTextField").performTextClearance()
        onRTSPField("UriTextField").performTextInput("rtsp://")
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun validation_malformedUri_disablesSave() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(item = testItem, screenActions = actionsNoOp)
        }
        onRTSPField("UriTextField").performTextClearance()
        onRTSPField("UriTextField").performTextInput("::::")
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun validation_emptyName_disablesSave() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsNoOp
            )
        }

        onRTSPField("NameTextField").performTextClearance()
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun validation_blankName_disablesSave() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(item = testItem, screenActions = actionsNoOp)
        }
        onRTSPField("NameTextField").performTextClearance()
        onRTSPField("NameTextField").performTextInput("   ")
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun interaction_modifyFieldsAndSave_invokesCallbackWithCorrectData() = runComposeUiTest {
        var capturedItem: RTSPItem? = null

        val actionsMock = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
                capturedItem = newItem
            }
        }

        setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock
            )
        }

        onRTSPField("NameTextField").performTextClearance()
        onRTSPField("NameTextField").performTextInput("New Name")

        onNodeWithTag("ForceTCPCheckbox").performClick().assertIsOn()

        onRTSPField("TagsTextField").performTextClearance()
        onRTSPField("TagsTextField").performTextInput("TagA,TagB")

        onNodeWithTag("SaveButton").performClick()

        assertEquals("New Name", capturedItem?.name)
        assertEquals(true, capturedItem?.forceTcp)
        assertEquals(listOf("TagA", "TagB"), capturedItem?.tags)
        assertEquals(testItem.id, capturedItem?.id)
    }

    @Test
    fun interaction_clearButton_resetsChanges() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsNoOp
            )
        }

        onRTSPField("NameTextField").performTextInput(" - Modified")
        onNodeWithTag("ClearButton").assertIsEnabled()

        onNodeWithTag("ClearButton").performClick()

        onRTSPField("NameTextField").assertTextContains("Test Camera")
        onNodeWithTag("ClearButton").assertIsNotEnabled()
    }

    @Test
    fun interaction_undoChanges_disablesClearButton() = runComposeUiTest {
        setContent {
            EditStreamItemScreen(item = testItem, screenActions = actionsNoOp)
        }
        onRTSPField("NameTextField").performTextInput("X")
        onNodeWithTag("ClearButton").assertIsEnabled()

        onRTSPField("NameTextField").performTextClearance()
        onRTSPField("NameTextField").performTextInput(testItem.name)
        onNodeWithTag("ClearButton").assertIsNotEnabled()
    }

    // --------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------

    private fun ComposeUiTest.onRTSPField(tag: String) = onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

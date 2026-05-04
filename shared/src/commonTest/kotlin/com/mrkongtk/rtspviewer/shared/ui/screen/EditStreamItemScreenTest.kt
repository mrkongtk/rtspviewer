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

    @Test
    fun initialRendering_populatesFieldsCorrectly() = runComposeUiTest {

        val actionsMock: EditStreamItemScreenActions = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
            }
        }

        setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock // Updated parameter name
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
    fun validation_invalidUri_showsErrorAndDisablesSave() = runComposeUiTest {

        val actionsMock: EditStreamItemScreenActions = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
            }
        }

        setContent {
            EditStreamItemScreen(
                item = testItem.copy(uri = ""),
                screenActions = actionsMock
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
    fun validation_emptyName_disablesSave() = runComposeUiTest {

        val actionsMock: EditStreamItemScreenActions = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
            }
        }

        setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock
            )
        }

        onRTSPField("NameTextField").performTextClearance()
        onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun interaction_modifyFieldsAndSave_invokesCallbackWithCorrectData() = runComposeUiTest {
        var capturedItem: RTSPItem? = null

        val actionsMock: EditStreamItemScreenActions = object : EditStreamItemScreenActions {
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

        // Modify Name
        onRTSPField("NameTextField").performTextClearance()
        onRTSPField("NameTextField").performTextInput("New Name")

        // Toggle TCP
        onNodeWithTag("ForceTCPCheckbox").performClick().assertIsOn()

        // Modify Tags
        onRTSPField("TagsTextField").performTextClearance()
        onRTSPField("TagsTextField").performTextInput("TagA,TagB")

        // Click Save
        onNodeWithTag("SaveButton").performClick()

        // Verify interface method call
        assertEquals("New Name", capturedItem?.name)
        assertEquals(true, capturedItem?.forceTcp)
        assertEquals(listOf("TagA", "TagB"), capturedItem?.tags)
        assertEquals(testItem.id, capturedItem?.id)
    }

    @Test
    fun interaction_clearButton_resetsChanges() = runComposeUiTest {

        val actionsMock: EditStreamItemScreenActions = object : EditStreamItemScreenActions {
            override fun onSaveItem(newItem: RTSPItem) {
            }
        }

        setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock
            )
        }

        onRTSPField("NameTextField").performTextInput(" - Modified")
        onNodeWithTag("ClearButton").assertIsEnabled()

        onNodeWithTag("ClearButton").performClick()

        onRTSPField("NameTextField").assertTextContains("Test Camera")
        onNodeWithTag("ClearButton").assertIsNotEnabled()
    }

    // --------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------

    /**
     * Helper to find the internal TextField node.
     * Your production code:
     * Column(modifier = modifier) { // "NameTextField" tag is here
     *    OutlinedTextField(modifier = Modifier.testTag("RTSPOutlinedTextField")) // Actual input
     * }
     */
    private fun ComposeUiTest.onRTSPField(tag: String) = onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

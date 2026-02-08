package com.mrkongtk.rtspviewer.ui.screen

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrkongtk.rtspviewer.shared.data.database.entity.RTSPItem
import com.mrkongtk.rtspviewer.ui.screen.action.EditStreamItemScreenActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * UI Test class for [EditStreamItemScreen].
 */
@RunWith(AndroidJUnit4::class)
class EditStreamItemScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock the interface instead of a raw lambda
    private val actionsMock: EditStreamItemScreenActions = mock()

    private val testItem = RTSPItem(
        id = 1,
        name = "Test Camera",
        uri = "rtsp://192.168.1.1",
        tags = listOf("Home", "Security"),
        order = 0,
        forceTcp = false
    )

    @Test
    fun initialRendering_populatesFieldsCorrectly() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock // Updated parameter name
            )
        }

        onRTSPField("NameTextField").assertTextContains("Test Camera")
        onRTSPField("UriTextField").assertTextContains("rtsp://192.168.1.1")
        onRTSPField("TagsTextField").assertTextContains("Home,Security")

        composeTestRule.onNodeWithTag("ForceTCPCheckbox").assertIsOff()
        composeTestRule.onNodeWithTag("SaveButton").assertIsEnabled()
        composeTestRule.onNodeWithTag("ClearButton").assertIsNotEnabled()
    }

    @Test
    fun validation_invalidUri_showsErrorAndDisablesSave() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem.copy(uri = ""),
                screenActions = actionsMock
            )
        }

        // Enter invalid scheme
        onRTSPField("UriTextField").performTextInput("http://google.com")
        composeTestRule.onNodeWithTag("SaveButton").assertIsNotEnabled()

        // Correct to valid RTSP
        onRTSPField("UriTextField").performTextClearance()
        onRTSPField("UriTextField").performTextInput("rtsp://valid.address")
        composeTestRule.onNodeWithTag("SaveButton").assertIsEnabled()
    }

    @Test
    fun validation_emptyName_disablesSave() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock
            )
        }

        onRTSPField("NameTextField").performTextClearance()
        composeTestRule.onNodeWithTag("SaveButton").assertIsNotEnabled()
    }

    @Test
    fun interaction_modifyFieldsAndSave_invokesCallbackWithCorrectData() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock
            )
        }

        // Modify Name
        onRTSPField("NameTextField").performTextClearance()
        onRTSPField("NameTextField").performTextInput("New Name")

        // Toggle TCP
        composeTestRule.onNodeWithTag("ForceTCPCheckbox").performClick().assertIsOn()

        // Modify Tags
        onRTSPField("TagsTextField").performTextClearance()
        onRTSPField("TagsTextField").performTextInput("TagA,TagB")

        // Click Save
        composeTestRule.onNodeWithTag("SaveButton").performClick()

        // Verify interface method call
        val captor = argumentCaptor<RTSPItem>()
        verify(actionsMock).onSaveItem(captor.capture())

        val capturedItem = captor.firstValue
        assert(capturedItem.name == "New Name")
        assert(capturedItem.forceTcp)
        assert(capturedItem.tags == listOf("TagA", "TagB"))
        assert(capturedItem.id == testItem.id)
    }

    @Test
    fun interaction_clearButton_resetsChanges() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                screenActions = actionsMock
            )
        }

        onRTSPField("NameTextField").performTextInput(" - Modified")
        composeTestRule.onNodeWithTag("ClearButton").assertIsEnabled()

        composeTestRule.onNodeWithTag("ClearButton").performClick()

        onRTSPField("NameTextField").assertTextContains("Test Camera")
        composeTestRule.onNodeWithTag("ClearButton").assertIsNotEnabled()
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
    private fun onRTSPField(tag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

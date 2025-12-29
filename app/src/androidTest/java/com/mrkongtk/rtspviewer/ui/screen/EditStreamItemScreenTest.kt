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
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * UI Test class for [EditStreamItemScreen].
 *
 * This class verifies the visual state, input validation, and user interactions
 * of the screen used to edit RTSP stream details.
 *
 * Key behaviors tested:
 * - Initial data population.
 * - Validation logic (enabling/disabling the Save button).
 * - "Clear/Reset" functionality.
 * - Interaction with the Save callback using Mockito.
 */
@RunWith(AndroidJUnit4::class)
class EditStreamItemScreenTest {

    /**
     * strict rule that grants control over the Compose content,
     * allowing us to set content, find nodes, and perform actions.
     */
    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock the callback function passed to the composable to verify it is called correctly
    private val onSaveMock: (RTSPItem) -> Unit = mock()

    // A default RTSPItem object to be used as the initial state for tests
    private val testItem = RTSPItem(
        id = 1,
        name = "Test Camera",
        uri = "rtsp://192.168.1.1",
        tags = listOf("Home", "Security"),
        order = 0,
        forceTcp = false
    )

    /**
     * Verifies that when the screen opens, all fields (Text, Checkboxes)
     * are populated with the data from the [testItem] object.
     */
    @Test
    fun initialRendering_populatesFieldsCorrectly() {
        // Arrange & Act: Render the screen
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                onSave = onSaveMock
            )
        }

        // Assert: Check Text Fields
        onRTSPField("NameTextField")
            .assertTextContains("Test Camera")

        onRTSPField("UriTextField")
            .assertTextContains("rtsp://192.168.1.1")

        // The Tags field logic joins list items with commas
        onRTSPField("TagsTextField")
            .assertTextContains("Home,Security")

        // Assert: Check Checkbox state
        composeTestRule.onNodeWithTag("ForceTCPCheckbox")
            .assertIsOff()

        // Assert: Save button should be enabled initially (assuming valid initial data)
        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsEnabled()

        // Assert: Clear button is disabled because no modifications have been made yet
        composeTestRule.onNodeWithTag("ClearButton")
            .assertIsNotEnabled()
    }

    /**
     * Tests validation logic: The Save button should be disabled if the URI is invalid,
     * and re-enabled once a valid RTSP URI is entered.
     */
    @Test
    fun validation_invalidUri_showsErrorAndDisablesSave() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem.copy(uri = ""), // Start with empty URI
                onSave = onSaveMock
            )
        }

        // 1. Act: Enter an invalid URI (wrong scheme)
        onRTSPField("UriTextField")
            .performTextClearance()

        onRTSPField("UriTextField")
            .performTextInput("http://google.com")

        // 2. Assert: Save button is disabled due to validation error
        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsNotEnabled()

        // 3. Act: Enter a valid RTSP URI
        onRTSPField("UriTextField")
            .performTextClearance()

        onRTSPField("UriTextField")
            .performTextInput("rtsp://valid.address")

        // 4. Assert: Save button is now enabled
        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsEnabled()
    }

    /**
     * Tests validation logic: The Name field is mandatory.
     * Clearing it should disable the Save button.
     */
    @Test
    fun validation_emptyName_disablesSave() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                onSave = onSaveMock
            )
        }

        // Act: Clear the Name field
        onRTSPField("NameTextField")
            .performTextClearance()

        // Assert: Save button becomes disabled
        composeTestRule.onNodeWithTag("SaveButton")
            .assertIsNotEnabled()
    }

    /**
     * Simulates a full user workflow: modifying multiple fields and clicking Save.
     * Uses [argumentCaptor] to ensure the callback receives the modified object, not the original.
     */
    @Test
    fun interaction_modifyFieldsAndSave_invokesCallbackWithCorrectData() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                onSave = onSaveMock
            )
        }

        // 1. Act: Modify Name
        onRTSPField("NameTextField")
            .performTextClearance()
        onRTSPField("NameTextField")
            .performTextInput("New Name")

        // 2. Act: Toggle TCP Checkbox
        composeTestRule.onNodeWithTag("ForceTCPCheckbox")
            .performClick()
            .assertIsOn() // Verify visual toggle state

        // 3. Act: Modify Tags
        onRTSPField("TagsTextField")
            .performTextClearance()
        onRTSPField("TagsTextField")
            .performTextInput("TagA,TagB")

        // 4. Act: Click Save
        composeTestRule.onNodeWithTag("SaveButton")
            .performClick()

        // 5. Assert: Capture the argument passed to onSave
        val captor = argumentCaptor<RTSPItem>()
        verify(onSaveMock).invoke(captor.capture())

        val capturedItem = captor.firstValue

        // Verify the captured item matches our inputs
        assert(capturedItem.name == "New Name")
        assert(capturedItem.forceTcp)
        assert(capturedItem.tags == listOf("TagA", "TagB"))
        // Verify the ID was preserved
        assert(capturedItem.id == testItem.id)
    }

    /**
     * Tests the "Clear" / "Reset" button.
     * It should only be enabled when changes exist, and clicking it
     * should revert fields to their original values.
     */
    @Test
    fun interaction_clearButton_resetsChanges() {
        composeTestRule.setContent {
            EditStreamItemScreen(
                item = testItem,
                onSave = onSaveMock
            )
        }

        // 1. Act: Make a change to enable the Clear button
        onRTSPField("NameTextField")
            .performTextInput(" - Modified")

        // Assert: Clear button is now active
        composeTestRule.onNodeWithTag("ClearButton")
            .assertIsEnabled()

        // 2. Act: Click the Clear button
        composeTestRule.onNodeWithTag("ClearButton")
            .performClick()

        // 3. Assert: Content is reverted to original value ("Test Camera")
        onRTSPField("NameTextField")
            .assertTextContains("Test Camera")

        // Assert: Clear button is disabled again
        composeTestRule.onNodeWithTag("ClearButton")
            .assertIsNotEnabled()
    }

    // --------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------

    /**
     * Helper function to find the actual input field within your custom RTSP text field layout.
     *
     * It looks for a node with the specific test tag "RTSPOutlinedTextField" that also
     * lives inside a parent/ancestor with the provided [tag].
     *
     * @param tag The unique testTag of the container (e.g., "NameTextField", "UriTextField")
     */
    private fun onRTSPField(tag: String) = composeTestRule.onNode(
        hasTestTag("RTSPOutlinedTextField") and hasAnyAncestor(hasTestTag(tag))
    )
}

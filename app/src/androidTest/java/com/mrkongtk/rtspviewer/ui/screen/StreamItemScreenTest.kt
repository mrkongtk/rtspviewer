package com.mrkongtk.rtspviewer.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrkongtk.rtspviewer.data.database.entity.RTSPItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class StreamItemScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Mock callbacks
    private val onEditMock: () -> Unit = mock()
    private val onDeleteMock: () -> Unit = mock()

    // Sample Data
    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Backyard Cam",
        // URI with credentials to test masking logic
        uri = "rtsp://admin:12345@192.168.1.50:554/stream",
        tags = listOf("Outdoor", "Security"),
        order = 0,
        forceTcp = true
    )

    @Test
    fun displaysCorrectMetadataAndMasksUri() {
        setContentWithInspectionMode(sampleItem)

        // 1. Check Name
        composeTestRule.onNodeWithTag("Name")
            .assertIsDisplayed()
            .assertTextEquals("Backyard Cam")

        // 2. Check URI Masking
        // The regex in your code replaces credentials with ***
        // rtsp://admin:12345@... -> rtsp://***:***@...
        val expectedMaskedUri = "rtsp://***:***@192.168.1.50:554/stream"
        composeTestRule.onNodeWithTag("Uri")
            .assertIsDisplayed()
            .assertTextEquals(expectedMaskedUri)

        // 3. Check Tags
        composeTestRule.onNodeWithTag("Tag Outdoor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("Tag Security").assertIsDisplayed()

        // 4. Check Force TCP Checkbox (Should be checked and disabled)
        composeTestRule.onNodeWithTag("ForceTCP")
            .assertIsDisplayed()
            .assertIsOn() // Since forceTcp = true in sample data
    }

    @Test
    fun verifiesEditAction() {
        setContentWithInspectionMode(sampleItem)

        // Open the "More Options" menu
        composeTestRule.onNodeWithTag("MoreButton").performClick()

        // Assert Edit button is visible and click it
        composeTestRule.onNodeWithTag("EditButton")
            .assertIsDisplayed()
            .performClick()

        // Verify the callback was invoked
        verify(onEditMock).invoke()
    }

    @Test
    fun verifiesDeleteAction_CancelsDialog() {
        setContentWithInspectionMode(sampleItem)

        // 1. Open Menu -> Click Delete
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("DeleteButton").performClick()

        // 2. Assert Confirmation Dialog is visible
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertIsDisplayed()

        // 3. Click Cancel
        composeTestRule.onNodeWithTag("DeleteCancelButton").performClick()

        // 4. Verify Dialog is gone
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertDoesNotExist()

        // 5. Verify the actual delete callback was NEVER called
        verify(onDeleteMock, never()).invoke()
    }

    @Test
    fun verifiesDeleteAction_ConfirmsDialog() {
        setContentWithInspectionMode(sampleItem)

        // 1. Open Menu -> Click Delete
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("DeleteButton").performClick()

        // 2. Click Confirm
        composeTestRule.onNodeWithTag("DeleteConfirmButton").performClick()

        // 3. Verify the actual delete callback WAS called
        verify(onDeleteMock).invoke()

        // 4. Verify Dialog is gone
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertDoesNotExist()
    }

    @Test
    fun verifiesForceTcpFalseState() {
        // Create an item with ForceTCP = false
        val tcpOffItem = sampleItem.copy(forceTcp = false)
        setContentWithInspectionMode(tcpOffItem)

        composeTestRule.onNodeWithTag("ForceTCP")
            .assertIsOff()
    }

    /**
     * Helper function to set content.
     * Crucially, this sets LocalInspectionMode to true.
     * This triggers the logic in VideoPlayerCompose to show a placeholder Text
     * instead of trying to load Hilt/ExoPlayer, preventing crashes in UI tests.
     */
    private fun setContentWithInspectionMode(item: RTSPItem) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                StreamItemScreen(
                    item = item,
                    onEditItemSelected = onEditMock,
                    onDeleteItemSelected = onDeleteMock
                )
            }
        }
    }
}

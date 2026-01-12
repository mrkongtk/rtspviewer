package com.mrkongtk.rtspviewer.ui.screen

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
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
import com.mrkongtk.rtspviewer.ui.screen.action.StreamItemScreenActions
import com.mrkongtk.rtspviewer.ui.theme.RTSPViewerTheme
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

    // Mock the actions interface to verify interactions
    private val screenActions: StreamItemScreenActions = mock()

    // Sample data used for verification
    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Front Door Cam",
        // Credentials included to verify the 'hideCredentialUri' extension logic
        uri = "rtsp://admin:password123@192.168.1.50:554/live",
        tags = listOf("Outdoor", "Entry"),
        order = 0,
        forceTcp = true
    )

    @Test
    fun portraitMode_displaysCorrectMetadataAndElements() {
        setupContent(sampleItem, Configuration.ORIENTATION_PORTRAIT)

        // 1. Verify Video Player container is displayed
        composeTestRule.onNodeWithTag("VideoPlayer").assertIsDisplayed()

        // 2. Verify Name matches
        composeTestRule.onNodeWithTag("Name")
            .assertIsDisplayed()
            .assertTextEquals("Front Door Cam")

        // 3. Verify Masked URI (logic: rtsp://***:***@...)
        val expectedMaskedUri = "rtsp://***:***@192.168.1.50:554/live"
        composeTestRule.onNodeWithTag("Uri")
            .assertIsDisplayed()
            .assertTextEquals(expectedMaskedUri)

        // 4. Verify Tags are rendered with correct tags
        composeTestRule.onNodeWithTag("Tag Outdoor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("Tag Entry").assertIsDisplayed()

        // 5. Verify Checkbox state reflects 'forceTcp = true'
        composeTestRule.onNodeWithTag("ForceTCP")
            .assertIsDisplayed()
            .assertIsOn()

        // 6. Verify Options button is present
        composeTestRule.onNodeWithTag("MoreButton").assertIsDisplayed()
    }

    @Test
    fun landscapeMode_showsImmersivePlayerOnly() {
        setupContent(sampleItem, Configuration.ORIENTATION_LANDSCAPE)

        // 1. Verify landscape-specific player tag
        composeTestRule.onNodeWithTag("VideoPlayerLandscape").assertIsDisplayed()

        // 2. Ensure metadata rows are hidden to provide immersive view
        composeTestRule.onNodeWithTag("Name").assertDoesNotExist()
        composeTestRule.onNodeWithTag("Uri").assertDoesNotExist()
        composeTestRule.onNodeWithTag("Tag Outdoor").assertDoesNotExist()
        composeTestRule.onNodeWithTag("MoreButton").assertDoesNotExist()
    }

    @Test
    fun editAction_triggersCallback() {
        setupContent(sampleItem, Configuration.ORIENTATION_PORTRAIT)

        // Open Dropdown -> Click Edit
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("EditButton")
            .assertIsDisplayed()
            .performClick()

        // Verify the action was bubbled up
        verify(screenActions).onEditItemSelected()
    }

    @Test
    fun deleteFlow_confirmsDeletion() {
        setupContent(sampleItem, Configuration.ORIENTATION_PORTRAIT)

        // Open Dropdown -> Click Delete
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("DeleteButton").performClick()

        // Verify Dialog appears
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertIsDisplayed()

        // Click Confirm
        composeTestRule.onNodeWithTag("DeleteConfirmButton").performClick()

        // Verify callback triggered and dialog closed
        verify(screenActions).onDeleteItemSelected()
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertDoesNotExist()
    }

    @Test
    fun deleteFlow_cancelsDeletion() {
        setupContent(sampleItem, Configuration.ORIENTATION_PORTRAIT)

        // Trigger Dialog
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("DeleteButton").performClick()

        // Click Cancel
        composeTestRule.onNodeWithTag("DeleteCancelButton").performClick()

        // Verify no deletion occurred and dialog is gone
        verify(screenActions, never()).onDeleteItemSelected()
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertDoesNotExist()
    }

    @Test
    fun metadata_reflectsForceTcpOff() {
        val tcpOffItem = sampleItem.copy(forceTcp = false)
        setupContent(tcpOffItem, Configuration.ORIENTATION_PORTRAIT)

        composeTestRule.onNodeWithTag("ForceTCP").assertIsOff()
    }

    /**
     * Helper to configure the environment for the screen.
     *
     * 1. Uses [LocalInspectionMode] to skip real ExoPlayer/Hilt initialization.
     * 2. Uses [LocalConfiguration] to simulate Portrait/Landscape.
     * 3. Wraps in [RTSPViewerTheme] to ensure semantic colors/styles are applied.
     */
    private fun setupContent(item: RTSPItem, orientation: Int) {
        val config = Configuration().apply {
            this.orientation = orientation
        }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalConfiguration provides config
            ) {
                RTSPViewerTheme {
                    StreamItemScreen(
                        item = item,
                        screenActions = screenActions
                    )
                }
            }
        }
    }
}

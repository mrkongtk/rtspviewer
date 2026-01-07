package com.mrkongtk.rtspviewer.ui.screen

import android.graphics.Bitmap
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
    private val onImageAvailableMock: (RTSPItem, Bitmap) -> Unit = mock()

    // Sample Data matching the entity structure
    private val sampleItem = RTSPItem(
        id = 1L,
        name = "Backyard Cam",
        // URI with credentials to test the masking regex: (rtsp://)(.+)(:)(.+)(@)
        uri = "rtsp://admin:12345@192.168.1.50:554/stream",
        tags = listOf("Outdoor", "Security"),
        order = 0,
        forceTcp = true
    )

    @Test
    fun displaysCorrectMetadata_AndPlaceholder_InInspectionMode() {
        setContentWithInspectionMode(sampleItem)

        // 1. Verify Video Placeholder (Logic in VideoPlayerCompose for Inspection Mode)
        composeTestRule.onNodeWithTag("EmptyVideo")
            .assertIsDisplayed()
            .assertTextEquals("Video Player Preview")

        // 2. Check Name Row
        composeTestRule.onNodeWithTag("Name")
            .assertIsDisplayed()
            .assertTextEquals("Backyard Cam")

        // 3. Check URI Masking logic (hideUriCredential)
        // Group 2 (admin) and Group 4 (12345) should be replaced by ***
        val expectedMaskedUri = "rtsp://***:***@192.168.1.50:554/stream"
        composeTestRule.onNodeWithTag("Uri")
            .assertIsDisplayed()
            .assertTextEquals(expectedMaskedUri)

        // 4. Check Tags (rendered via FlowRow with testTag("Tag $tag"))
        composeTestRule.onNodeWithTag("Tag Outdoor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("Tag Security").assertIsDisplayed()

        // 5. Check Force TCP Checkbox (state check)
        composeTestRule.onNodeWithTag("ForceTCP")
            .assertIsDisplayed()
            .assertIsOn()
    }

    @Test
    fun verifiesEditAction_TriggersCallback() {
        setContentWithInspectionMode(sampleItem)

        // Open the "More Options" dropdown
        composeTestRule.onNodeWithTag("MoreButton").performClick()

        // Click Edit
        composeTestRule.onNodeWithTag("EditButton")
            .assertIsDisplayed()
            .performClick()

        // Verify ViewModel/Navigation callback
        verify(onEditMock).invoke()
    }

    @Test
    fun verifiesDeleteFlow_Confirm() {
        setContentWithInspectionMode(sampleItem)

        // Open Menu -> Click Delete
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("DeleteButton").performClick()

        // Verify Dialog appears
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertIsDisplayed()

        // Click Confirm
        composeTestRule.onNodeWithTag("DeleteConfirmButton").performClick()

        // Verify callback was called and dialog is dismissed
        verify(onDeleteMock).invoke()
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertDoesNotExist()
    }

    @Test
    fun verifiesDeleteFlow_Cancel() {
        setContentWithInspectionMode(sampleItem)

        // Open Menu -> Click Delete
        composeTestRule.onNodeWithTag("MoreButton").performClick()
        composeTestRule.onNodeWithTag("DeleteButton").performClick()

        // Click Cancel
        composeTestRule.onNodeWithTag("DeleteCancelButton").performClick()

        // Verify callback was NOT called and dialog is dismissed
        verify(onDeleteMock, never()).invoke()
        composeTestRule.onNodeWithTag("DeleteConfirmDialog").assertDoesNotExist()
    }

    @Test
    fun verifiesForceTcpOffState() {
        val tcpOffItem = sampleItem.copy(forceTcp = false)
        setContentWithInspectionMode(tcpOffItem)

        composeTestRule.onNodeWithTag("ForceTCP")
            .assertIsOff()
    }

    /**
     * Helper to inject LocalInspectionMode.
     * This prevents the Hilt-based hiltViewModel() call inside VideoPlayerCompose
     * from executing, which would fail in a functional UI test environment.
     */
    private fun setContentWithInspectionMode(item: RTSPItem) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                StreamItemScreen(
                    item = item,
                    onEditItemSelected = onEditMock,
                    onDeleteItemSelected = onDeleteMock,
                    onImageAvailable = onImageAvailableMock
                )
            }
        }
    }
}

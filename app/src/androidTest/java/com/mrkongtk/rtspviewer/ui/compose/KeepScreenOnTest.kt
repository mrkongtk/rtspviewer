package com.mrkongtk.rtspviewer.ui.compose

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeepScreenOnTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test Case: Verify that when KeepScreenOn(true) is composed,
     * the underlying view's keepScreenOn property is set to true.
     */
    @Test
    fun keepScreenOn_whenTrue_setsViewProperty() {
        var view: View? = null

        composeTestRule.setContent {
            view = LocalView.current
            KeepScreenOn(enable = true)
        }

        composeTestRule.runOnIdle {
            assertTrue("View.keepScreenOn should be true", view?.keepScreenOn ?: false)
        }
    }

    /**
     * Test Case: Verify that when KeepScreenOn(false) is composed,
     * the underlying view's keepScreenOn property is set to false.
     */
    @Test
    fun keepScreenOn_whenFalse_setsViewPropertyFalse() {
        var view: View? = null

        composeTestRule.setContent {
            view = LocalView.current
            KeepScreenOn(enable = false)
        }

        composeTestRule.runOnIdle {
            assertFalse("View.keepScreenOn should be false", view?.keepScreenOn ?: true)
        }
    }

    /**
     * Test Case: Verify that toggling the 'enable' parameter
     * updates the view property dynamically.
     */
    @Test
    fun keepScreenOn_togglesProperty_whenStateChanges() {
        var view: View? = null
        var isEnabled by mutableStateOf(true)

        composeTestRule.setContent {
            view = LocalView.current
            KeepScreenOn(enable = isEnabled)
        }

        // Check initial true state
        composeTestRule.runOnIdle {
            assertTrue(view?.keepScreenOn ?: false)
        }

        // Toggle to false
        isEnabled = false
        composeTestRule.waitForIdle()

        // Check updated false state
        composeTestRule.runOnIdle {
            assertFalse(view?.keepScreenOn ?: true)
        }
    }

    /**
     * Test Case: Verify that when the Composable is removed from the
     * composition (disposed), it resets the flag to false.
     */
    @Test
    fun keepScreenOn_resetsToFalse_onDisposal() {
        var view: View? = null
        var showComponent by mutableStateOf(true)

        composeTestRule.setContent {
            view = LocalView.current
            if (showComponent) {
                KeepScreenOn(enable = true)
            }
        }

        // Verify it is true initially
        composeTestRule.runOnIdle {
            assertTrue(view?.keepScreenOn ?: false)
        }

        // Remove from composition
        showComponent = false
        composeTestRule.waitForIdle()

        // Verify onDispose triggered the reset
        composeTestRule.runOnIdle {
            assertFalse("View.keepScreenOn should be reset to false after disposal", view?.keepScreenOn ?: true)
        }
    }
}

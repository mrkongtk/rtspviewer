package com.mrkongtk.rtspviewer.shared.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
abstract class KeepScreenOnTest {

    @Composable
    abstract fun WhenContentSet()

    abstract fun isScreenKeepOn(): Boolean?

    /**
     * Test Case: Verify that when KeepScreenOn(true) is composed,
     * the underlying view's keepScreenOn property is set to true.
     */
    @Test
    fun keepScreenOn_whenTrue_setsViewProperty() = runComposeUiTest {
        setContent {
            WhenContentSet()
            KeepScreenOn(enable = true)
        }

        runOnIdle {
            val isScreenKeepOn = isScreenKeepOn()
            assertNotNull(isScreenKeepOn, "isScreenKeepOn should not be null")
            assertTrue( isScreenKeepOn, "View.keepScreenOn should be true")
        }
    }

    /**
     * Test Case: Verify that when KeepScreenOn(false) is composed,
     * the underlying view's keepScreenOn property is set to false.
     */
    @Test
    fun keepScreenOn_whenFalse_setsViewPropertyFalse() = runComposeUiTest {

        setContent {
            WhenContentSet()
            KeepScreenOn(enable = false)
        }

        runOnIdle {
            val isScreenKeepOn = isScreenKeepOn()
            assertNotNull(isScreenKeepOn,"isScreenKeepOn should not be null")
            assertFalse(isScreenKeepOn,"View.keepScreenOn should be false")
        }
    }

    /**
     * Test Case: Verify that toggling the 'enable' parameter
     * updates the view property dynamically.
     */
    @Test
    fun keepScreenOn_togglesProperty_whenStateChanges() = runComposeUiTest {
        var isEnabled by mutableStateOf(true)

        setContent {
            WhenContentSet()
            KeepScreenOn(enable = isEnabled)
        }

        // Check initial true state
        runOnIdle {
            val isScreenKeepOn = isScreenKeepOn()
            assertNotNull(isScreenKeepOn,"isScreenKeepOn should not be null")
            assertTrue(isScreenKeepOn)
        }

        // Toggle to false
        isEnabled = false
        waitForIdle()

        // Check updated false state
        runOnIdle {
            val isScreenKeepOn = isScreenKeepOn()
            assertNotNull(isScreenKeepOn,"isScreenKeepOn should not be null")
            assertFalse(isScreenKeepOn)
        }
    }

    /**
     * Test Case: Verify that when the Composable is removed from the
     * composition (disposed), it resets the flag to false.
     */
    @Test
    fun keepScreenOn_resetsToFalse_onDisposal() = runComposeUiTest {
        var showComponent by mutableStateOf(true)

        setContent {
            WhenContentSet()
            if (showComponent) {
                KeepScreenOn(enable = true)
            }
        }

        // Verify it is true initially
        runOnIdle {
            val isScreenKeepOn = isScreenKeepOn()
            assertNotNull(isScreenKeepOn,"isScreenKeepOn should not be null")
            assertTrue(isScreenKeepOn)
        }

        // Remove from composition
        showComponent = false
        waitForIdle()

        // Verify onDispose triggered the reset
        runOnIdle {
            val isScreenKeepOn = isScreenKeepOn()
            assertNotNull(isScreenKeepOn,"isScreenKeepOn should not be null")
            assertFalse(isScreenKeepOn)
        }
    }
}

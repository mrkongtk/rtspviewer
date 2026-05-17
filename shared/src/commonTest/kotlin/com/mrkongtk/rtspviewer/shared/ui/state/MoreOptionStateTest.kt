package com.mrkongtk.rtspviewer.shared.ui.state

import kotlin.test.Test
import kotlin.test.assertEquals

class MoreOptionStateTest {

    @Test
    fun fromValue_returns_correct_state() {
        assertEquals(MoreOptionState.OPEN, MoreOptionState.fromValue(true))
        assertEquals(MoreOptionState.CLOSED, MoreOptionState.fromValue(false))
    }

    @Test
    fun value_property_returns_correct_boolean() {
        assertEquals(true, MoreOptionState.OPEN.value)
        assertEquals(false, MoreOptionState.CLOSED.value)
    }

    @Test
    fun not_operator_toggles_state() {
        assertEquals(MoreOptionState.CLOSED, !MoreOptionState.OPEN)
        assertEquals(MoreOptionState.OPEN, !MoreOptionState.CLOSED)
    }

    @Test
    fun double_negation_returns_original_state() {
        val state = MoreOptionState.OPEN
        assertEquals(state, !!state)
        
        val closedState = MoreOptionState.CLOSED
        assertEquals(closedState, !!closedState)
    }
}

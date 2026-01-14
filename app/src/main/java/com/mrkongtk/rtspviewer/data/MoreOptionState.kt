package com.mrkongtk.rtspviewer.data

/**
 * Encapsulates the visual state of the "More Options" menu.
 * Using an Enum instead of a Boolean makes state transitions more explicit.
 */
enum class MoreOptionState(val value: Boolean) {
    OPEN(true),
    CLOSED(false);

    companion object {
        fun fromValue(value: Boolean): MoreOptionState = if (value) OPEN else CLOSED
    }
}

/**
 * Extension operator to toggle the menu state using [!state] syntax.
 */
operator fun MoreOptionState.not(): MoreOptionState = if (this == MoreOptionState.OPEN) {
    MoreOptionState.CLOSED
} else {
    MoreOptionState.OPEN
}

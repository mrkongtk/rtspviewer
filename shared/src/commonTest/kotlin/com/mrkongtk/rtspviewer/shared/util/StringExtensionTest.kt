package com.mrkongtk.rtspviewer.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for String extension functions.
 * Tests the positional replacement logic for UI string templates.
 */
class StringExtensionTest {

    @Test
    fun formatText_with_single_argument_replaces_percent1() {
        val template = "Viewing %1"
        val expected = "Viewing Living Room"
        val result = template.formatText("Living Room")

        assertEquals(expected, result)
    }

    @Test
    fun formatText_with_multiple_arguments_replaces_in_correct_order() {
        val template = "Camera %1 is located in %2"
        val expected = "Camera Front Door is located in Backyard"
        val result = template.formatText("Front Door", "Backyard")

        assertEquals(expected, result)
    }

    @Test
    fun formatText_with_list_argument_works_same_as_vararg() {
        val template = "Editing %1"
        val args = listOf("Kitchen Cam")
        val expected = "Editing Kitchen Cam"
        val result = template.formatText(args)

        assertEquals(expected, result)
    }

    @Test
    fun formatText_handles_out_of_order_placeholders() {
        // The logic replaces %1 with args[0], %2 with args[1] regardless of position in string
        val template = "Target: %2, Source: %1"
        val expected = "Target: Destination, Source: Origin"
        val result = template.formatText("Origin", "Destination")

        assertEquals(expected, result)
    }

    @Test
    fun formatText_replaces_all_occurrences_of_the_same_placeholder() {
        val template = "%1 and %1 and %1"
        val expected = "Alert and Alert and Alert"
        val result = template.formatText("Alert")

        assertEquals(expected, result)
    }

    @Test
    fun formatText_leaves_placeholder_untouched_if_no_matching_argument_is_provided() {
        val template = "Valid: %1, Missing: %2"
        val expected = "Valid: Success, Missing: %2"
        val result = template.formatText("Success")

        assertEquals(expected, result)
    }

    @Test
    fun formatText_ignores_extra_arguments_that_have_no_corresponding_placeholder() {
        val template = "Only %1"
        val expected = "Only One"
        val result = template.formatText("One", "Two", "Three")

        assertEquals(expected, result)
    }

    @Test
    fun formatText_returns_original_string_if_no_placeholders_exist() {
        val template = "Static string with no tags"
        val result = template.formatText("Unused")

        assertEquals(template, result)
    }

    @Test
    fun formatText_handles_empty_strings_and_special_characters_in_arguments() {
        val template = "User: [%1], Status: [%2]"
        val expected = "User: [], Status: [Active! @#$]"
        val result = template.formatText("", "Active! @#$")

        assertEquals(expected, result)
    }
}

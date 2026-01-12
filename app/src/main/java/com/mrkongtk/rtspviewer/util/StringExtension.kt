package com.mrkongtk.rtspviewer.util

/**
 * Extension functions for the [String] class to handle custom text formatting and template replacement.
 *
 * These utilities allow for a simple positional replacement strategy (e.g., replacing "%1" with a value),
 * which is particularly useful for dynamic UI components like the AppBar or Dialogs where
 * localized strings contain placeholders.
 */

/**
 * Formats a string by replacing positional placeholders (e.g., %1, %2) with the provided [args].
 * This is a convenience wrapper for the List-based [formatText] function.
 *
 * @param args Variable number of string arguments to insert into the template.
 * @return The formatted string with placeholders replaced.
 */
internal fun String.formatText(vararg args: String): String {
    return formatText(args.toList())
}

/**
 * Performs dynamic string formatting by replacing placeholders with values from a [List].
 *
 * **Replacement Logic:**
 * The function maps arguments to indices starting at 1. For example:
 * - `%1` is replaced by the first item in [args].
 * - `%2` is replaced by the second item in [args].
 *
 * **Example:**
 * ```kotlin
 * "Viewing %1 in %2".formatText(listOf("Camera 1", "HD"))
 * // Result: "Viewing Camera 1 in HD"
 * ```
 *
 * @param args A list of strings used to fill the placeholders.
 * @return A new string with all applicable placeholders replaced.
 */
internal fun String.formatText(args: List<String>): String {
    // 1. Create a combined list where the first element is the template string itself.
    // 2. Use reduceIndexed to iterate through the arguments.
    // 3. Since the template is at index 0, the first actual argument is at index 1,
    //    matching the "%1" placeholder syntax.
    return (listOf(this) + args).reduceIndexed { index, template, nextValue ->
        template.replace("%$index", nextValue)
    }
}

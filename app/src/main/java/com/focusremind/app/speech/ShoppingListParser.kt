package com.focusremind.app.speech

/**
 * Detects "add X to shopping list" voice commands, completely separate from
 * TimeParser — a shopping list item has no time/date at all, so it must be
 * checked BEFORE TimeParser even runs, and short-circuit that whole flow.
 *
 * Polish only for now — a first, narrow pattern to validate the feature in
 * practice before expanding to other languages/phrasings.
 */
object ShoppingListParser {

    // "Dodaj żółty ser do listy zakupów" / "dodaj mleko do zakupów"
    private val triggerRegex = Regex(
        """^dodaj\s+(.+?)\s+do\s+(?:listy\s+zakup[oó]w|zakup[oó]w)\.?$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Returns the extracted product name (e.g. "żółty ser") if the text
     * matches a shopping-list command, or null if it doesn't — in which
     * case the caller should fall back to normal reminder parsing.
     */
    fun parse(text: String): String? {
        val match = triggerRegex.find(text.trim()) ?: return null
        val item = match.groupValues[1].trim()
        if (item.isBlank()) return null
        return item.replaceFirstChar { it.uppercase() }
    }
}

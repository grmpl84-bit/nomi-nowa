package com.focusremind.app.speech

/**
 * Detects "add X to shopping list" voice commands, completely separate from
 * TimeParser — a shopping list item has no time/date at all, so it must be
 * checked BEFORE TimeParser even runs, and short-circuit that whole flow.
 *
 * Polish + English.
 */
object ShoppingListParser {

    // "Dodaj żółty ser do listy zakupów" / "dodaj mleko do zakupów"
    private val triggerRegexPl = Regex(
        """^dodaj\s+(.+?)\s+do\s+(?:listy\s+zakup[oó]w|zakup[oó]w)\.?$""",
        RegexOption.IGNORE_CASE
    )

    // "Add milk to the shopping list" / "add milk to my grocery list" /
    // "add milk to shopping list" (no article, common in casual speech)
    private val triggerRegexEn = Regex(
        """^add\s+(.+?)\s+to\s+(?:the\s+|my\s+)?(?:shopping|grocery)\s+list\.?$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Returns the extracted product name (e.g. "żółty ser" / "milk") if the
     * text matches a shopping-list command, or null if it doesn't — in
     * which case the caller should fall back to normal reminder parsing.
     */
    fun parse(text: String): String? {
        val trimmed = text.trim()
        val match = triggerRegexPl.find(trimmed) ?: triggerRegexEn.find(trimmed) ?: return null
        val item = match.groupValues[1].trim()
        if (item.isBlank()) return null
        return item.replaceFirstChar { it.uppercase() }
    }
}

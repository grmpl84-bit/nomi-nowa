package com.focusremind.app.speech

/**
 * Detects "when I'm home/at work/in the car, ..." voice commands — a
 * reminder that fires from the location mechanism (WiFi/Bluetooth,
 * configured in Settings) instead of a clock time. Checked before
 * TimeParser, same pattern as ShoppingListParser/RecurringVoiceParser.
 *
 * Polish + English. Deliberately covers only the handful of most natural
 * phrasings rather than every possible way to say it — narrow on purpose,
 * same philosophy as the other voice parsers this session.
 */
object LocationVoiceParser {

    data class Result(val cleanedText: String, val locationTrigger: String)

    private val plPatterns = listOf(
        Regex("""^(?:jak|gdy|kiedy)\s+będę\s+w\s+domu,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "HOME",
        Regex("""^(?:jak|gdy|kiedy)\s+wrócę\s+do\s+domu,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "HOME",
        Regex("""^(?:jak|gdy|kiedy)\s+będę\s+w\s+pracy,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "WORK",
        Regex("""^(?:jak|gdy|kiedy)\s+dotrę\s+do\s+pracy,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "WORK",
        Regex("""^(?:jak|gdy|kiedy)\s+będę\s+w\s+samochodzie,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "CAR",
        Regex("""^(?:jak|gdy|kiedy)\s+wsiądę\s+do\s+samochodu,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "CAR"
    )

    private val enPatterns = listOf(
        Regex("""^when\s+i'?m\s+home,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "HOME",
        Regex("""^when\s+i\s+get\s+home,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "HOME",
        Regex("""^when\s+i'?m\s+at\s+work,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "WORK",
        Regex("""^when\s+i\s+get\s+to\s+work,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "WORK",
        Regex("""^when\s+i'?m\s+in\s+the\s+car,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "CAR",
        Regex("""^when\s+i\s+get\s+in\s+the\s+car,?\s+(.+)$""", RegexOption.IGNORE_CASE) to "CAR"
    )

    fun parse(text: String): Result? {
        val trimmed = text.trim()
        for ((pattern, trigger) in plPatterns + enPatterns) {
            pattern.find(trimmed)?.let { m ->
                val content = m.groupValues[1].trim()
                if (content.isNotBlank()) {
                    return Result(content.replaceFirstChar { it.uppercase() }, trigger)
                }
            }
        }
        return null
    }
}

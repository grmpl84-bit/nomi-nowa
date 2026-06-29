package com.focusremind.app.speech

import java.util.Calendar

object TimeParser {

    data class Result(val triggerAt: Long, val cleanedText: String)

    // Polish word numbers → digits
    private val plNumbers = mapOf(
        "minutę" to 1, "minutke" to 1, "minutkę" to 1, "minute" to 1,
        "jedną" to 1, "jedna" to 1, "jeden" to 1,
        "dwie" to 2, "dwa" to 2, "dwóch" to 2, "dwoch" to 2,
        "trzy" to 3, "trzech" to 3,
        "cztery" to 4, "czterech" to 4,
        "pięć" to 5, "pieć" to 5, "piec" to 5, "pięciu" to 5,
        "sześć" to 6, "szesc" to 6, "sześciu" to 6,
        "siedem" to 7, "siedmiu" to 7,
        "osiem" to 8, "ośmiu" to 8, "osmiu" to 8,
        "dziewięć" to 9, "dziewiec" to 9, "dziewięciu" to 9,
        "dziesięć" to 10, "dziesiec" to 10, "dziesięciu" to 10,
        "jedenaście" to 11, "jedenascie" to 11, "jedenastej" to 11,
        "dwanaście" to 12, "dwanascie" to 12, "dwunastej" to 12,
        "trzynaście" to 13, "trzynascie" to 13, "trzynastej" to 13,
        "czternaście" to 14, "czternascie" to 14, "czternastej" to 14,
        "piętnaście" to 15, "pietnascie" to 15, "piętnastej" to 15,
        "szesnaście" to 16, "szesnascie" to 16, "szesnastej" to 16,
        "siedemnaście" to 17, "siedemnascie" to 17, "siedemnastej" to 17,
        "osiemnaście" to 18, "osiemnascie" to 18, "osiemnastej" to 18,
        "dziewiętnaście" to 19, "dziewietnascie" to 19, "dziewiętnastej" to 19,
        "dwadzieścia" to 20, "dwadziescia" to 20, "dwudziestej" to 20,
        "dwudziesta" to 20, "dwudziesty" to 20,
        "trzydzieści" to 30, "trzydziesci" to 30, "trzydziestej" to 30,
        "czterdzieści" to 40, "czterdziesci" to 40,
        "pół" to -1, "pol" to -1, "wpół" to -1 // special: half
    )

    // Polish ordinal hours (used in "o dwudziestej", "o ósmej")
    private val plOrdinalHours = mapOf(
        "pierwszej" to 1, "drugiej" to 2, "trzeciej" to 3, "czwartej" to 4,
        "piątej" to 5, "piatej" to 5, "szóstej" to 6, "szostej" to 6,
        "siódmej" to 7, "siodmej" to 7, "ósmej" to 8, "osmej" to 8,
        "dziewiątej" to 9, "dziewiatej" to 9, "dziesiątej" to 10, "dziesiatej" to 10,
        "jedenastej" to 11, "dwunastej" to 12, "trzynastej" to 13,
        "czternastej" to 14, "piętnastej" to 15, "pietnastej" to 15,
        "szesnastej" to 16, "siedemnastej" to 17, "osiemnastej" to 18,
        "dziewiętnastej" to 19, "dziewietnastej" to 19,
        "dwudziestej" to 20, "dwudziestej pierwszej" to 21,
        "dwudziestej drugiej" to 22, "dwudziestej trzeciej" to 23
    )

    // Polish months
    private val plMonths = mapOf(
        "stycznia" to 1, "styczeń" to 1, "styczen" to 1,
        "lutego" to 2, "luty" to 2,
        "marca" to 3, "marzec" to 3,
        "kwietnia" to 4, "kwiecień" to 4, "kwiecien" to 4,
        "maja" to 5, "maj" to 5,
        "czerwca" to 6, "czerwiec" to 6,
        "lipca" to 7, "lipiec" to 7,
        "sierpnia" to 8, "sierpień" to 8, "sierpien" to 8,
        "września" to 9, "wrzesień" to 9, "wrzesien" to 9,
        "października" to 10, "październik" to 10, "pazdziernika" to 10, "pazdziernik" to 10,
        "listopada" to 11, "listopad" to 11,
        "grudnia" to 12, "grudzień" to 12, "grudzien" to 12
    )

    // Polish days of week
    private val plDaysOfWeek = mapOf(
        "poniedziałek" to Calendar.MONDAY, "poniedzialek" to Calendar.MONDAY,
        "w poniedziałek" to Calendar.MONDAY, "w poniedzialek" to Calendar.MONDAY,
        "wtorek" to Calendar.TUESDAY, "we wtorek" to Calendar.TUESDAY,
        "środę" to Calendar.WEDNESDAY, "srode" to Calendar.WEDNESDAY,
        "w środę" to Calendar.WEDNESDAY, "w srode" to Calendar.WEDNESDAY,
        "czwartek" to Calendar.THURSDAY, "w czwartek" to Calendar.THURSDAY,
        "piątek" to Calendar.FRIDAY, "piatek" to Calendar.FRIDAY,
        "w piątek" to Calendar.FRIDAY, "w piatek" to Calendar.FRIDAY,
        "sobotę" to Calendar.SATURDAY, "sobote" to Calendar.SATURDAY,
        "w sobotę" to Calendar.SATURDAY, "w sobote" to Calendar.SATURDAY,
        "niedzielę" to Calendar.SUNDAY, "niedziele" to Calendar.SUNDAY,
        "w niedzielę" to Calendar.SUNDAY, "w niedziele" to Calendar.SUNDAY
    )

    // German word numbers
    private val deNumbers = mapOf(
        "eine" to 1, "einer" to 1, "einem" to 1, "eins" to 1,
        "zwei" to 2, "drei" to 3, "vier" to 4, "fünf" to 5, "funf" to 5,
        "sechs" to 6, "sieben" to 7, "acht" to 8, "neun" to 9,
        "zehn" to 10, "fünfzehn" to 15, "funfzehn" to 15,
        "zwanzig" to 20, "dreißig" to 30, "dreissig" to 30
    )

    // English word numbers
    private val enNumbers = mapOf(
        "one" to 1, "a" to 1, "an" to 1,
        "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "fifteen" to 15, "twenty" to 20, "thirty" to 30,
        "half" to -1
    )

    private fun extractNumber(text: String): Int? {
        Regex("""(\d+)""").find(text)?.let { return it.groupValues[1].toInt() }
        for ((word, num) in plNumbers + enNumbers + deNumbers) {
            if (text.contains(word)) return num
        }
        return null
    }

    /**
     * Extract hour from text - supports both digits and Polish ordinal words.
     * E.g. "dwudziestej" → 20, "ósmej" → 8, "15" → 15
     */
    private fun extractHour(text: String): Int? {
        // Check ordinal hours first (longest match first)
        for ((word, hour) in plOrdinalHours.entries.sortedByDescending { it.key.length }) {
            if (text.contains(word)) return hour
        }
        // Then try digits
        Regex("""(\d{1,2})""").find(text)?.let { return it.groupValues[1].toInt() }
        // Then try word numbers
        for ((word, num) in plNumbers) {
            if (num in 1..23 && text.contains(word)) return num
        }
        return null
    }

    /**
     * Extract minute from text after hour has been found.
     */
    private fun extractMinute(text: String): Int {
        // "trzydzieści" "piętnaście" etc.
        Regex("""(\d{1,2})""").find(text)?.let { return it.groupValues[1].toInt() }
        for ((word, num) in plNumbers) {
            if (num in 1..59 && text.contains(word)) return num
        }
        return 0
    }

    /**
     * Find Polish month in text and return month number (1-12) and match position.
     */
    private fun findMonth(text: String): Pair<Int, String>? {
        for ((word, month) in plMonths.entries.sortedByDescending { it.key.length }) {
            if (text.contains(word)) return Pair(month, word)
        }
        return null
    }

    /**
     * Get next occurrence of a day-of-week.
     */
    private fun getNextDayOfWeek(dayOfWeek: Int): Calendar {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_WEEK)
        var daysAhead = dayOfWeek - today
        if (daysAhead <= 0) daysAhead += 7
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        return cal
    }

    fun parse(input: String): Result? {
        val text = input.lowercase().trim()

        // === POLSKIE WYRAŻENIA ===

        // === DATE + TIME: "X miesiąca o godzinie" (e.g. "11 sierpnia o dwudziestej") ===
        // Pattern: [day] [month] o [hour]:[min]
        findMonth(text)?.let { (month, monthWord) ->
            // Find day number near the month
            val beforeMonth = text.substringBefore(monthWord).trim()
            val day = Regex("""(\d{1,2})""").find(beforeMonth)?.groupValues?.get(1)?.toInt()
                ?: extractNumber(beforeMonth)

            if (day != null && day in 1..31) {
                // Find time - look for "o [hour]" pattern
                val afterMonth = text.substringAfter(monthWord)
                var hour = 9 // default
                var minute = 0

                // Try "o dwudziestej", "o 20", "o dwudziestej trzeciej"
                Regex("""o\s+(.+)""").find(afterMonth)?.let { timeMatch ->
                    val timeText = timeMatch.groupValues[1]
                    extractHour(timeText)?.let { h ->
                        hour = h
                        // Look for minutes after the hour word
                        val afterHour = timeText.replace(Regex("""\d+"""), "")
                        minute = extractMinute(afterHour.replace(
                            plOrdinalHours.keys.find { timeText.contains(it) } ?: "", ""
                        ).trim())
                    }
                }
                // Also try just digits: "o 20:30"
                Regex("""o\s+(\d{1,2})(?::(\d{2}))?""").find(text)?.let {
                    hour = it.groupValues[1].toInt()
                    minute = it.groupValues[2].toIntOrNull() ?: 0
                }

                val cal = Calendar.getInstance().apply {
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    // If date is in the past, move to next year
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.YEAR, 1)
                    }
                }
                val cleaned = text
                    .replace(Regex("""\d+\s*$monthWord"""), "")
                    .replace(Regex("""o\s+.+"""), "")
                    .replace(monthWord, "")
                    .trim()
                return Result(cal.timeInMillis, cleaned)
            }
        }

        // === DAY OF WEEK + TIME: "w poniedziałek o 15" ===
        for ((dayName, dayOfWeek) in plDaysOfWeek.entries.sortedByDescending { it.key.length }) {
            if (text.contains(dayName)) {
                val cal = getNextDayOfWeek(dayOfWeek)
                // Look for time
                val afterDay = text.substringAfter(dayName)
                Regex("""o\s+(\d{1,2})(?::(\d{2}))?""").find(afterDay)?.let {
                    cal.set(Calendar.HOUR_OF_DAY, it.groupValues[1].toInt())
                    cal.set(Calendar.MINUTE, it.groupValues[2].toIntOrNull() ?: 0)
                }
                // Try ordinal hours
                if (afterDay.contains(" o ")) {
                    val timeText = afterDay.substringAfter(" o ").trim()
                    extractHour(timeText)?.let { cal.set(Calendar.HOUR_OF_DAY, it) }
                }
                val cleaned = text.replace(dayName, "").replace(Regex("""o\s+\S+"""), "").trim()
                return Result(cal.timeInMillis, cleaned)
            }
        }

        // "za X minut/minutę/minuty"
        Regex("""za\s+(.+?)\s*min(?:ut[ęy]?[ęe]?)?""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) {
                val ms = num.toLong() * 60_000
                return Result(System.currentTimeMillis() + ms, text.replace(it.value, "").trim())
            }
        }

        // "za minutę" / "za minutke"
        if (text.contains("za minutę") || text.contains("za minute") || text.contains("za minutke") || text.contains("za minutkę")) {
            val cleaned = text.replace(Regex("za minut[ęe]|za minutk[ęe]"), "").trim()
            return Result(System.currentTimeMillis() + 60_000, cleaned)
        }

        // "za pół godziny"
        if (text.contains("za pół godziny") || text.contains("za pol godziny")) {
            return Result(System.currentTimeMillis() + 30 * 60_000, text.replace(Regex("za p[oó]ł godziny"), "").trim())
        }

        // "za kwadrans"
        if (text.contains("za kwadrans")) {
            return Result(System.currentTimeMillis() + 15 * 60_000, text.replace("za kwadrans", "").trim())
        }

        // "za godzinę"
        if (text.contains("za godzinę") || text.contains("za godzine")) {
            return Result(System.currentTimeMillis() + 3_600_000, text.replace(Regex("za godzin[ęe]"), "").trim())
        }

        // "za X godzin/godziny"
        Regex("""za\s+(.+?)\s*godzin[ęy]?""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) {
                val ms = num.toLong() * 3_600_000
                return Result(System.currentTimeMillis() + ms, text.replace(it.value, "").trim())
            }
        }

        // "za X dni/dzień"
        Regex("""za\s+(.+?)\s*(?:dni|dzień|dzie[nń])""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, num); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // "za X sekund"
        Regex("""za\s+(.+?)\s*sekund""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) {
                val ms = num.toLong() * 1000
                return Result(System.currentTimeMillis() + ms, text.replace(it.value, "").trim())
            }
        }

        // "jutro o X" (with digits or ordinal)
        Regex("""jutro\s+o\s+(.+)""").find(text)?.let {
            val timeText = it.groupValues[1]
            val h = extractHour(timeText)
            if (h != null) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // "jutro" (alone)
        if (text.contains("jutro") && !text.contains(" o ")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("jutro", "").trim())
        }

        // "pojutrze"
        if (text.contains("pojutrze")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("pojutrze", "").trim())
        }

        // "o dwudziestej", "o ósmej" etc. (ordinal hours without date = today/tomorrow)
        for ((ordinal, hour) in plOrdinalHours.entries.sortedByDescending { it.key.length }) {
            if (text.contains("o $ordinal")) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                return Result(cal.timeInMillis, text.replace("o $ordinal", "").trim())
            }
        }

        // "o X:XX" / "o X" (digits, today or tomorrow)
        Regex("""o\s+(\d{1,2})(?::(\d{2}))?""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        // === ENGLISH EXPRESSIONS ===

        Regex("""in\s+(\d+)\s*min(?:ute)?s?""").find(text)?.let {
            val ms = it.groupValues[1].toLong() * 60_000
            return Result(System.currentTimeMillis() + ms, text.replace(it.value, "").trim())
        }

        Regex("""in\s+(\d+)\s*hours?""").find(text)?.let {
            val ms = it.groupValues[1].toLong() * 3_600_000
            return Result(System.currentTimeMillis() + ms, text.replace(it.value, "").trim())
        }

        Regex("""in\s+(\d+)\s*days?""").find(text)?.let {
            val days = it.groupValues[1].toInt()
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        Regex("""tomorrow\s+(?:at\s+)?(\d{1,2})(?::(\d{2}))?""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        if (text.contains("tomorrow")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("tomorrow", "").trim())
        }

        Regex("""at\s+(\d{1,2})(?::(\d{2}))?""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        // === GERMAN ===

        Regex("""in\s+(\d+)\s*minuten?""").find(text)?.let {
            val ms = it.groupValues[1].toLong() * 60_000
            return Result(System.currentTimeMillis() + ms, text.replace(it.value, "").trim())
        }

        Regex("""in\s+(\d+)\s*stunden?""").find(text)?.let {
            val ms = it.groupValues[1].toLong() * 3_600_000
            return Result(System.currentTimeMillis() + ms, text.replace(it.value, "").trim())
        }

        if (text.contains("halben stunde") || text.contains("halbe stunde")) {
            return Result(System.currentTimeMillis() + 30 * 60_000, text.replace(Regex("in einer? halben? stunde"), "").trim())
        }

        if (text.contains("in einer stunde")) {
            return Result(System.currentTimeMillis() + 3_600_000, text.replace("in einer stunde", "").trim())
        }

        Regex("""in\s+(\d+)\s*tagen?""").find(text)?.let {
            val days = it.groupValues[1].toInt()
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        Regex("""morgen\s+(?:um\s+)?(\d{1,2})(?::(\d{2}))?""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        if (text.contains("morgen") && !text.contains(" um ")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("morgen", "").trim())
        }

        if (text.contains("übermorgen") || text.contains("ubermorgen")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(Regex("[üu]bermorgen"), "").trim())
        }

        Regex("""um\s+(\d{1,2})(?::(\d{2}))?\s*(?:uhr)?""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toIntOrNull() ?: 0
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        return null
    }
}

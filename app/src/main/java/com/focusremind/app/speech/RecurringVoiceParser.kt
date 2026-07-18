package com.focusremind.app.speech

import java.util.Calendar

/**
 * Detects recurring reminder voice commands ("codziennie o 21 ...", "every
 * day at 9pm ..."), completely separate from normal one-time TimeParser
 * parsing — similar in spirit to ShoppingListParser: checked before
 * TimeParser, short-circuits the whole one-time-reminder flow when matched.
 *
 * Deliberately narrow scope, matching exactly what the database supports
 * today (DAILY / WEEKLY / BIWEEKLY / MONTHLY) — no "every other day",
 * "twice a day", or "every weekday", since those would need a different
 * data model entirely.
 *
 * Polish + English for now.
 */
object RecurringVoiceParser {

    data class Result(val triggerAt: Long, val cleanedText: String, val recurrence: String)

    private val plDayNames = linkedMapOf(
        "poniedziałek" to Calendar.MONDAY,
        "wtorek" to Calendar.TUESDAY,
        "środę" to Calendar.WEDNESDAY, "środa" to Calendar.WEDNESDAY,
        "czwartek" to Calendar.THURSDAY,
        "piątek" to Calendar.FRIDAY,
        "sobotę" to Calendar.SATURDAY, "sobota" to Calendar.SATURDAY,
        "niedzielę" to Calendar.SUNDAY, "niedziela" to Calendar.SUNDAY
    )
    private val enDayNames = linkedMapOf(
        "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY, "wednesday" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY, "friday" to Calendar.FRIDAY, "saturday" to Calendar.SATURDAY,
        "sunday" to Calendar.SUNDAY
    )
    private val plDayParts = mapOf(
        "po południu" to 15, "przed południem" to 10,
        "wieczorem" to 19, "w nocy" to 22, "rano" to 8
    )
    private val enDayParts = mapOf(
        "morning" to 8, "afternoon" to 15, "evening" to 19, "night" to 22
    )

    fun parse(text: String): Result? {
        val lower = text.trim().lowercase()
        parsePolish(lower)?.let { return it }
        parseEnglish(lower)?.let { return it }
        return null
    }

    /** Parses a bounded clock-time token only ("21", "21:30", "9pm", "9:30pm"). */
    private fun parseClockTime(raw: String): Pair<Int, Int>? {
        val s = raw.trim().lowercase()
        Regex("""^(\d{1,2}):(\d{2})\s*(am|pm)?$""").find(s)?.let {
            var h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            val ampm = it.groupValues[3]
            if (ampm == "pm" && h < 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            if (h in 0..23 && m in 0..59) return Pair(h, m)
        }
        Regex("""^(\d{1,2})\s*(am|pm)$""").find(s)?.let {
            var h = it.groupValues[1].toInt()
            val ampm = it.groupValues[2]
            if (ampm == "pm" && h < 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            if (h in 0..23) return Pair(h, 0)
        }
        Regex("""^(\d{1,2})$""").find(s)?.let {
            val h = it.groupValues[1].toInt()
            if (h in 0..23) return Pair(h, 0)
        }
        return null
    }

    private fun triggerForDaily(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun triggerForNextDayOfWeek(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_WEEK)
        var daysAhead = dayOfWeek - today
        if (daysAhead < 0) daysAhead += 7
        if (daysAhead == 0) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
            }
            if (candidate.timeInMillis <= System.currentTimeMillis()) daysAhead = 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        cal.set(Calendar.HOUR_OF_DAY, hour); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }

    private fun capitalize(s: String) = s.trim().replaceFirstChar { it.uppercase() }

    private fun parsePolish(lower: String): Result? {
        // "w każdy/każdą [dzień] o [godzina] [treść]" -> WEEKLY, that day
        for ((day, calDay) in plDayNames) {
            Regex("""^w\s+każd[ąy]\s+$day\s+o\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
                parseClockTime(m.groupValues[1])?.let { (h, min) ->
                    val content = m.groupValues[2]
                    if (content.isNotBlank()) return Result(triggerForNextDayOfWeek(calDay, h, min), capitalize(content), "WEEKLY")
                }
            }
        }
        // "codziennie o [godzina] [treść]" -> DAILY
        Regex("""^codziennie\s+o\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "DAILY")
            }
        }
        // "codziennie rano/wieczorem/... [treść]" -> DAILY, part of day
        for ((part, hour) in plDayParts.entries.sortedByDescending { it.key.length }) {
            Regex("""^codziennie\s+$part\s+(.+)$""").find(lower)?.let { m ->
                val content = m.groupValues[1]
                if (content.isNotBlank()) return Result(triggerForDaily(hour, 0), capitalize(content), "DAILY")
            }
        }
        // "co tydzień o [godzina] [treść]" -> WEEKLY, starting from today's weekday
        Regex("""^co\s+tydzień\s+o\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "WEEKLY")
            }
        }
        // "co 2 tygodnie" / "co dwa tygodnie" o [godzina] [treść] -> BIWEEKLY
        Regex("""^co\s+(?:2|dwa)\s+tygodnie\s+o\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "BIWEEKLY")
            }
        }
        // "co miesiąc o [godzina] [treść]" -> MONTHLY
        Regex("""^co\s+miesiąc\s+o\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "MONTHLY")
            }
        }
        return null
    }

    private fun parseEnglish(lower: String): Result? {
        // "every [day] at [time] [content]" -> WEEKLY, that day
        for ((day, calDay) in enDayNames) {
            Regex("""^every\s+$day\s+at\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
                parseClockTime(m.groupValues[1])?.let { (h, min) ->
                    val content = m.groupValues[2]
                    if (content.isNotBlank()) return Result(triggerForNextDayOfWeek(calDay, h, min), capitalize(content), "WEEKLY")
                }
            }
        }
        // "every day at [time] [content]" / "daily at [time] [content]" -> DAILY
        Regex("""^(?:every\s+day|daily)\s+at\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "DAILY")
            }
        }
        // "every morning/evening/afternoon/night [content]" -> DAILY, part of day
        for ((part, hour) in enDayParts.entries.sortedByDescending { it.key.length }) {
            Regex("""^every\s+$part\s+(.+)$""").find(lower)?.let { m ->
                val content = m.groupValues[1]
                if (content.isNotBlank()) return Result(triggerForDaily(hour, 0), capitalize(content), "DAILY")
            }
        }
        // "every week at [time] [content]" / "weekly at [time] [content]" -> WEEKLY
        Regex("""^(?:every\s+week|weekly)\s+at\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "WEEKLY")
            }
        }
        // "every 2 weeks" / "every two weeks" / "every other week" at [time] [content] -> BIWEEKLY
        Regex("""^every\s+(?:2|two)\s+weeks\s+at\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "BIWEEKLY")
            }
        }
        Regex("""^every\s+other\s+week\s+at\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "BIWEEKLY")
            }
        }
        // "every month at [time] [content]" / "monthly at [time] [content]" -> MONTHLY
        Regex("""^(?:every\s+month|monthly)\s+at\s+(\S+)\s+(.+)$""").find(lower)?.let { m ->
            parseClockTime(m.groupValues[1])?.let { (h, min) ->
                val content = m.groupValues[2]
                if (content.isNotBlank()) return Result(triggerForDaily(h, min), capitalize(content), "MONTHLY")
            }
        }
        return null
    }
}

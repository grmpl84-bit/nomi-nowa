package com.focusremind.app.speech

import java.util.Calendar

object TimeParser {

    data class Result(val triggerAt: Long, val cleanedText: String)

    // ─────────────────────────────────────────────
    // POLISH
    // ─────────────────────────────────────────────

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
        "jedenaście" to 11, "jedenascie" to 11,
        "dwanaście" to 12, "dwanascie" to 12,
        "trzynaście" to 13, "trzynascie" to 13,
        "czternaście" to 14, "czternascie" to 14,
        "piętnaście" to 15, "pietnascie" to 15,
        "szesnaście" to 16, "szesnascie" to 16,
        "siedemnaście" to 17, "siedemnascie" to 17,
        "osiemnaście" to 18, "osiemnascie" to 18,
        "dziewiętnaście" to 19, "dziewietnascie" to 19,
        "dwadzieścia" to 20, "dwadziescia" to 20, "dwudziestej" to 20,
        "dwudziesta" to 20, "dwudziesty" to 20,
        "trzydzieści" to 30, "trzydziesci" to 30, "trzydziestej" to 30,
        "czterdzieści" to 40, "czterdziesci" to 40,
        "pół" to -1, "pol" to -1, "wpół" to -1
    )

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

    private val plDaysOfWeek = mapOf(
        "w poniedziałek" to Calendar.MONDAY, "w poniedzialek" to Calendar.MONDAY,
        "poniedziałek" to Calendar.MONDAY, "poniedzialek" to Calendar.MONDAY,
        "we wtorek" to Calendar.TUESDAY, "wtorek" to Calendar.TUESDAY,
        "w środę" to Calendar.WEDNESDAY, "w srode" to Calendar.WEDNESDAY,
        "środę" to Calendar.WEDNESDAY, "srode" to Calendar.WEDNESDAY,
        "w czwartek" to Calendar.THURSDAY, "czwartek" to Calendar.THURSDAY,
        "w piątek" to Calendar.FRIDAY, "w piatek" to Calendar.FRIDAY,
        "piątek" to Calendar.FRIDAY, "piatek" to Calendar.FRIDAY,
        "w sobotę" to Calendar.SATURDAY, "w sobote" to Calendar.SATURDAY,
        "sobotę" to Calendar.SATURDAY, "sobote" to Calendar.SATURDAY,
        "w niedzielę" to Calendar.SUNDAY, "w niedziele" to Calendar.SUNDAY,
        "niedzielę" to Calendar.SUNDAY, "niedziele" to Calendar.SUNDAY
    )

    // ─────────────────────────────────────────────
    // ENGLISH
    // ─────────────────────────────────────────────

    private val enNumbers = mapOf(
        "one" to 1, "a" to 1, "an" to 1,
        "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12,
        "fifteen" to 15, "twenty" to 20, "thirty" to 30,
        "half" to -1, "quarter" to 15
    )

    private val enDaysOfWeek = mapOf(
        "next monday" to Calendar.MONDAY, "monday" to Calendar.MONDAY,
        "next tuesday" to Calendar.TUESDAY, "tuesday" to Calendar.TUESDAY,
        "next wednesday" to Calendar.WEDNESDAY, "wednesday" to Calendar.WEDNESDAY,
        "next thursday" to Calendar.THURSDAY, "thursday" to Calendar.THURSDAY,
        "next friday" to Calendar.FRIDAY, "friday" to Calendar.FRIDAY,
        "next saturday" to Calendar.SATURDAY, "saturday" to Calendar.SATURDAY,
        "next sunday" to Calendar.SUNDAY, "sunday" to Calendar.SUNDAY
    )

    // ─────────────────────────────────────────────
    // GERMAN
    // ─────────────────────────────────────────────

    private val deNumbers = mapOf(
        "eine" to 1, "einer" to 1, "einem" to 1, "eins" to 1, "ein" to 1,
        "zwei" to 2, "drei" to 3, "vier" to 4, "fünf" to 5, "funf" to 5,
        "sechs" to 6, "sieben" to 7, "acht" to 8, "neun" to 9,
        "zehn" to 10, "elf" to 11, "zwölf" to 12, "zwolf" to 12,
        "fünfzehn" to 15, "funfzehn" to 15, "viertel" to 15,
        "zwanzig" to 20, "dreißig" to 30, "dreissig" to 30,
        "halb" to -1
    )

    private val deDaysOfWeek = mapOf(
        "nächsten montag" to Calendar.MONDAY, "am montag" to Calendar.MONDAY, "montag" to Calendar.MONDAY,
        "nächsten dienstag" to Calendar.TUESDAY, "am dienstag" to Calendar.TUESDAY, "dienstag" to Calendar.TUESDAY,
        "nächsten mittwoch" to Calendar.WEDNESDAY, "am mittwoch" to Calendar.WEDNESDAY, "mittwoch" to Calendar.WEDNESDAY,
        "nächsten donnerstag" to Calendar.THURSDAY, "am donnerstag" to Calendar.THURSDAY, "donnerstag" to Calendar.THURSDAY,
        "nächsten freitag" to Calendar.FRIDAY, "am freitag" to Calendar.FRIDAY, "freitag" to Calendar.FRIDAY,
        "nächsten samstag" to Calendar.SATURDAY, "am samstag" to Calendar.SATURDAY, "samstag" to Calendar.SATURDAY,
        "nächsten sonntag" to Calendar.SUNDAY, "am sonntag" to Calendar.SUNDAY, "sonntag" to Calendar.SUNDAY
    )

    // ─────────────────────────────────────────────
    // SPANISH
    // ─────────────────────────────────────────────

    private val esNumbers = mapOf(
        "un" to 1, "una" to 1, "uno" to 1,
        "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
        "once" to 11, "doce" to 12, "quince" to 15,
        "veinte" to 20, "treinta" to 30,
        "media" to -1, "cuarto" to 15
    )

    private val esDaysOfWeek = mapOf(
        "el próximo lunes" to Calendar.MONDAY, "el lunes" to Calendar.MONDAY, "lunes" to Calendar.MONDAY,
        "el próximo martes" to Calendar.TUESDAY, "el martes" to Calendar.TUESDAY, "martes" to Calendar.TUESDAY,
        "el próximo miércoles" to Calendar.WEDNESDAY, "el miércoles" to Calendar.WEDNESDAY, "miércoles" to Calendar.WEDNESDAY,
        "el proximo miercoles" to Calendar.WEDNESDAY, "el miercoles" to Calendar.WEDNESDAY, "miercoles" to Calendar.WEDNESDAY,
        "el próximo jueves" to Calendar.THURSDAY, "el jueves" to Calendar.THURSDAY, "jueves" to Calendar.THURSDAY,
        "el próximo viernes" to Calendar.FRIDAY, "el viernes" to Calendar.FRIDAY, "viernes" to Calendar.FRIDAY,
        "el próximo sábado" to Calendar.SATURDAY, "el sábado" to Calendar.SATURDAY, "sábado" to Calendar.SATURDAY,
        "el proximo sabado" to Calendar.SATURDAY, "el sabado" to Calendar.SATURDAY, "sabado" to Calendar.SATURDAY,
        "el próximo domingo" to Calendar.SUNDAY, "el domingo" to Calendar.SUNDAY, "domingo" to Calendar.SUNDAY
    )

    private val esMonths = mapOf(
        "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4,
        "mayo" to 5, "junio" to 6, "julio" to 7, "agosto" to 8,
        "septiembre" to 9, "octubre" to 10, "noviembre" to 11, "diciembre" to 12
    )

    // ─────────────────────────────────────────────
    // FRENCH
    // ─────────────────────────────────────────────

    private val frNumbers = mapOf(
        "un" to 1, "une" to 1,
        "deux" to 2, "trois" to 3, "quatre" to 4, "cinq" to 5,
        "six" to 6, "sept" to 7, "huit" to 8, "neuf" to 9, "dix" to 10,
        "onze" to 11, "douze" to 12, "quinze" to 15,
        "vingt" to 20, "trente" to 30,
        "demi" to -1, "demie" to -1, "quart" to 15
    )

    private val frDaysOfWeek = mapOf(
        "lundi prochain" to Calendar.MONDAY, "le lundi" to Calendar.MONDAY, "lundi" to Calendar.MONDAY,
        "mardi prochain" to Calendar.TUESDAY, "le mardi" to Calendar.TUESDAY, "mardi" to Calendar.TUESDAY,
        "mercredi prochain" to Calendar.WEDNESDAY, "le mercredi" to Calendar.WEDNESDAY, "mercredi" to Calendar.WEDNESDAY,
        "jeudi prochain" to Calendar.THURSDAY, "le jeudi" to Calendar.THURSDAY, "jeudi" to Calendar.THURSDAY,
        "vendredi prochain" to Calendar.FRIDAY, "le vendredi" to Calendar.FRIDAY, "vendredi" to Calendar.FRIDAY,
        "samedi prochain" to Calendar.SATURDAY, "le samedi" to Calendar.SATURDAY, "samedi" to Calendar.SATURDAY,
        "dimanche prochain" to Calendar.SUNDAY, "le dimanche" to Calendar.SUNDAY, "dimanche" to Calendar.SUNDAY
    )

    private val frMonths = mapOf(
        "janvier" to 1, "février" to 2, "fevrier" to 2, "mars" to 3, "avril" to 4,
        "mai" to 5, "juin" to 6, "juillet" to 7, "août" to 8, "aout" to 8,
        "septembre" to 9, "octobre" to 10, "novembre" to 11, "décembre" to 12, "decembre" to 12
    )

    // ─────────────────────────────────────────────
    // ITALIAN
    // ─────────────────────────────────────────────

    private val itNumbers = mapOf(
        "un" to 1, "uno" to 1, "una" to 1,
        "due" to 2, "tre" to 3, "quattro" to 4, "cinque" to 5,
        "sei" to 6, "sette" to 7, "otto" to 8, "nove" to 9, "dieci" to 10,
        "undici" to 11, "dodici" to 12, "quindici" to 15,
        "venti" to 20, "trenta" to 30,
        "mezza" to -1, "mezzo" to -1, "quarto" to 15
    )

    private val itDaysOfWeek = mapOf(
        "il prossimo lunedì" to Calendar.MONDAY, "il prossimo lunedi" to Calendar.MONDAY,
        "lunedì" to Calendar.MONDAY, "lunedi" to Calendar.MONDAY,
        "il prossimo martedì" to Calendar.TUESDAY, "il prossimo martedi" to Calendar.TUESDAY,
        "martedì" to Calendar.TUESDAY, "martedi" to Calendar.TUESDAY,
        "il prossimo mercoledì" to Calendar.WEDNESDAY, "il prossimo mercoledi" to Calendar.WEDNESDAY,
        "mercoledì" to Calendar.WEDNESDAY, "mercoledi" to Calendar.WEDNESDAY,
        "il prossimo giovedì" to Calendar.THURSDAY, "il prossimo giovedi" to Calendar.THURSDAY,
        "giovedì" to Calendar.THURSDAY, "giovedi" to Calendar.THURSDAY,
        "il prossimo venerdì" to Calendar.FRIDAY, "il prossimo venerdi" to Calendar.FRIDAY,
        "venerdì" to Calendar.FRIDAY, "venerdi" to Calendar.FRIDAY,
        "il prossimo sabato" to Calendar.SATURDAY, "sabato" to Calendar.SATURDAY,
        "la prossima domenica" to Calendar.SUNDAY, "domenica" to Calendar.SUNDAY
    )

    private val itMonths = mapOf(
        "gennaio" to 1, "febbraio" to 2, "marzo" to 3, "aprile" to 4,
        "maggio" to 5, "giugno" to 6, "luglio" to 7, "agosto" to 8,
        "settembre" to 9, "ottobre" to 10, "novembre" to 11, "dicembre" to 12
    )

    // ─────────────────────────────────────────────
    // RUSSIAN
    // ─────────────────────────────────────────────

    private val ruNumbers = mapOf(
        "одну" to 1, "одна" to 1, "один" to 1,
        "две" to 2, "два" to 2,
        "три" to 3, "четыре" to 4, "пять" to 5,
        "шесть" to 6, "семь" to 7, "восемь" to 8, "девять" to 9, "десять" to 10,
        "одиннадцать" to 11, "двенадцать" to 12, "пятнадцать" to 15,
        "двадцать" to 20, "тридцать" to 30
    )

    private val ruDaysOfWeek = mapOf(
        "в следующий понедельник" to Calendar.MONDAY, "в понедельник" to Calendar.MONDAY, "понедельник" to Calendar.MONDAY,
        "в следующий вторник" to Calendar.TUESDAY, "во вторник" to Calendar.TUESDAY, "вторник" to Calendar.TUESDAY,
        "в следующую среду" to Calendar.WEDNESDAY, "в среду" to Calendar.WEDNESDAY, "среда" to Calendar.WEDNESDAY,
        "в следующий четверг" to Calendar.THURSDAY, "в четверг" to Calendar.THURSDAY, "четверг" to Calendar.THURSDAY,
        "в следующую пятницу" to Calendar.FRIDAY, "в пятницу" to Calendar.FRIDAY, "пятница" to Calendar.FRIDAY,
        "в следующую субботу" to Calendar.SATURDAY, "в субботу" to Calendar.SATURDAY, "суббота" to Calendar.SATURDAY,
        "в следующее воскресенье" to Calendar.SUNDAY, "в воскресенье" to Calendar.SUNDAY, "воскресенье" to Calendar.SUNDAY
    )

    private val ruMonths = mapOf(
        "января" to 1, "февраля" to 2, "марта" to 3, "апреля" to 4,
        "мая" to 5, "июня" to 6, "июля" to 7, "августа" to 8,
        "сентября" to 9, "октября" to 10, "ноября" to 11, "декабря" to 12
    )

    // ─────────────────────────────────────────────
    // SHARED HELPERS
    // ─────────────────────────────────────────────

    private fun extractNumber(text: String): Int? {
        Regex("""(\d+)""").find(text)?.let { return it.groupValues[1].toInt() }
        for ((word, num) in plNumbers + enNumbers + deNumbers + esNumbers + frNumbers + itNumbers + ruNumbers) {
            if (text.contains(word)) return num
        }
        return null
    }

    /**
     * Extract both hour AND minute from a time string.
     * Handles: "17:30", "17.30", "17h30" (French), "17 30", "5pm", "5:30am", bare "17".
     */
    private fun extractTime(text: String): Pair<Int, Int>? {
        // AM/PM: "5:30pm", "5pm", "5:30am", "5am"
        Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)""").find(text)?.let {
            var h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toIntOrNull() ?: 0
            val ampm = it.groupValues[3]
            if (ampm == "pm" && h < 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            if (h in 0..23 && m in 0..59) return Pair(h, m)
        }
        // French "17h30" / "9h" format
        Regex("""(\d{1,2})h(\d{2})""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) return Pair(h, m)
        }
        Regex("""(\d{1,2})h\b""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            if (h in 0..23) return Pair(h, 0)
        }
        // Standard "17:30", "17.30"
        Regex("""(\d{1,2})[:\.](\d{2})""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) return Pair(h, m)
        }
        // Space separator "17 30" — only if exactly 2 digits follow
        Regex("""(\d{1,2})\s+(\d{2})\b""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) return Pair(h, m)
        }
        // Polish ordinal hours: "dwudziestej", "ósmej" etc.
        for ((word, hour) in plOrdinalHours.entries.sortedByDescending { it.key.length }) {
            if (text.contains(word)) {
                val afterOrdinal = text.substringAfter(word).trim()
                val minute = extractMinuteFromText(afterOrdinal)
                return Pair(hour, minute)
            }
        }
        // Bare hour digit
        Regex("""(\d{1,2})""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            if (h in 0..23) return Pair(h, 0)
        }
        // Word numbers for hour
        for ((word, num) in plNumbers + enNumbers + deNumbers) {
            if (num in 1..23 && text.contains(word)) return Pair(num, 0)
        }
        return null
    }

    private fun extractHour(text: String): Int? = extractTime(text)?.first

    private fun extractMinuteFromText(text: String): Int {
        Regex("""(\d{1,2})""").find(text)?.let {
            val m = it.groupValues[1].toInt()
            if (m in 0..59) return m
        }
        for ((word, num) in plNumbers + enNumbers + deNumbers) {
            if (num in 1..59 && text.contains(word)) return num
        }
        return 0
    }

    private fun extractMinute(text: String): Int = extractMinuteFromText(text)

    private fun findMonth(text: String, months: Map<String, Int>): Pair<Int, String>? {
        for ((word, month) in months.entries.sortedByDescending { it.key.length }) {
            if (text.contains(word)) return Pair(month, word)
        }
        return null
    }

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

    /** Build a Calendar for a specific date+time, advancing year if past. */
    private fun calendarForDate(month: Int, day: Int, hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.YEAR, 1)
        }
    }

    /** Apply time from "o/at/à/alle/в X:XX" substring to a Calendar. */
    private fun applyTimeFromText(cal: Calendar, timeText: String) {
        extractTime(timeText)?.let { (h, m) ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
        }
    }

    /** Handle day-of-week + optional time pattern for any language. */
    private fun parseDayOfWeek(
        text: String,
        daysMap: Map<String, Int>,
        timePrefix: Regex
    ): Result? {
        for ((dayName, dayOfWeek) in daysMap.entries.sortedByDescending { it.key.length }) {
            if (text.contains(dayName)) {
                val cal = getNextDayOfWeek(dayOfWeek)
                val afterDay = text.substringAfter(dayName)
                timePrefix.find(afterDay)?.let { applyTimeFromText(cal, it.groupValues[1]) }
                val cleaned = text.replace(dayName, "")
                    .replace(timePrefix, "").trim()
                return Result(cal.timeInMillis, cleaned)
            }
        }
        return null
    }

    // ─────────────────────────────────────────────
    // MAIN PARSE
    // ─────────────────────────────────────────────

    fun parse(input: String): Result? {
        val text = input.lowercase().trim()

        // ══════════════════════════════════════════
        // POLISH
        // ══════════════════════════════════════════

        // Date + month: "11 sierpnia o 20:30"
        findMonth(text, plMonths)?.let { (month, monthWord) ->
            val beforeMonth = text.substringBefore(monthWord).trim()
            val day = Regex("""(\d{1,2})""").find(beforeMonth)?.groupValues?.get(1)?.toInt()
                ?: extractNumber(beforeMonth)
            if (day != null && day in 1..31) {
                var hour = 9; var minute = 0
                val afterMonth = text.substringAfter(monthWord)
                Regex("""o\s+((?:\S+\s+){0,1}\S+)""").find(afterMonth)?.let { m ->
                    extractTime(m.groupValues[1])?.let { (h, min) -> hour = h; minute = min }
                }
                val cal = calendarForDate(month, day, hour, minute)
                val cleaned = text.replace(Regex("""\d+\s*$monthWord"""), "")
                    .replace(Regex("""o\s+(?:\S+\s+){0,1}\S+"""), "").replace(monthWord, "").trim()
                return Result(cal.timeInMillis, cleaned)
            }
        }

        // Day of week PL: "w poniedziałek o 15:30"
        parseDayOfWeek(text, plDaysOfWeek, Regex("""o\s+((?:\S+\s+){0,1}\S+)"""))?.let { return it }

        // "za X minut/minutę/minuty"
        Regex("""za\s+(.+?)\s*min(?:ut[ęy]?[ęe]?)?""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) return Result(System.currentTimeMillis() + num * 60_000L, text.replace(it.value, "").trim())
        }
        if (text.contains("za minutę") || text.contains("za minute") || text.contains("za minutke") || text.contains("za minutkę")) {
            return Result(System.currentTimeMillis() + 60_000, text.replace(Regex("za minut[ęe]|za minutk[ęe]"), "").trim())
        }
        if (text.contains("za pół godziny") || text.contains("za pol godziny")) {
            return Result(System.currentTimeMillis() + 30 * 60_000L, text.replace(Regex("za p[oó]ł godziny"), "").trim())
        }
        if (text.contains("za kwadrans")) {
            return Result(System.currentTimeMillis() + 15 * 60_000L, text.replace("za kwadrans", "").trim())
        }
        if (text.contains("za godzinę") || text.contains("za godzine")) {
            return Result(System.currentTimeMillis() + 3_600_000L, text.replace(Regex("za godzin[ęe]"), "").trim())
        }
        Regex("""za\s+(.+?)\s*godzin[ęy]?""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) return Result(System.currentTimeMillis() + num * 3_600_000L, text.replace(it.value, "").trim())
        }
        Regex("""za\s+(.+?)\s*(?:dni|dzień|dzie[nń])""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, num); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        Regex("""za\s+(.+?)\s*sekund""").find(text)?.let {
            val num = extractNumber(it.groupValues[1])
            if (num != null && num > 0) return Result(System.currentTimeMillis() + num * 1000L, text.replace(it.value, "").trim())
        }

        // "jutro o 17:30"
        Regex("""jutro\s+o\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        if (text.contains("jutro") && !text.contains(" o ")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("jutro", "").trim())
        }
        if (text.contains("pojutrze")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("pojutrze", "").trim())
        }

        // "o dwudziestej [trzydzieści]"
        for ((ordinal, hour) in plOrdinalHours.entries.sortedByDescending { it.key.length }) {
            if (text.contains("o $ordinal")) {
                val afterOrdinal = text.substringAfter("o $ordinal").trim()
                val minute = extractMinuteFromText(afterOrdinal)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                return Result(cal.timeInMillis, text.replace("o $ordinal", "").trim())
            }
        }

        // "o 17:30" / "o 17.30" / "o 17 30" / "o 17"
        Regex("""o\s+(\d[\d:.\s]*)""").find(text)?.let {
            extractTime(it.groupValues[1].trim())?.let { (h, m) ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // ══════════════════════════════════════════
        // ENGLISH
        // ══════════════════════════════════════════

        Regex("""in\s+(\d+)\s*min(?:ute)?s?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 60_000, text.replace(it.value, "").trim())
        }
        Regex("""in\s+(\d+)\s*hours?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 3_600_000, text.replace(it.value, "").trim())
        }
        Regex("""in\s+(\d+)\s*days?""").find(text)?.let {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it.groupValues[1].toInt()); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }
        if (text.contains("in half an hour") || text.contains("in a half hour")) {
            return Result(System.currentTimeMillis() + 30 * 60_000L, text.replace(Regex("in (a )?half (an )?hour"), "").trim())
        }
        if (text.contains("in a quarter") || text.contains("in quarter of an hour")) {
            return Result(System.currentTimeMillis() + 15 * 60_000L, text.replace(Regex("in (a )?quarter.*hour"), "").trim())
        }
        if (text.contains("in an hour")) {
            return Result(System.currentTimeMillis() + 3_600_000L, text.replace("in an hour", "").trim())
        }

        // Day of week EN: "monday at 15:30" / "next friday at 9am"
        parseDayOfWeek(text, enDaysOfWeek, Regex("""(?:at\s+)((?:\S+\s+){0,1}\S+)"""))?.let { return it }

        // "tonight at 9:30" / "this evening at 9"
        if (text.contains("tonight") || text.contains("this evening")) {
            val trigger = if (text.contains("tonight")) "tonight" else "this evening"
            Regex("""(?:at\s+)?(\d[\d:.h]*)""").find(text.substringAfter(trigger))?.let {
                extractTime(it.groupValues[1])?.let { (h, m) ->
                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                    return Result(cal.timeInMillis, text.replace(trigger, "").replace(Regex("at\\s+(?:\\S+\\s+){0,1}\\S+"), "").trim())
                }
            }
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(trigger, "").trim())
        }

        // "tomorrow at 9:30"
        Regex("""tomorrow\s+(?:at\s+)?((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        if (text.contains("tomorrow")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("tomorrow", "").trim())
        }
        if (text.contains("day after tomorrow")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("day after tomorrow", "").trim())
        }

        // "at 17:30" / "at 5pm"
        Regex("""at\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // ══════════════════════════════════════════
        // GERMAN
        // ══════════════════════════════════════════

        Regex("""in\s+(\d+)\s*minuten?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 60_000, text.replace(it.value, "").trim())
        }
        Regex("""in\s+(\d+)\s*stunden?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 3_600_000, text.replace(it.value, "").trim())
        }
        if (text.contains("in einer viertelstunde") || text.contains("in einem viertel stunde")) {
            return Result(System.currentTimeMillis() + 15 * 60_000L, text.replace(Regex("in einer? viertel ?stunde"), "").trim())
        }
        if (text.contains("halben stunde") || text.contains("halbe stunde")) {
            return Result(System.currentTimeMillis() + 30 * 60_000L, text.replace(Regex("in einer? halben? stunde"), "").trim())
        }
        if (text.contains("in einer stunde")) {
            return Result(System.currentTimeMillis() + 3_600_000L, text.replace("in einer stunde", "").trim())
        }
        Regex("""in\s+(\d+)\s*tagen?""").find(text)?.let {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it.groupValues[1].toInt()); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }

        // Day of week DE: "am montag um 9:30"
        parseDayOfWeek(text, deDaysOfWeek, Regex("""um\s+((?:\S+\s+){0,1}\S+)"""))?.let { return it }

        // "heute abend um 20:30"
        if (text.contains("heute abend") || text.contains("heute nacht")) {
            val trigger = if (text.contains("heute abend")) "heute abend" else "heute nacht"
            Regex("""um\s+((?:\S+\s+){0,1}\S+)""").find(text.substringAfter(trigger))?.let {
                extractTime(it.groupValues[1])?.let { (h, m) ->
                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                    return Result(cal.timeInMillis, text.replace(trigger, "").replace(Regex("um\\s+(?:\\S+\\s+){0,1}\\S+"), "").trim())
                }
            }
        }

        // "morgen um 9:30"
        Regex("""morgen\s+(?:um\s+)?((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        if (text.contains("morgen") && !text.contains(" um ")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("morgen", "").trim())
        }
        if (text.contains("übermorgen") || text.contains("ubermorgen")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(Regex("[üu]bermorgen"), "").trim())
        }

        // "um 17:30 Uhr" / "um 5"
        Regex("""um\s+(.+?)(?:\s*uhr)?$""").find(text)?.let {
            extractTime(it.groupValues[1].trim())?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // ══════════════════════════════════════════
        // SPANISH
        // ══════════════════════════════════════════

        // "en 5 minutos"
        Regex("""en\s+(\d+)\s*minutos?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 60_000, text.replace(it.value, "").trim())
        }
        // "en 2 horas"
        Regex("""en\s+(\d+)\s*horas?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 3_600_000, text.replace(it.value, "").trim())
        }
        // "en 3 días"
        Regex("""en\s+(\d+)\s*d[íi]as?""").find(text)?.let {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it.groupValues[1].toInt()); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }
        // "dentro de media hora" / "en media hora"
        if (text.contains("media hora")) {
            return Result(System.currentTimeMillis() + 30 * 60_000L, text.replace(Regex("(en|dentro de) media hora"), "").trim())
        }
        // "en un cuarto de hora"
        if (text.contains("cuarto de hora")) {
            return Result(System.currentTimeMillis() + 15 * 60_000L, text.replace(Regex("(en|dentro de) un cuarto de hora"), "").trim())
        }
        // "en una hora"
        if (text.contains("en una hora")) {
            return Result(System.currentTimeMillis() + 3_600_000L, text.replace("en una hora", "").trim())
        }

        // Date + month ES: "11 de agosto a las 20:30"
        findMonth(text, esMonths)?.let { (month, monthWord) ->
            val beforeMonth = text.substringBefore(monthWord).trim()
            val day = Regex("""(\d{1,2})""").find(beforeMonth)?.groupValues?.get(1)?.toInt()
            if (day != null && day in 1..31) {
                var hour = 9; var minute = 0
                val afterMonth = text.substringAfter(monthWord)
                Regex("""a las\s+((?:\S+\s+){0,1}\S+)""").find(afterMonth)?.let { m ->
                    extractTime(m.groupValues[1])?.let { (h, min) -> hour = h; minute = min }
                }
                val cal = calendarForDate(month, day, hour, minute)
                return Result(cal.timeInMillis, text.replace(Regex("""\d+\s*(de\s*)?$monthWord"""), "").replace(Regex("""a las\s+(?:\S+\s+){0,1}\S+"""), "").trim())
            }
        }

        // Day of week ES: "el lunes a las 15:30"
        parseDayOfWeek(text, esDaysOfWeek, Regex("""a las\s+((?:\S+\s+){0,1}\S+)"""))?.let { return it }

        // "esta tarde a las 7" / "esta noche a las 9"
        if (text.contains("esta tarde") || text.contains("esta noche")) {
            val trigger = if (text.contains("esta tarde")) "esta tarde" else "esta noche"
            val defaultHour = if (text.contains("esta tarde")) 18 else 21
            Regex("""a las\s+((?:\S+\s+){0,1}\S+)""").find(text.substringAfter(trigger))?.let {
                extractTime(it.groupValues[1])?.let { (h, m) ->
                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                    return Result(cal.timeInMillis, text.replace(trigger, "").replace(Regex("a las\\s+(?:\\S+\\s+){0,1}\\S+"), "").trim())
                }
            }
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, defaultHour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(trigger, "").trim())
        }

        // "mañana a las 9:30"
        Regex("""ma[ñn]ana\s+a las\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        if (text.contains("mañana") || text.contains("manana")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(Regex("ma[ñn]ana"), "").trim())
        }
        if (text.contains("pasado mañana") || text.contains("pasado manana")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(Regex("pasado ma[ñn]ana"), "").trim())
        }

        // "a las 17:30"
        Regex("""a las\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // ══════════════════════════════════════════
        // FRENCH
        // ══════════════════════════════════════════

        // "dans 5 minutes"
        Regex("""dans\s+(\d+)\s*minutes?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 60_000, text.replace(it.value, "").trim())
        }
        // "dans 2 heures"
        Regex("""dans\s+(\d+)\s*heures?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 3_600_000, text.replace(it.value, "").trim())
        }
        // "dans 3 jours"
        Regex("""dans\s+(\d+)\s*jours?""").find(text)?.let {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it.groupValues[1].toInt()); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }
        // "dans une demi-heure" / "dans une demi heure"
        if (text.contains("demi-heure") || text.contains("demi heure")) {
            return Result(System.currentTimeMillis() + 30 * 60_000L, text.replace(Regex("dans une demi[- ]heure"), "").trim())
        }
        // "dans un quart d'heure"
        if (text.contains("quart d") && text.contains("heure")) {
            return Result(System.currentTimeMillis() + 15 * 60_000L, text.replace(Regex("dans un quart d.heure"), "").trim())
        }
        // "dans une heure"
        if (text.contains("dans une heure")) {
            return Result(System.currentTimeMillis() + 3_600_000L, text.replace("dans une heure", "").trim())
        }

        // Date + month FR: "11 août à 20h30"
        findMonth(text, frMonths)?.let { (month, monthWord) ->
            val beforeMonth = text.substringBefore(monthWord).trim()
            val day = Regex("""(\d{1,2})""").find(beforeMonth)?.groupValues?.get(1)?.toInt()
            if (day != null && day in 1..31) {
                var hour = 9; var minute = 0
                val afterMonth = text.substringAfter(monthWord)
                Regex("""[àa]\s+((?:\S+\s+){0,1}\S+)""").find(afterMonth)?.let { m ->
                    extractTime(m.groupValues[1])?.let { (h, min) -> hour = h; minute = min }
                }
                val cal = calendarForDate(month, day, hour, minute)
                return Result(cal.timeInMillis, text.replace(Regex("""\d+\s*(le\s*)?$monthWord"""), "").replace(Regex("""[àa]\s+(?:\S+\s+){0,1}\S+"""), "").trim())
            }
        }

        // Day of week FR: "lundi à 15h30"
        parseDayOfWeek(text, frDaysOfWeek, Regex("""[àa]\s+((?:\S+\s+){0,1}\S+)"""))?.let { return it }

        // "ce soir à 21h" / "cet après-midi à 17h"
        if (text.contains("ce soir") || text.contains("cette nuit")) {
            val trigger = if (text.contains("ce soir")) "ce soir" else "cette nuit"
            Regex("""[àa]\s+((?:\S+\s+){0,1}\S+)""").find(text.substringAfter(trigger))?.let {
                extractTime(it.groupValues[1])?.let { (h, m) ->
                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                    return Result(cal.timeInMillis, text.replace(trigger, "").replace(Regex("[àa]\\s+(?:\\S+\\s+){0,1}\\S+"), "").trim())
                }
            }
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(trigger, "").trim())
        }

        // "demain à 9h30"
        Regex("""demain\s+[àa]\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        if (text.contains("demain")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("demain", "").trim())
        }
        if (text.contains("après-demain") || text.contains("apres-demain")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(Regex("apr[eè]s-demain"), "").trim())
        }

        // "à 17h30" / "à 17:30"
        Regex("""[àa]\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // ══════════════════════════════════════════
        // ITALIAN
        // ══════════════════════════════════════════

        // "tra 5 minuti" / "fra 5 minuti"
        Regex("""(?:tra|fra)\s+(\d+)\s*minuti?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 60_000, text.replace(it.value, "").trim())
        }
        // "tra 2 ore"
        Regex("""(?:tra|fra)\s+(\d+)\s*ore?""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 3_600_000, text.replace(it.value, "").trim())
        }
        // "tra 3 giorni"
        Regex("""(?:tra|fra)\s+(\d+)\s*giorni?""").find(text)?.let {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it.groupValues[1].toInt()); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }
        // "tra mezz'ora" / "tra mezzora"
        if (text.contains("mezz") && text.contains("ora")) {
            return Result(System.currentTimeMillis() + 30 * 60_000L, text.replace(Regex("(?:tra|fra) mezz.?ora"), "").trim())
        }
        // "tra un quarto d'ora"
        if (text.contains("quarto d") && text.contains("ora")) {
            return Result(System.currentTimeMillis() + 15 * 60_000L, text.replace(Regex("(?:tra|fra) un quarto d.ora"), "").trim())
        }
        // "tra un'ora"
        if (text.contains("tra un'ora") || text.contains("fra un'ora") || text.contains("tra un ora")) {
            return Result(System.currentTimeMillis() + 3_600_000L, text.replace(Regex("(?:tra|fra) un.?ora"), "").trim())
        }

        // Date + month IT: "11 agosto alle 20:30"
        findMonth(text, itMonths)?.let { (month, monthWord) ->
            val beforeMonth = text.substringBefore(monthWord).trim()
            val day = Regex("""(\d{1,2})""").find(beforeMonth)?.groupValues?.get(1)?.toInt()
            if (day != null && day in 1..31) {
                var hour = 9; var minute = 0
                val afterMonth = text.substringAfter(monthWord)
                Regex("""alle\s+((?:\S+\s+){0,1}\S+)""").find(afterMonth)?.let { m ->
                    extractTime(m.groupValues[1])?.let { (h, min) -> hour = h; minute = min }
                }
                val cal = calendarForDate(month, day, hour, minute)
                return Result(cal.timeInMillis, text.replace(Regex("""\d+\s*(di\s*)?$monthWord"""), "").replace(Regex("""alle\s+(?:\S+\s+){0,1}\S+"""), "").trim())
            }
        }

        // Day of week IT: "lunedì alle 15:30"
        parseDayOfWeek(text, itDaysOfWeek, Regex("""alle\s+((?:\S+\s+){0,1}\S+)"""))?.let { return it }

        // "stasera alle 21:30" / "stanotte alle 23"
        if (text.contains("stasera") || text.contains("stanotte")) {
            val trigger = if (text.contains("stasera")) "stasera" else "stanotte"
            Regex("""alle\s+((?:\S+\s+){0,1}\S+)""").find(text.substringAfter(trigger))?.let {
                extractTime(it.groupValues[1])?.let { (h, m) ->
                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                    return Result(cal.timeInMillis, text.replace(trigger, "").replace(Regex("alle\\s+(?:\\S+\\s+){0,1}\\S+"), "").trim())
                }
            }
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(trigger, "").trim())
        }

        // "domani alle 9:30"
        Regex("""domani\s+(?:alle\s+)?((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        if (text.contains("domani")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("domani", "").trim())
        }
        if (text.contains("dopodomani")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("dopodomani", "").trim())
        }

        // "alle 17:30"
        Regex("""alle\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // ══════════════════════════════════════════
        // RUSSIAN
        // ══════════════════════════════════════════

        // "через 5 минут"
        Regex("""через\s+(\d+)\s*минут""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 60_000, text.replace(it.value, "").trim())
        }
        // "через 2 часа" / "через час"
        Regex("""через\s+(\d+)\s*час""").find(text)?.let {
            return Result(System.currentTimeMillis() + it.groupValues[1].toLong() * 3_600_000, text.replace(it.value, "").trim())
        }
        if (text.contains("через час")) {
            return Result(System.currentTimeMillis() + 3_600_000L, text.replace("через час", "").trim())
        }
        // "через 3 дня"
        Regex("""через\s+(\d+)\s*дн""").find(text)?.let {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, it.groupValues[1].toInt()); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace(it.value, "").trim())
        }
        // "через полчаса"
        if (text.contains("через полчаса") || text.contains("через пол часа")) {
            return Result(System.currentTimeMillis() + 30 * 60_000L, text.replace(Regex("через полчаса|через пол часа"), "").trim())
        }
        // "через четверть часа"
        if (text.contains("через четверть")) {
            return Result(System.currentTimeMillis() + 15 * 60_000L, text.replace(Regex("через четверть.*"), "").trim())
        }

        // Date + month RU: "11 августа в 20:30"
        findMonth(text, ruMonths)?.let { (month, monthWord) ->
            val beforeMonth = text.substringBefore(monthWord).trim()
            val day = Regex("""(\d{1,2})""").find(beforeMonth)?.groupValues?.get(1)?.toInt()
            if (day != null && day in 1..31) {
                var hour = 9; var minute = 0
                val afterMonth = text.substringAfter(monthWord)
                Regex("""в\s+((?:\S+\s+){0,1}\S+)""").find(afterMonth)?.let { m ->
                    extractTime(m.groupValues[1])?.let { (h, min) -> hour = h; minute = min }
                }
                val cal = calendarForDate(month, day, hour, minute)
                return Result(cal.timeInMillis, text.replace(Regex("""\d+\s*$monthWord"""), "").replace(Regex("""в\s+(?:\S+\s+){0,1}\S+"""), "").trim())
            }
        }

        // Day of week RU: "в понедельник в 15:30"
        parseDayOfWeek(text, ruDaysOfWeek, Regex("""в\s+(\d[\d:.\s]*)"""))?.let { return it }

        // "сегодня вечером в 21:30"
        if (text.contains("сегодня вечером") || text.contains("вечером")) {
            Regex("""в\s+(\d[\d:.\s]*)""").find(text)?.let {
                extractTime(it.groupValues[1])?.let { (h, m) ->
                    val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                    return Result(cal.timeInMillis, text.replace(Regex("(сегодня )?вечером"), "").replace(Regex("в\\s+(?:\\S+\\s+){0,1}\\S+"), "").trim())
                }
            }
        }

        // "завтра в 9:30"
        Regex("""завтра\s+в\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        if (text.contains("завтра")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("завтра", "").trim())
        }
        if (text.contains("послезавтра")) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
            return Result(cal.timeInMillis, text.replace("послезавтра", "").trim())
        }

        // "в 17:30"
        Regex("""в\s+((?:\S+\s+){0,1}\S+)""").find(text)?.let {
            extractTime(it.groupValues[1])?.let { (h, m) ->
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        // ══════════════════════════════════════════
        // FALLBACK: bare "17:30" or "17.30" (no language prefix)
        // Requires separator to avoid matching plain numbers by accident
        // ══════════════════════════════════════════
        Regex("""(\d{1,2})[:\.](\d{2})""").find(text)?.let {
            val h = it.groupValues[1].toInt()
            val m = it.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }
        // "5pm" / "9am" standalone
        Regex("""(\d{1,2})\s*(am|pm)""").find(text)?.let {
            var h = it.groupValues[1].toInt()
            val ampm = it.groupValues[2]
            if (ampm == "pm" && h < 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            if (h in 0..23) {
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                return Result(cal.timeInMillis, text.replace(it.value, "").trim())
            }
        }

        return null
    }
}

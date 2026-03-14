package com.family.grandhelper.util

import java.util.Calendar

data class TimeParseResult(
    val hour: Int,
    val minute: Int,
    val calendar: Calendar,
    val displayText: String
)

object TimeParser {

    private val koreanNumbers = mapOf(
        "한" to 1, "두" to 2, "세" to 3, "네" to 4, "다섯" to 5,
        "여섯" to 6, "일곱" to 7, "여덟" to 8, "아홉" to 9, "열" to 10,
        "열한" to 11, "열두" to 12
    )

    fun parse(transcript: String): TimeParseResult {
        val now = Calendar.getInstance()

        // 1. Check relative time ("N분 뒤", "N시간 뒤")
        parseRelativeTime(transcript, now)?.let { return it }

        // 2. Parse absolute time
        val dayOffset = parseDayOffset(transcript)
        val hour = parseHour(transcript)
        val minute = parseMinute(transcript)
        val period = parsePeriod(transcript)

        if (hour == null) {
            // No time found, default to a reasonable alarm
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, maxOf(dayOffset, 1))
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            return TimeParseResult(7, 0, cal, formatDisplay(cal))
        }

        // Apply AM/PM based on period
        val resolvedHour = resolveHour(hour, period)
        val resolvedMinute = minute ?: 0

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, resolvedHour)
            set(Calendar.MINUTE, resolvedMinute)
            set(Calendar.SECOND, 0)
        }

        // If the time has already passed today and no day was specified, advance to tomorrow
        if (dayOffset == 0 && cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return TimeParseResult(resolvedHour, resolvedMinute, cal, formatDisplay(cal))
    }

    private fun parseRelativeTime(transcript: String, now: Calendar): TimeParseResult? {
        // "N분 뒤/후"
        val minutePattern = Regex("(\\d+|[가-힣]+)\\s*분\\s*(뒤|후)")
        minutePattern.find(transcript)?.let { match ->
            val amount = parseNumber(match.groupValues[1]) ?: return null
            val cal = (now.clone() as Calendar).apply {
                add(Calendar.MINUTE, amount)
            }
            return TimeParseResult(
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal,
                "${amount}분 후"
            )
        }

        // "N시간 뒤/후"
        val hourPattern = Regex("(\\d+|[가-힣]+)\\s*시간\\s*(뒤|후)")
        hourPattern.find(transcript)?.let { match ->
            val amount = parseNumber(match.groupValues[1]) ?: return null
            val cal = (now.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, amount)
            }
            return TimeParseResult(
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal,
                "${amount}시간 후"
            )
        }

        return null
    }

    private fun parseDayOffset(transcript: String): Int {
        // "N일 뒤/후" 패턴
        val dayPattern = Regex("(\\d+|[가-힣]+)\\s*일\\s*(뒤|후)")
        dayPattern.find(transcript)?.let { match ->
            parseNumber(match.groupValues[1])?.let { return it }
        }

        return when {
            transcript.contains("글피") -> 3
            transcript.contains("사흘") -> 3
            transcript.contains("모레") -> 2
            transcript.contains("이틀") -> 2
            transcript.contains("내일") -> 1
            transcript.contains("오늘") -> 0
            else -> 0
        }
    }

    private fun parseHour(transcript: String): Int? {
        // "N시" pattern with Arabic numerals
        val digitPattern = Regex("(\\d{1,2})\\s*시")
        digitPattern.find(transcript)?.let {
            return it.groupValues[1].toIntOrNull()
        }

        // Korean number + 시 ("한 시", "두 시", etc.)
        for ((word, num) in koreanNumbers) {
            if (transcript.contains("${word}시") || transcript.contains("${word} 시")) {
                return num
            }
        }

        return null
    }

    private fun parseMinute(transcript: String): Int? {
        // "반" = 30분
        if (Regex("시\\s*반").containsMatchIn(transcript)) {
            return 30
        }

        // "N분" but not "N분 뒤/후" (relative time)
        val minutePattern = Regex("(\\d{1,2})\\s*분(?!\\s*(뒤|후))")
        minutePattern.find(transcript)?.let {
            return it.groupValues[1].toIntOrNull()
        }

        return null
    }

    private fun parsePeriod(transcript: String): DayPeriod {
        return when {
            transcript.contains("새벽") -> DayPeriod.DAWN       // 3-5
            transcript.contains("아침") -> DayPeriod.MORNING    // 6-9
            transcript.contains("오전") -> DayPeriod.AM         // AM
            transcript.contains("점심") -> DayPeriod.NOON       // 12
            transcript.contains("오후") -> DayPeriod.PM         // PM
            transcript.contains("저녁") -> DayPeriod.EVENING    // 18-21
            transcript.contains("밤") -> DayPeriod.NIGHT        // 21-23
            else -> DayPeriod.UNSPECIFIED
        }
    }

    private fun resolveHour(hour: Int, period: DayPeriod): Int {
        // If hour is already in 24h format (13-23), return as-is
        if (hour >= 13) return hour

        return when (period) {
            DayPeriod.DAWN -> hour                  // 새벽 3시 = 3
            DayPeriod.MORNING -> hour               // 아침 6시 = 6
            DayPeriod.AM -> hour                    // 오전 = as-is
            DayPeriod.NOON -> if (hour == 12) 12 else hour + 12
            DayPeriod.PM -> if (hour == 12) 12 else hour + 12
            DayPeriod.EVENING -> if (hour in 1..6) hour + 12 else hour
            DayPeriod.NIGHT -> if (hour in 1..11) hour + 12 else hour
            DayPeriod.UNSPECIFIED -> {
                // Best guess: if hour <= 6, likely PM; if hour >= 7, likely AM (alarm context)
                hour
            }
        }
    }

    private fun parseNumber(text: String): Int? {
        text.toIntOrNull()?.let { return it }
        return koreanNumbers[text]
    }

    private fun formatDisplay(cal: Calendar): String {
        val now = Calendar.getInstance()
        val dayDiff = ((cal.timeInMillis - now.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

        val dayStr = when {
            isSameDay(cal, now) -> "오늘"
            dayDiff <= 1 -> "내일"
            dayDiff == 2 -> "모레"
            else -> "글피"
        }

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "오전" else "오후"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val minuteStr = if (minute > 0) ":%02d".format(minute) else ":00"

        return "$dayStr $amPm ${displayHour}${minuteStr}"
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
            && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private enum class DayPeriod {
        DAWN, MORNING, AM, NOON, PM, EVENING, NIGHT, UNSPECIFIED
    }
}

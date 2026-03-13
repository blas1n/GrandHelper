package com.family.grandhelper.intent

import com.family.grandhelper.util.TimeParser

class ParameterExtractor {

    fun extractAlarm(transcript: String): IntentResult.Alarm {
        val timeResult = TimeParser.parse(transcript)
        return IntentResult.Alarm(
            hour = timeResult.hour,
            minute = timeResult.minute,
            label = extractAlarmLabel(transcript),
            displayText = timeResult.displayText
        )
    }

    fun extractCall(transcript: String): IntentResult.Call {
        val contact = extractContact(transcript)
        return IntentResult.Call(contactAlias = contact ?: "")
    }

    fun extractNavigation(transcript: String): IntentResult.Navigation {
        val destination = extractDestination(transcript)
        return IntentResult.Navigation(destination = destination ?: "")
    }

    fun extractKakaoTalk(transcript: String): IntentResult.KakaoTalk {
        val contact = extractContact(transcript)
        val message = extractMessage(transcript, contact)
        return IntentResult.KakaoTalk(
            contactAlias = contact ?: "",
            message = message
        )
    }

    private fun extractContact(transcript: String): String? {
        // Korean postpositions to strip after contact name
        val postpositions = listOf(
            "이한테", "한테서", "에게서", "한테다", "에다가",
            "한테", "에게", "보고", "더러"
        )

        // Try to find "X한테/에게" pattern
        for (pp in postpositions) {
            val idx = transcript.indexOf(pp)
            if (idx > 0) {
                // Walk backwards from postposition to find the contact name
                val before = transcript.substring(0, idx).trim()
                val words = before.split(" ")
                return words.lastOrNull()?.trim()
            }
        }

        return null
    }

    private fun extractDestination(transcript: String): String? {
        val patterns = listOf(
            Regex("(.+?)까지"),
            Regex("(.+?)(?:으로|로)\\s*(?:가|길|안내)"),
            Regex("(.+?)\\s*가는\\s*길"),
            Regex("(.+?)\\s*길\\s*안내"),
        )

        for (pattern in patterns) {
            pattern.find(transcript)?.let { match ->
                val dest = match.groupValues[1].trim()
                // Remove common prefixes like "네비 틀어서", "길 안내"
                return dest
                    .replace(Regex("^(네비|내비|길안내|길찾기)\\s*"), "")
                    .replace(Regex("^(틀어서|켜서|찍어서)\\s*"), "")
                    .trim()
                    .ifBlank { null }
            }
        }

        // Fallback: try to find destination after navigation keywords
        val navKeywords = listOf("네비", "내비", "길 안내", "길찾기")
        for (keyword in navKeywords) {
            val idx = transcript.indexOf(keyword)
            if (idx >= 0) {
                val before = transcript.substring(0, idx).trim()
                if (before.isNotBlank()) {
                    return before.split(" ").lastOrNull()?.replace(Regex("[까지으로]$"), "")
                }
            }
        }

        return null
    }

    private fun extractMessage(transcript: String, contact: String?): String {
        // Pattern: "X한테 Y(라고/다고) 카톡 보내줘"
        val messagePatterns = listOf(
            Regex("(?:한테|에게)\\s+(.+?)(?:라고|다고|이라고)?\\s*(?:카톡|카카오톡|메시지|문자|톡)"),
            Regex("(?:한테|에게)\\s+(.+?)\\s*(?:보내|전해|전달)"),
        )

        for (pattern in messagePatterns) {
            pattern.find(transcript)?.let { match ->
                return match.groupValues[1].trim()
            }
        }

        // Fallback: everything between contact and action keyword
        if (contact != null) {
            val contactIdx = transcript.indexOf(contact)
            if (contactIdx >= 0) {
                val after = transcript.substring(contactIdx + contact.length)
                    .replace(Regex("^(한테|에게)\\s*"), "")
                    .replace(Regex("(라고|다고|이라고)?\\s*(카톡|카카오톡|메시지|톡|보내|전해|전달).*$"), "")
                    .trim()
                if (after.isNotBlank()) return after
            }
        }

        return ""
    }

    private fun extractAlarmLabel(transcript: String): String? {
        // Try to extract context like "약 먹기", "회의"
        val labelPatterns = listOf(
            Regex("(.+?)\\s*알람"),
            Regex("알람.*?(?:맞춰|설정).*?\\s(.+)$"),
        )

        for (pattern in labelPatterns) {
            pattern.find(transcript)?.let { match ->
                val label = match.groupValues[1].trim()
                // Filter out time-related words
                if (!label.matches(Regex(".*[0-9시분].*")) && label.length in 2..20) {
                    return label
                }
            }
        }

        return null
    }
}

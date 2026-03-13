package com.family.grandhelper.intent

import com.family.grandhelper.config.ContactAliasConfig
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
        val resolvedName = contact?.let { ContactAliasConfig.resolve(it) }
        return IntentResult.Call(
            contactAlias = contact ?: "",
            resolvedName = resolvedName
        )
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
        // 1. ContactAliasConfig에서 알려진 별명 직접 매칭
        val aliasMatch = ContactAliasConfig.findMatchingAlias(transcript)
        if (aliasMatch != null) return aliasMatch

        // 2. 조사 기반 패턴 매칭
        val postpositions = listOf(
            "이한테", "한테서", "에게서", "한테다", "에다가",
            "한테", "에게", "보고", "더러"
        )

        for (pp in postpositions) {
            val idx = transcript.indexOf(pp)
            if (idx > 0) {
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
                return dest
                    .replace(Regex("^(네비|내비|길안내|길찾기)\\s*"), "")
                    .replace(Regex("^(틀어서|켜서|찍어서)\\s*"), "")
                    .trim()
                    .ifBlank { null }
            }
        }

        // Fallback: 네비 키워드 앞의 단어
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
        val messagePatterns = listOf(
            Regex("(?:한테|에게)\\s+(.+?)(?:라고|다고|이라고)?\\s*(?:카톡|카카오톡|메시지|문자|톡)"),
            Regex("(?:한테|에게)\\s+(.+?)\\s*(?:보내|전해|전달)"),
        )

        for (pattern in messagePatterns) {
            pattern.find(transcript)?.let { match ->
                return match.groupValues[1].trim()
            }
        }

        // Fallback: 연락처와 액션 키워드 사이의 텍스트
        if (contact != null) {
            val contactIdx = transcript.indexOf(contact)
            if (contactIdx >= 0) {
                val after = transcript.substring(contactIdx + contact.length)
                    .replace(Regex("^(이?한테|에게)\\s*"), "")
                    .replace(Regex("(라고|다고|이라고)?\\s*(카톡|카카오톡|메시지|톡|보내|전해|전달).*$"), "")
                    .trim()
                if (after.isNotBlank()) return after
            }
        }

        return ""
    }

    private fun extractAlarmLabel(transcript: String): String? {
        val labelPatterns = listOf(
            Regex("(.+?)\\s*알람"),
            Regex("알람.*?(?:맞춰|설정).*?\\s(.+)$"),
        )

        for (pattern in labelPatterns) {
            pattern.find(transcript)?.let { match ->
                val label = match.groupValues[1].trim()
                if (!label.matches(Regex(".*[0-9시분].*")) && label.length in 2..20) {
                    return label
                }
            }
        }

        return null
    }
}

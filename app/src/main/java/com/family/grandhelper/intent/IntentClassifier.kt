package com.family.grandhelper.intent

import kotlinx.coroutines.runBlocking

class IntentClassifier(
    private val llmClient: LlmClient? = null
) {

    enum class IntentType {
        ALARM, CALL, NAVIGATION, KAKAOTALK, UNKNOWN
    }

    /**
     * 1차: 로컬 키워드 매칭 (즉시, 오프라인)
     * 2차: LLM 폴백 (UNKNOWN일 때만, 네트워크 필요)
     */
    fun classify(transcript: String): IntentType {
        val localResult = classifyLocal(transcript)
        if (localResult != IntentType.UNKNOWN) return localResult

        // LLM 폴백
        if (llmClient != null) {
            try {
                val llmResult = runBlocking { llmClient.classify(transcript) }
                if (llmResult != null) {
                    return llmClient.toIntentType(llmResult)
                }
            } catch (_: Exception) {
                // LLM 실패 시 UNKNOWN 반환
            }
        }

        return IntentType.UNKNOWN
    }

    fun classifyLocal(transcript: String): IntentType {
        val normalized = transcript.replace(" ", "").lowercase()
        return when {
            normalized.containsAny(
                "알람", "알림맞", "깨워", "일어나", "타이머",
                "시에맞", "시에깨", "시알람", "분뒤에깨", "분후에깨"
            ) -> IntentType.ALARM

            normalized.containsAny(
                "카톡", "카카오톡", "메시지보내", "문자보내",
                "톡보내", "메세지"
            ) -> IntentType.KAKAOTALK

            normalized.containsAny(
                "네비", "길안내", "길찾기", "까지가", "어떻게가",
                "가는길", "길좀", "내비"
            ) -> IntentType.NAVIGATION

            normalized.containsAny(
                "전화", "통화", "콜해", "연락", "전화걸",
                "전화해", "폰해"
            ) -> IntentType.CALL

            else -> IntentType.UNKNOWN
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}

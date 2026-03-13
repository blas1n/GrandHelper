package com.family.grandhelper.intent

class IntentClassifier {

    enum class IntentType {
        ALARM, CALL, NAVIGATION, KAKAOTALK, UNKNOWN
    }

    fun classify(transcript: String): IntentType {
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

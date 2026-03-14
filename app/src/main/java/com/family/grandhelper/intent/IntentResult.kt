package com.family.grandhelper.intent

sealed class IntentResult {

    data class Alarm(
        val hour: Int,
        val minute: Int,
        val label: String?,
        val displayText: String
    ) : IntentResult()

    data class Call(
        val contactAlias: String,
        val resolvedName: String? = null,
        val phoneNumber: String? = null
    ) : IntentResult()

    data class Navigation(
        val destination: String
    ) : IntentResult()

    data class Unknown(val transcript: String) : IntentResult()
}

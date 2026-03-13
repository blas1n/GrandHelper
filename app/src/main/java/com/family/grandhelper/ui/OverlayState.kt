package com.family.grandhelper.ui

import com.family.grandhelper.intent.IntentResult

sealed class OverlayState {
    data object Hidden : OverlayState()
    data object Listening : OverlayState()
    data class ListeningWithText(val partialText: String) : OverlayState()
    data object Thinking : OverlayState()
    data class Confirming(
        val icon: String,
        val message: String,
        val intentResult: IntentResult
    ) : OverlayState()
    data object Executing : OverlayState()
    data class Done(val message: String, val subMessage: String) : OverlayState()
    data class Error(val message: String) : OverlayState()
}

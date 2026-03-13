package com.family.grandhelper.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingSpeak: Pair<String, (() -> Unit)?>? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                tts?.setSpeechRate(0.85f)
                isReady = true

                // Play pending speech if any
                pendingSpeak?.let { (text, onDone) ->
                    speak(text, onDone)
                    pendingSpeak = null
                }
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady) {
            pendingSpeak = text to onDone
            return
        }

        val utteranceId = UUID.randomUUID().toString()

        if (onDone != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        onDone()
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {}
            })
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

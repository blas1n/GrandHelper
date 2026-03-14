package com.family.grandhelper.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechManager(private val context: Context) {

    interface Listener {
        fun onPartialResult(text: String)
        fun onFinalResult(text: String)
        fun onError(errorCode: Int)
    }

    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun ensureRecognizer() {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        }
    }

    fun startListening(listener: Listener) {
        this.listener = listener

        // 이전 세션 취소
        recognizer?.cancel()

        ensureRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // cancel() 후 약간의 딜레이로 recognizer 안정화
        handler.postDelayed({
            try {
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("SpeechManager", "startListening failed", e)
                val cb = this.listener
                this.listener = null
                cb?.onError(SpeechRecognizer.ERROR_CLIENT)
            }
        }, 150)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        listener = null
        recognizer?.destroy()
        recognizer = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("SpeechManager", "Ready for speech")
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onPartialResults(partialResults: Bundle?) {
            val results = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = results?.firstOrNull()
            if (!text.isNullOrBlank()) {
                listener?.onPartialResult(text)
            }
        }

        override fun onResults(results: Bundle?) {
            val texts = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = texts?.firstOrNull()
            if (!text.isNullOrBlank()) {
                val cb = listener
                listener = null
                cb?.onFinalResult(text)
            } else {
                val cb = listener
                listener = null
                cb?.onError(SpeechRecognizer.ERROR_NO_MATCH)
            }
        }

        override fun onError(error: Int) {
            Log.w("SpeechManager", "Recognition error: $error")
            val cb = listener
            listener = null
            cb?.onError(error)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}

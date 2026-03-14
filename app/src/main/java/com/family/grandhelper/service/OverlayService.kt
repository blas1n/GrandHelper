package com.family.grandhelper.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.family.grandhelper.GrandHelperApp
import com.family.grandhelper.R
import com.family.grandhelper.action.ActionExecutor
import com.family.grandhelper.action.ActionResult
import com.family.grandhelper.intent.IntentClassifier
import com.family.grandhelper.intent.IntentResult
import com.family.grandhelper.config.LlmConfig
import com.family.grandhelper.intent.ParameterExtractor
import com.family.grandhelper.speech.SpeechManager
import com.family.grandhelper.speech.TtsManager
import com.family.grandhelper.ui.OverlayState
import com.family.grandhelper.ui.OverlayView

class OverlayService : Service() {

    companion object {
        const val ACTION_TOGGLE = "ACTION_TOGGLE_OVERLAY"
        const val ACTION_STOP = "ACTION_STOP_SERVICE"
        private const val NOTIFICATION_ID = 1
        private const val AUTO_DISMISS_DELAY = 2200L
        private const val ERROR_DISMISS_DELAY = 3000L
        var isRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: OverlayView
    private lateinit var speechManager: SpeechManager
    private lateinit var ttsManager: TtsManager
    private lateinit var intentClassifier: IntentClassifier
    private lateinit var parameterExtractor: ParameterExtractor
    private lateinit var actionExecutor: ActionExecutor
    private val handler = Handler(Looper.getMainLooper())

    private var currentState: OverlayState = OverlayState.Hidden
    private var currentIntentResult: IntentResult? = null
    private var isOverlayAdded = false

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = OverlayView(this)
        speechManager = SpeechManager(this)
        ttsManager = TtsManager(this)
        // LLM 폴백: 로컬 매칭 실패 시에만 호출
        // assets/llm_config.json에서 API 설정 로드 (gitignored)
        LlmConfig.load(this)
        val llmClient = LlmConfig.createClient()
        intentClassifier = IntentClassifier(llmClient)
        parameterExtractor = ParameterExtractor()
        actionExecutor = ActionExecutor(this)

        setupOverlayCallbacks()

        startForeground(NOTIFICATION_ID, createNotification())

        windowManager.addView(overlayView.rootView, layoutParams)
        isOverlayAdded = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> toggleOverlay()
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        speechManager.destroy()
        ttsManager.shutdown()
        if (isOverlayAdded) {
            windowManager.removeView(overlayView.rootView)
            isOverlayAdded = false
        }
        super.onDestroy()
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayCallbacks() {
        overlayView.btnConfirm.setOnClickListener {
            currentIntentResult?.let { onConfirm(it) }
        }

        overlayView.btnCancel.setOnClickListener {
            ttsManager.speak(getString(R.string.tts_cancel))
            dismiss()
        }

        // 오버레이 바깥 터치 시 dismiss
        overlayView.rootView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE
                && currentState !is OverlayState.Hidden) {
                dismiss()
            }
            false
        }
    }

    private fun toggleOverlay() {
        if (currentState !is OverlayState.Hidden) {
            dismiss()
        } else {
            startListening()
        }
    }

    private fun setState(state: OverlayState) {
        currentState = state
        handler.post { overlayView.updateState(state) }

        // Update window flags based on state
        when (state) {
            is OverlayState.Confirming -> setTouchable(true)
            else -> setTouchable(false)
        }
    }

    private fun setTouchable(touchable: Boolean) {
        if (!isOverlayAdded) return
        if (touchable) {
            layoutParams.flags = layoutParams.flags and
                (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE).inv()
        } else {
            layoutParams.flags = layoutParams.flags or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.updateViewLayout(overlayView.rootView, layoutParams)
    }

    private var sttRetryCount = 0
    private val MAX_STT_RETRIES = 2

    private fun startListening() {
        setState(OverlayState.Listening)
        sttRetryCount = 0
        doStartListening()
    }

    private fun doStartListening() {
        speechManager.startListening(object : SpeechManager.Listener {
            override fun onPartialResult(text: String) {
                setState(OverlayState.ListeningWithText(text))
            }

            override fun onFinalResult(text: String) {
                processTranscript(text)
            }

            override fun onError(errorCode: Int) {
                android.util.Log.w("OverlayService", "STT error: $errorCode, retry=$sttRetryCount")
                // 일시적 에러는 재시도
                if (sttRetryCount < MAX_STT_RETRIES && isRetryableError(errorCode)) {
                    sttRetryCount++
                    handler.postDelayed({ doStartListening() }, 500)
                } else {
                    setState(OverlayState.Error(getString(R.string.error_title)))
                    ttsManager.speak(getString(R.string.tts_error))
                    handler.postDelayed({ dismiss() }, ERROR_DISMISS_DELAY)
                }
            }
        })
    }

    private fun isRetryableError(errorCode: Int): Boolean {
        return errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH
            || errorCode == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            || errorCode == android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY
            || errorCode == android.speech.SpeechRecognizer.ERROR_CLIENT
    }

    private fun processTranscript(transcript: String) {
        setState(OverlayState.Thinking)

        // Small delay for visual feedback of "thinking" state
        handler.postDelayed({
            val intentType = intentClassifier.classify(transcript)

            val intentResult = when (intentType) {
                IntentClassifier.IntentType.ALARM ->
                    parameterExtractor.extractAlarm(transcript)
                IntentClassifier.IntentType.CALL ->
                    parameterExtractor.extractCall(transcript)
                IntentClassifier.IntentType.NAVIGATION ->
                    parameterExtractor.extractNavigation(transcript)
                IntentClassifier.IntentType.UNKNOWN ->
                    IntentResult.Unknown(transcript)
            }

            currentIntentResult = intentResult

            when (intentResult) {
                is IntentResult.Unknown -> {
                    setState(OverlayState.Error(getString(R.string.error_unknown_command)))
                    ttsManager.speak(getString(R.string.error_unknown_command))
                    handler.postDelayed({ dismiss() }, ERROR_DISMISS_DELAY)
                }
                else -> {
                    val (icon, message) = buildConfirmation(intentResult)
                    setState(OverlayState.Confirming(icon, message, intentResult))
                    ttsManager.speak(message.replace("\n", " "))
                }
            }
        }, 800)
    }

    private fun buildConfirmation(result: IntentResult): Pair<String, String> {
        return when (result) {
            is IntentResult.Alarm -> {
                "\u23F0" to "${result.displayText}에\n${getString(R.string.alarm_confirm)}"
            }
            is IntentResult.Call -> {
                "\uD83D\uDCDE" to "${result.contactAlias}에게\n${getString(R.string.call_confirm)}"
            }
            is IntentResult.Navigation -> {
                "\uD83D\uDDFA\uFE0F" to "${result.destination}까지\n${getString(R.string.navi_confirm)}"
            }
            is IntentResult.Unknown -> "" to ""
        }
    }

    private fun onConfirm(intentResult: IntentResult) {
        setState(OverlayState.Executing)

        handler.postDelayed({
            val result = actionExecutor.execute(intentResult)
            when (result) {
                is ActionResult.Success -> {
                    setState(OverlayState.Done(result.message, result.subMessage))
                    ttsManager.speak(result.message)
                    handler.postDelayed({ dismiss() }, AUTO_DISMISS_DELAY)
                }
                is ActionResult.Failure -> {
                    setState(OverlayState.Error(result.message))
                    ttsManager.speak(result.message)
                    handler.postDelayed({ dismiss() }, ERROR_DISMISS_DELAY)
                }
            }
        }, 500)
    }

    private fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        speechManager.stopListening()
        ttsManager.stop()
        currentIntentResult = null
        setState(OverlayState.Hidden)
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GrandHelperApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .addAction(0, getString(R.string.notification_stop), stopPendingIntent)
            .build()
    }
}

package com.family.grandhelper.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.family.grandhelper.R

class OverlayView(context: Context) {

    val rootView: View = LayoutInflater.from(context).inflate(R.layout.overlay_panel, null)

    private val backdrop: View = rootView.findViewById(R.id.backdrop)
    private val panel: LinearLayout = rootView.findViewById(R.id.panel)

    // State views
    private val stateListening: View = rootView.findViewById(R.id.state_listening)
    private val stateThinking: View = rootView.findViewById(R.id.state_thinking)
    private val stateConfirming: View = rootView.findViewById(R.id.state_confirming)
    private val stateExecuting: View = rootView.findViewById(R.id.state_executing)
    private val stateDone: View = rootView.findViewById(R.id.state_done)
    private val stateError: View = rootView.findViewById(R.id.state_error)

    // Listening views
    private val partialText: TextView = stateListening.findViewById(R.id.partial_text)
    private val waveBars: LinearLayout = stateListening.findViewById(R.id.wave_bars)

    // Confirming views
    private val confirmIcon: TextView = stateConfirming.findViewById(R.id.confirm_icon)
    private val confirmMessage: TextView = stateConfirming.findViewById(R.id.confirm_message)
    val btnCancel: Button = stateConfirming.findViewById(R.id.btn_cancel)
    val btnConfirm: Button = stateConfirming.findViewById(R.id.btn_confirm)

    // Done views
    private val doneMessage: TextView = stateDone.findViewById(R.id.done_message)
    private val doneSubMessage: TextView = stateDone.findViewById(R.id.done_sub_message)

    // Error views
    private val errorTitle: TextView = stateError.findViewById(R.id.error_title)

    private var waveAnimators: List<ObjectAnimator> = emptyList()

    var onBackdropClick: (() -> Unit)? = null

    init {
        backdrop.setOnClickListener { onBackdropClick?.invoke() }
    }

    fun updateState(state: OverlayState) {
        when (state) {
            is OverlayState.Hidden -> hide()
            is OverlayState.Listening -> showListening(null)
            is OverlayState.ListeningWithText -> showListening(state.partialText)
            is OverlayState.Thinking -> showThinking()
            is OverlayState.Confirming -> showConfirming(state.icon, state.message)
            is OverlayState.Executing -> showExecuting()
            is OverlayState.Done -> showDone(state.message, state.subMessage)
            is OverlayState.Error -> showError(state.message)
        }
    }

    private fun hide() {
        stopWaveAnimation()
        if (panel.visibility == View.VISIBLE) {
            val slideDown = AnimationUtils.loadAnimation(rootView.context, R.anim.slide_down)
            panel.startAnimation(slideDown)
            backdrop.animate().alpha(0f).setDuration(250).withEndAction {
                backdrop.visibility = View.GONE
            }
            slideDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(a: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                override fun onAnimationEnd(a: android.view.animation.Animation?) {
                    panel.visibility = View.GONE
                    hideAllStates()
                }
            })
        } else {
            panel.visibility = View.GONE
            backdrop.visibility = View.GONE
            hideAllStates()
        }
    }

    private fun showPanel() {
        if (panel.visibility != View.VISIBLE) {
            backdrop.alpha = 0f
            backdrop.visibility = View.VISIBLE
            backdrop.animate().alpha(1f).setDuration(300)

            panel.visibility = View.VISIBLE
            val slideUp = AnimationUtils.loadAnimation(rootView.context, R.anim.slide_up)
            panel.startAnimation(slideUp)
        }
    }

    private fun showListening(text: String?) {
        showPanel()
        switchToState(stateListening)
        startWaveAnimation()

        if (text != null) {
            partialText.text = "\"$text\""
            partialText.visibility = View.VISIBLE
        } else {
            partialText.visibility = View.GONE
        }
    }

    private fun showThinking() {
        switchToState(stateThinking)
        val fadeIn = AnimationUtils.loadAnimation(rootView.context, R.anim.scale_in)
        stateThinking.startAnimation(fadeIn)
        stopWaveAnimation()
    }

    private fun showConfirming(icon: String, message: String) {
        switchToState(stateConfirming)
        confirmIcon.text = icon
        confirmMessage.text = message
        val scaleIn = AnimationUtils.loadAnimation(rootView.context, R.anim.scale_in)
        stateConfirming.startAnimation(scaleIn)
    }

    private fun showExecuting() {
        switchToState(stateExecuting)
        val fadeIn = AnimationUtils.loadAnimation(rootView.context, R.anim.fade_in)
        stateExecuting.startAnimation(fadeIn)
    }

    private fun showDone(message: String, subMessage: String) {
        switchToState(stateDone)
        doneMessage.text = message
        doneSubMessage.text = subMessage
        val scaleIn = AnimationUtils.loadAnimation(rootView.context, R.anim.scale_in)
        stateDone.startAnimation(scaleIn)
    }

    private fun showError(message: String) {
        switchToState(stateError)
        errorTitle.text = message
        val scaleIn = AnimationUtils.loadAnimation(rootView.context, R.anim.scale_in)
        stateError.startAnimation(scaleIn)
    }

    private fun switchToState(target: View) {
        hideAllStates()
        target.visibility = View.VISIBLE
    }

    private fun hideAllStates() {
        stateListening.visibility = View.GONE
        stateThinking.visibility = View.GONE
        stateConfirming.visibility = View.GONE
        stateExecuting.visibility = View.GONE
        stateDone.visibility = View.GONE
        stateError.visibility = View.GONE
    }

    private fun startWaveAnimation() {
        stopWaveAnimation()
        waveAnimators = (0 until waveBars.childCount).map { i ->
            val bar = waveBars.getChildAt(i)
            ObjectAnimator.ofFloat(bar, "scaleY", 1f, 4f).apply {
                duration = 700
                startDelay = (i * 120).toLong()
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }
    }

    private fun stopWaveAnimation() {
        waveAnimators.forEach { it.cancel() }
        waveAnimators = emptyList()
        // Reset bar scales
        for (i in 0 until waveBars.childCount) {
            waveBars.getChildAt(i).scaleY = 1f
        }
    }
}

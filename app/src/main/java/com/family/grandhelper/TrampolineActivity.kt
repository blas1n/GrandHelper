package com.family.grandhelper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.family.grandhelper.service.OverlayService

class TrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleOverlay()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        toggleOverlay()
    }

    private fun toggleOverlay() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_TOGGLE
        }
        startForegroundService(intent)
        finish()
    }
}

package com.family.grandhelper.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        var instance: VoiceAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Phase 4: KakaoTalk UI automation events
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}

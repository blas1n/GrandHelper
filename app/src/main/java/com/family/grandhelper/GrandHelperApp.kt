package com.family.grandhelper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class GrandHelperApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "overlay_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_title)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

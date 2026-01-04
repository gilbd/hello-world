package com.babymonitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BabyMonitorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Monitoring service channel
            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when baby monitoring is active"
                setShowBadge(false)
            }

            // Alert channel for cry/motion detection
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Baby Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for baby crying or movement"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            notificationManager.createNotificationChannel(monitoringChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_MONITORING = "monitoring_channel"
        const val CHANNEL_ALERTS = "alerts_channel"
    }
}

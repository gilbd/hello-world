package com.babymonitor.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.babymonitor.BabyMonitorApp
import com.babymonitor.R
import com.babymonitor.ui.MainActivity

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISMISS_ALERT -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager
                notificationManager.cancel(ALERT_NOTIFICATION_ID)
            }
        }
    }

    companion object {
        const val ACTION_DISMISS_ALERT = "com.babymonitor.DISMISS_ALERT"
        const val ALERT_NOTIFICATION_ID = 1001

        fun showAlertNotification(
            context: Context,
            title: String,
            message: String,
            alertType: AlertNotificationType
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            // Intent to open the app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent to dismiss
            val dismissIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_DISMISS_ALERT
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val icon = when (alertType) {
                AlertNotificationType.CRY_DETECTED -> android.R.drawable.ic_dialog_alert
                AlertNotificationType.MOTION_DETECTED -> android.R.drawable.ic_menu_view
                AlertNotificationType.CONNECTION_LOST -> android.R.drawable.ic_dialog_info
            }

            val notification = NotificationCompat.Builder(context, BabyMonitorApp.CHANNEL_ALERTS)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openPendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Dismiss",
                    dismissPendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_view,
                    "Open",
                    openPendingIntent
                )
                .build()

            notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
        }
    }
}

enum class AlertNotificationType {
    CRY_DETECTED,
    MOTION_DETECTED,
    CONNECTION_LOST
}

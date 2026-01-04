package com.babymonitor.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.babymonitor.BabyMonitorApp
import com.babymonitor.detection.AlertManager
import com.babymonitor.detection.AudioAnalyzer
import com.babymonitor.detection.MotionDetector
import com.babymonitor.notification.AlertNotificationType
import com.babymonitor.notification.NotificationReceiver
import com.babymonitor.streaming.StreamingManager
import com.babymonitor.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject
    lateinit var motionDetector: MotionDetector

    @Inject
    lateinit var audioAnalyzer: AudioAnalyzer

    @Inject
    lateinit var alertManager: AlertManager

    @Inject
    lateinit var streamingManager: StreamingManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MonitoringService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        Log.d(TAG, "Starting monitoring service")

        // Start foreground service with notification
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start audio analysis
        audioAnalyzer.startAnalysis()

        // Observe alerts and send notifications
        serviceScope.launch {
            alertManager.alertState.collectLatest { state ->
                if (state.hasActiveAlert && state.currentAlert != null) {
                    val alertType = when (state.currentAlert.type) {
                        com.babymonitor.detection.AlertType.CRY_DETECTED ->
                            AlertNotificationType.CRY_DETECTED
                        com.babymonitor.detection.AlertType.MOTION_DETECTED ->
                            AlertNotificationType.MOTION_DETECTED
                        com.babymonitor.detection.AlertType.CONNECTION_LOST ->
                            AlertNotificationType.CONNECTION_LOST
                    }

                    NotificationReceiver.showAlertNotification(
                        context = this@MonitoringService,
                        title = "Baby Monitor Alert",
                        message = state.currentAlert.message,
                        alertType = alertType
                    )
                }
            }
        }

        // Update notification periodically with status
        serviceScope.launch {
            while (isActive) {
                updateNotification()
                delay(30_000) // Update every 30 seconds
            }
        }
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Stopping monitoring service")
        isMonitoring = false

        audioAnalyzer.stopAnalysis()
        streamingManager.stopStreaming()
        serviceScope.cancel()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MonitoringService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BabyMonitorApp.CHANNEL_MONITORING)
            .setContentTitle("Baby Monitor Active")
            .setContentText("Monitoring for baby activity...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val statusText = buildString {
            append("Monitoring active")
            if (motionDetector.isEnabled) append(" • Motion")
            if (audioAnalyzer.isEnabled) append(" • Audio")
        }

        val notification = NotificationCompat.Builder(this, BabyMonitorApp.CHANNEL_MONITORING)
            .setContentTitle("Baby Monitor Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "MonitoringService destroyed")
    }

    companion object {
        private const val TAG = "MonitoringService"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.babymonitor.START_MONITORING"
        const val ACTION_STOP = "com.babymonitor.STOP_MONITORING"

        fun start(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}

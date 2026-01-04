package com.babymonitor.detection

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _alertState = MutableStateFlow(AlertState())
    val alertState: StateFlow<AlertState> = _alertState.asStateFlow()

    private val alertHistory = mutableListOf<Alert>()
    private var lastCryAlertTime = 0L
    private var lastMotionAlertTime = 0L

    // Cooldown periods to prevent alert spam
    private val cryAlertCooldown = 10_000L // 10 seconds
    private val motionAlertCooldown = 5_000L // 5 seconds

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun onCryDetected(audioState: AudioState) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCryAlertTime < cryAlertCooldown) {
            return // Still in cooldown
        }

        lastCryAlertTime = currentTime

        val alert = Alert(
            type = AlertType.CRY_DETECTED,
            message = "Baby crying detected!",
            timestamp = currentTime,
            data = mapOf(
                "volume" to audioState.volume,
                "frequency" to audioState.estimatedFrequency
            )
        )

        addAlert(alert)
        triggerVibration(AlertType.CRY_DETECTED)

        Log.d(TAG, "Cry alert triggered: ${alert.message}")
    }

    fun onMotionDetected(motionState: MotionState) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastMotionAlertTime < motionAlertCooldown) {
            return // Still in cooldown
        }

        lastMotionAlertTime = currentTime

        val alert = Alert(
            type = AlertType.MOTION_DETECTED,
            message = "Movement detected!",
            timestamp = currentTime,
            data = mapOf(
                "motionLevel" to motionState.motionLevel,
                "intensity" to motionState.averageIntensity
            )
        )

        addAlert(alert)
        triggerVibration(AlertType.MOTION_DETECTED)

        Log.d(TAG, "Motion alert triggered: ${alert.message}")
    }

    private fun addAlert(alert: Alert) {
        alertHistory.add(0, alert)

        // Keep only last 100 alerts
        if (alertHistory.size > MAX_ALERT_HISTORY) {
            alertHistory.removeAt(alertHistory.lastIndex)
        }

        _alertState.value = AlertState(
            currentAlert = alert,
            hasActiveAlert = true,
            alertHistory = alertHistory.toList()
        )
    }

    private fun triggerVibration(alertType: AlertType) {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = when (alertType) {
                    AlertType.CRY_DETECTED -> longArrayOf(0, 500, 200, 500, 200, 500)
                    AlertType.MOTION_DETECTED -> longArrayOf(0, 200, 100, 200)
                    AlertType.CONNECTION_LOST -> longArrayOf(0, 1000)
                }
                vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(500)
            }
        }
    }

    fun dismissAlert() {
        _alertState.value = _alertState.value.copy(
            hasActiveAlert = false,
            currentAlert = null
        )
    }

    fun clearAlertHistory() {
        alertHistory.clear()
        _alertState.value = AlertState()
    }

    fun getAlertHistory(): List<Alert> = alertHistory.toList()

    companion object {
        private const val TAG = "AlertManager"
        private const val MAX_ALERT_HISTORY = 100
    }
}

data class AlertState(
    val currentAlert: Alert? = null,
    val hasActiveAlert: Boolean = false,
    val alertHistory: List<Alert> = emptyList()
)

data class Alert(
    val type: AlertType,
    val message: String,
    val timestamp: Long,
    val data: Map<String, Any> = emptyMap()
)

enum class AlertType {
    CRY_DETECTED,
    MOTION_DETECTED,
    CONNECTION_LOST
}

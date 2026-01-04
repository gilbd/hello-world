package com.babymonitor.shared.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform alert management engine.
 * Coordinates alerts from motion and cry detection with cooldown logic.
 */
class AlertEngine {

    private val _alertState = MutableStateFlow(AlertState())
    val alertState: StateFlow<AlertState> = _alertState.asStateFlow()

    private val alertHistory = mutableListOf<Alert>()
    private var lastCryAlertTime = 0L
    private var lastMotionAlertTime = 0L

    // Cooldown periods (ms)
    var cryAlertCooldown: Long = 10_000L
    var motionAlertCooldown: Long = 5_000L

    fun onCryDetected(audioResult: AudioResult): Alert? {
        val currentTime = currentTimeMillis()

        if (currentTime - lastCryAlertTime < cryAlertCooldown) {
            return null
        }

        lastCryAlertTime = currentTime

        val alert = Alert(
            type = AlertType.CRY_DETECTED,
            message = "Baby crying detected!",
            timestamp = currentTime,
            metadata = mapOf(
                "volume" to audioResult.volume.toString(),
                "frequency" to audioResult.estimatedFrequency.toString()
            )
        )

        addAlert(alert)
        return alert
    }

    fun onMotionDetected(motionResult: MotionResult): Alert? {
        val currentTime = currentTimeMillis()

        if (currentTime - lastMotionAlertTime < motionAlertCooldown) {
            return null
        }

        lastMotionAlertTime = currentTime

        val alert = Alert(
            type = AlertType.MOTION_DETECTED,
            message = "Movement detected!",
            timestamp = currentTime,
            metadata = mapOf(
                "motionLevel" to motionResult.motionLevel.toString(),
                "intensity" to motionResult.averageIntensity.toString()
            )
        )

        addAlert(alert)
        return alert
    }

    private fun addAlert(alert: Alert) {
        alertHistory.add(0, alert)

        if (alertHistory.size > MAX_ALERT_HISTORY) {
            alertHistory.removeAt(alertHistory.lastIndex)
        }

        _alertState.value = AlertState(
            currentAlert = alert,
            hasActiveAlert = true,
            alertHistory = alertHistory.toList()
        )
    }

    fun dismissAlert() {
        _alertState.value = _alertState.value.copy(
            hasActiveAlert = false,
            currentAlert = null
        )
    }

    fun clearHistory() {
        alertHistory.clear()
        _alertState.value = AlertState()
    }

    fun getHistory(): List<Alert> = alertHistory.toList()

    companion object {
        const val MAX_ALERT_HISTORY = 100
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
    val metadata: Map<String, String> = emptyMap()
)

enum class AlertType {
    CRY_DETECTED,
    MOTION_DETECTED,
    CONNECTION_LOST
}

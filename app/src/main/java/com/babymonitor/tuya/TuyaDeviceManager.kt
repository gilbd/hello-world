package com.babymonitor.tuya

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the phone's registration as a Tuya IoT device.
 *
 * This allows the baby monitor app to appear in the Tuya SmartLife app
 * as a smart camera device that can be viewed and controlled.
 *
 * Setup requirements:
 * 1. Create account at https://developer.tuya.com
 * 2. Create an IPC (IP Camera) product
 * 3. Get Product ID (PID), UUID, and Auth Key
 * 4. Configure in TuyaConfig
 */
@Singleton
class TuyaDeviceManager @Inject constructor(
    private val context: Context,
    private val tuyaConfig: TuyaConfig,
    private val tuyaMqttClient: TuyaMqttClient,
    private val tuyaP2PServer: TuyaP2PServer
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _deviceState = MutableStateFlow<TuyaDeviceState>(TuyaDeviceState.Unregistered)
    val deviceState: StateFlow<TuyaDeviceState> = _deviceState.asStateFlow()

    private val _connectionState = MutableStateFlow<TuyaConnectionState>(TuyaConnectionState.Disconnected)
    val connectionState: StateFlow<TuyaConnectionState> = _connectionState.asStateFlow()

    private var deviceId: String? = null
    private var localKey: String? = null

    /**
     * Initialize the Tuya device SDK.
     * Call this on app startup.
     */
    fun initialize() {
        Log.d(TAG, "Initializing Tuya Device Manager")

        // Check if already registered
        val savedDeviceId = loadDeviceId()
        if (savedDeviceId != null) {
            deviceId = savedDeviceId
            localKey = loadLocalKey()
            _deviceState.value = TuyaDeviceState.Registered(savedDeviceId)
            connectToCloud()
        }
    }

    /**
     * Register this phone as a Tuya IoT device.
     * This should be called during the setup/pairing flow.
     */
    suspend fun registerDevice(): Result<String> = withContext(Dispatchers.IO) {
        try {
            _deviceState.value = TuyaDeviceState.Registering

            // Generate unique device ID if not exists
            val uuid = tuyaConfig.uuid ?: generateDeviceUUID()

            // Activate device with Tuya cloud
            val activationResult = activateWithCloud(uuid)

            if (activationResult.isSuccess) {
                val result = activationResult.getOrThrow()
                deviceId = result.deviceId
                localKey = result.localKey

                // Save for persistence
                saveDeviceId(result.deviceId)
                saveLocalKey(result.localKey)

                _deviceState.value = TuyaDeviceState.Registered(result.deviceId)

                // Connect to cloud
                connectToCloud()

                Result.success(result.deviceId)
            } else {
                _deviceState.value = TuyaDeviceState.Error("Registration failed")
                Result.failure(activationResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            _deviceState.value = TuyaDeviceState.Error(e.message ?: "Registration failed")
            Result.failure(e)
        }
    }

    /**
     * Generate a pairing QR code that users can scan in SmartLife app.
     */
    fun generatePairingQRCode(): String {
        val pairingData = JSONObject().apply {
            put("pid", tuyaConfig.productId)
            put("uuid", tuyaConfig.uuid ?: generateDeviceUUID())
            put("v", "2.0")
            put("s", "1") // Pairing mode: 1 = QR code
        }
        return pairingData.toString()
    }

    /**
     * Start AP (Access Point) pairing mode.
     * The phone creates a WiFi hotspot for SmartLife app to connect.
     */
    suspend fun startAPPairing() {
        _deviceState.value = TuyaDeviceState.Pairing(PairingMode.AP)
        // Implementation would create WiFi hotspot
        // and wait for SmartLife app connection
    }

    /**
     * Connect to Tuya cloud via MQTT.
     */
    private fun connectToCloud() {
        scope.launch {
            try {
                _connectionState.value = TuyaConnectionState.Connecting

                val connected = tuyaMqttClient.connect(
                    deviceId = deviceId!!,
                    localKey = localKey!!,
                    productId = tuyaConfig.productId
                )

                if (connected) {
                    _connectionState.value = TuyaConnectionState.Connected

                    // Start P2P server for video streaming
                    tuyaP2PServer.start(deviceId!!, localKey!!)

                    // Report device status
                    reportDeviceOnline()

                    // Listen for commands
                    listenForCommands()
                } else {
                    _connectionState.value = TuyaConnectionState.Error("Connection failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cloud connection failed", e)
                _connectionState.value = TuyaConnectionState.Error(e.message ?: "Connection failed")
            }
        }
    }

    /**
     * Report device as online with current capabilities.
     */
    private suspend fun reportDeviceOnline() {
        val status = buildDeviceStatus()
        tuyaMqttClient.reportStatus(status)
    }

    /**
     * Build current device status for Tuya.
     */
    private fun buildDeviceStatus(): Map<String, Any> {
        return mapOf(
            "101" to true,  // Device online
            "103" to false, // Motion detection (off initially)
            "104" to false, // Cry detection (off initially)
            "106" to 0,     // Night vision mode (0=auto, 1=on, 2=off)
            "108" to false, // Recording status
            "109" to 100,   // Battery level (100 for plugged phones)
            "150" to true   // Privacy mode off (camera active)
        )
    }

    /**
     * Listen for commands from Tuya cloud / SmartLife app.
     */
    private fun listenForCommands() {
        tuyaMqttClient.setCommandListener { command ->
            scope.launch {
                handleCommand(command)
            }
        }
    }

    /**
     * Handle incoming command from SmartLife app.
     */
    private suspend fun handleCommand(command: TuyaCommand) {
        Log.d(TAG, "Received command: ${command.dpId} = ${command.value}")

        when (command.dpId) {
            "103" -> {
                // Motion detection toggle
                val enabled = command.value as? Boolean ?: false
                onMotionDetectionToggle?.invoke(enabled)
            }
            "104" -> {
                // Cry detection toggle
                val enabled = command.value as? Boolean ?: false
                onCryDetectionToggle?.invoke(enabled)
            }
            "106" -> {
                // Night vision mode
                val mode = command.value as? Int ?: 0
                onNightVisionChange?.invoke(mode)
            }
            "119" -> {
                // PTZ control (if supported)
                val direction = command.value as? String
                onPTZControl?.invoke(direction ?: "stop")
            }
            "150" -> {
                // Privacy mode (turn camera on/off)
                val privacyOn = command.value as? Boolean ?: false
                onPrivacyModeChange?.invoke(privacyOn)
            }
        }

        // Acknowledge command
        tuyaMqttClient.acknowledgeCommand(command)
    }

    /**
     * Report an alert (motion/cry detected) to Tuya cloud.
     */
    suspend fun reportAlert(type: AlertType, message: String) {
        val alertData = mapOf(
            "type" to type.tuyaCode,
            "time" to System.currentTimeMillis(),
            "msg" to message
        )
        tuyaMqttClient.reportAlert(alertData)
    }

    /**
     * Update device status in Tuya cloud.
     */
    suspend fun updateStatus(dpId: String, value: Any) {
        tuyaMqttClient.reportStatus(mapOf(dpId to value))
    }

    /**
     * Disconnect from Tuya cloud.
     */
    fun disconnect() {
        scope.launch {
            tuyaMqttClient.disconnect()
            tuyaP2PServer.stop()
            _connectionState.value = TuyaConnectionState.Disconnected
        }
    }

    /**
     * Unregister this device from Tuya.
     */
    suspend fun unregisterDevice() {
        disconnect()
        clearDeviceData()
        _deviceState.value = TuyaDeviceState.Unregistered
    }

    // Callbacks for command handling
    var onMotionDetectionToggle: ((Boolean) -> Unit)? = null
    var onCryDetectionToggle: ((Boolean) -> Unit)? = null
    var onNightVisionChange: ((Int) -> Unit)? = null
    var onPTZControl: ((String) -> Unit)? = null
    var onPrivacyModeChange: ((Boolean) -> Unit)? = null

    // Persistence helpers
    private fun saveDeviceId(id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DEVICE_ID, id).apply()
    }

    private fun loadDeviceId(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEVICE_ID, null)
    }

    private fun saveLocalKey(key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOCAL_KEY, key).apply()
    }

    private fun loadLocalKey(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCAL_KEY, null)
    }

    private fun clearDeviceData() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun generateDeviceUUID(): String {
        return UUID.randomUUID().toString().replace("-", "").take(20)
    }

    private suspend fun activateWithCloud(uuid: String): Result<ActivationResult> {
        // This would call Tuya's activation API
        // For now, return a placeholder
        return Result.success(ActivationResult(
            deviceId = "tuya_${uuid}",
            localKey = generateLocalKey()
        ))
    }

    private fun generateLocalKey(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }

    companion object {
        private const val TAG = "TuyaDeviceManager"
        private const val PREFS_NAME = "tuya_device"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LOCAL_KEY = "local_key"
    }
}

sealed class TuyaDeviceState {
    object Unregistered : TuyaDeviceState()
    object Registering : TuyaDeviceState()
    data class Pairing(val mode: PairingMode) : TuyaDeviceState()
    data class Registered(val deviceId: String) : TuyaDeviceState()
    data class Error(val message: String) : TuyaDeviceState()
}

sealed class TuyaConnectionState {
    object Disconnected : TuyaConnectionState()
    object Connecting : TuyaConnectionState()
    object Connected : TuyaConnectionState()
    data class Error(val message: String) : TuyaConnectionState()
}

enum class PairingMode {
    QR_CODE,
    AP,
    BLUETOOTH
}

enum class AlertType(val tuyaCode: Int) {
    MOTION(1),
    CRY(2),
    SOUND(3)
}

data class ActivationResult(
    val deviceId: String,
    val localKey: String
)

data class TuyaCommand(
    val dpId: String,
    val value: Any
)

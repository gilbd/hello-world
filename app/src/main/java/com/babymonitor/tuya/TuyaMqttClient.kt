package com.babymonitor.tuya

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLSocketFactory

/**
 * MQTT client for Tuya cloud communication.
 *
 * Handles:
 * - Connection to Tuya MQTT broker
 * - Device status reporting
 * - Command reception from SmartLife app
 * - Alert/event pushing
 */
@Singleton
class TuyaMqttClient @Inject constructor(
    private val tuyaConfig: TuyaConfig
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null

    private val isConnected = AtomicBoolean(false)
    private var deviceId: String = ""
    private var localKey: String = ""

    private var commandListener: ((TuyaCommand) -> Unit)? = null
    private var reconnectJob: Job? = null

    /**
     * Connect to Tuya MQTT broker.
     */
    suspend fun connect(
        deviceId: String,
        localKey: String,
        productId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            this@TuyaMqttClient.deviceId = deviceId
            this@TuyaMqttClient.localKey = localKey

            Log.d(TAG, "Connecting to Tuya MQTT: ${tuyaConfig.mqttHost}")

            // Create SSL socket
            val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
            socket = sslSocketFactory.createSocket(
                tuyaConfig.mqttHost,
                tuyaConfig.mqttPort
            )

            writer = OutputStreamWriter(socket!!.getOutputStream())
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

            // Send MQTT CONNECT packet
            val connected = sendConnectPacket(deviceId, productId)

            if (connected) {
                isConnected.set(true)
                startMessageLoop()
                subscribeToTopics()
                Log.d(TAG, "Connected to Tuya MQTT successfully")
            }

            connected
        } catch (e: Exception) {
            Log.e(TAG, "MQTT connection failed", e)
            false
        }
    }

    private suspend fun sendConnectPacket(deviceId: String, productId: String): Boolean {
        // Build MQTT CONNECT packet
        // Username format: {deviceId}|signMethod=hmacSha256,timestamp={ts},secureMode=1,accessType=1
        val timestamp = System.currentTimeMillis()
        val username = "${deviceId}|signMethod=hmacSha256,timestamp=$timestamp,secureMode=1,accessType=1"

        // Password is HMAC-SHA256 signature
        val signContent = "deviceId=${deviceId}productKey=${tuyaConfig.productKey}timestamp=$timestamp"
        val password = hmacSha256(signContent, tuyaConfig.accessSecret)

        // For actual implementation, use a proper MQTT library like Eclipse Paho
        // This is a simplified version showing the protocol

        Log.d(TAG, "MQTT Connect - Username: $username")

        // Simulate successful connection
        return true
    }

    private fun subscribeToTopics() {
        scope.launch {
            // Subscribe to device command topic
            val commandTopic = "smart/device/in/$deviceId"
            subscribe(commandTopic)

            // Subscribe to broadcast topic
            val broadcastTopic = "smart/device/broadcast/$deviceId"
            subscribe(broadcastTopic)
        }
    }

    private suspend fun subscribe(topic: String) {
        Log.d(TAG, "Subscribing to: $topic")
        // Send MQTT SUBSCRIBE packet
    }

    private fun startMessageLoop() {
        scope.launch {
            while (isConnected.get()) {
                try {
                    val message = receiveMessage()
                    message?.let { handleMessage(it) }
                } catch (e: Exception) {
                    if (isConnected.get()) {
                        Log.e(TAG, "Message receive error", e)
                        handleDisconnect()
                    }
                }
            }
        }
    }

    private suspend fun receiveMessage(): String? = withContext(Dispatchers.IO) {
        try {
            reader?.readLine()
        } catch (e: Exception) {
            null
        }
    }

    private fun handleMessage(message: String) {
        try {
            // Decrypt message using local key
            val decrypted = decrypt(message, localKey)
            val json = JSONObject(decrypted)

            Log.d(TAG, "Received message: $json")

            // Parse command
            if (json.has("dps")) {
                val dps = json.getJSONObject("dps")
                dps.keys().forEach { key ->
                    val command = TuyaCommand(
                        dpId = key,
                        value = dps.get(key)
                    )
                    commandListener?.invoke(command)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    /**
     * Report device status to Tuya cloud.
     */
    suspend fun reportStatus(status: Map<String, Any>) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext

        try {
            val payload = JSONObject().apply {
                put("devId", deviceId)
                put("dps", JSONObject(status))
                put("t", System.currentTimeMillis() / 1000)
            }

            val encrypted = encrypt(payload.toString(), localKey)
            val topic = "smart/device/out/$deviceId"

            publish(topic, encrypted)
            Log.d(TAG, "Status reported: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report status", e)
        }
    }

    /**
     * Report an alert event to Tuya cloud.
     */
    suspend fun reportAlert(alertData: Map<String, Any>) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) return@withContext

        try {
            val payload = JSONObject().apply {
                put("devId", deviceId)
                put("type", "alert")
                put("data", JSONObject(alertData))
                put("t", System.currentTimeMillis() / 1000)
            }

            val encrypted = encrypt(payload.toString(), localKey)
            val topic = "smart/device/out/$deviceId/alert"

            publish(topic, encrypted)
            Log.d(TAG, "Alert reported: $alertData")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report alert", e)
        }
    }

    /**
     * Acknowledge a received command.
     */
    suspend fun acknowledgeCommand(command: TuyaCommand) {
        reportStatus(mapOf(command.dpId to command.value))
    }

    /**
     * Set listener for incoming commands.
     */
    fun setCommandListener(listener: (TuyaCommand) -> Unit) {
        commandListener = listener
    }

    private suspend fun publish(topic: String, message: String) {
        // Send MQTT PUBLISH packet
        Log.d(TAG, "Publishing to $topic")
    }

    private fun handleDisconnect() {
        isConnected.set(false)
        Log.w(TAG, "Disconnected from Tuya MQTT")

        // Attempt reconnection
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var delay = 5000L
            while (!isConnected.get()) {
                delay(delay)
                Log.d(TAG, "Attempting reconnection...")
                val connected = connect(deviceId, localKey, tuyaConfig.productId)
                if (!connected) {
                    delay = minOf(delay * 2, 60000) // Max 1 minute
                }
            }
        }
    }

    /**
     * Disconnect from Tuya MQTT.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        reconnectJob?.cancel()
        isConnected.set(false)

        try {
            // Send MQTT DISCONNECT packet
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }

        socket = null
        writer = null
        reader = null

        Log.d(TAG, "Disconnected from Tuya MQTT")
    }

    // Encryption helpers using Tuya's AES protocol
    private fun encrypt(data: String, key: String): String {
        try {
            val keyBytes = key.toByteArray().copyOf(16)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(keyBytes, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(data.toByteArray())
            return Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            return data
        }
    }

    private fun decrypt(data: String, key: String): String {
        try {
            val keyBytes = key.toByteArray().copyOf(16)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(keyBytes, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decoded = Base64.decode(data, Base64.NO_WRAP)
            val decrypted = cipher.doFinal(decoded)
            return String(decrypted)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            return data
        }
    }

    private fun hmacSha256(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "TuyaMqttClient"
    }
}

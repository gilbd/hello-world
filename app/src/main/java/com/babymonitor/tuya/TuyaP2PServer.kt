package com.babymonitor.tuya

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P2P server for Tuya video streaming.
 *
 * Implements Tuya's P2P protocol for direct video streaming
 * to SmartLife app viewers. This enables low-latency video
 * without going through cloud servers.
 *
 * Protocol overview:
 * 1. Device registers with Tuya P2P server
 * 2. SmartLife app requests P2P connection
 * 3. STUN/TURN negotiation for NAT traversal
 * 4. Direct UDP streaming to viewer
 */
@Singleton
class TuyaP2PServer @Inject constructor(
    private val tuyaConfig: TuyaConfig
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _streamingState = MutableStateFlow<StreamingState>(StreamingState.Idle)
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    private var udpSocket: DatagramSocket? = null
    private var deviceId: String = ""
    private var localKey: String = ""

    private val isRunning = AtomicBoolean(false)
    private val viewers = ConcurrentHashMap<String, ViewerSession>()

    private var videoDataCallback: (() -> ByteArray?)? = null
    private var audioDataCallback: (() -> ByteArray?)? = null

    /**
     * Start the P2P server for video streaming.
     */
    fun start(deviceId: String, localKey: String) {
        if (isRunning.get()) return

        this.deviceId = deviceId
        this.localKey = localKey

        scope.launch {
            try {
                isRunning.set(true)
                _streamingState.value = StreamingState.Starting

                // Initialize UDP socket for P2P
                udpSocket = DatagramSocket(P2P_PORT)

                // Register with Tuya P2P dispatch server
                registerWithDispatchServer()

                _streamingState.value = StreamingState.Ready
                Log.d(TAG, "P2P server started on port $P2P_PORT")

                // Start listening for connections
                startListening()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start P2P server", e)
                _streamingState.value = StreamingState.Error(e.message ?: "Start failed")
                isRunning.set(false)
            }
        }
    }

    /**
     * Stop the P2P server.
     */
    fun stop() {
        isRunning.set(false)
        scope.launch {
            try {
                // Disconnect all viewers
                viewers.values.forEach { it.disconnect() }
                viewers.clear()

                // Close socket
                udpSocket?.close()
                udpSocket = null

                _streamingState.value = StreamingState.Idle
                Log.d(TAG, "P2P server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping P2P server", e)
            }
        }
    }

    /**
     * Register with Tuya's P2P dispatch server.
     * This allows SmartLife app to discover this device for P2P connection.
     */
    private suspend fun registerWithDispatchServer() = withContext(Dispatchers.IO) {
        try {
            val dispatchHost = getDispatchServer()
            Log.d(TAG, "Registering with dispatch server: $dispatchHost")

            // Build registration packet
            val regPacket = buildRegistrationPacket()

            // Send to dispatch server
            val address = InetAddress.getByName(dispatchHost)
            val packet = DatagramPacket(regPacket, regPacket.size, address, DISPATCH_PORT)
            udpSocket?.send(packet)

            // Wait for acknowledgment
            val response = ByteArray(1024)
            val responsePacket = DatagramPacket(response, response.size)
            udpSocket?.soTimeout = 5000
            udpSocket?.receive(responsePacket)

            Log.d(TAG, "Registered with dispatch server")
        } catch (e: Exception) {
            Log.e(TAG, "Dispatch registration failed", e)
            throw e
        }
    }

    /**
     * Get the P2P dispatch server based on region.
     */
    private fun getDispatchServer(): String {
        return when (tuyaConfig.region) {
            TuyaRegion.CHINA -> "a1.tuyacn.com"
            TuyaRegion.US -> "a1.tuyaus.com"
            TuyaRegion.EU -> "a1.tuyaeu.com"
            TuyaRegion.INDIA -> "a1.tuyain.com"
        }
    }

    /**
     * Build device registration packet for P2P dispatch.
     */
    private fun buildRegistrationPacket(): ByteArray {
        val buffer = ByteBuffer.allocate(256)

        // Packet header
        buffer.putInt(PACKET_HEADER_MAGIC)
        buffer.putShort(PACKET_TYPE_REGISTER.toShort())
        buffer.putShort(0) // Reserved

        // Device info
        val deviceIdBytes = deviceId.toByteArray()
        buffer.putInt(deviceIdBytes.size)
        buffer.put(deviceIdBytes)

        // Protocol version
        buffer.putInt(PROTOCOL_VERSION)

        // Capabilities
        buffer.putInt(CAPABILITY_VIDEO or CAPABILITY_AUDIO or CAPABILITY_TALK)

        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }

    /**
     * Start listening for P2P connection requests.
     */
    private fun startListening() {
        scope.launch {
            val buffer = ByteArray(4096)

            while (isRunning.get()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.soTimeout = 1000
                    udpSocket?.receive(packet)

                    handlePacket(packet)
                } catch (e: java.net.SocketTimeoutException) {
                    // Normal timeout, continue listening
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        Log.e(TAG, "Error receiving packet", e)
                    }
                }
            }
        }
    }

    /**
     * Handle incoming P2P packet.
     */
    private suspend fun handlePacket(packet: DatagramPacket) {
        val data = packet.data.copyOf(packet.length)
        val senderAddress = packet.address
        val senderPort = packet.port

        if (data.size < 8) return

        val buffer = ByteBuffer.wrap(data)
        val magic = buffer.int
        if (magic != PACKET_HEADER_MAGIC) return

        val packetType = buffer.short.toInt()

        when (packetType) {
            PACKET_TYPE_CONNECT -> handleConnectRequest(senderAddress, senderPort, data)
            PACKET_TYPE_HEARTBEAT -> handleHeartbeat(senderAddress, senderPort)
            PACKET_TYPE_VIDEO_REQUEST -> handleVideoRequest(senderAddress, senderPort)
            PACKET_TYPE_AUDIO_REQUEST -> handleAudioRequest(senderAddress, senderPort)
            PACKET_TYPE_TALK_DATA -> handleTalkData(data)
            PACKET_TYPE_DISCONNECT -> handleDisconnect(senderAddress, senderPort)
        }
    }

    /**
     * Handle P2P connection request from SmartLife app.
     */
    private suspend fun handleConnectRequest(
        address: InetAddress,
        port: Int,
        data: ByteArray
    ) {
        try {
            val buffer = ByteBuffer.wrap(data)
            buffer.position(8) // Skip header

            // Read session ID
            val sessionIdLen = buffer.int
            val sessionIdBytes = ByteArray(sessionIdLen)
            buffer.get(sessionIdBytes)
            val sessionId = String(sessionIdBytes)

            // Read auth token
            val tokenLen = buffer.int
            val tokenBytes = ByteArray(tokenLen)
            buffer.get(tokenBytes)

            // Verify auth token
            if (!verifyAuthToken(tokenBytes)) {
                Log.w(TAG, "Invalid auth token from ${address.hostAddress}")
                sendConnectResponse(address, port, sessionId, false)
                return
            }

            // Create viewer session
            val session = ViewerSession(
                sessionId = sessionId,
                address = address,
                port = port
            )
            viewers[sessionId] = session

            _streamingState.value = StreamingState.Streaming(viewers.size)

            // Send success response
            sendConnectResponse(address, port, sessionId, true)

            // Start streaming to this viewer
            startStreamingToViewer(session)

            Log.d(TAG, "Viewer connected: $sessionId from ${address.hostAddress}:$port")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling connect request", e)
        }
    }

    /**
     * Verify authentication token from viewer.
     */
    private fun verifyAuthToken(token: ByteArray): Boolean {
        // Decrypt and verify token using local key
        try {
            val keyBytes = localKey.toByteArray().copyOf(16)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(keyBytes, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decrypted = String(cipher.doFinal(token))

            // Token format: deviceId|timestamp
            val parts = decrypted.split("|")
            if (parts.size != 2) return false
            if (parts[0] != deviceId) return false

            // Check timestamp (valid for 5 minutes)
            val timestamp = parts[1].toLongOrNull() ?: return false
            val now = System.currentTimeMillis() / 1000
            return (now - timestamp) < 300

        } catch (e: Exception) {
            Log.e(TAG, "Token verification failed", e)
            return false
        }
    }

    /**
     * Send connection response to viewer.
     */
    private suspend fun sendConnectResponse(
        address: InetAddress,
        port: Int,
        sessionId: String,
        success: Boolean
    ) = withContext(Dispatchers.IO) {
        val buffer = ByteBuffer.allocate(128)

        buffer.putInt(PACKET_HEADER_MAGIC)
        buffer.putShort(PACKET_TYPE_CONNECT_RESP.toShort())
        buffer.putShort(0)

        val sessionBytes = sessionId.toByteArray()
        buffer.putInt(sessionBytes.size)
        buffer.put(sessionBytes)
        buffer.put(if (success) 1 else 0)

        val data = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(data)

        val packet = DatagramPacket(data, data.size, address, port)
        udpSocket?.send(packet)
    }

    /**
     * Start streaming video/audio to a viewer.
     */
    private fun startStreamingToViewer(session: ViewerSession) {
        session.streamingJob = scope.launch {
            var frameNumber = 0L

            while (isRunning.get() && session.isConnected) {
                try {
                    // Get video frame
                    val videoData = videoDataCallback?.invoke()
                    if (videoData != null) {
                        sendVideoFrame(session, videoData, frameNumber++)
                    }

                    // Get audio data
                    val audioData = audioDataCallback?.invoke()
                    if (audioData != null) {
                        sendAudioData(session, audioData)
                    }

                    // ~30 FPS
                    delay(33)

                } catch (e: Exception) {
                    Log.e(TAG, "Streaming error", e)
                    session.disconnect()
                }
            }
        }
    }

    /**
     * Send video frame to viewer.
     */
    private suspend fun sendVideoFrame(
        session: ViewerSession,
        frameData: ByteArray,
        frameNumber: Long
    ) = withContext(Dispatchers.IO) {
        // Fragment large frames
        val maxPayload = MAX_PACKET_SIZE - HEADER_SIZE
        val fragments = (frameData.size + maxPayload - 1) / maxPayload

        for (i in 0 until fragments) {
            val offset = i * maxPayload
            val length = minOf(maxPayload, frameData.size - offset)

            val buffer = ByteBuffer.allocate(HEADER_SIZE + length)

            // Header
            buffer.putInt(PACKET_HEADER_MAGIC)
            buffer.putShort(PACKET_TYPE_VIDEO_DATA.toShort())
            buffer.putShort(0)

            // Frame info
            buffer.putLong(frameNumber)
            buffer.putInt(i) // Fragment index
            buffer.putInt(fragments) // Total fragments
            buffer.putInt(frameData.size) // Total frame size

            // Payload
            buffer.put(frameData, offset, length)

            val packet = ByteArray(buffer.position())
            buffer.flip()
            buffer.get(packet)

            val datagram = DatagramPacket(packet, packet.size, session.address, session.port)
            udpSocket?.send(datagram)
        }
    }

    /**
     * Send audio data to viewer.
     */
    private suspend fun sendAudioData(
        session: ViewerSession,
        audioData: ByteArray
    ) = withContext(Dispatchers.IO) {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + audioData.size)

        buffer.putInt(PACKET_HEADER_MAGIC)
        buffer.putShort(PACKET_TYPE_AUDIO_DATA.toShort())
        buffer.putShort(0)

        buffer.putLong(System.currentTimeMillis())
        buffer.put(audioData)

        val packet = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(packet)

        val datagram = DatagramPacket(packet, packet.size, session.address, session.port)
        udpSocket?.send(datagram)
    }

    /**
     * Handle heartbeat from viewer.
     */
    private suspend fun handleHeartbeat(address: InetAddress, port: Int) {
        val sessionId = viewers.entries.find {
            it.value.address == address && it.value.port == port
        }?.key ?: return

        viewers[sessionId]?.lastHeartbeat = System.currentTimeMillis()

        // Send heartbeat response
        val buffer = ByteBuffer.allocate(16)
        buffer.putInt(PACKET_HEADER_MAGIC)
        buffer.putShort(PACKET_TYPE_HEARTBEAT_RESP.toShort())
        buffer.putShort(0)
        buffer.putLong(System.currentTimeMillis())

        val data = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(data)

        withContext(Dispatchers.IO) {
            val packet = DatagramPacket(data, data.size, address, port)
            udpSocket?.send(packet)
        }
    }

    /**
     * Handle video stream request.
     */
    private fun handleVideoRequest(address: InetAddress, port: Int) {
        val session = viewers.entries.find {
            it.value.address == address && it.value.port == port
        }?.value ?: return

        session.videoEnabled = true
        Log.d(TAG, "Video streaming enabled for ${session.sessionId}")
    }

    /**
     * Handle audio stream request.
     */
    private fun handleAudioRequest(address: InetAddress, port: Int) {
        val session = viewers.entries.find {
            it.value.address == address && it.value.port == port
        }?.value ?: return

        session.audioEnabled = true
        Log.d(TAG, "Audio streaming enabled for ${session.sessionId}")
    }

    /**
     * Handle two-way talk data from viewer.
     */
    private fun handleTalkData(data: ByteArray) {
        // Extract audio and play through speaker
        val buffer = ByteBuffer.wrap(data)
        buffer.position(8) // Skip header

        val audioData = ByteArray(buffer.remaining())
        buffer.get(audioData)

        onTalkDataReceived?.invoke(audioData)
    }

    /**
     * Handle viewer disconnect.
     */
    private fun handleDisconnect(address: InetAddress, port: Int) {
        val sessionId = viewers.entries.find {
            it.value.address == address && it.value.port == port
        }?.key ?: return

        viewers[sessionId]?.disconnect()
        viewers.remove(sessionId)

        val viewerCount = viewers.size
        _streamingState.value = if (viewerCount > 0) {
            StreamingState.Streaming(viewerCount)
        } else {
            StreamingState.Ready
        }

        Log.d(TAG, "Viewer disconnected: $sessionId")
    }

    /**
     * Set callback for video data.
     */
    fun setVideoDataCallback(callback: () -> ByteArray?) {
        videoDataCallback = callback
    }

    /**
     * Set callback for audio data.
     */
    fun setAudioDataCallback(callback: () -> ByteArray?) {
        audioDataCallback = callback
    }

    /**
     * Callback for two-way talk audio received from viewer.
     */
    var onTalkDataReceived: ((ByteArray) -> Unit)? = null

    /**
     * Get current viewer count.
     */
    fun getViewerCount(): Int = viewers.size

    companion object {
        private const val TAG = "TuyaP2PServer"

        private const val P2P_PORT = 6668
        private const val DISPATCH_PORT = 6667

        private const val PACKET_HEADER_MAGIC = 0x55AA55AA.toInt()
        private const val PROTOCOL_VERSION = 3

        // Packet types
        private const val PACKET_TYPE_REGISTER = 0x01
        private const val PACKET_TYPE_CONNECT = 0x10
        private const val PACKET_TYPE_CONNECT_RESP = 0x11
        private const val PACKET_TYPE_DISCONNECT = 0x12
        private const val PACKET_TYPE_HEARTBEAT = 0x20
        private const val PACKET_TYPE_HEARTBEAT_RESP = 0x21
        private const val PACKET_TYPE_VIDEO_REQUEST = 0x30
        private const val PACKET_TYPE_VIDEO_DATA = 0x31
        private const val PACKET_TYPE_AUDIO_REQUEST = 0x40
        private const val PACKET_TYPE_AUDIO_DATA = 0x41
        private const val PACKET_TYPE_TALK_DATA = 0x42

        // Capabilities
        private const val CAPABILITY_VIDEO = 0x01
        private const val CAPABILITY_AUDIO = 0x02
        private const val CAPABILITY_TALK = 0x04

        private const val MAX_PACKET_SIZE = 1400
        private const val HEADER_SIZE = 32
    }
}

/**
 * Represents a connected viewer session.
 */
class ViewerSession(
    val sessionId: String,
    val address: InetAddress,
    val port: Int
) {
    var isConnected = true
    var lastHeartbeat = System.currentTimeMillis()
    var videoEnabled = false
    var audioEnabled = false
    var streamingJob: Job? = null

    fun disconnect() {
        isConnected = false
        streamingJob?.cancel()
    }
}

/**
 * P2P streaming state.
 */
sealed class StreamingState {
    object Idle : StreamingState()
    object Starting : StreamingState()
    object Ready : StreamingState()
    data class Streaming(val viewerCount: Int) : StreamingState()
    data class Error(val message: String) : StreamingState()
}

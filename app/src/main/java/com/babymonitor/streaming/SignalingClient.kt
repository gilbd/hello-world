package com.babymonitor.streaming

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signaling client for WebRTC connection establishment.
 *
 * This implementation uses WebSocket for signaling.
 * For production, you would connect to your own signaling server.
 *
 * The signaling flow:
 * 1. Camera (host) creates a room with a unique code
 * 2. Viewer joins the room using the code
 * 3. They exchange SDP offers/answers and ICE candidates
 * 4. Direct peer-to-peer connection is established
 */
@Singleton
class SignalingClient @Inject constructor() {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _messages = Channel<SignalingMessage>(Channel.BUFFERED)
    val messages: Flow<SignalingMessage> = _messages.receiveAsFlow()

    private var currentRoomCode: String? = null
    private var isHost: Boolean = false

    /**
     * Connect to signaling server and join/create a room
     */
    suspend fun connect(roomCode: String, asHost: Boolean = false) {
        currentRoomCode = roomCode
        isHost = asHost

        // For local network discovery without a server, we can use mDNS or
        // a simple local server. This example shows the WebSocket approach.
        //
        // In production, replace with your signaling server URL:
        // val serverUrl = "wss://your-signaling-server.com/ws?room=$roomCode&host=$asHost"

        // For development/local use, you can run a simple signaling server
        // or use a peer-to-peer discovery mechanism
        val serverUrl = buildSignalingUrl(roomCode, asHost)

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
        Log.d(TAG, "Connecting to signaling server: $serverUrl")
    }

    private fun buildSignalingUrl(roomCode: String, asHost: Boolean): String {
        // This is a placeholder. In production, use your own signaling server.
        // For local testing, you can use:
        // 1. A local Node.js/Python signaling server
        // 2. Firebase Realtime Database
        // 3. A hosted service like PubNub, Ably, or your own backend
        return "wss://localhost:8080/signal?room=$roomCode&host=$asHost"
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                val type = json.getString("type")

                when (type) {
                    "offer" -> {
                        val sdp = SessionDescription(
                            SessionDescription.Type.OFFER,
                            json.getString("sdp")
                        )
                        _messages.trySend(SignalingMessage.Offer(sdp))
                    }
                    "answer" -> {
                        val sdp = SessionDescription(
                            SessionDescription.Type.ANSWER,
                            json.getString("sdp")
                        )
                        _messages.trySend(SignalingMessage.Answer(sdp))
                    }
                    "ice_candidate" -> {
                        val candidate = IceCandidate(
                            json.getString("sdpMid"),
                            json.getInt("sdpMLineIndex"),
                            json.getString("candidate")
                        )
                        _messages.trySend(SignalingMessage.IceCandidate(candidate))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing signaling message", e)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error: ${t.message}")
            // For local development without a signaling server,
            // we can fall back to manual exchange or local discovery
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
        }
    }

    suspend fun sendOffer(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp.description)
            put("room", currentRoomCode)
        }
        webSocket?.send(json.toString())
        Log.d(TAG, "Sent offer")
    }

    suspend fun sendAnswer(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp.description)
            put("room", currentRoomCode)
        }
        webSocket?.send(json.toString())
        Log.d(TAG, "Sent answer")
    }

    suspend fun sendIceCandidate(candidate: IceCandidate) {
        val json = JSONObject().apply {
            put("type", "ice_candidate")
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
            put("room", currentRoomCode)
        }
        webSocket?.send(json.toString())
    }

    suspend fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        currentRoomCode = null
    }

    companion object {
        private const val TAG = "SignalingClient"
    }
}

sealed class SignalingMessage {
    data class Offer(val sdp: SessionDescription) : SignalingMessage()
    data class Answer(val sdp: SessionDescription) : SignalingMessage()
    data class IceCandidate(val candidate: org.webrtc.IceCandidate) : SignalingMessage()
}

package com.babymonitor.streaming

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signalingClient: SignalingClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private val _streamingState = MutableStateFlow<StreamingState>(StreamingState.Idle)
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val eglBase: EglBase by lazy { EglBase.create() }

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun initialize() {
        Log.d(TAG, "Initializing WebRTC")

        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        Log.d(TAG, "WebRTC initialized")
    }

    fun startStreaming(videoSource: VideoSource? = null): String {
        _streamingState.value = StreamingState.Connecting

        // Generate a unique room code for this session
        val code = generateRoomCode()
        _roomCode.value = code

        try {
            createPeerConnection()
            setupLocalTracks(videoSource)
            createOffer()

            _streamingState.value = StreamingState.Streaming(code)
            Log.d(TAG, "Streaming started with room code: $code")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming", e)
            _streamingState.value = StreamingState.Error(e.message ?: "Unknown error")
        }

        return code
    }

    fun joinStream(roomCode: String, remoteRenderer: SurfaceViewRenderer) {
        _streamingState.value = StreamingState.Connecting
        _roomCode.value = roomCode

        try {
            createPeerConnection(remoteRenderer)

            scope.launch {
                signalingClient.connect(roomCode)
                signalingClient.messages.collect { message ->
                    handleSignalingMessage(message)
                }
            }

            Log.d(TAG, "Joining stream with room code: $roomCode")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to join stream", e)
            _streamingState.value = StreamingState.Error(e.message ?: "Unknown error")
        }
    }

    private fun createPeerConnection(remoteRenderer: SurfaceViewRenderer? = null) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE connection state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        _streamingState.value = StreamingState.Streaming(_roomCode.value ?: "")
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        _streamingState.value = StreamingState.Disconnected
                    }
                    else -> {}
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE gathering state: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    scope.launch {
                        signalingClient.sendIceCandidate(it)
                    }
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "Stream added")
                stream?.videoTracks?.firstOrNull()?.let { track ->
                    remoteRenderer?.let { renderer ->
                        track.addSink(renderer)
                    }
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "Stream removed")
            }

            override fun onDataChannel(channel: DataChannel?) {}

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "Track added")
            }
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
    }

    private fun setupLocalTracks(videoSource: VideoSource?) {
        // Create audio track
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)

        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track)
        }

        // Create video track if we have a source
        videoSource?.let { source ->
            localVideoTrack = peerConnectionFactory?.createVideoTrack("video_track", source)
            localVideoTrack?.let { track ->
                peerConnection?.addTrack(track)
            }
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            scope.launch {
                                signalingClient.sendOffer(it)
                            }
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, it)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Create offer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun handleSignalingMessage(message: SignalingMessage) {
        when (message) {
            is SignalingMessage.Offer -> {
                handleRemoteOffer(message.sdp)
            }
            is SignalingMessage.Answer -> {
                handleRemoteAnswer(message.sdp)
            }
            is SignalingMessage.IceCandidate -> {
                handleRemoteIceCandidate(message.candidate)
            }
        }
    }

    private fun handleRemoteOffer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                createAnswer()
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Set remote offer failed: $error")
            }
        }, sdp)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            scope.launch {
                                signalingClient.sendAnswer(it)
                            }
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, it)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Create answer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun handleRemoteAnswer(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote answer set successfully")
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Set remote answer failed: $error")
            }
        }, sdp)
    }

    private fun handleRemoteIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun stopStreaming() {
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()

        localVideoTrack = null
        localAudioTrack = null
        videoCapturer = null
        surfaceTextureHelper = null
        peerConnection = null

        _streamingState.value = StreamingState.Idle
        _roomCode.value = null

        scope.launch {
            signalingClient.disconnect()
        }

        Log.d(TAG, "Streaming stopped")
    }

    fun getEglContext() = eglBase.eglBaseContext

    fun shutdown() {
        stopStreaming()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase.release()
        scope.cancel()
    }

    private fun generateRoomCode(): String {
        // Generate a 6-character alphanumeric code
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    companion object {
        private const val TAG = "StreamingManager"
    }
}

sealed class StreamingState {
    object Idle : StreamingState()
    object Connecting : StreamingState()
    data class Streaming(val roomCode: String) : StreamingState()
    object Disconnected : StreamingState()
    data class Error(val message: String) : StreamingState()
}

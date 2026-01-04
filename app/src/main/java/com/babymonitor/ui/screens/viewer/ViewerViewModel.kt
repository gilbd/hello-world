package com.babymonitor.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymonitor.streaming.StreamingManager
import com.babymonitor.streaming.StreamingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val streamingManager: StreamingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var renderer: SurfaceViewRenderer? = null

    init {
        viewModelScope.launch {
            streamingManager.streamingState.collect { state ->
                _uiState.update {
                    it.copy(
                        isConnecting = state is StreamingState.Connecting,
                        isConnected = state is StreamingState.Streaming,
                        errorMessage = if (state is StreamingState.Error) state.message else null
                    )
                }
            }
        }
    }

    fun updateRoomCode(code: String) {
        _uiState.update {
            it.copy(
                enteredRoomCode = code,
                isRoomCodeValid = code.length == 6
            )
        }
    }

    fun initRenderer(surfaceViewRenderer: SurfaceViewRenderer) {
        renderer = surfaceViewRenderer
        try {
            surfaceViewRenderer.init(streamingManager.getEglContext(), null)
        } catch (e: Exception) {
            // Already initialized
        }
    }

    fun connect(surfaceViewRenderer: SurfaceViewRenderer?) {
        val roomCode = _uiState.value.enteredRoomCode
        if (roomCode.length != 6) return

        viewModelScope.launch {
            streamingManager.initialize()
            surfaceViewRenderer?.let {
                streamingManager.joinStream(roomCode, it)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            streamingManager.stopStreaming()
        }
        _uiState.update {
            it.copy(
                isConnected = false,
                isConnecting = false
            )
        }
    }

    fun toggleMute() {
        _uiState.update {
            it.copy(isMuted = !it.isMuted)
        }
        // TODO: Implement actual audio muting via WebRTC
    }

    fun dismissAlert() {
        _uiState.update {
            it.copy(
                hasAlert = false,
                alertMessage = null,
                alertType = null
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        renderer?.release()
        disconnect()
    }
}

data class ViewerUiState(
    val enteredRoomCode: String = "",
    val isRoomCodeValid: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val hasAlert: Boolean = false,
    val alertMessage: String? = null,
    val alertType: String? = null,
    val errorMessage: String? = null
)

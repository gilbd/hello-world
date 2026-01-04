package com.babymonitor.ui.screens.camera

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymonitor.camera.CameraManager
import com.babymonitor.camera.CameraState
import com.babymonitor.detection.AlertManager
import com.babymonitor.detection.AudioAnalyzer
import com.babymonitor.detection.MotionDetector
import com.babymonitor.streaming.StreamingManager
import com.babymonitor.streaming.StreamingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val motionDetector: MotionDetector,
    private val audioAnalyzer: AudioAnalyzer,
    private val alertManager: AlertManager,
    private val streamingManager: StreamingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        // Set up motion detector as frame analyzer
        cameraManager.setFrameAnalyzer(motionDetector)

        // Observe camera state
        viewModelScope.launch {
            cameraManager.cameraState.collect { state ->
                _uiState.update {
                    it.copy(cameraState = state)
                }
            }
        }

        // Observe motion detection
        viewModelScope.launch {
            motionDetector.motionState.collect { state ->
                if (state.isMotionDetected && _uiState.value.isMotionDetectionEnabled) {
                    alertManager.onMotionDetected(state)
                }
                _uiState.update {
                    it.copy(
                        motionDetected = state.isMotionDetected,
                        motionLevel = state.motionLevel
                    )
                }
            }
        }

        // Observe audio analysis
        viewModelScope.launch {
            audioAnalyzer.audioState.collect { state ->
                if (state.isCryDetected && _uiState.value.isCryDetectionEnabled) {
                    alertManager.onCryDetected(state)
                }
                _uiState.update {
                    it.copy(
                        cryDetected = state.isCryDetected,
                        audioLevel = state.volume
                    )
                }
            }
        }

        // Observe alerts
        viewModelScope.launch {
            alertManager.alertState.collect { state ->
                _uiState.update {
                    it.copy(
                        hasActiveAlert = state.hasActiveAlert,
                        currentAlertMessage = state.currentAlert?.message
                    )
                }
            }
        }

        // Observe streaming state
        viewModelScope.launch {
            streamingManager.streamingState.collect { state ->
                _uiState.update {
                    it.copy(
                        isStreaming = state is StreamingState.Streaming,
                        streamingState = state
                    )
                }
            }
        }

        // Observe room code
        viewModelScope.launch {
            streamingManager.roomCode.collect { code ->
                _uiState.update {
                    it.copy(roomCode = code)
                }
            }
        }

        // Observe night vision
        viewModelScope.launch {
            cameraManager.isNightVisionEnabled.collect { enabled ->
                _uiState.update {
                    it.copy(isNightVisionEnabled = enabled)
                }
            }
        }
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraManager.startCamera(lifecycleOwner, previewView)
    }

    fun toggleStreaming() {
        viewModelScope.launch {
            if (_uiState.value.isStreaming) {
                streamingManager.stopStreaming()
            } else {
                streamingManager.initialize()
                streamingManager.startStreaming()
            }
        }
    }

    fun toggleMotionDetection() {
        val newValue = !_uiState.value.isMotionDetectionEnabled
        motionDetector.configure(enabled = newValue)
        _uiState.update {
            it.copy(isMotionDetectionEnabled = newValue)
        }
    }

    fun toggleCryDetection() {
        val newValue = !_uiState.value.isCryDetectionEnabled
        audioAnalyzer.configure(enabled = newValue)

        if (newValue) {
            audioAnalyzer.startAnalysis()
        } else {
            audioAnalyzer.stopAnalysis()
        }

        _uiState.update {
            it.copy(isCryDetectionEnabled = newValue)
        }
    }

    fun toggleNightVision() {
        cameraManager.toggleNightVision()
    }

    fun dismissAlert() {
        alertManager.dismissAlert()
    }

    fun stopMonitoring() {
        cameraManager.stopCamera()
        audioAnalyzer.stopAnalysis()
        streamingManager.stopStreaming()
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}

data class CameraUiState(
    val cameraState: CameraState = CameraState.Idle,
    val isStreaming: Boolean = false,
    val streamingState: StreamingState = StreamingState.Idle,
    val roomCode: String? = null,
    val isMotionDetectionEnabled: Boolean = true,
    val isCryDetectionEnabled: Boolean = true,
    val motionDetected: Boolean = false,
    val motionLevel: Float = 0f,
    val cryDetected: Boolean = false,
    val audioLevel: Float = 0f,
    val hasActiveAlert: Boolean = false,
    val currentAlertMessage: String? = null,
    val isNightVisionEnabled: Boolean = false
)

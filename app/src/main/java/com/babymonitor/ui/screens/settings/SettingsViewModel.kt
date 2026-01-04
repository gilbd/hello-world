package com.babymonitor.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymonitor.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        motionDetectionEnabled = settings.motionDetectionEnabled,
                        motionSensitivity = settings.motionSensitivity,
                        cryDetectionEnabled = settings.cryDetectionEnabled,
                        crySensitivity = settings.crySensitivity,
                        volumeThreshold = settings.volumeThreshold,
                        notificationsEnabled = settings.notificationsEnabled,
                        vibrationEnabled = settings.vibrationEnabled,
                        soundEnabled = settings.soundEnabled,
                        videoResolution = settings.videoResolution,
                        frameRate = settings.frameRate
                    )
                }
            }
        }
    }

    fun setMotionDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMotionDetectionEnabled(enabled)
        }
    }

    fun setMotionSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            settingsRepository.setMotionSensitivity(sensitivity)
        }
    }

    fun setCryDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCryDetectionEnabled(enabled)
        }
    }

    fun setCrySensitivity(sensitivity: Float) {
        viewModelScope.launch {
            settingsRepository.setCrySensitivity(sensitivity)
        }
    }

    fun setVolumeThreshold(threshold: Float) {
        viewModelScope.launch {
            settingsRepository.setVolumeThreshold(threshold)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
        }
    }

    fun setVideoResolution(resolution: String) {
        viewModelScope.launch {
            settingsRepository.setVideoResolution(resolution)
        }
    }

    fun setFrameRate(frameRate: String) {
        viewModelScope.launch {
            settingsRepository.setFrameRate(frameRate)
        }
    }
}

data class SettingsUiState(
    val motionDetectionEnabled: Boolean = true,
    val motionSensitivity: Float = 0.5f,
    val cryDetectionEnabled: Boolean = true,
    val crySensitivity: Float = 0.5f,
    val volumeThreshold: Float = 0.15f,
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val videoResolution: String = "720p",
    val frameRate: String = "24 fps"
)

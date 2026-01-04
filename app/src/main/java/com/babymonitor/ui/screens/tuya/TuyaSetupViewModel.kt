package com.babymonitor.ui.screens.tuya

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babymonitor.tuya.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TuyaSetupViewModel @Inject constructor(
    private val tuyaConfig: TuyaConfig,
    private val tuyaDeviceManager: TuyaDeviceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TuyaSetupUiState>(TuyaSetupUiState.NotConfigured)
    val uiState: StateFlow<TuyaSetupUiState> = _uiState.asStateFlow()

    val deviceState: StateFlow<TuyaDeviceState> = tuyaDeviceManager.deviceState
    val connectionState: StateFlow<TuyaConnectionState> = tuyaDeviceManager.connectionState

    private var qrCodeData: String = ""

    init {
        // Load saved configuration
        tuyaConfig.loadConfig()

        if (tuyaConfig.isConfigured()) {
            _uiState.value = TuyaSetupUiState.Configured
            // Initialize device manager
            tuyaDeviceManager.initialize()
        }
    }

    fun configure(
        accessId: String,
        accessSecret: String,
        productId: String,
        productKey: String,
        region: TuyaRegion
    ) {
        tuyaConfig.configure(
            accessId = accessId,
            accessSecret = accessSecret,
            productId = productId,
            productKey = productKey,
            region = region
        )

        _uiState.value = TuyaSetupUiState.Configured
    }

    fun startQRPairing() {
        viewModelScope.launch {
            qrCodeData = tuyaDeviceManager.generatePairingQRCode()
            // The device state will be updated by the manager
            // Start registration process
            tuyaDeviceManager.registerDevice()
        }
    }

    fun startAPPairing() {
        viewModelScope.launch {
            tuyaDeviceManager.startAPPairing()
        }
    }

    fun getQRCodeData(): String {
        if (qrCodeData.isEmpty()) {
            qrCodeData = tuyaDeviceManager.generatePairingQRCode()
        }
        return qrCodeData
    }

    fun cancelPairing() {
        viewModelScope.launch {
            tuyaDeviceManager.unregisterDevice()
        }
    }

    fun disconnect() {
        tuyaDeviceManager.disconnect()
    }

    fun unregister() {
        viewModelScope.launch {
            tuyaDeviceManager.unregisterDevice()
        }
    }

    fun retry() {
        if (tuyaConfig.isConfigured()) {
            _uiState.value = TuyaSetupUiState.Configured
            tuyaDeviceManager.initialize()
        } else {
            _uiState.value = TuyaSetupUiState.NotConfigured
        }
    }

    fun clearConfiguration() {
        viewModelScope.launch {
            tuyaDeviceManager.unregisterDevice()
            tuyaConfig.clearConfig()
            _uiState.value = TuyaSetupUiState.NotConfigured
        }
    }
}

sealed class TuyaSetupUiState {
    object NotConfigured : TuyaSetupUiState()
    object Configured : TuyaSetupUiState()
    data class Error(val message: String) : TuyaSetupUiState()
}

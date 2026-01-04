package com.babymonitor.detection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

@Singleton
class AudioAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var analysisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    // Configuration
    var isEnabled: Boolean = true
        private set
    var volumeThreshold: Float = DEFAULT_VOLUME_THRESHOLD
        private set
    var cryDetectionSensitivity: Float = DEFAULT_CRY_SENSITIVITY
        private set

    // Cry detection parameters
    private val cryFrequencyRangeLow = 250f  // Hz - typical baby cry starts
    private val cryFrequencyRangeHigh = 600f // Hz - typical baby cry peak
    private val cryDurationThreshold = 500L  // ms - minimum cry duration
    private var lastCryStartTime: Long = 0
    private var consecutiveCryFrames = 0

    fun configure(
        enabled: Boolean = isEnabled,
        volumeThreshold: Float = this.volumeThreshold,
        crySensitivity: Float = cryDetectionSensitivity
    ) {
        this.isEnabled = enabled
        this.volumeThreshold = volumeThreshold.coerceIn(0.01f, 1f)
        this.cryDetectionSensitivity = crySensitivity.coerceIn(0.1f, 1f)
    }

    fun startAnalysis() {
        if (!isEnabled) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio permission not granted")
            return
        }

        if (isRecording) return

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * 2

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            analysisJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)

                while (isActive && isRecording) {
                    val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1

                    if (readResult > 0) {
                        analyzeAudioBuffer(buffer, readResult)
                    }

                    delay(50) // Analyze every 50ms
                }
            }

            Log.d(TAG, "Audio analysis started")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio analysis", e)
            stopAnalysis()
        }
    }

    fun stopAnalysis() {
        isRecording = false
        analysisJob?.cancel()
        analysisJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio record", e)
        }
        audioRecord = null

        _audioState.value = AudioState()
        Log.d(TAG, "Audio analysis stopped")
    }

    private fun analyzeAudioBuffer(buffer: ShortArray, size: Int) {
        // Calculate RMS volume
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / size)
        val normalizedVolume = (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)

        // Calculate decibels
        val decibels = if (rms > 0) 20 * log10(rms / Short.MAX_VALUE) else -100.0

        // Perform simple frequency analysis using zero-crossing rate
        val zeroCrossingRate = calculateZeroCrossingRate(buffer, size)
        val estimatedFrequency = zeroCrossingRate * SAMPLE_RATE / 2

        // Detect cry based on volume and frequency characteristics
        val isCryLikeSound = isCryDetected(
            normalizedVolume,
            estimatedFrequency,
            decibels.toFloat()
        )

        val currentTime = System.currentTimeMillis()
        var isCryConfirmed = false

        if (isCryLikeSound) {
            if (consecutiveCryFrames == 0) {
                lastCryStartTime = currentTime
            }
            consecutiveCryFrames++

            // Confirm cry if it persists for the threshold duration
            if (currentTime - lastCryStartTime >= cryDurationThreshold) {
                isCryConfirmed = true
            }
        } else {
            consecutiveCryFrames = 0
        }

        _audioState.value = AudioState(
            volume = normalizedVolume,
            decibels = decibels.toFloat(),
            estimatedFrequency = estimatedFrequency,
            isCryDetected = isCryConfirmed,
            isLoudSound = normalizedVolume > volumeThreshold,
            timestamp = currentTime
        )

        if (isCryConfirmed) {
            Log.d(TAG, "Baby cry detected! Volume: $normalizedVolume, Freq: $estimatedFrequency Hz")
        }
    }

    private fun calculateZeroCrossingRate(buffer: ShortArray, size: Int): Float {
        var crossings = 0
        for (i in 1 until size) {
            if ((buffer[i] >= 0 && buffer[i - 1] < 0) ||
                (buffer[i] < 0 && buffer[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / size
    }

    private fun isCryDetected(
        volume: Float,
        frequency: Float,
        decibels: Float
    ): Boolean {
        // Adjusted thresholds based on sensitivity
        val adjustedVolumeThreshold = volumeThreshold * (1.5f - cryDetectionSensitivity)

        // Check if volume is significant
        if (volume < adjustedVolumeThreshold) return false

        // Check if frequency is in baby cry range
        val isInCryFrequencyRange = frequency in cryFrequencyRangeLow..cryFrequencyRangeHigh

        // Baby cries typically are above 60dB
        val isLoudEnough = decibels > -40f

        return isInCryFrequencyRange && isLoudEnough
    }

    fun reset() {
        consecutiveCryFrames = 0
        lastCryStartTime = 0
        _audioState.value = AudioState()
    }

    fun shutdown() {
        stopAnalysis()
        scope.cancel()
    }

    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val DEFAULT_VOLUME_THRESHOLD = 0.15f
        const val DEFAULT_CRY_SENSITIVITY = 0.5f
    }
}

data class AudioState(
    val volume: Float = 0f,
    val decibels: Float = -100f,
    val estimatedFrequency: Float = 0f,
    val isCryDetected: Boolean = false,
    val isLoudSound: Boolean = false,
    val timestamp: Long = 0
)

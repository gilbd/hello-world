package com.babymonitor.shared.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Cross-platform baby cry detection engine.
 * Analyzes audio samples to detect baby crying using frequency and volume analysis.
 */
class CryDetectionEngine {

    private val _audioState = MutableStateFlow(AudioResult())
    val audioState: StateFlow<AudioResult> = _audioState.asStateFlow()

    var isEnabled: Boolean = true
        private set
    var volumeThreshold: Float = DEFAULT_VOLUME_THRESHOLD
        private set
    var cryDetectionSensitivity: Float = DEFAULT_CRY_SENSITIVITY
        private set

    // Baby cry frequency characteristics
    private val cryFrequencyRangeLow = 250f   // Hz
    private val cryFrequencyRangeHigh = 600f  // Hz
    private val cryDurationThreshold = 500L   // ms

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

    /**
     * Analyze audio samples for cry detection.
     * @param samples Audio samples as normalized floats (-1.0 to 1.0)
     * @param sampleRate Sample rate in Hz (e.g., 44100)
     * @return AudioResult with detection status
     */
    fun analyzeAudio(samples: FloatArray, sampleRate: Int): AudioResult {
        if (!isEnabled || samples.isEmpty()) {
            return AudioResult()
        }

        // Calculate RMS volume
        var sum = 0.0
        for (sample in samples) {
            sum += sample * sample
        }
        val rms = sqrt(sum / samples.size).toFloat()
        val normalizedVolume = rms.coerceIn(0f, 1f)

        // Calculate decibels
        val decibels = if (rms > 0) (20 * log10(rms.toDouble())).toFloat() else -100f

        // Estimate frequency using zero-crossing rate
        val zeroCrossingRate = calculateZeroCrossingRate(samples)
        val estimatedFrequency = zeroCrossingRate * sampleRate / 2

        // Detect cry based on characteristics
        val isCryLikeSound = isCryDetected(normalizedVolume, estimatedFrequency, decibels)

        val currentTime = currentTimeMillis()
        var isCryConfirmed = false

        if (isCryLikeSound) {
            if (consecutiveCryFrames == 0) {
                lastCryStartTime = currentTime
            }
            consecutiveCryFrames++

            if (currentTime - lastCryStartTime >= cryDurationThreshold) {
                isCryConfirmed = true
            }
        } else {
            consecutiveCryFrames = 0
        }

        val result = AudioResult(
            volume = normalizedVolume,
            decibels = decibels,
            estimatedFrequency = estimatedFrequency,
            isCryDetected = isCryConfirmed,
            isLoudSound = normalizedVolume > volumeThreshold,
            timestamp = currentTime
        )

        _audioState.value = result
        return result
    }

    /**
     * Analyze audio samples from Short array (common format from platform APIs).
     */
    fun analyzeAudioShort(samples: ShortArray, sampleRate: Int): AudioResult {
        val floatSamples = FloatArray(samples.size) { i ->
            samples[i].toFloat() / Short.MAX_VALUE
        }
        return analyzeAudio(floatSamples, sampleRate)
    }

    private fun calculateZeroCrossingRate(samples: FloatArray): Float {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0 && samples[i - 1] < 0) ||
                (samples[i] < 0 && samples[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / samples.size
    }

    private fun isCryDetected(volume: Float, frequency: Float, decibels: Float): Boolean {
        val adjustedVolumeThreshold = volumeThreshold * (1.5f - cryDetectionSensitivity)

        if (volume < adjustedVolumeThreshold) return false

        val isInCryFrequencyRange = frequency in cryFrequencyRangeLow..cryFrequencyRangeHigh
        val isLoudEnough = decibels > -40f

        return isInCryFrequencyRange && isLoudEnough
    }

    fun reset() {
        consecutiveCryFrames = 0
        lastCryStartTime = 0
        _audioState.value = AudioResult()
    }

    companion object {
        const val DEFAULT_VOLUME_THRESHOLD = 0.15f
        const val DEFAULT_CRY_SENSITIVITY = 0.5f
        const val RECOMMENDED_SAMPLE_RATE = 44100
    }
}

data class AudioResult(
    val volume: Float = 0f,
    val decibels: Float = -100f,
    val estimatedFrequency: Float = 0f,
    val isCryDetected: Boolean = false,
    val isLoudSound: Boolean = false,
    val timestamp: Long = 0
)

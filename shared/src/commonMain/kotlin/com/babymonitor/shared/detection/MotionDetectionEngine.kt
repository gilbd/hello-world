package com.babymonitor.shared.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Cross-platform motion detection engine.
 * Analyzes luminance data from camera frames to detect movement.
 */
class MotionDetectionEngine {

    private var previousFrame: IntArray? = null
    private var frameCount = 0
    private val analyzeEveryNFrames = 3

    private val _motionState = MutableStateFlow(MotionResult())
    val motionState: StateFlow<MotionResult> = _motionState.asStateFlow()

    var sensitivityThreshold: Int = DEFAULT_SENSITIVITY
        private set
    var motionAreaThreshold: Float = DEFAULT_MOTION_AREA_THRESHOLD
        private set
    var isEnabled: Boolean = true
        private set

    fun configure(
        sensitivity: Int = sensitivityThreshold,
        motionAreaThreshold: Float = this.motionAreaThreshold,
        enabled: Boolean = isEnabled
    ) {
        this.sensitivityThreshold = sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
        this.motionAreaThreshold = motionAreaThreshold.coerceIn(0.001f, 0.5f)
        this.isEnabled = enabled
    }

    /**
     * Analyze a frame represented as luminance values.
     * @param luminanceData Array of luminance values (0-255)
     * @param width Frame width
     * @param height Frame height
     * @return true if motion was detected
     */
    fun analyzeFrame(luminanceData: IntArray, width: Int, height: Int): Boolean {
        if (!isEnabled) return false

        frameCount++
        if (frameCount % analyzeEveryNFrames != 0) return false

        val result = previousFrame?.let { prevFrame ->
            if (prevFrame.size == luminanceData.size) {
                detectMotion(prevFrame, luminanceData)
            } else null
        } ?: MotionResult()

        previousFrame = luminanceData.copyOf()
        _motionState.value = result

        return result.isMotionDetected
    }

    /**
     * Downsample frame for efficient processing.
     * Call this before analyzeFrame if working with full resolution.
     */
    fun downsampleFrame(
        data: IntArray,
        originalWidth: Int,
        originalHeight: Int,
        factor: Int = 4
    ): IntArray {
        val dsWidth = originalWidth / factor
        val dsHeight = originalHeight / factor
        val result = IntArray(dsWidth * dsHeight)

        for (y in 0 until dsHeight) {
            for (x in 0 until dsWidth) {
                val srcY = y * factor
                val srcX = x * factor
                result[y * dsWidth + x] = data[srcY * originalWidth + srcX]
            }
        }

        return result
    }

    private fun detectMotion(prevFrame: IntArray, currentFrame: IntArray): MotionResult {
        var changedPixels = 0
        val totalPixels = currentFrame.size
        var maxDiff = 0
        var sumDiff = 0L

        for (i in currentFrame.indices) {
            val diff = abs(currentFrame[i] - prevFrame[i])
            if (diff > sensitivityThreshold) {
                changedPixels++
                sumDiff += diff
                if (diff > maxDiff) maxDiff = diff
            }
        }

        val motionRatio = changedPixels.toFloat() / totalPixels
        val isMotionDetected = motionRatio > motionAreaThreshold
        val avgDiff = if (changedPixels > 0) (sumDiff / changedPixels).toInt() else 0

        return MotionResult(
            isMotionDetected = isMotionDetected,
            motionLevel = motionRatio,
            changedPixelCount = changedPixels,
            averageIntensity = avgDiff,
            timestamp = currentTimeMillis()
        )
    }

    fun reset() {
        previousFrame = null
        frameCount = 0
        _motionState.value = MotionResult()
    }

    companion object {
        const val MIN_SENSITIVITY = 10
        const val MAX_SENSITIVITY = 100
        const val DEFAULT_SENSITIVITY = 30
        const val DEFAULT_MOTION_AREA_THRESHOLD = 0.02f
    }
}

data class MotionResult(
    val isMotionDetected: Boolean = false,
    val motionLevel: Float = 0f,
    val changedPixelCount: Int = 0,
    val averageIntensity: Int = 0,
    val timestamp: Long = 0
)

// Platform-specific time function
expect fun currentTimeMillis(): Long

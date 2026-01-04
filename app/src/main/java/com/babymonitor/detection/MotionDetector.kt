package com.babymonitor.detection

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MotionDetector @Inject constructor() : FrameAnalyzer {

    private var previousFrame: IntArray? = null
    private var frameCount = 0
    private val analyzeEveryNFrames = 3 // Analyze every 3rd frame for performance

    private val _motionState = MutableStateFlow(MotionState())
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    // Configuration
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

    override fun analyze(imageProxy: ImageProxy) {
        if (!isEnabled) return

        frameCount++
        if (frameCount % analyzeEveryNFrames != 0) return

        try {
            val currentFrame = extractLuminance(imageProxy)
            val width = imageProxy.width
            val height = imageProxy.height

            previousFrame?.let { prevFrame ->
                if (prevFrame.size == currentFrame.size) {
                    val motionResult = detectMotion(prevFrame, currentFrame, width, height)
                    _motionState.value = motionResult
                }
            }

            previousFrame = currentFrame

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame", e)
        }
    }

    private fun extractLuminance(imageProxy: ImageProxy): IntArray {
        val yBuffer = imageProxy.planes[0].buffer
        val width = imageProxy.width
        val height = imageProxy.height

        // Downsample for performance (analyze at 1/4 resolution)
        val downsampleFactor = 4
        val dsWidth = width / downsampleFactor
        val dsHeight = height / downsampleFactor
        val luminance = IntArray(dsWidth * dsHeight)

        val rowStride = imageProxy.planes[0].rowStride

        for (y in 0 until dsHeight) {
            for (x in 0 until dsWidth) {
                val srcY = y * downsampleFactor
                val srcX = x * downsampleFactor
                val index = srcY * rowStride + srcX
                luminance[y * dsWidth + x] = yBuffer.get(index).toInt() and 0xFF
            }
        }

        return luminance
    }

    private fun detectMotion(
        prevFrame: IntArray,
        currentFrame: IntArray,
        originalWidth: Int,
        originalHeight: Int
    ): MotionState {
        var changedPixels = 0
        val totalPixels = currentFrame.size
        var maxDiff = 0
        var sumDiff = 0L

        // Detect regions with significant changes
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

        if (isMotionDetected) {
            Log.d(TAG, "Motion detected! Ratio: ${String.format("%.4f", motionRatio)}, " +
                    "Changed pixels: $changedPixels, Avg diff: $avgDiff")
        }

        return MotionState(
            isMotionDetected = isMotionDetected,
            motionLevel = motionRatio,
            changedPixelCount = changedPixels,
            averageIntensity = avgDiff,
            timestamp = System.currentTimeMillis()
        )
    }

    fun reset() {
        previousFrame = null
        frameCount = 0
        _motionState.value = MotionState()
    }

    companion object {
        private const val TAG = "MotionDetector"
        const val MIN_SENSITIVITY = 10
        const val MAX_SENSITIVITY = 100
        const val DEFAULT_SENSITIVITY = 30
        const val DEFAULT_MOTION_AREA_THRESHOLD = 0.02f // 2% of frame must change
    }
}

data class MotionState(
    val isMotionDetected: Boolean = false,
    val motionLevel: Float = 0f,
    val changedPixelCount: Int = 0,
    val averageIntensity: Int = 0,
    val timestamp: Long = 0
)

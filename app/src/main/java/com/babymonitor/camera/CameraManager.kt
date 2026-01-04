package com.babymonitor.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.babymonitor.detection.FrameAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _isNightVisionEnabled = MutableStateFlow(false)
    val isNightVisionEnabled: StateFlow<Boolean> = _isNightVisionEnabled.asStateFlow()

    private var frameAnalyzer: FrameAnalyzer? = null

    fun setFrameAnalyzer(analyzer: FrameAnalyzer) {
        frameAnalyzer = analyzer
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ) {
        _cameraState.value = CameraState.Starting

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Preview use case
                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image analysis for motion detection
                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            frameAnalyzer?.analyze(imageProxy)
                            imageProxy.close()
                        }
                    }

                // Camera selector
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                _cameraState.value = CameraState.Running
                Log.d(TAG, "Camera started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Camera start failed", e)
                _cameraState.value = CameraState.Error(e.message ?: "Unknown error")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            _cameraState.value = CameraState.Idle
            Log.d(TAG, "Camera stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }

    fun toggleNightVision() {
        camera?.cameraControl?.let { control ->
            val newValue = !_isNightVisionEnabled.value
            _isNightVisionEnabled.value = newValue

            // Enable torch for night vision
            control.enableTorch(newValue)
        }
    }

    fun setZoom(zoomRatio: Float) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    fun getMinZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
    }

    fun getMaxZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}

sealed class CameraState {
    object Idle : CameraState()
    object Starting : CameraState()
    object Running : CameraState()
    data class Error(val message: String) : CameraState()
}

package com.babymonitor.detection

import androidx.camera.core.ImageProxy

interface FrameAnalyzer {
    fun analyze(imageProxy: ImageProxy)
}

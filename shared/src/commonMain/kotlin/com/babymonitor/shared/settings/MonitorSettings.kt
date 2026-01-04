package com.babymonitor.shared.settings

/**
 * Cross-platform settings model for the baby monitor.
 */
data class MonitorSettings(
    // Motion detection
    val motionDetectionEnabled: Boolean = true,
    val motionSensitivity: Float = 0.5f,

    // Cry detection
    val cryDetectionEnabled: Boolean = true,
    val crySensitivity: Float = 0.5f,
    val volumeThreshold: Float = 0.15f,

    // Notifications
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,

    // Video
    val videoResolution: VideoResolution = VideoResolution.HD_720P,
    val frameRate: FrameRate = FrameRate.FPS_24,

    // Night mode
    val autoNightMode: Boolean = true
)

enum class VideoResolution(val width: Int, val height: Int, val displayName: String) {
    SD_480P(854, 480, "480p"),
    HD_720P(1280, 720, "720p"),
    FHD_1080P(1920, 1080, "1080p")
}

enum class FrameRate(val fps: Int, val displayName: String) {
    FPS_15(15, "15 fps"),
    FPS_24(24, "24 fps"),
    FPS_30(30, "30 fps")
}

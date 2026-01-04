package com.babymonitor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            motionDetectionEnabled = preferences[Keys.MOTION_DETECTION_ENABLED] ?: true,
            motionSensitivity = preferences[Keys.MOTION_SENSITIVITY] ?: 0.5f,
            cryDetectionEnabled = preferences[Keys.CRY_DETECTION_ENABLED] ?: true,
            crySensitivity = preferences[Keys.CRY_SENSITIVITY] ?: 0.5f,
            volumeThreshold = preferences[Keys.VOLUME_THRESHOLD] ?: 0.15f,
            notificationsEnabled = preferences[Keys.NOTIFICATIONS_ENABLED] ?: true,
            vibrationEnabled = preferences[Keys.VIBRATION_ENABLED] ?: true,
            soundEnabled = preferences[Keys.SOUND_ENABLED] ?: true,
            videoResolution = preferences[Keys.VIDEO_RESOLUTION] ?: "720p",
            frameRate = preferences[Keys.FRAME_RATE] ?: "24 fps"
        )
    }

    suspend fun setMotionDetectionEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.MOTION_DETECTION_ENABLED] = enabled }
    }

    suspend fun setMotionSensitivity(sensitivity: Float) {
        dataStore.edit { it[Keys.MOTION_SENSITIVITY] = sensitivity }
    }

    suspend fun setCryDetectionEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.CRY_DETECTION_ENABLED] = enabled }
    }

    suspend fun setCrySensitivity(sensitivity: Float) {
        dataStore.edit { it[Keys.CRY_SENSITIVITY] = sensitivity }
    }

    suspend fun setVolumeThreshold(threshold: Float) {
        dataStore.edit { it[Keys.VOLUME_THRESHOLD] = threshold }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVideoResolution(resolution: String) {
        dataStore.edit { it[Keys.VIDEO_RESOLUTION] = resolution }
    }

    suspend fun setFrameRate(frameRate: String) {
        dataStore.edit { it[Keys.FRAME_RATE] = frameRate }
    }

    private object Keys {
        val MOTION_DETECTION_ENABLED = booleanPreferencesKey("motion_detection_enabled")
        val MOTION_SENSITIVITY = floatPreferencesKey("motion_sensitivity")
        val CRY_DETECTION_ENABLED = booleanPreferencesKey("cry_detection_enabled")
        val CRY_SENSITIVITY = floatPreferencesKey("cry_sensitivity")
        val VOLUME_THRESHOLD = floatPreferencesKey("volume_threshold")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIDEO_RESOLUTION = stringPreferencesKey("video_resolution")
        val FRAME_RATE = stringPreferencesKey("frame_rate")
    }
}

data class AppSettings(
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

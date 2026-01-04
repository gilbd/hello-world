package com.babymonitor.tuya

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for Tuya IoT integration.
 *
 * To get these values:
 * 1. Go to https://developer.tuya.com
 * 2. Create a Cloud Project
 * 3. Create an IPC (IP Camera) product under "Product Development"
 * 4. Select "Security & Video Surveillance" > "Smart Camera"
 * 5. Get the Product ID from product details
 * 6. Get Access ID and Access Secret from Cloud Project
 *
 * For device activation, you'll also need:
 * - UUID: Unique identifier for each device (auto-generated)
 * - Auth Key: Obtained during device licensing
 */
@Singleton
class TuyaConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Tuya Cloud Project credentials
    var accessId: String = ""
        private set
    var accessSecret: String = ""
        private set

    // Product credentials (from Tuya IoT Platform)
    var productId: String = ""
        private set
    var productKey: String = ""
        private set

    // Device credentials (unique per device)
    var uuid: String? = null
        private set
    var authKey: String? = null
        private set

    // Cloud region
    var region: TuyaRegion = TuyaRegion.US
        private set

    // MQTT endpoints by region
    val mqttHost: String
        get() = when (region) {
            TuyaRegion.CHINA -> "m1.tuyacn.com"
            TuyaRegion.US -> "m1.tuyaus.com"
            TuyaRegion.EU -> "m1.tuyaeu.com"
            TuyaRegion.INDIA -> "m1.tuyain.com"
        }

    val mqttPort: Int = 8883 // TLS port

    // API endpoints by region
    val apiHost: String
        get() = when (region) {
            TuyaRegion.CHINA -> "openapi.tuyacn.com"
            TuyaRegion.US -> "openapi.tuyaus.com"
            TuyaRegion.EU -> "openapi.tuyaeu.com"
            TuyaRegion.INDIA -> "openapi.tuyain.com"
        }

    /**
     * Configure Tuya credentials.
     * Call this before initializing TuyaDeviceManager.
     */
    fun configure(
        accessId: String,
        accessSecret: String,
        productId: String,
        productKey: String = "",
        region: TuyaRegion = TuyaRegion.US
    ) {
        this.accessId = accessId
        this.accessSecret = accessSecret
        this.productId = productId
        this.productKey = productKey
        this.region = region
        saveConfig()
    }

    /**
     * Set device-specific credentials.
     * These are obtained during device activation/licensing.
     */
    fun setDeviceCredentials(uuid: String, authKey: String) {
        this.uuid = uuid
        this.authKey = authKey
        saveConfig()
    }

    /**
     * Check if Tuya is properly configured.
     */
    fun isConfigured(): Boolean {
        return accessId.isNotBlank() &&
                accessSecret.isNotBlank() &&
                productId.isNotBlank()
    }

    /**
     * Check if device credentials are set.
     */
    fun hasDeviceCredentials(): Boolean {
        return uuid != null && authKey != null
    }

    private fun saveConfig() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS_ID, accessId)
            .putString(KEY_ACCESS_SECRET, accessSecret)
            .putString(KEY_PRODUCT_ID, productId)
            .putString(KEY_PRODUCT_KEY, productKey)
            .putString(KEY_UUID, uuid)
            .putString(KEY_AUTH_KEY, authKey)
            .putString(KEY_REGION, region.name)
            .apply()
    }

    fun loadConfig() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        accessId = prefs.getString(KEY_ACCESS_ID, "") ?: ""
        accessSecret = prefs.getString(KEY_ACCESS_SECRET, "") ?: ""
        productId = prefs.getString(KEY_PRODUCT_ID, "") ?: ""
        productKey = prefs.getString(KEY_PRODUCT_KEY, "") ?: ""
        uuid = prefs.getString(KEY_UUID, null)
        authKey = prefs.getString(KEY_AUTH_KEY, null)
        region = try {
            TuyaRegion.valueOf(prefs.getString(KEY_REGION, "US") ?: "US")
        } catch (e: Exception) {
            TuyaRegion.US
        }
    }

    fun clearConfig() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
        accessId = ""
        accessSecret = ""
        productId = ""
        productKey = ""
        uuid = null
        authKey = null
        region = TuyaRegion.US
    }

    companion object {
        private const val PREFS_NAME = "tuya_config"
        private const val KEY_ACCESS_ID = "access_id"
        private const val KEY_ACCESS_SECRET = "access_secret"
        private const val KEY_PRODUCT_ID = "product_id"
        private const val KEY_PRODUCT_KEY = "product_key"
        private const val KEY_UUID = "uuid"
        private const val KEY_AUTH_KEY = "auth_key"
        private const val KEY_REGION = "region"
    }
}

enum class TuyaRegion {
    CHINA,
    US,
    EU,
    INDIA
}

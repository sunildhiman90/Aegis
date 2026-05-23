package app.aegis.platform

/**
 * Platform-specific device ID provider
 * Android: Uses ANDROID_ID from Settings.Secure
 * iOS: Uses identifierForVendor from UIDevice
 */
expect class DeviceIdProvider {
    /**
     * Returns a unique, anonymous device identifier
     */
    fun getDeviceId(): String
}

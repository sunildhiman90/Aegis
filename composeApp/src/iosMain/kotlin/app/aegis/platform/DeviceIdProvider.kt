package app.aegis.platform

import platform.UIKit.UIDevice

/**
 * iOS implementation of DeviceIdProvider
 * Uses identifierForVendor from UIDevice
 */
actual class DeviceIdProvider {
    
    actual fun getDeviceId(): String {
        val uuid = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "UNKNOWN"
        return formatDeviceId(uuid)
    }
    
    private fun formatDeviceId(uuid: String): String {
        val cleaned = uuid.replace("-", "").take(8)
        if (cleaned.length < 8) return "AE-$cleaned"
        return "AE-${cleaned.take(3).uppercase()}-${cleaned.substring(3, 6).uppercase()}-${cleaned.substring(6, 7).uppercase()}"
    }
}

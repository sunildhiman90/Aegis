package app.aegis.platform

import android.content.Context
import android.provider.Settings

/**
 * Android implementation of DeviceIdProvider
 * Uses ANDROID_ID from Settings.Secure
 */
actual class DeviceIdProvider(
    private val context: Context,
) {
    actual fun getDeviceId(): String {
        val androidId =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID,
            )
        // Note: ANDROID_ID changes on factory reset.
        // It is unique per user/device combo but not persistent across wipes.
        // Format as AE-XXX-XXX-X for display
        return formatDeviceId(androidId ?: "UNKNOWN")
    }

    private fun formatDeviceId(id: String): String {
        if (id.length < 8) return "AE-$id"
        return "AE-${id.take(3).uppercase()}-${id.substring(3, 6).uppercase()}-${id.substring(6, 7).uppercase()}"
    }
}

package app.aegis.data.settings

import com.russhwolf.settings.Settings

/**
 * Repository for managing app settings and preferences
 */
class AppSettingsRepository(
    private val settings: Settings = Settings()
) {
    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
        private const val THEME_MODE_LIGHT = "light"
        private const val THEME_MODE_DARK = "dark"
        private const val THEME_MODE_SYSTEM = "system"

        // Language preferences
        private const val KEY_LANGUAGE_CODE = "language_code"
        private const val KEY_LANGUAGE_NAME = "language_name"
        private const val DEFAULT_LANGUAGE = "en"

        // Device ID
        private const val KEY_DEVICE_ID = "device_id"
    }

    // Onboarding
    var isOnboardingComplete: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) {
            settings.putBoolean(KEY_ONBOARDING_COMPLETE, value)
        }

    // Theme Mode - supports Light, Dark, and System Default
    fun getThemeMode(): app.aegis.domain.model.AppThemeMode {
        return try {
            // Try reading as string (new format)
            val stored = settings.getString(KEY_IS_DARK_THEME, THEME_MODE_SYSTEM)
            when (stored) {
                THEME_MODE_LIGHT -> app.aegis.domain.model.AppThemeMode.LIGHT
                THEME_MODE_DARK -> app.aegis.domain.model.AppThemeMode.DARK
                else -> app.aegis.domain.model.AppThemeMode.SYSTEM_DEFAULT
            }
        } catch (e: ClassCastException) {
            // Migrate old boolean value
            val oldDarkTheme = try {
                settings.getBoolean(KEY_IS_DARK_THEME, false)
            } catch (e: Exception) {
                false
            }
            val newMode = if (oldDarkTheme) {
                app.aegis.domain.model.AppThemeMode.DARK
            } else {
                app.aegis.domain.model.AppThemeMode.LIGHT
            }
            setThemeMode(newMode)
            newMode
        }
    }

    fun setThemeMode(mode: app.aegis.domain.model.AppThemeMode) {
        val value = when (mode) {
            app.aegis.domain.model.AppThemeMode.LIGHT -> THEME_MODE_LIGHT
            app.aegis.domain.model.AppThemeMode.DARK -> THEME_MODE_DARK
            app.aegis.domain.model.AppThemeMode.SYSTEM_DEFAULT -> THEME_MODE_SYSTEM
        }
        settings.putString(KEY_IS_DARK_THEME, value)
    }

    // Backward compatibility - defaults to false (light mode)
    @Deprecated("Use getThemeMode() instead")
    fun getStoredDarkTheme(): Boolean {
        return getThemeMode() == app.aegis.domain.model.AppThemeMode.DARK
    }

    @Deprecated("Use getThemeMode() instead")
    fun isDarkTheme(isSystemDark: Boolean): Boolean {
        return when (getThemeMode()) {
            app.aegis.domain.model.AppThemeMode.LIGHT -> false
            app.aegis.domain.model.AppThemeMode.DARK -> true
            app.aegis.domain.model.AppThemeMode.SYSTEM_DEFAULT -> isSystemDark
        }
    }

    @Deprecated("Use setThemeMode() instead")
    fun setDarkTheme(isDark: Boolean) {
        setThemeMode(if (isDark) app.aegis.domain.model.AppThemeMode.DARK else app.aegis.domain.model.AppThemeMode.LIGHT)
    }

    fun clearThemePreference() {
        settings.remove(KEY_IS_DARK_THEME)
    }

    // Sensitivity Level
    // Sensitivity Level
    fun getSensitivity(): app.aegis.models.SensitivityLevel {
        val stored = settings.getString("sensitivity_level", "BALANCED")
        return try {
            app.aegis.models.SensitivityLevel.valueOf(stored)
        } catch (e: Exception) {
            app.aegis.models.SensitivityLevel.BALANCED
        }
    }

    fun setSensitivity(level: app.aegis.models.SensitivityLevel) {
        settings.putString("sensitivity_level", level.name)
    }

    // Language preferences
    fun getLanguageCode(): String {
        return settings.getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE)
    }

    fun setLanguageCode(code: String) {
        settings.putString(KEY_LANGUAGE_CODE, code)
    }

    fun getLanguageName(): String {
        return settings.getString(KEY_LANGUAGE_NAME, "English")
    }

    fun setLanguageName(name: String) {
        settings.putString(KEY_LANGUAGE_NAME, name)
    }

    // Device ID - Stored for persistence but not shown in UI
    fun getDeviceId(): String? {
        return settings.getStringOrNull(KEY_DEVICE_ID)
    }

    fun setDeviceId(id: String) {
        settings.putString(KEY_DEVICE_ID, id)
    }
}

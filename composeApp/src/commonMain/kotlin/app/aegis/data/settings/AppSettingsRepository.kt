package app.aegis.data.settings

import com.russhwolf.settings.ObservableSettings
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
    }

    // Onboarding
    var isOnboardingComplete: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) {
            settings.putBoolean(KEY_ONBOARDING_COMPLETE, value)
        }

    // Theme - null means system default
    // We store -1 for system, 0 for light, 1 for dark to match boolean logic somewhat or use string/int
    // Simpler: use 3-state logic if possible, or just helper methods

    fun isDarkTheme(isSystemDark: Boolean): Boolean {
        // defaults to system if not set
        return if (settings.hasKey(KEY_IS_DARK_THEME)) {
            settings.getBoolean(KEY_IS_DARK_THEME, false)
        } else {
            isSystemDark
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        settings.putBoolean(KEY_IS_DARK_THEME, isDark)
    }

    fun clearThemePreference() {
        settings.remove(KEY_IS_DARK_THEME)
    }
}

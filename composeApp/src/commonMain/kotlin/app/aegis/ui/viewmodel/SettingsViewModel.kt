package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screen
 */
class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    fun setDarkTheme(enabled: Boolean) {
        settingsRepository.setDarkTheme(enabled)
    }

    fun clearThemePreference() {
        settingsRepository.clearThemePreference()
    }

    fun isDarkTheme(isSystemDark: Boolean): Boolean {
        return settingsRepository.isDarkTheme(isSystemDark)
    }
}

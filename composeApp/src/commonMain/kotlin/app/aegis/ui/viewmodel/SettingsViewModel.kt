package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.data.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.aegis.domain.model.AppThemeMode

/**
 * ViewModel for Settings screen
 */
class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    fun getThemeMode(): AppThemeMode {
        return settingsRepository.getThemeMode()
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}

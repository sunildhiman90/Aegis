package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.model.AppThemeMode
import app.aegis.domain.model.TrustedContact
import app.aegis.domain.repository.TrustedContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Settings screen
 */
class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository,
    trustedContactRepository: TrustedContactRepository
) : ViewModel() {

    /**
     * Top 3 trusted contacts for preview in settings
     */
    val topContacts: StateFlow<List<TrustedContact>> = trustedContactRepository.getAllContacts()
        .map { contacts -> contacts.take(3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getThemeMode(): AppThemeMode {
        return settingsRepository.getThemeMode()
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}


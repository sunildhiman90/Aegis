package app.aegis.ui.viewmodel

import aegis.shared.generated.resources.Res
import aegis.shared.generated.resources.settings_theme_dark
import aegis.shared.generated.resources.settings_theme_light
import aegis.shared.generated.resources.settings_theme_system
import androidx.lifecycle.viewModelScope
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.model.AppThemeItem
import app.aegis.domain.model.AppThemeMode
import app.aegis.domain.model.TrustedContact
import app.aegis.domain.repository.TrustedContactRepository
import app.aegis.domain.usecase.GetLanguagesUseCase
import app.aegis.helper.LanguageHelper
import app.aegis.ui.events.SettingsScreenUiEvent
import app.aegis.ui.state.SettingsScreenUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * ViewModel for Settings screen
 * Extends LanguageViewModel to inherit language management capabilities
 */
class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository,
    trustedContactRepository: TrustedContactRepository,
    getLanguagesUseCase: GetLanguagesUseCase,
    languageHelper: LanguageHelper,
) : LanguageViewModel(
    appSettingsRepository = settingsRepository,
    getLanguagesUseCase = getLanguagesUseCase,
    languageHelper = languageHelper,
) {

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

    private var _settingsScreenUiState = MutableStateFlow(SettingsScreenUiState())
    val settingsScreenUiState: StateFlow<SettingsScreenUiState> = _settingsScreenUiState.asStateFlow()

    init {
        viewModelScope.launch {
            _settingsScreenUiState.update {
                it.copy(
                    loadingThemes = true,
                    selectedAppTheme = getAppThemeDisplayName(settingsRepository.getThemeMode()),
                    customApiKey = settingsRepository.getCustomApiKey(),
                    customModelId = settingsRepository.getCustomModelId(),
                    isDemoMode = settingsRepository.isDemoMode(),
                )
            }
            val themes = getAppThemes()
            _settingsScreenUiState.update {
                it.copy(
                    themes = themes,
                    loadingThemes = false,
                )
            }
        }
    }

    private suspend fun getAppThemes(): List<AppThemeItem> =
        listOf(
            AppThemeItem(
                name = getString(Res.string.settings_theme_light),
                code = AppThemeMode.LIGHT.name,
            ),
            AppThemeItem(
                name = getString(Res.string.settings_theme_dark),
                code = AppThemeMode.DARK.name,
            ),
            AppThemeItem(
                name = getString(Res.string.settings_theme_system),
                code = AppThemeMode.SYSTEM_DEFAULT.name,
            ),
        )

    private suspend fun getAppThemeDisplayName(themeMode: AppThemeMode): String =
        when (themeMode) {
            AppThemeMode.LIGHT -> getString(Res.string.settings_theme_light)
            AppThemeMode.DARK -> getString(Res.string.settings_theme_dark)
            AppThemeMode.SYSTEM_DEFAULT -> getString(Res.string.settings_theme_system)
        }

    fun onSettingsScreenUiEvent(event: SettingsScreenUiEvent) {
        when (event) {
            is SettingsScreenUiEvent.OnCurrentLanguageChange -> {
                viewModelScope.launch {
                    changeLanguage(event.languageCode)
                    // Reload themes with new language strings
                    _settingsScreenUiState.update {
                        it.copy(loadingThemes = true)
                    }
                    val themes = getAppThemes()
                    _settingsScreenUiState.update {
                        it.copy(
                            themes = themes,
                            loadingThemes = false,
                        )
                    }
                    updateSelectedThemeString()
                }
            }

            is SettingsScreenUiEvent.OnThemeChange -> {
                viewModelScope.launch {
                    val mode = AppThemeMode.valueOf(event.theme)
                    settingsRepository.setThemeMode(mode)
                    _settingsScreenUiState.update {
                        it.copy(
                            selectedAppTheme = getAppThemeDisplayName(mode),
                        )
                    }
                }
            }

            is SettingsScreenUiEvent.OnShowNotificationValueChange -> {
                _settingsScreenUiState.update {
                    it.copy(
                        showNotificationsSettingValue = event.show,
                    )
                }
            }

            is SettingsScreenUiEvent.ToggleAppLanguageDialog -> {
                _settingsScreenUiState.update {
                    it.copy(
                        showAppLanguageDialog = event.show,
                    )
                }
            }

            is SettingsScreenUiEvent.ToggleAppThemeDialog -> {
                _settingsScreenUiState.update {
                    it.copy(
                        showAppThemeDialog = event.show,
                    )
                }
            }

            is SettingsScreenUiEvent.ToggleShowNotificationsDialog -> {
                _settingsScreenUiState.update {
                    it.copy(
                        showNotificationSettingsDialog = event.show,
                    )
                }
            }

            is SettingsScreenUiEvent.OnCustomApiKeyChange -> {
                settingsRepository.setCustomApiKey(event.apiKey)
                _settingsScreenUiState.update {
                    it.copy(
                        customApiKey = event.apiKey,
                    )
                }
            }

            is SettingsScreenUiEvent.OnCustomModelIdChange -> {
                settingsRepository.setCustomModelId(event.modelId)
                _settingsScreenUiState.update {
                    it.copy(
                        customModelId = event.modelId,
                    )
                }
            }

            is SettingsScreenUiEvent.OnDemoModeToggle -> {
                settingsRepository.setDemoMode(event.enabled)
                _settingsScreenUiState.update {
                    it.copy(
                        isDemoMode = event.enabled,
                    )
                }
            }
        }
    }

    private fun updateSelectedThemeString() {
        viewModelScope.launch {
            _settingsScreenUiState.update {
                it.copy(
                    selectedAppTheme = getAppThemeDisplayName(settingsRepository.getThemeMode()),
                )
            }
        }
    }

    fun getThemeMode(): AppThemeMode {
        return settingsRepository.getThemeMode()
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun getSensitivity(): app.aegis.models.SensitivityLevel {
        return settingsRepository.getSensitivity()
    }

    fun setSensitivity(level: app.aegis.models.SensitivityLevel) {
        settingsRepository.setSensitivity(level)
    }
}

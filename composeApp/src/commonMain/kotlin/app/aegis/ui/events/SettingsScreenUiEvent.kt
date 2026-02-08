package app.aegis.ui.events

/**
 * UI events for Settings screen
 */
sealed interface SettingsScreenUiEvent {
    data class OnCurrentLanguageChange(val languageCode: String) : SettingsScreenUiEvent
    data class OnThemeChange(val theme: String) : SettingsScreenUiEvent
    data class OnShowNotificationValueChange(val show: Boolean) : SettingsScreenUiEvent
    data class ToggleAppLanguageDialog(val show: Boolean) : SettingsScreenUiEvent
    data class ToggleAppThemeDialog(val show: Boolean) : SettingsScreenUiEvent
    data class ToggleShowNotificationsDialog(val show: Boolean) : SettingsScreenUiEvent
    data class OnCustomApiKeyChange(val apiKey: String) : SettingsScreenUiEvent
}

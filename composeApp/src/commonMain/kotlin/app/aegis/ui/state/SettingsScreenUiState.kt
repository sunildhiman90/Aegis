package app.aegis.ui.state

import app.aegis.domain.model.AppThemeItem

/**
 * UI state for Settings screen
 */
data class SettingsScreenUiState(
    val selectedAppTheme: String = "",
    val themes: List<AppThemeItem> = emptyList(),
    val loadingThemes: Boolean = false,
    val showAppLanguageDialog: Boolean = false,
    val showAppThemeDialog: Boolean = false,
    val showNotificationSettingsDialog: Boolean = false,
    val showNotificationsSettingValue: Boolean = false
)

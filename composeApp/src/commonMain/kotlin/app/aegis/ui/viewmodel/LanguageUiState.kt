package app.aegis.ui.viewmodel

import app.aegis.domain.model.AppLanguageItem

/**
 * UI state for language selection
 */
data class LanguageUiState(
    val selectedLanguageCode: String = "en",
    val languages: List<AppLanguageItem>? = null
)

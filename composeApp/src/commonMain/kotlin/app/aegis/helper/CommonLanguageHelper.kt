package app.aegis.helper

import app.aegis.core.AppConstants
import app.aegis.core.AppConstants.EN
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.usecase.GetLanguagesUseCase

/**
 * This class provides common helper functions for Multi Language Support
 */
abstract class CommonLanguageHelper(
    private val appSettingsRepository: AppSettingsRepository,
    private val getLanguagesUseCase: GetLanguagesUseCase,
) : LanguageHelper {

    override fun setLanguageCode(languageCode: String) {
        appSettingsRepository.setLanguageCode(languageCode)
    }

    override suspend fun changeLanguage(languageCode: String) {
        setLanguageCode(languageCode)

        appSettingsRepository.setLanguageName(
            getLanguageNameByCode(languageCode) ?: EN
        )
    }

    override suspend fun getLanguageNameByCode(languageCode: String): String? {
        val languages = getLanguagesUseCase.invoke()
        val language = languages.firstOrNull { it.code == languageCode }?.name
        return language
    }

    override suspend fun getCurrentLanguage(): String {
        val languageCode = appSettingsRepository.getLanguageCode()
        return getLanguageNameByCode(languageCode) ?: EN
    }
}

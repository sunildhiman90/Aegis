package app.aegis.helper

import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.usecase.GetLanguagesUseCase
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific implementation of LanguageHelper
 * Uses NSUserDefaults to set AppleLanguages
 */
class IOSLanguageHelper(
    appSettingsRepository: AppSettingsRepository,
    getLanguagesUseCase: GetLanguagesUseCase,
) : CommonLanguageHelper(appSettingsRepository, getLanguagesUseCase) {

    override suspend fun changeLanguage(languageCode: String) {
        println("IOSLanguageHelper changeLanguage: $languageCode")
        super.changeLanguage(languageCode)

        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.setObject(listOf(languageCode), forKey = "AppleLanguages")
        userDefaults.synchronize()
    }
}

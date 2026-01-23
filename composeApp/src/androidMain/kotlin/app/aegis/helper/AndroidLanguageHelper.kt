package app.aegis.helper

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.usecase.GetLanguagesUseCase
import java.util.Locale

/**
 * Android-specific implementation of LanguageHelper
 * Handles system-level locale changes using LocaleManager (API 33+) or AppCompatDelegate
 */
class AndroidLanguageHelper(
    appSettingsRepository: AppSettingsRepository,
    getLanguagesUseCase: GetLanguagesUseCase,
    private val context: Context,
) : CommonLanguageHelper(
    appSettingsRepository,
    getLanguagesUseCase,
) {

    override suspend fun changeLanguage(languageCode: String) {
        println("AndroidLanguageHelper changeLanguage: $languageCode")
        super.changeLanguage(languageCode)

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocales(LocaleList.forLanguageTags(languageCode))
        context.createConfigurationContext(config)

        // Due to per-app language changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(languageCode)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
    }
}

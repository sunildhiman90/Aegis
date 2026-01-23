package app.aegis

import android.app.Application
import android.content.res.Configuration
import app.aegis.core.AppConstants
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.di.initKoin
import app.aegis.helper.LanguageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext

class AegisApplication : Application() {
    val scope = MainScope()

    private val appSettingsRepository: AppSettingsRepository by inject()
    private val languageHelper: LanguageHelper by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@AegisApplication)
        }
        setupLanguage()
    }

    private fun setupLanguage() {
        scope.launch {
            val currentSelectedLanguage = appSettingsRepository.getLanguageCode()
            withContext(Dispatchers.IO) {
                languageHelper.changeLanguage(currentSelectedLanguage)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupLanguage()
    }
}

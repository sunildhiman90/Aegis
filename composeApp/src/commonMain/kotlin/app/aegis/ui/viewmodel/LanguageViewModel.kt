package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.domain.model.AppLanguageItem
import app.aegis.domain.usecase.GetLanguagesUseCase
import app.aegis.helper.LanguageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for language management
 */
open class LanguageViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val getLanguagesUseCase: GetLanguagesUseCase,
    private val languageHelper: LanguageHelper,
) : ViewModel() {

    // Trigger to refresh languages list when needed
    private val refreshLanguages = MutableStateFlow(0)

    // Cached languages list to avoid reloading on language changes
    @OptIn(ExperimentalCoroutinesApi::class)
    private val cachedLanguages: StateFlow<List<AppLanguageItem>> =
        refreshLanguages
            .flatMapLatest {
                loadLanguagesItems()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    private val currentLanguage = MutableStateFlow("")

    val languageUiState =
        combine(currentLanguage, cachedLanguages) { currentLanguage, languages ->
            LanguageUiState(
                selectedLanguageCode = currentLanguage,
                languages = languages,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LanguageUiState(),
        )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            setupLanguage()
        }
    }

    private fun loadLanguagesItems(): Flow<List<AppLanguageItem>> =
        flow {
            println("loadLanguagesItems")
            emit(getLanguagesUseCase.invoke())
        }

    /**
     * This will set app default language to EN as default, Otherwise whatever is selected
     */
    private fun setupLanguage() {
        val currentSelectedLanguage = appSettingsRepository.getLanguageCode()
        currentLanguage.update { currentSelectedLanguage }
    }

    /**
     * This method will change app language to the selected language
     * @param languageCode is the code of Language we are changing our app Language to
     */
    suspend fun changeLanguage(languageCode: String) {
        println("LanguageViewModel changeLanguage: $languageCode")
        languageHelper.setLanguageCode(languageCode)
        languageHelper.changeLanguage(languageCode)
        currentLanguage.update { languageCode }

        // Trigger a refresh of the languages list
        refreshLanguages.update { it + 1 }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}

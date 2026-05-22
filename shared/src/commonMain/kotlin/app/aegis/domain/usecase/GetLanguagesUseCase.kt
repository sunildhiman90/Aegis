package app.aegis.domain.usecase

import app.aegis.domain.model.AppLanguageItem

/**
 * Use case for retrieving available languages
 */
interface GetLanguagesUseCase {
    suspend operator fun invoke(): List<AppLanguageItem>
}

package app.aegis.domain.usecase

import aegis.shared.generated.resources.*
import app.aegis.core.AppConstants.EN
import app.aegis.core.AppConstants.FR
import app.aegis.core.AppConstants.HI
import app.aegis.core.AppConstants.SP
import app.aegis.domain.model.AppLanguageItem
import org.jetbrains.compose.resources.getString

/**
 * Implementation of GetLanguagesUseCase
 * Returns the list of supported languages with their localized names
 */
class GetLanguagesUseCaseImpl : GetLanguagesUseCase {
    override suspend fun invoke(): List<AppLanguageItem> =
        listOf(
            AppLanguageItem(
                name = getString(Res.string.text_language_english),
                code = EN,
                flagDrawable = Res.drawable.uk,
            ),
            AppLanguageItem(
                name = getString(Res.string.text_language_french),
                code = FR,
                flagDrawable = Res.drawable.france,
            ),
            AppLanguageItem(
                name = getString(Res.string.text_language_hindi),
                code = HI,
                flagDrawable = Res.drawable.ic_hindi,
            ),
            AppLanguageItem(
                name = getString(Res.string.text_language_spanish),
                code = SP,
                flagDrawable = Res.drawable.spain,
            ),
        )
}

package app.aegis.domain.model

import org.jetbrains.compose.resources.DrawableResource

/**
 * Represents a language option in the app
 */
data class AppLanguageItem(
    val name: String,
    val code: String,
    val flagDrawable: DrawableResource
)

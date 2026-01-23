package app.aegis.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition local for providing current language code throughout the app
 */
val LocalLocalization = staticCompositionLocalOf { "en" }

/**
 * Wrapper composable that provides language context to all child composables
 * @param language Current language code (e.g., "en", "es", "fr")
 * @param content The content to wrap with localization context
 */
@Composable
fun LocalizedApp(
    language: String = "en",
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLocalization provides language,
        content = content,
    )
}

package app.aegis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Main Aegis theme wrapper.
 * Supports both dark and light themes based on system preference.
 *
 * Now uses standard MaterialTheme for main colors and LocalAegisCustomColors for semantic extensions.
 */
@Composable
fun AegisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AegisDarkColorScheme else AegisLightColorScheme
    val customColors = if (darkTheme) AegisDarkCustomColors else AegisLightCustomColors

    CompositionLocalProvider(
        LocalAegisCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AegisTypography, // Ensure typography is still provided
            content = content
        )
    }
}


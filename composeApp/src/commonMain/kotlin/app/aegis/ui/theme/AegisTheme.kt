package app.aegis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Local composition for Aegis-specific colors
 */
val LocalAegisColors = staticCompositionLocalOf { DarkColorScheme }

/**
 * Material3 dark color scheme mapped from AegisColors
 */
private val AegisMaterialDarkColorScheme = darkColorScheme(
    primary = AegisDarkColors.Primary,
    onPrimary = Color(0xFF121212),  // Dark text on gold for readability
    primaryContainer = AegisDarkColors.PrimaryContainer,
    onPrimaryContainer = AegisDarkColors.TextPrimary,

    secondary = AegisDarkColors.TrustBadge,
    onSecondary = AegisDarkColors.TextPrimary,

    tertiary = AegisDarkColors.Success,
    onTertiary = AegisDarkColors.TextPrimary,

    background = AegisDarkColors.Background,
    onBackground = AegisDarkColors.TextPrimary,

    surface = AegisDarkColors.Surface,
    onSurface = AegisDarkColors.TextPrimary,
    surfaceVariant = AegisDarkColors.SurfaceVariant,
    onSurfaceVariant = AegisDarkColors.TextSecondary,

    error = AegisDarkColors.Error,
    onError = AegisDarkColors.TextPrimary,
    errorContainer = AegisDarkColors.ErrorContainer,
    onErrorContainer = AegisDarkColors.TextPrimary,

    outline = AegisDarkColors.CardBorder,
    outlineVariant = AegisDarkColors.Divider
)

/**
 * Material3 light color scheme mapped from AegisColors
 */
private val AegisMaterialLightColorScheme = lightColorScheme(
    primary = AegisLightColors.Primary,
    onPrimary = AegisLightColors.Surface,
    primaryContainer = AegisLightColors.PrimaryContainer,
    onPrimaryContainer = AegisLightColors.TextPrimary,

    secondary = AegisLightColors.TrustBadge,
    onSecondary = AegisLightColors.Surface,

    tertiary = AegisLightColors.Success,
    onTertiary = AegisLightColors.Surface,

    background = AegisLightColors.Background,
    onBackground = AegisLightColors.TextPrimary,

    surface = AegisLightColors.Surface,
    onSurface = AegisLightColors.TextPrimary,
    surfaceVariant = AegisLightColors.SurfaceVariant,
    onSurfaceVariant = AegisLightColors.TextSecondary,

    error = AegisLightColors.Error,
    onError = AegisLightColors.Surface,
    errorContainer = AegisLightColors.ErrorContainer,
    onErrorContainer = AegisLightColors.TextPrimary,

    outline = AegisLightColors.CardBorder,
    outlineVariant = AegisLightColors.Divider
)

/**
 * Main Aegis theme wrapper.
 * Supports both dark and light themes based on system preference.
 */
@Composable
fun AegisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val materialColorScheme = if (darkTheme) AegisMaterialDarkColorScheme else AegisMaterialLightColorScheme

    CompositionLocalProvider(LocalAegisColors provides colorScheme) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}

/**
 * Extension to access Aegis colors from anywhere in the composition
 */
object AegisTheme {
    val colors: AegisColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAegisColors.current
}

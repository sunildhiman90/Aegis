package app.aegis.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Aegis Color Palette - Dark and Light Theme Colors
 * Colors based on Stitch design guidelines
 */

// === DARK THEME COLORS (Stitch Guidelines) ===
private object AegisDarkPalette {
    // Background Colors
    val Background = Color(0xFF121212)          // Deep Charcoal (Stitch)
    val Surface = Color(0xFF1E1E1E)             // Lighter Grey cards (Stitch)
    val SurfaceVariant = Color(0xFF2D2D2D)      // Slightly elevated surface

    // Primary Accent - Aegis Gold (Stitch)
    val Primary = Color(0xFFFFD700)             // Aegis Gold
    val PrimaryVariant = Color(0xFFE6C200)      // Darker gold for pressed states (Secondary container in M3/Inverse Primary?)
    val PrimaryContainer = Color(0xFF3D3000)    // Gold container background

    // Text Colors
    val TextPrimary = Color(0xFFFFFFFF)         // White text
    val TextSecondary = Color(0xFFB0B0B0)       // Gray text
    val TextTertiary = Color(0xFF757575)        // Subtle gray text

    // Status Colors (Stitch)
    val Success = Color(0xFF2E7D32)             // Forest Green (Stitch)
    val SuccessContainer = Color(0xFF1B5E20)    // Darker green container
    val Warning = Color(0xFFFFC107)             // Amber (Stitch)
    val WarningContainer = Color(0xFF422006)    // Amber container
    val Error = Color(0xFFB71C1C)               // Deep Red (Stitch)
    val ErrorContainer = Color(0xFF4A0A0A)      // Red container

    // Component Colors
    val CardBorder = Color(0xFF333333)          // Card border
    val Divider = Color(0xFF2D2D2D)             // Dividers
    val IconDefault = Color(0xFFB0B0B0)         // Default icon color

    // Special Colors
    val ShieldGreen = Color(0xFF2E7D32)         // Active protection shield (Forest Green)
    val ShieldYellow = Color(0xFFFFC107)        // Warning/setup shield (Amber)
    val TrustBadge = Color(0xFF6366F1)          // Trusted contacts badge (Indigo)
}

// === LIGHT THEME COLORS (Stitch Guidelines) ===
private object AegisLightPalette {
    // Background Colors
    val Background = Color(0xFFF5F5F5)          // Off-White (Stitch)
    val Surface = Color(0xFFFFFFFF)             // Pure White (Stitch)
    val SurfaceVariant = Color(0xFFEEEEEE)      // Slightly elevated surface

    // Primary Accent - Vibrant Blue or Gold (Stitch)
    val Primary = Color(0xFF1976D2)             // Vibrant Blue (Stitch)
    val PrimaryVariant = Color(0xFF1565C0)      // Darker blue for pressed states
    val PrimaryContainer = Color(0xFFBBDEFB)    // Blue container background

    // Text Colors (Stitch)
    val TextPrimary = Color(0xFF212121)         // High Contrast Black (Stitch)
    val TextSecondary = Color(0xFF616161)       // Gray text
    val TextTertiary = Color(0xFF9E9E9E)        // Light gray text

    // Status Colors (Stitch)
    val Success = Color(0xFF2E7D32)             // Forest Green
    val SuccessContainer = Color(0xFFC8E6C9)    // Light green container
    val Warning = Color(0xFFFFC107)             // Amber (Stitch)
    val WarningContainer = Color(0xFFFFF8E1)    // Light amber container
    val Error = Color(0xFFD32F2F)               // Bright Red (Stitch)
    val ErrorContainer = Color(0xFFFFCDD2)      // Light red container

    // Component Colors
    val CardBorder = Color(0xFFE0E0E0)          // Light gray border
    val Divider = Color(0xFFE0E0E0)             // Dividers
    val IconDefault = Color(0xFF616161)         // Default icon color

    // Special Colors
    val ShieldGreen = Color(0xFF2E7D32)         // Active protection shield
    val ShieldYellow = Color(0xFFFFC107)        // Warning/setup shield
    val TrustBadge = Color(0xFF6366F1)          // Trusted contacts badge
}

/**
 * Standard Material 3 Light Color Scheme
 */
val AegisLightColorScheme = lightColorScheme(
    primary = AegisLightPalette.Primary,
    onPrimary = AegisLightPalette.Surface,
    primaryContainer = AegisLightPalette.PrimaryContainer,
    onPrimaryContainer = AegisLightPalette.TextPrimary,
    secondary = AegisLightPalette.TrustBadge, // Mapping trust badge to secondary for standard slots
    onSecondary = AegisLightPalette.Surface,
    tertiary = AegisLightPalette.Success,     // Mapping success to tertiary for standard slots
    onTertiary = AegisLightPalette.Surface,
    background = AegisLightPalette.Background,
    onBackground = AegisLightPalette.TextPrimary,
    surface = AegisLightPalette.Surface,
    onSurface = AegisLightPalette.TextPrimary,
    surfaceVariant = AegisLightPalette.SurfaceVariant,
    onSurfaceVariant = AegisLightPalette.TextSecondary,
    error = AegisLightPalette.Error,
    onError = AegisLightPalette.Surface,
    errorContainer = AegisLightPalette.ErrorContainer,
    onErrorContainer = AegisLightPalette.TextPrimary,
    outline = AegisLightPalette.CardBorder,
    outlineVariant = AegisLightPalette.Divider
)

/**
 * Standard Material 3 Dark Color Scheme
 */
val AegisDarkColorScheme = darkColorScheme(
    primary = AegisDarkPalette.Primary,
    onPrimary = Color(0xFF121212), // Dark text on gold for readability
    primaryContainer = AegisDarkPalette.PrimaryContainer,
    onPrimaryContainer = AegisDarkPalette.TextPrimary,
    secondary = AegisDarkPalette.TrustBadge,
    onSecondary = AegisDarkPalette.TextPrimary,
    tertiary = AegisDarkPalette.Success,
    onTertiary = AegisDarkPalette.TextPrimary,
    background = AegisDarkPalette.Background,
    onBackground = AegisDarkPalette.TextPrimary,
    surface = AegisDarkPalette.Surface,
    onSurface = AegisDarkPalette.TextPrimary,
    surfaceVariant = AegisDarkPalette.SurfaceVariant,
    onSurfaceVariant = AegisDarkPalette.TextSecondary,
    error = AegisDarkPalette.Error,
    onError = AegisDarkPalette.TextPrimary,
    errorContainer = AegisDarkPalette.ErrorContainer,
    onErrorContainer = AegisDarkPalette.TextPrimary,
    outline = AegisDarkPalette.CardBorder,
    outlineVariant = AegisDarkPalette.Divider
)

/**
 * Custom Semantic Colors for Aegis
 * These are colors that don't fit into standard Material slots or are distinct semantic roles.
 */
data class AegisCustomColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val shieldGreen: Color,
    val shieldYellow: Color,
    val trustBadge: Color,
    val iconDefault: Color,
    val textTertiary: Color
)

val AegisLightCustomColors = AegisCustomColors(
    success = AegisLightPalette.Success,
    successContainer = AegisLightPalette.SuccessContainer,
    warning = AegisLightPalette.Warning,
    warningContainer = AegisLightPalette.WarningContainer,
    shieldGreen = AegisLightPalette.ShieldGreen,
    shieldYellow = AegisLightPalette.ShieldYellow,
    trustBadge = AegisLightPalette.TrustBadge,
    iconDefault = AegisLightPalette.IconDefault,
    textTertiary = AegisLightPalette.TextTertiary
)

val AegisDarkCustomColors = AegisCustomColors(
    success = AegisDarkPalette.Success,
    successContainer = AegisDarkPalette.SuccessContainer,
    warning = AegisDarkPalette.Warning,
    warningContainer = AegisDarkPalette.WarningContainer,
    shieldGreen = AegisDarkPalette.ShieldGreen,
    shieldYellow = AegisDarkPalette.ShieldYellow,
    trustBadge = AegisDarkPalette.TrustBadge,
    iconDefault = AegisDarkPalette.IconDefault,
    textTertiary = AegisDarkPalette.TextTertiary
)

/**
 * CompositionLocal to provide custom colors
 */
val LocalAegisCustomColors = staticCompositionLocalOf { AegisDarkCustomColors }


// =========================================================================
// Extensions for Unified Usage via MaterialTheme.colorScheme.xxx
// =========================================================================

val ColorScheme.success: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.success

val ColorScheme.successContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.successContainer

val ColorScheme.warning: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.warning

val ColorScheme.warningContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.warningContainer

val ColorScheme.shieldGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.shieldGreen

val ColorScheme.shieldYellow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.shieldYellow

val ColorScheme.trustBadge: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.trustBadge

val ColorScheme.iconDefault: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.iconDefault

val ColorScheme.textTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAegisCustomColors.current.textTertiary

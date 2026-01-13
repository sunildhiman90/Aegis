package app.aegis.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aegis Color Palette - Dark and Light Theme Colors
 * Colors based on Stitch design guidelines
 */

// === DARK THEME COLORS (Stitch Guidelines) ===
object AegisDarkColors {
    // Background Colors
    val Background = Color(0xFF121212)          // Deep Charcoal (Stitch)
    val Surface = Color(0xFF1E1E1E)             // Lighter Grey cards (Stitch)
    val SurfaceVariant = Color(0xFF2D2D2D)      // Slightly elevated surface

    // Primary Accent - Aegis Gold (Stitch)
    val Primary = Color(0xFFFFD700)             // Aegis Gold
    val PrimaryVariant = Color(0xFFE6C200)      // Darker gold for pressed states
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
    val CardBackground = Color(0xFF1E1E1E)      // Same as Surface (Stitch)
    val CardBorder = Color(0xFF333333)          // Card border
    val Divider = Color(0xFF2D2D2D)             // Dividers
    val IconDefault = Color(0xFFB0B0B0)         // Default icon color

    // Special Colors
    val ShieldGreen = Color(0xFF2E7D32)         // Active protection shield (Forest Green)
    val ShieldYellow = Color(0xFFFFC107)        // Warning/setup shield (Amber)
    val TrustBadge = Color(0xFF6366F1)          // Trusted contacts badge (Indigo)
}

// === LIGHT THEME COLORS (Stitch Guidelines) ===
object AegisLightColors {
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
    val CardBackground = Color(0xFFFFFFFF)      // White card background
    val CardBorder = Color(0xFFE0E0E0)          // Light gray border
    val Divider = Color(0xFFE0E0E0)             // Dividers
    val IconDefault = Color(0xFF616161)         // Default icon color

    // Special Colors
    val ShieldGreen = Color(0xFF2E7D32)         // Active protection shield
    val ShieldYellow = Color(0xFFFFC107)        // Warning/setup shield
    val TrustBadge = Color(0xFF6366F1)          // Trusted contacts badge
}

/**
 * Unified color interface for theme-aware color access
 */
data class AegisColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val primaryVariant: Color,
    val primaryContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val divider: Color,
    val iconDefault: Color,
    val shieldGreen: Color,
    val shieldYellow: Color,
    val trustBadge: Color
)

val DarkColorScheme = AegisColorScheme(
    background = AegisDarkColors.Background,
    surface = AegisDarkColors.Surface,
    surfaceVariant = AegisDarkColors.SurfaceVariant,
    primary = AegisDarkColors.Primary,
    primaryVariant = AegisDarkColors.PrimaryVariant,
    primaryContainer = AegisDarkColors.PrimaryContainer,
    textPrimary = AegisDarkColors.TextPrimary,
    textSecondary = AegisDarkColors.TextSecondary,
    textTertiary = AegisDarkColors.TextTertiary,
    success = AegisDarkColors.Success,
    successContainer = AegisDarkColors.SuccessContainer,
    warning = AegisDarkColors.Warning,
    warningContainer = AegisDarkColors.WarningContainer,
    error = AegisDarkColors.Error,
    errorContainer = AegisDarkColors.ErrorContainer,
    cardBackground = AegisDarkColors.CardBackground,
    cardBorder = AegisDarkColors.CardBorder,
    divider = AegisDarkColors.Divider,
    iconDefault = AegisDarkColors.IconDefault,
    shieldGreen = AegisDarkColors.ShieldGreen,
    shieldYellow = AegisDarkColors.ShieldYellow,
    trustBadge = AegisDarkColors.TrustBadge
)

val LightColorScheme = AegisColorScheme(
    background = AegisLightColors.Background,
    surface = AegisLightColors.Surface,
    surfaceVariant = AegisLightColors.SurfaceVariant,
    primary = AegisLightColors.Primary,
    primaryVariant = AegisLightColors.PrimaryVariant,
    primaryContainer = AegisLightColors.PrimaryContainer,
    textPrimary = AegisLightColors.TextPrimary,
    textSecondary = AegisLightColors.TextSecondary,
    textTertiary = AegisLightColors.TextTertiary,
    success = AegisLightColors.Success,
    successContainer = AegisLightColors.SuccessContainer,
    warning = AegisLightColors.Warning,
    warningContainer = AegisLightColors.WarningContainer,
    error = AegisLightColors.Error,
    errorContainer = AegisLightColors.ErrorContainer,
    cardBackground = AegisLightColors.CardBackground,
    cardBorder = AegisLightColors.CardBorder,
    divider = AegisLightColors.Divider,
    iconDefault = AegisLightColors.IconDefault,
    shieldGreen = AegisLightColors.ShieldGreen,
    shieldYellow = AegisLightColors.ShieldYellow,
    trustBadge = AegisLightColors.TrustBadge
)

/**
 * Legacy AegisColors object for backward compatibility
 * Now provides theme-aware colors through LocalAegisColors
 */
object AegisColors {
    // These are now dynamically provided through LocalAegisColors
    // Kept for backward compatibility - defaults to dark theme values
    val Background = AegisDarkColors.Background
    val Surface = AegisDarkColors.Surface
    val SurfaceVariant = AegisDarkColors.SurfaceVariant
    val Primary = AegisDarkColors.Primary
    val PrimaryVariant = AegisDarkColors.PrimaryVariant
    val PrimaryContainer = AegisDarkColors.PrimaryContainer
    val TextPrimary = AegisDarkColors.TextPrimary
    val TextSecondary = AegisDarkColors.TextSecondary
    val TextTertiary = AegisDarkColors.TextTertiary
    val Success = AegisDarkColors.Success
    val SuccessContainer = AegisDarkColors.SuccessContainer
    val Warning = AegisDarkColors.Warning
    val WarningContainer = AegisDarkColors.WarningContainer
    val Error = AegisDarkColors.Error
    val ErrorContainer = AegisDarkColors.ErrorContainer
    val CardBackground = AegisDarkColors.CardBackground
    val CardBorder = AegisDarkColors.CardBorder
    val Divider = AegisDarkColors.Divider
    val IconDefault = AegisDarkColors.IconDefault
    val ShieldGreen = AegisDarkColors.ShieldGreen
    val ShieldYellow = AegisDarkColors.ShieldYellow
    val TrustBadge = AegisDarkColors.TrustBadge

    // Danger overlay colors (same for both themes)
    val DangerGradientStart = Color(0xFFB71C1C)
    val DangerGradientEnd = Color(0xFF4A0A0A)
    val CriticalRed = Color(0xFFB71C1C)

    // Navigation colors reference Surface
    val NavBackground = Surface
    val NavSelected = Primary
    val NavUnselected = TextSecondary

    // Button colors
    val ButtonPrimary = Primary
    val ButtonPrimaryText = Color(0xFF121212)  // Dark text on gold
    val ButtonSecondary = Color(0xFF2D2D2D)
    val ButtonSecondaryText = TextPrimary
    val ButtonDanger = Error
    val ButtonDangerText = Color.White
}

package app.aegis.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aegis Color Palette - Dark and Light Theme Colors
 * Colors extracted from design mockups
 */

// === DARK THEME COLORS ===
object AegisDarkColors {
    // Background Colors
    val Background = Color(0xFF0D1117)          // Deep navy background
    val Surface = Color(0xFF161B22)             // Card/elevated surface
    val SurfaceVariant = Color(0xFF1C2128)      // Slightly elevated surface

    // Primary Accent
    val Primary = Color(0xFF3B82F6)             // Main blue accent
    val PrimaryVariant = Color(0xFF2563EB)      // Darker blue for pressed states
    val PrimaryContainer = Color(0xFF1E3A5F)    // Blue container background

    // Text Colors
    val TextPrimary = Color(0xFFFFFFFF)         // White text
    val TextSecondary = Color(0xFF8B949E)       // Gray text
    val TextTertiary = Color(0xFF6E7681)        // Subtle gray text

    // Status Colors
    val Success = Color(0xFF22C55E)             // Green for active/success
    val SuccessContainer = Color(0xFF143620)    // Green container
    val Warning = Color(0xFFF59E0B)             // Amber for warnings
    val WarningContainer = Color(0xFF422006)    // Amber container
    val Error = Color(0xFFDC2626)               // Red for errors/danger
    val ErrorContainer = Color(0xFF450A0A)      // Red container

    // Component Colors
    val CardBackground = Color(0xFF1A1F26)      // Card background
    val CardBorder = Color(0xFF30363D)          // Card border
    val Divider = Color(0xFF21262D)             // Dividers
    val IconDefault = Color(0xFF8B949E)         // Default icon color

    // Special Colors
    val ShieldGreen = Color(0xFF22C55E)         // Active protection shield
    val ShieldYellow = Color(0xFFFBBF24)        // Warning/setup shield
    val TrustBadge = Color(0xFF6366F1)          // Trusted contacts badge
}

// === LIGHT THEME COLORS ===
object AegisLightColors {
    // Background Colors
    val Background = Color(0xFFF8FAFC)          // Light gray background
    val Surface = Color(0xFFFFFFFF)             // White surface
    val SurfaceVariant = Color(0xFFF1F5F9)      // Slightly elevated surface

    // Primary Accent (same as dark)
    val Primary = Color(0xFF3B82F6)             // Main blue accent
    val PrimaryVariant = Color(0xFF2563EB)      // Darker blue for pressed states
    val PrimaryContainer = Color(0xFFDBEAFE)    // Blue container background

    // Text Colors
    val TextPrimary = Color(0xFF1E293B)         // Dark slate text
    val TextSecondary = Color(0xFF64748B)       // Slate gray text
    val TextTertiary = Color(0xFF94A3B8)        // Light slate text

    // Status Colors
    val Success = Color(0xFF16A34A)             // Green for active/success
    val SuccessContainer = Color(0xFFDCFCE7)    // Green container
    val Warning = Color(0xFFD97706)             // Amber for warnings
    val WarningContainer = Color(0xFFFEF3C7)    // Amber container
    val Error = Color(0xFFDC2626)               // Red for errors/danger
    val ErrorContainer = Color(0xFFFEE2E2)      // Red container

    // Component Colors
    val CardBackground = Color(0xFFFFFFFF)      // White card background
    val CardBorder = Color(0xFFE2E8F0)          // Light gray border
    val Divider = Color(0xFFE2E8F0)             // Dividers
    val IconDefault = Color(0xFF64748B)         // Default icon color

    // Special Colors
    val ShieldGreen = Color(0xFF16A34A)         // Active protection shield
    val ShieldYellow = Color(0xFFD97706)        // Warning/setup shield
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
    val DangerGradientStart = Color(0xFFB91C1C)
    val DangerGradientEnd = Color(0xFF7F1D1D)
    val CriticalRed = Color(0xFFDC2626)

    // Navigation colors reference Surface
    val NavBackground = Surface
    val NavSelected = Primary
    val NavUnselected = TextSecondary

    // Button colors
    val ButtonPrimary = Primary
    val ButtonPrimaryText = Color.White
    val ButtonSecondary = Color(0xFF21262D)
    val ButtonSecondaryText = TextPrimary
    val ButtonDanger = Error
    val ButtonDangerText = Color.White
}

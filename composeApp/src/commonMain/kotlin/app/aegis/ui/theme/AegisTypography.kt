package app.aegis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Aegis Typography - Consistent text styles across the app
 */
val AegisTypography = Typography(
    // === DISPLAY ===
    displayLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeight = 36.sp
    ),
    // displaySmall not customized, using default or falling back

    // === HEADLINE ===
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 26.sp
    ),

    // === TITLE ===
    titleLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 16.sp
    ),

    // === BODY ===
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 16.sp
    ),

    // === LABEL ===
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        lineHeight = 14.sp
    )
)

// === SPECIAL / CUSTOM STYLES (Extensions) ===

val Typography.caption: TextStyle
    get() = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp,
        lineHeight = 14.sp
    )

val Typography.overline: TextStyle
    get() = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        lineHeight = 14.sp
    )

val Typography.button: TextStyle
    get() = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    )


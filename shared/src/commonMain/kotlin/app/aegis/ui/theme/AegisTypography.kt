package app.aegis.ui.theme

import aegis.shared.generated.resources.Res
import aegis.shared.generated.resources.noto_sans_bold
import aegis.shared.generated.resources.noto_sans_medium
import aegis.shared.generated.resources.noto_sans_regular
import aegis.shared.generated.resources.space_grotesk_bold
import aegis.shared.generated.resources.space_grotesk_light
import aegis.shared.generated.resources.space_grotesk_medium
import aegis.shared.generated.resources.space_grotesk_regular
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font

/**
 * Aegis Typography - Consistent text styles across the app
 */
@Composable
fun aegisTypography(): Typography {
    val spaceGrotesk = FontFamily(
        Font(Res.font.space_grotesk_regular, FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
        Font(Res.font.space_grotesk_bold, FontWeight.Bold),
        Font(Res.font.space_grotesk_light, FontWeight.Light),
    )

    val notoSans = FontFamily(
        Font(Res.font.noto_sans_regular, FontWeight.Normal),
        Font(Res.font.noto_sans_medium, FontWeight.Medium),
        Font(Res.font.noto_sans_bold, FontWeight.Bold),
    )

    return Typography(
        // === DISPLAY ===
        displayLarge = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            lineHeight = 40.sp
        ),
        displayMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            lineHeight = 36.sp
        ),
        // displaySmall not customized, using default with font applied if we wanted, but leaving default for now or fallback?
        // Better to define all or at least the ones we modify.
        // For consistency, M3 typically uses the same font family for all Display/Headline/Title.
        displaySmall = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 36.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),

        // === HEADLINE ===
        headlineLarge = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = 26.sp
        ),

        // === TITLE ===
        titleLarge = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
            lineHeight = 20.sp
        ),
        titleSmall = TextStyle(
            fontFamily = spaceGrotesk,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
            lineHeight = 16.sp
        ),

        // === BODY ===
        bodyLarge = TextStyle(
            fontFamily = notoSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = notoSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = notoSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
            lineHeight = 16.sp
        ),

        // === LABEL ===
        labelLarge = TextStyle(
            fontFamily = notoSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = notoSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = notoSans,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            lineHeight = 14.sp
        )
    )
}

// === SPECIAL / CUSTOM STYLES (Extensions) ===

val Typography.caption: TextStyle
    get() = this.bodySmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp,
        lineHeight = 14.sp
    )

val Typography.overline: TextStyle
    get() = this.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        lineHeight = 14.sp
    )

val Typography.button: TextStyle
    get() = this.labelLarge.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    )

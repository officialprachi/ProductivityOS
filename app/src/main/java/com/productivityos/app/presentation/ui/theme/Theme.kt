package com.productivityos.app.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.productivityos.app.domain.model.FontSize

// ── Brand Gradients (shared across all screens) ───────────────
// Primary brand gradient: blue → cyan → green
val GradientPrimary = Brush.linearGradient(
    colors = listOf(BrandBlue, BrandCyan, BrandGreen)
)

// Softer version for stat highlights
val GradientAccent = Brush.linearGradient(
    colors = listOf(AccentBlue, AccentCyan, AccentGreen)
)

// Vertical gradient for progress bars
val GradientVertical = Brush.verticalGradient(
    colors = listOf(BrandBlue, BrandGreen)
)

// Subtle card header tint
val GradientCardStrip = Brush.horizontalGradient(
    colors = listOf(BrandBlue.copy(alpha = 0.25f), BrandGreen.copy(alpha = 0.0f))
)

// ── Color Schemes ─────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = BrandBlue,
    onPrimary          = White,
    primaryContainer   = BrandBlueAlpha20,
    secondary          = BrandGreen,
    onSecondary        = White,
    secondaryContainer = BrandGreenAlpha20,
    tertiary           = AccentCyan,
    background         = Background,
    onBackground       = TextPrimary,
    surface            = CardBg,
    onSurface          = TextPrimary,
    surfaceVariant     = CardBgHigh,
    onSurfaceVariant   = TextSecondary,
    outline            = SurfaceBorder,
    error              = Danger,
    onError            = White
)

private val LightColorScheme = lightColorScheme(
    primary            = BrandBlue,
    onPrimary          = White,
    primaryContainer   = BrandBlueAlpha12,
    secondary          = BrandGreen,
    onSecondary        = White,
    secondaryContainer = BrandGreenAlpha12,
    tertiary           = AccentCyan,
    background         = LightBackground,
    onBackground       = LightTextPrimary,
    surface            = LightSurface,
    onSurface          = LightTextPrimary,
    surfaceVariant     = LightSurfaceHigh,
    onSurfaceVariant   = LightTextSecondary,
    outline            = LightSurfaceBorder,
    error              = Danger,
    onError            = White
)

// ── CompositionLocals ─────────────────────────────────────────
val LocalIsDarkTheme = staticCompositionLocalOf { true }
val LocalFontScale   = staticCompositionLocalOf { 1f }

fun FontSize.toScale(): Float = when (this) {
    FontSize.SMALL  -> 0.85f
    FontSize.MEDIUM -> 1f
    FontSize.LARGE  -> 1.15f
    FontSize.XLARGE -> 1.3f
}

private fun scaledTypography(scale: Float) = AppTypography.copy(
    displayLarge   = AppTypography.displayLarge.scale(scale),
    displayMedium  = AppTypography.displayMedium.scale(scale),
    headlineMedium = AppTypography.headlineMedium.scale(scale),
    headlineSmall  = AppTypography.headlineSmall.scale(scale),
    bodyLarge      = AppTypography.bodyLarge.scale(scale),
    bodyMedium     = AppTypography.bodyMedium.scale(scale),
    bodySmall      = AppTypography.bodySmall.scale(scale),
    labelLarge     = AppTypography.labelLarge.scale(scale),
    labelMedium    = AppTypography.labelMedium.scale(scale),
    labelSmall     = AppTypography.labelSmall.scale(scale)
)

private fun TextStyle.scale(factor: Float) = copy(fontSize = fontSize * factor)

// ── Theme Entry Point ─────────────────────────────────────────
@Composable
fun ProductivityOSTheme(
    darkTheme: Boolean = true,
    fontSize: FontSize = FontSize.MEDIUM,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val fontScale   = fontSize.toScale()

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalFontScale   provides fontScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = scaledTypography(fontScale),
            content     = content
        )
    }
}
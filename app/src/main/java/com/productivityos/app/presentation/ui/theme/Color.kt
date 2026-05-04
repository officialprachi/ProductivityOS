package com.productivityos.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background & Surface ──────────────────────────────────────
val Background  = Color(0xFF0A0F14)
val CardBg      = Color(0xFF111921)   // was: Surface
val CardBgHigh  = Color(0xFF18242F)   // was: SurfaceHigh
val CardBorder  = Color(0xFF1E2D3D)   // was: SurfaceBorder

// Aliases so existing code using old names still compiles
val SurfaceHigh   get() = CardBgHigh
val SurfaceBorder get() = CardBorder

// ── Brand Gradient Stops ──────────────────────────────────────
val BrandBlue    = Color(0xFF1A73E8)
val BrandCyan    = Color(0xFF00BCD4)
val BrandTeal    = Color(0xFF00897B)
val BrandGreen   = Color(0xFF00C853)
val AccentBlue   = Color(0xFF42A5F5)
val AccentGreen  = Color(0xFF69F0AE)
val AccentCyan   = BrandCyan

// Alpha variants
val BrandBlueAlpha12  = Color(0x1F1A73E8)
val BrandBlueAlpha20  = Color(0x331A73E8)
val BrandGreenAlpha12 = Color(0x1F00C853)
val BrandGreenAlpha20 = Color(0x3300C853)
val AccentCyanAlpha15 = Color(0x2600BCD4)

// ── Text ──────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFECF0F5)
val TextSecondary = Color(0xFF7A9BB5)
val TextTertiary  = Color(0xFF3D5A73)

// ── Semantic ──────────────────────────────────────────────────
val Success      = Color(0xFF00C853)
val Warning      = Color(0xFFFFAB40)
val Danger       = Color(0xFFEF5350)
val Info         = Color(0xFF42A5F5)

val SuccessAlpha = Color(0x2000C853)
val WarningAlpha = Color(0x20FFAB40)
val DangerAlpha  = Color(0x20EF5350)

// ── Neutral ───────────────────────────────────────────────────
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// ── Light Mode ────────────────────────────────────────────────
val LightBackground    = Color(0xFFF0F6FF)
val LightSurface       = Color(0xFFFFFFFF)
val LightSurfaceHigh   = Color(0xFFE8F4FD)
val LightSurfaceBorder = Color(0xFFCDE3F5)
val LightTextPrimary   = Color(0xFF0D1B2A)
val LightTextSecondary = Color(0xFF4A6FA5)

// ── Semantic aliases ──────────────────────────────────────────
val FocusColor    = BrandBlue
val GoalColor     = BrandGreen
val StreakColor   = AccentCyan
val NeutralColor  = TextSecondary
val DistractColor = Danger

// ── Legacy aliases (old Plum/Rose/Ink palette) ────────────────
val Plum          = BrandBlue
val PlumLight     = BrandBlueAlpha12
val PlumMid       = BrandBlueAlpha20
val Rose          = Danger
val RoseLight     = DangerAlpha
val RoseMid       = Color(0x40EF5350)
val Mauve         = TextSecondary
val MauveLight    = Color(0x1F7A9BB5)
val Ink           = Background
val Ink2          = CardBg
val Ink3          = CardBorder
val Paper         = LightBackground
val Paper2        = LightSurface
val Paper3        = LightSurfaceHigh
val ScoreBarBg    = Color(0x33FFFFFF)
val ScoreBarFill  = White
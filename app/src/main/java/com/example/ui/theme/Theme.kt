package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class PuzzleExtendedColors(
    val boardBackground: Color,
    val gridDot: Color,
    val arrowDefault: Color,
    val arrowHighlightEdge: Color,
    val arrowShadow: Color,
    val arrowActiveBlue: Color,
    val arrowBlockedRed: Color,
    val heartFull: Color,
    val heartEmpty: Color,
    val pillBackground: Color,
    val victoryGradientStart: Color,
    val victoryGradientEnd: Color,
    val goldStar: Color
)

val LocalPuzzleColors = staticCompositionLocalOf {
    PuzzleExtendedColors(
        boardBackground = PaperWhite,
        gridDot = GridDotLight,
        arrowDefault = ArrowDeepNavy,
        arrowHighlightEdge = ArrowNavyHighlight,
        arrowShadow = ArrowNavyShadow,
        arrowActiveBlue = ElectricBlue,
        arrowBlockedRed = HeartRed,
        heartFull = HeartRed,
        heartEmpty = HeartEmptyLight,
        pillBackground = SurfacePill,
        victoryGradientStart = ElectricBlue,
        victoryGradientEnd = DeepAzure,
        goldStar = GoldStar
    )
}

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = PaperWhite,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = DeepAzure,
    secondary = ArrowDeepNavy,
    onSecondary = PaperWhite,
    secondaryContainer = SurfacePill,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = SkyGlow,
    onTertiary = ArrowDeepNavy,
    background = CanvasOffWhite,
    onBackground = TextPrimaryLight,
    surface = PaperWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfacePill,
    onSurfaceVariant = TextSecondaryLight,
    error = HeartRed,
    onError = PaperWhite,
    outline = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyGlow,
    onPrimary = NightNavyBg,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = ArrowChalkWhite,
    onSecondary = NightNavyBg,
    secondaryContainer = NightPill,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = ElectricBlue,
    onTertiary = PaperWhite,
    background = NightNavyBg,
    onBackground = TextPrimaryDark,
    surface = NightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NightPill,
    onSurfaceVariant = TextSecondaryDark,
    error = HeartRed,
    onError = PaperWhite,
    outline = Color(0xFF334155)
)

private val TapAwayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun TapAwayArrowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        PuzzleExtendedColors(
            boardBackground = NightSurface,
            gridDot = GridDotDark,
            arrowDefault = ArrowChalkWhite,
            arrowHighlightEdge = ArrowChalkHighlight,
            arrowShadow = ArrowChalkShadow,
            arrowActiveBlue = SkyGlow,
            arrowBlockedRed = HeartRed,
            heartFull = HeartRed,
            heartEmpty = HeartEmptyDark,
            pillBackground = NightPill,
            victoryGradientStart = Color(0xFF1565C0),
            victoryGradientEnd = Color(0xFF0A2E7A),
            goldStar = GoldStar
        )
    } else {
        PuzzleExtendedColors(
            boardBackground = PaperWhite,
            gridDot = GridDotLight,
            arrowDefault = ArrowDeepNavy,
            arrowHighlightEdge = ArrowNavyHighlight,
            arrowShadow = ArrowNavyShadow,
            arrowActiveBlue = ElectricBlue,
            arrowBlockedRed = HeartRed,
            heartFull = HeartRed,
            heartEmpty = HeartEmptyLight,
            pillBackground = SurfacePill,
            victoryGradientStart = ElectricBlue,
            victoryGradientEnd = DeepAzure,
            goldStar = GoldStar
        )
    }

    CompositionLocalProvider(LocalPuzzleColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TapAwayTypography,
            shapes = TapAwayShapes,
            content = content
        )
    }
}

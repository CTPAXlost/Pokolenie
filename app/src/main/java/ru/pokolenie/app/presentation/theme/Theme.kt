package ru.pokolenie.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF0B0F14)
val InkElevated = Color(0xFF141A22)
val InkLine = Color(0xFF243041)
val Brass = Color(0xFFD4A017)
val BrassSoft = Color(0xFFE8D5A3)
val Mist = Color(0xFFB7C0CC)
val MistDim = Color(0xFF7E8A99)
val SignalGreen = Color(0xFF3DDC97)
val SignalRed = Color(0xFFE26D5A)

private val PokolenieDarkColors = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    secondary = BrassSoft,
    onSecondary = Ink,
    background = Ink,
    onBackground = Mist,
    surface = InkElevated,
    onSurface = Mist,
    surfaceVariant = Color(0xFF1B2430),
    onSurfaceVariant = MistDim,
    outline = InkLine,
    error = SignalRed,
    onError = Color.White
)

private val PokolenieTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 48.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = MistDim
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun PokolenieTheme(content: @Composable () -> Unit) {
    // Brand identity is always dark; ignore system light for consistency.
    val dark = isSystemInDarkTheme() || true
    MaterialTheme(
        colorScheme = if (dark) PokolenieDarkColors else PokolenieDarkColors,
        typography = PokolenieTypography,
        content = content
    )
}

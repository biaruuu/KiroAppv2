package com.kiro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KiroColors {
    val Bg = Color(0xFF121116)
    val NavBar = Color(0xFF17161C)
    val Surface = Color(0xFF1C1B22)
    val SurfaceHigh = Color(0xFF2B2A33)
    val Accent = Color(0xFF7C6BF2)
    val TextSecondary = Color(0xFFB6B4C2)
}

private val KiroDarkScheme = darkColorScheme(
    primary = KiroColors.Accent,
    onPrimary = Color.White,
    background = KiroColors.Bg,
    onBackground = Color.White,
    surface = KiroColors.Surface,
    onSurface = Color.White,
    surfaceVariant = KiroColors.SurfaceHigh,
    onSurfaceVariant = KiroColors.TextSecondary,
    error = Color(0xFFFF6B6B),
)

@Composable
fun KiroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KiroDarkScheme,
        typography = KiroTypography,
        content = content,
    )
}

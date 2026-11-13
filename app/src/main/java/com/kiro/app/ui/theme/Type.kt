package com.kiro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kiro.app.R

/**
 * The app font. The four slots are generated at build time by `prepareFonts` in app/build.gradle.kts:
 * Roboto by default, or your own files from fonts-private/.
 */
val AppFont = FontFamily(
    Font(R.font.app_light, FontWeight.Light),
    Font(R.font.app_regular, FontWeight.Normal),
    Font(R.font.app_medium, FontWeight.Medium),
    Font(R.font.app_bold, FontWeight.Bold),
)

private fun TextStyle.withAppFont() = copy(fontFamily = AppFont)

val KiroTypography: Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.withAppFont(),
        displayMedium = t.displayMedium.withAppFont(),
        displaySmall = t.displaySmall.withAppFont(),
        headlineLarge = t.headlineLarge.withAppFont(),
        headlineMedium = t.headlineMedium.withAppFont(),
        headlineSmall = t.headlineSmall.withAppFont(),
        titleLarge = t.titleLarge.withAppFont(),
        titleMedium = t.titleMedium.withAppFont(),
        titleSmall = t.titleSmall.withAppFont(),
        bodyLarge = t.bodyLarge.withAppFont(),
        bodyMedium = t.bodyMedium.withAppFont(),
        bodySmall = t.bodySmall.withAppFont(),
        labelLarge = t.labelLarge.withAppFont(),
        labelMedium = t.labelMedium.withAppFont(),
        labelSmall = t.labelSmall.withAppFont(),
    )
}

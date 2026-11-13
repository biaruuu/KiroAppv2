package com.kiro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kiro.app.R

/** Roboto (Google Fonts), static files bundled in res/font. */
val Roboto = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold),
    Font(R.font.roboto_black, FontWeight.Black),
)

private fun TextStyle.withRoboto() = copy(fontFamily = Roboto)

val KiroTypography: Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.withRoboto(),
        displayMedium = t.displayMedium.withRoboto(),
        displaySmall = t.displaySmall.withRoboto(),
        headlineLarge = t.headlineLarge.withRoboto(),
        headlineMedium = t.headlineMedium.withRoboto(),
        headlineSmall = t.headlineSmall.withRoboto(),
        titleLarge = t.titleLarge.withRoboto(),
        titleMedium = t.titleMedium.withRoboto(),
        titleSmall = t.titleSmall.withRoboto(),
        bodyLarge = t.bodyLarge.withRoboto(),
        bodyMedium = t.bodyMedium.withRoboto(),
        bodySmall = t.bodySmall.withRoboto(),
        labelLarge = t.labelLarge.withRoboto(),
        labelMedium = t.labelMedium.withRoboto(),
        labelSmall = t.labelSmall.withRoboto(),
    )
}

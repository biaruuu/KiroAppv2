package com.kiro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.kiro.app.R

/**
 * Roboto (Google Fonts) as a variable font bundled at res/font/roboto.ttf.
 * Each weight below is the same file with a different "wght" axis value.
 */
@OptIn(ExperimentalTextApi::class)
private fun robotoWeight(weight: Int) = Font(
    resId = R.font.roboto,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Roboto = FontFamily(
    robotoWeight(400),
    robotoWeight(500),
    robotoWeight(700),
    robotoWeight(900),
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

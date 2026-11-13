package com.kiro.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Width of one poster in a horizontal shelf: about 3 across on phones, larger (capped) on tablets. */
@Composable
fun rememberPosterWidth(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return (w * 0.31f).coerceIn(104f, 190f).dp
}

/**
 * Height of the Home hero banner.
 * Phones in portrait: tall poster-style banner. Tablets and landscape: a wide banner that never
 * takes more than ~70% of the screen height.
 */
@Composable
fun rememberHeroHeight(): Dp {
    val c = LocalConfiguration.current
    val w = c.screenWidthDp.toFloat()
    val h = c.screenHeightDp.toFloat()
    val height = if (w < 600f) minOf(w / 0.78f, h * 0.78f) else minOf(w * 0.5f, h * 0.72f)
    return height.dp
}

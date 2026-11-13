package com.kiro.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Feather icons (https://feathericons.com, MIT license) as Compose vectors:
 * 24x24 viewport, 2px round strokes. Colour comes from Icon(tint = ...).
 */
object FeatherIcons {
    val Home: ImageVector by lazy {
        feather("Home", "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", "M9 22V12h6v10")
    }

    val Search: ImageVector by lazy {
        feather("Search", "M11 3a8 8 0 1 0 0 16a8 8 0 1 0 0-16z", "M21 21l-4.35-4.35")
    }

    val Bell: ImageVector by lazy {
        feather("Bell", "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9", "M13.73 21a2 2 0 0 1-3.46 0")
    }

    val X: ImageVector by lazy {
        feather("X", "M18 6L6 18", "M6 6l12 12")
    }

    val Clock: ImageVector by lazy {
        feather("Clock", "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z", "M12 6v6l4 2")
    }

    val CloudOff: ImageVector by lazy {
        feather(
            "CloudOff",
            "M22.61 16.95A5 5 0 0 0 18 10h-1.26a8 8 0 0 0-7.05-6M5 5a8 8 0 0 0 4 15h9a5 5 0 0 0 1.7-.3",
            "M1 1l22 22",
        )
    }

    /** Feather "play", filled so it reads well on the white Play buttons. */
    val PlayFilled: ImageVector by lazy {
        feather("Play", "M5 3l14 9-14 9z", filled = true)
    }

    private fun feather(name: String, vararg paths: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(
            name = "Feather.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            for (d in paths) {
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}

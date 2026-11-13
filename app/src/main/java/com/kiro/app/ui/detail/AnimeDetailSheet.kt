package com.kiro.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.kiro.app.ui.icons.FeatherIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiro.app.data.Anime
import com.kiro.app.ui.components.AnimeImage
import com.kiro.app.ui.components.POSTER_RATIO
import com.kiro.app.ui.theme.KiroColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailSheet(anime: Anime, onDismiss: () -> Unit, onPlay: (Anime) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KiroColors.Surface,
        contentColor = Color.White,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row {
                AnimeImage(
                    url = anime.bestImage(),
                    contentDescription = anime.displayTitle(),
                    modifier = Modifier
                        .width(110.dp)
                        .height((110 / POSTER_RATIO).dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column(Modifier.padding(start = 16.dp)) {
                    Text(
                        text = anime.displayTitle(),
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    val meta = metaLine(anime)
                    if (meta.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(text = meta, color = KiroColors.TextSecondary, fontSize = 14.sp)
                    }
                    if (anime.status.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(text = anime.status, color = KiroColors.TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onPlay(anime) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) {
                Icon(FeatherIcons.PlayFilled, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (anime.trailerUrl.isNotEmpty()) "Play trailer" else "More info",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (anime.genres.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    anime.genres.forEach { g ->
                        Text(
                            text = g,
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(KiroColors.SurfaceHigh)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = anime.synopsis.ifEmpty { "No synopsis available." },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

private fun metaLine(a: Anime): String {
    val parts = mutableListOf<String>()
    if (a.type.isNotEmpty()) parts += a.type
    if (a.year > 0) parts += a.year.toString()
    if (a.episodes > 0) parts += "${a.episodes} eps"
    if (a.score > 0) parts += "★ " + String.format(Locale.US, "%.1f", a.score)
    return parts.joinToString("  ·  ")
}

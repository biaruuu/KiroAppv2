package com.kiro.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiro.app.data.Anime
import com.kiro.app.ui.components.AnimeImage
import com.kiro.app.ui.components.ErrorView
import com.kiro.app.ui.components.KiroLogo
import com.kiro.app.ui.components.ShelfRow
import com.kiro.app.ui.components.SkeletonRow
import com.kiro.app.ui.components.shimmer
import com.kiro.app.ui.theme.KiroColors
import com.kiro.app.util.tagline
import kotlinx.coroutines.delay

private const val HERO_RATIO = 0.78f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (Anime) -> Unit,
    onPlay: (Anime) -> Unit,
) {
    val context = LocalContext.current
    val hasContent = state.hero.isNotEmpty() || state.rows.isNotEmpty()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            !hasContent && state.loading -> HomeSkeleton()
            !hasContent && state.error != null -> ErrorView(state.error, onRetry)
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (state.hero.isNotEmpty()) {
                    item(key = "hero") {
                        HeroPager(
                            items = state.hero,
                            onOpen = onOpen,
                            onPlay = onPlay,
                            onBell = {
                                Toast.makeText(context, "You're all caught up", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                } else {
                    item(key = "top-spacer") { Spacer(Modifier.statusBarsPadding().height(16.dp)) }
                }

                if (state.continueWatching.isNotEmpty()) {
                    item(key = "continue") {
                        ShelfRow("Continue Watching", state.continueWatching, onOpen)
                    }
                }

                items(state.rows, key = { it.id }) { row ->
                    ShelfRow(row.title, row.items, onOpen)
                }

                if (state.loading) {
                    item(key = "loading-more") { SkeletonRow() }
                }
            }
        }
    }
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(HERO_RATIO)
                .shimmer(),
        )
        Spacer(Modifier.height(28.dp))
        SkeletonRow()
        Spacer(Modifier.height(28.dp))
        SkeletonRow()
    }
}

@Composable
private fun HeroPager(
    items: List<Anime>,
    onOpen: (Anime) -> Unit,
    onPlay: (Anime) -> Unit,
    onBell: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-advance every 5 s, but never fight the user's finger.
    LaunchedEffect(items.size) {
        while (items.size > 1) {
            delay(5_000)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
            }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(HERO_RATIO),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { items[it].id },
        ) { page ->
            HeroPage(items[page], onOpen = { onOpen(items[page]) }, onPlay = { onPlay(items[page]) })
        }

        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KiroLogo()
            IconButton(onClick = onBell) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color.White)
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
        ) {
            repeat(items.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

@Composable
private fun HeroPage(anime: Anime, onOpen: () -> Unit, onPlay: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(onClick = onOpen),
    ) {
        AnimeImage(
            url = anime.bestImage(),
            contentDescription = anime.displayTitle(),
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.28f to Color.Transparent,
                            0.5f to Color.Transparent,
                            1f to KiroColors.Bg,
                        ),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = anime.displayTitle(),
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val tagline = anime.synopsis.tagline()
            if (tagline.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tagline,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .width(120.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

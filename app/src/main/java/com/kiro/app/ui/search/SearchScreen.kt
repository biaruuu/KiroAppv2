package com.kiro.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiro.app.data.Anime
import com.kiro.app.ui.BrowseGenres
import com.kiro.app.ui.Genre
import com.kiro.app.ui.components.AnimeCard
import com.kiro.app.ui.components.ErrorView
import com.kiro.app.ui.components.ScreenMessage
import com.kiro.app.ui.theme.KiroColors

@Composable
fun SearchScreen(
    state: SearchUiState,
    bottomPadding: Dp,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onRecentClick: (String) -> Unit,
    onRecentRemove: (String) -> Unit,
    onRecentClear: () -> Unit,
    onGenre: (Genre) -> Unit,
    onClearGenre: () -> Unit,
    onOpen: (Anime) -> Unit,
    onRetry: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val inResults = state.activeGenre != null || state.query.trim().length >= SearchViewModel.MIN_QUERY

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SearchField(
            query = state.query,
            onChange = onQueryChange,
            onSubmit = {
                keyboard?.hide()
                focusManager.clearFocus()
                onSubmit()
            },
            onClear = onClear,
        )

        Box(Modifier.fillMaxSize()) {
            if (inResults) {
                ResultsContent(state, bottomPadding, onOpen, onRetry, onClearGenre)
            } else {
                DiscoverContent(state, bottomPadding, onRecentClick, onRecentRemove, onRecentClear, onGenre, onOpen)
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        placeholder = { Text("Search anime, e.g. Naruto") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = KiroColors.SurfaceHigh,
            unfocusedContainerColor = KiroColors.SurfaceHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = KiroColors.Accent,
            focusedPlaceholderColor = KiroColors.TextSecondary,
            unfocusedPlaceholderColor = KiroColors.TextSecondary,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
    )
}

// ------------------------------------------------------------------ results

@Composable
private fun ResultsContent(
    state: SearchUiState,
    bottomPadding: Dp,
    onOpen: (Anime) -> Unit,
    onRetry: () -> Unit,
    onClearGenre: () -> Unit,
) {
    when {
        state.error != null && state.results.isEmpty() -> ErrorView(state.error, onRetry)

        state.loading && state.results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KiroColors.Accent)
        }

        state.searched && state.results.isEmpty() -> ScreenMessage(
            title = "No results",
            body = "Try a different title or check the spelling.",
        )

        state.results.isNotEmpty() -> Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    val genre = state.activeGenre
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = genre?.name ?: "Results for “${state.query.trim()}”",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        if (genre != null) {
                            IconButton(onClick = onClearGenre) {
                                Icon(Icons.Filled.Close, contentDescription = "Close genre")
                            }
                        }
                    }
                }
                items(state.results, key = { it.id }) { anime ->
                    AnimeCard(anime = anime, onClick = { onOpen(anime) }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (state.loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = KiroColors.Accent,
                    trackColor = Color.Transparent,
                )
            }
        }

        else -> Box(Modifier.fillMaxSize())
    }
}

// ------------------------------------------------------------------ discover (empty query)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverContent(
    state: SearchUiState,
    bottomPadding: Dp,
    onRecentClick: (String) -> Unit,
    onRecentRemove: (String) -> Unit,
    onRecentClear: () -> Unit,
    onGenre: (Genre) -> Unit,
    onOpen: (Anime) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottomPadding + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (state.query.isNotBlank()) {
            item(key = "hint", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Keep typing — at least ${SearchViewModel.MIN_QUERY} characters.",
                    color = KiroColors.TextSecondary,
                )
            }
        }

        if (state.recent.isNotEmpty()) {
            item(key = "recent", span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("Recent searches")
                        TextButton(onClick = onRecentClear) { Text("Clear all", color = KiroColors.Accent) }
                    }
                    state.recent.forEach { q ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onRecentClick(q) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.History, contentDescription = null, tint = KiroColors.TextSecondary)
                            Text(
                                text = q,
                                color = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                maxLines = 1,
                            )
                            IconButton(onClick = { onRecentRemove(q) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove $q",
                                    tint = KiroColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "genres", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                SectionLabel("Browse by genre")
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BrowseGenres.forEach { genre ->
                        Text(
                            text = genre.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(KiroColors.SurfaceHigh)
                                .clickable { onGenre(genre) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        if (state.trending.isNotEmpty()) {
            item(key = "trending-label", span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel("Trending now")
            }
            items(state.trending, key = { it.id }) { anime ->
                AnimeCard(anime = anime, onClick = { onOpen(anime) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
}

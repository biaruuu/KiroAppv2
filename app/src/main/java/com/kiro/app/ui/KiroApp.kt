package com.kiro.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import com.kiro.app.ui.icons.FeatherIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiro.app.data.Anime
import com.kiro.app.ui.detail.AnimeDetailSheet
import com.kiro.app.ui.home.HomeScreen
import com.kiro.app.ui.home.HomeViewModel
import com.kiro.app.ui.search.SearchScreen
import com.kiro.app.ui.search.SearchViewModel
import com.kiro.app.ui.theme.KiroColors

private const val TAB_HOME = 0
private const val TAB_SEARCH = 1

@Composable
fun KiroApp(
    homeVm: HomeViewModel = viewModel(),
    searchVm: SearchViewModel = viewModel(),
) {
    val context = LocalContext.current
    val wide = LocalConfiguration.current.screenWidthDp >= 840
    val home by homeVm.state.collectAsStateWithLifecycle()
    val search by searchVm.state.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var selected by remember { mutableStateOf<Anime?>(null) }
    val homeListState = rememberLazyListState()

    BackHandler(enabled = tab != TAB_HOME) { tab = TAB_HOME }

    val play: (Anime) -> Unit = { anime ->
        homeVm.recordWatch(anime)
        openExternally(context, anime)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (!wide) KiroBottomBar(selected = tab, onSelect = { tab = it }) },
    ) { padding ->
        val bottom = padding.calculateBottomPadding()
        Row(Modifier.fillMaxSize()) {
            if (wide) KiroRail(selected = tab, onSelect = { tab = it })
            Box(Modifier.weight(1f).fillMaxHeight()) {
            when (tab) {
                TAB_HOME -> HomeScreen(
                    state = home,
                    listState = homeListState,
                    bottomPadding = bottom,
                    onRefresh = homeVm::refresh,
                    onRetry = homeVm::retry,
                    onOpen = { selected = it },
                    onPlay = play,
                )

                else -> SearchScreen(
                    state = search,
                    bottomPadding = bottom,
                    onQueryChange = searchVm::onQueryChange,
                    onSubmit = searchVm::onSubmit,
                    onClear = searchVm::onClear,
                    onRecentClick = searchVm::onRecentClick,
                    onRecentRemove = searchVm::onRecentRemove,
                    onRecentClear = searchVm::onRecentClear,
                    onGenre = searchVm::onGenre,
                    onClearGenre = searchVm::onClearGenre,
                    onOpen = {
                        searchVm.onResultOpened()
                        selected = it
                    },
                    onRetry = searchVm::onRetry,
                )
            }
            }
        }
    }

    selected?.let { anime ->
        AnimeDetailSheet(
            anime = anime,
            onDismiss = { selected = null },
            onPlay = play,
        )
    }
}

@Composable
private fun KiroBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = KiroColors.Accent,
        selectedTextColor = KiroColors.Accent,
        unselectedIconColor = Color.White,
        unselectedTextColor = Color.White,
        indicatorColor = Color.Transparent,
    )
    NavigationBar(containerColor = KiroColors.NavBar, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = selected == TAB_HOME,
            onClick = { onSelect(TAB_HOME) },
            icon = {
                Icon(
                    FeatherIcons.Home,
                    contentDescription = null,
                )
            },
            label = { Text("Home") },
            colors = colors,
        )
        NavigationBarItem(
            selected = selected == TAB_SEARCH,
            onClick = { onSelect(TAB_SEARCH) },
            icon = {
                Icon(
                    FeatherIcons.Search,
                    contentDescription = null,
                )
            },
            label = { Text("Search") },
            colors = colors,
        )
    }
}

/** Side navigation for tablets and other wide screens. */
@Composable
private fun KiroRail(selected: Int, onSelect: (Int) -> Unit) {
    val colors = NavigationRailItemDefaults.colors(
        selectedIconColor = KiroColors.Accent,
        selectedTextColor = KiroColors.Accent,
        unselectedIconColor = Color.White,
        unselectedTextColor = Color.White,
        indicatorColor = Color.Transparent,
    )
    NavigationRail(containerColor = KiroColors.NavBar) {
        NavigationRailItem(
            selected = selected == TAB_HOME,
            onClick = { onSelect(TAB_HOME) },
            icon = { Icon(FeatherIcons.Home, contentDescription = null) },
            label = { Text("Home") },
            colors = colors,
        )
        NavigationRailItem(
            selected = selected == TAB_SEARCH,
            onClick = { onSelect(TAB_SEARCH) },
            icon = { Icon(FeatherIcons.Search, contentDescription = null) },
            label = { Text("Search") },
            colors = colors,
        )
    }
}

/** Opens the trailer (YouTube) or, when there is none, the title's AniList page. */
private fun openExternally(context: Context, anime: Anime) {
    val target = anime.trailerUrl.ifBlank { anime.url }
    if (target.isBlank()) {
        Toast.makeText(context, "Nothing to play for this title yet", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
    }
}

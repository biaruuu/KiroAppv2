package com.kiro.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiro.app.data.Anime
import com.kiro.app.data.AnimeRepository
import com.kiro.app.data.WatchHistory
import com.kiro.app.util.ioCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShelfRow(val id: String, val title: String, val items: List<Anime>)

data class HomeUiState(
    val hero: List<Anime> = emptyList(),
    val rows: List<ShelfRow> = emptyList(),
    val continueWatching: List<Anime> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
)

private class RowSpec(val id: String, val title: String, val load: (AnimeRepository) -> List<Anime>)

private val ROWS = listOf(
    RowSpec("season", "Airing This Season") { it.seasonNow(20) },
    RowSpec("sports", "Fierce Competition in a Match") { it.byGenre("Sports", 20) },
    RowSpec("romance", "Can't Hide My Feelings for You!") { it.byGenre("Romance", 20) },
    RowSpec("fantasy", "It's Beyond Imagination") { it.byGenre("Fantasy", 20) },
    RowSpec("popular", "Time to Binge-Watch") { it.topByPopularity(20) },
    RowSpec("magic", "Magical Worlds You Can't Resist") { it.byGenre("Mahou Shoujo", 20) },
    RowSpec("comedy", "Laugh Out Loud") { it.byGenre("Comedy", 20) },
    RowSpec("action", "Pulse-Pounding Action") { it.byGenre("Action", 20) },
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AnimeRepository.getInstance()
    private val watchHistory = WatchHistory(app)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load(force = false)
    }

    fun retry() = load(force = false)

    fun refresh() = load(force = true)

    /** Called when the user presses Play: adds the title to "Continue Watching". */
    fun recordWatch(anime: Anime) {
        viewModelScope.launch {
            ioCall { watchHistory.add(anime) }
            loadContinueWatching()
        }
    }

    private suspend fun loadContinueWatching() {
        ioCall { watchHistory.getAll() }.onSuccess { list ->
            _state.update { it.copy(continueWatching = list) }
        }
    }

    private fun load(force: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            val initial = _state.value.hero.isEmpty() && _state.value.rows.isEmpty()
            _state.update { it.copy(loading = initial, refreshing = !initial, error = null) }
            if (force) repo.clearCache()

            loadContinueWatching()

            var failure: Throwable? = null
            var newHero: List<Anime> = emptyList()
            val newRows = mutableListOf<ShelfRow>()

            ioCall { repo.topAiring(8) }
                .onSuccess { newHero = it }
                .onFailure { failure = it }
            if (initial && newHero.isNotEmpty()) {
                _state.update { it.copy(hero = newHero) }
            }

            for (spec in ROWS) {
                ioCall { spec.load(repo) }
                    .onSuccess { items ->
                        if (items.isNotEmpty()) {
                            newRows += ShelfRow(spec.id, spec.title, items)
                            // First load: show rows as they arrive. Refresh: swap everything at the end.
                            if (initial) _state.update { it.copy(rows = newRows.toList()) }
                        }
                    }
                    .onFailure { failure = it }
            }

            _state.update { s ->
                val hero = if (newHero.isNotEmpty()) newHero else s.hero
                val rows = if (newRows.isNotEmpty()) newRows.toList() else s.rows
                s.copy(
                    hero = hero,
                    rows = rows,
                    loading = false,
                    refreshing = false,
                    error = if (hero.isEmpty() && rows.isEmpty()) {
                        failure?.message ?: "Couldn't load anime. Check your connection."
                    } else null,
                )
            }
        }
    }
}

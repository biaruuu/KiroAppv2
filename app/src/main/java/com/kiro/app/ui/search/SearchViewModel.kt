package com.kiro.app.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiro.app.core.SearchRanker
import com.kiro.app.data.Anime
import com.kiro.app.data.AnimeRepository
import com.kiro.app.data.SearchHistory
import com.kiro.app.ui.Genre
import com.kiro.app.util.ioCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchUiState(
    val query: String = "",
    val activeGenre: Genre? = null,
    val results: List<Anime> = emptyList(),
    val loading: Boolean = false,
    val searched: Boolean = false,
    val error: String? = null,
    val recent: List<String> = emptyList(),
    val trending: List<Anime> = emptyList(),
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        const val MIN_QUERY = 2
        private const val DEBOUNCE_MS = 450L
        private const val RESULT_LIMIT = 25
    }

    private val repo = AnimeRepository.getInstance()
    private val history = SearchHistory(app)

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        loadRecent()
        loadTrending()
    }

    fun onQueryChange(q: String) {
        job?.cancel()
        _state.update { it.copy(query = q, activeGenre = null, error = null) }

        if (q.trim().length < MIN_QUERY) {
            _state.update { it.copy(results = emptyList(), loading = false, searched = false) }
            return
        }
        _state.update { it.copy(loading = true) }
        job = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(q.trim())
        }
    }

    /** Keyboard "Search" action: search right away and remember the query. */
    fun onSubmit() {
        val q = _state.value.query.trim()
        if (q.length < MIN_QUERY) return
        job?.cancel()
        addToHistory(q)
        job = viewModelScope.launch { runSearch(q) }
    }

    fun onClear() = onQueryChange("")

    fun onRecentClick(q: String) {
        onQueryChange(q)
        onSubmit()
    }

    fun onRecentRemove(q: String) {
        viewModelScope.launch {
            ioCall { history.remove(q) }
            loadRecentNow()
        }
    }

    fun onRecentClear() {
        viewModelScope.launch {
            ioCall { history.clear() }
            loadRecentNow()
        }
    }

    fun onGenre(genre: Genre) {
        job?.cancel()
        _state.update {
            it.copy(query = "", activeGenre = genre, results = emptyList(), loading = true, searched = false, error = null)
        }
        job = viewModelScope.launch {
            ioCall { repo.byGenre(genre.name, RESULT_LIMIT) }
                .onSuccess { list ->
                    _state.update {
                        if (it.activeGenre == genre) it.copy(results = list, loading = false, searched = true) else it
                    }
                }
                .onFailure { e ->
                    _state.update {
                        if (it.activeGenre == genre) {
                            it.copy(results = emptyList(), loading = false, searched = true, error = e.message ?: "Couldn't load this genre")
                        } else it
                    }
                }
        }
    }

    fun onClearGenre() {
        job?.cancel()
        _state.update { it.copy(activeGenre = null, results = emptyList(), loading = false, searched = false, error = null) }
    }

    fun onRetry() {
        val genre = _state.value.activeGenre
        if (genre != null) onGenre(genre) else onSubmit()
    }

    /** Remember the current query once the user actually opens a result. */
    fun onResultOpened() {
        val q = _state.value.query.trim()
        if (q.length >= MIN_QUERY) addToHistory(q)
    }

    // ------------------------------------------------------------------ internals

    private suspend fun runSearch(q: String) {
        _state.update { it.copy(loading = true, error = null) }
        ioCall { repo.search(q, RESULT_LIMIT) }
            .onSuccess { list ->
                val ranked = withContext(Dispatchers.Default) { SearchRanker.rank(q, list) }
                _state.update {
                    if (it.query.trim() == q) it.copy(results = ranked, loading = false, searched = true) else it
                }
            }
            .onFailure { e ->
                _state.update {
                    if (it.query.trim() == q) {
                        it.copy(results = emptyList(), loading = false, searched = true, error = e.message ?: "Search failed")
                    } else it
                }
            }
    }

    private fun addToHistory(q: String) {
        viewModelScope.launch {
            ioCall { history.add(q) }
            loadRecentNow()
        }
    }

    private fun loadRecent() {
        viewModelScope.launch { loadRecentNow() }
    }

    private suspend fun loadRecentNow() {
        ioCall { history.getAll() }.onSuccess { list -> _state.update { it.copy(recent = list) } }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            // Same cache key as the Home "Time to Binge-Watch" row, so this is usually free.
            ioCall { repo.topByPopularity(20) }.onSuccess { list ->
                _state.update { it.copy(trending = list.take(12)) }
            }
        }
    }
}

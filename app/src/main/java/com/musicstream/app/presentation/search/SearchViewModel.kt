package com.musicstream.app.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.DownloadProgress
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val greeting: String = "",
    val query: String = "",
    val genres: List<Genre> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val savedStateHandle: SavedStateHandle,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState(
        query = savedStateHandle.get<String>("query") ?: ""
    ))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow(savedStateHandle.get<String>("query") ?: "")

    init {
        loadData()
        setupSearch()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // Set greeting based on time of day
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Good Morning 👋"
                hour < 17 -> "Good Afternoon ☀️"
                else -> "Good Evening 🌙"
            }

            combine(
                musicRepository.getGenres(),
                musicRepository.getTrendingSearches(),
                musicRepository.getSearchHistory(),
                musicRepository.getPlaylists(),
                musicRepository.getDownloadingSongs()
            ) { genres, searches, history, playlists, downloading ->
                _uiState.update {
                    it.copy(
                        greeting = greeting,
                        genres = genres,
                        trendingSearches = searches,
                        searchHistory = history,
                        playlists = playlists,
                        downloadingSongs = downloading
                    )
                }
            }.collect()
        }
    }

    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .onEach { query ->
                    savedStateHandle["query"] = query
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    }
                }
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    _uiState.update { it.copy(isSearching = true) }
                    musicRepository.searchSongs(query)
                        .catch { e ->
                            android.util.Log.e("SearchViewModel", "Search error: ${e.message}")
                            _uiState.update { it.copy(isSearching = false) }
                            emit(emptyList())
                        }
                }
                .collect { results ->
                    _uiState.update {
                        it.copy(searchResults = results, isSearching = false)
                    }
                    if (results.isNotEmpty() && _searchQuery.value.isNotBlank()) {
                        musicRepository.addSearchHistory(_searchQuery.value)
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(query = query) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            musicRepository.clearSearchHistory()
        }
    }

    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            musicRepository.deleteSearchHistory(query)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            musicRepository.deleteDownload(songId)
            // Re-trigger current search to update UI
            val currentQuery = _searchQuery.value
            _searchQuery.value = ""
            _searchQuery.value = currentQuery
        }
    }

    fun downloadSong(song: Song) {
        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.worker.AudioDownloadWorker.enqueue(context, song)
    }
}

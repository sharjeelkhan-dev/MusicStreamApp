package com.musicstream.app.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val genres: List<Genre> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val savedStateHandle: SavedStateHandle
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

    private fun loadData() {
        viewModelScope.launch {
            combine(
                musicRepository.getGenres(),
                musicRepository.getTrendingSearches(),
                musicRepository.getPlaylists()
            ) { genres, searches, playlists ->
                Triple(genres, searches, playlists)
            }.collect { (genres, searches, playlists) ->
                _uiState.update { 
                    it.copy(
                        genres = genres, 
                        trendingSearches = searches,
                        playlists = playlists
                    ) 
                }
            }
        }
    }

    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .onEach { query ->
                    savedStateHandle["query"] = query
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, query = query) }
                    }
                }
                .debounce(300)
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    _uiState.update { it.copy(isSearching = true, query = query) }
                    musicRepository.searchSongs(query)
                }
                .collect { results ->
                    _uiState.update {
                        it.copy(searchResults = results, isSearching = false)
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(query = query) }
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
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
}

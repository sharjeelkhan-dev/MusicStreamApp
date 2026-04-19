package com.musicstream.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "",
    val featuredSong: Song? = null,
    val trendingSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val newSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
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
                musicRepository.getFeaturedSong(),
                musicRepository.getTrendingSongs(),
                musicRepository.searchSongs("new releases").map { songs -> 
                    songs.filter { !it.isExplicit } 
                },
                musicRepository.getRecentlyPlayed(),
                musicRepository.getPlaylists()
            ) { featured, trending, new, recent, playlists ->
                HomeUiState(
                    greeting = greeting,
                    featuredSong = featured,
                    trendingSongs = trending,
                    newSongs = new,
                    recentlyPlayed = recent,
                    playlists = playlists,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
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

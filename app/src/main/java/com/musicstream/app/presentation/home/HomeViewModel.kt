package com.musicstream.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.DownloadProgress
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
    val downloads: List<Song> = emptyList(),
    val newSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // In a real app, this would trigger a network fetch that updates the DB
            // For now, we'll just reload the data which triggers the .onStart { ... } network logic in RepositoryImpl
            loadData()
            _isRefreshing.value = false
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
                musicRepository.getFeaturedSong(),
                musicRepository.getTrendingSongs(),
                musicRepository.searchSongs("new releases").map { songs -> 
                    songs.filter { !it.isExplicit } 
                },
                musicRepository.getRecentlyPlayed(),
                musicRepository.getPlaylists(),
                musicRepository.getDownloads()
            ) { args: Array<*> ->
                HomeUiState(
                    greeting = greeting,
                    featuredSong = args[0] as? Song,
                    trendingSongs = args[1] as List<Song>,
                    newSongs = args[2] as List<Song>,
                    recentlyPlayed = args[3] as List<Song>,
                    playlists = args[4] as List<Playlist>,
                    downloads = args[5] as List<Song>,
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

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
        }
    }

    fun deleteAllPlaylists() {
        viewModelScope.launch {
            musicRepository.deleteAllPlaylists()
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            musicRepository.downloadSong(song).collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        _uiState.update { it.copy(
                            downloadingSongs = it.downloadingSongs + (song.id to progress.percent)
                        ) }
                    }
                    is DownloadProgress.Completed, is DownloadProgress.Failed -> {
                        _uiState.update { it.copy(
                            downloadingSongs = it.downloadingSongs - song.id
                        ) }
                    }
                }
            }
        }
    }
}

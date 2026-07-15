package com.musicstream.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User? = null,
    val greeting: String = "",
    val featuredSongs: List<Song> = emptyList(),
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
    private val musicRepository: MusicRepository,
    private val userRepository: UserRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var refreshCount = 0

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshCount++
            // Add a small delay to show the refresh indicator
            kotlinx.coroutines.delay(800)
            loadData()
            _isRefreshing.value = false
        }
    }

    private var dataJob: Job? = null

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val year = calendar.get(java.util.Calendar.YEAR)

            val greeting = when {
                hour < 12 -> "Good Morning 👋"
                hour < 17 -> "Good Afternoon ☀️"
                else -> "Good Evening 🌙"
            }
            
            val trendingQueries = listOf("Top Charts India", "New Releases 2026", "Trending Songs Today")
            val currentTrendingQuery = trendingQueries[refreshCount % trendingQueries.size]
            val currentTerm = "Latest Hits $year"

            _uiState.update { it.copy(greeting = greeting) }

            // Sequential collection to ensure reliability
            launch {
                userRepository.getCurrentUser().collect { user ->
                    _uiState.update { it.copy(user = user) }
                }
            }

            launch {
                musicRepository.getDownloadingSongs().collect { downloading ->
                    _uiState.update { it.copy(downloadingSongs = downloading) }
                }
            }

            launch {
                musicRepository.getTrendingSongs(currentTrendingQuery)
                    .catch { e ->
                        android.util.Log.e("HomeViewModel", "Trending error: ${e.message}")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    .collect { trending ->
                        _uiState.update {
                            val featured = if (it.recentlyPlayed.isNotEmpty()) it.recentlyPlayed else trending
                            it.copy(
                                trendingSongs = trending,
                                featuredSongs = featured.take(5),
                                isLoading = false 
                            ) 
                        }
                    }
            }

            launch {
                musicRepository.searchSongs(currentTerm)
                    .catch { e -> android.util.Log.e("HomeViewModel", "NewSongs error: ${e.message}") }
                    .collect { songs ->
                        _uiState.update { it.copy(newSongs = songs) }
                    }
            }

            launch {
                musicRepository.getRecentlyPlayed().collect { recent ->
                    _uiState.update {
                        val featured = if (recent.isNotEmpty()) recent else it.trendingSongs
                        it.copy(
                            recentlyPlayed = recent,
                            featuredSongs = featured.take(5)
                        ) 
                    }
                }
            }

            launch {
                musicRepository.getPlaylists().collect { playlists ->
                    _uiState.update { it.copy(playlists = playlists) }
                }
            }

            launch {
                musicRepository.getDownloads().collect { downloads ->
                    _uiState.update { it.copy(downloads = downloads) }
                }
            }
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

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            musicRepository.deleteDownload(songId)
            loadData()
        }
    }

    fun deleteAllPlaylists() {
        viewModelScope.launch {
            musicRepository.deleteAllPlaylists()
        }
    }

    fun downloadSong(song: Song) {
        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.service.DownloadService.start(context, song)
    }
}

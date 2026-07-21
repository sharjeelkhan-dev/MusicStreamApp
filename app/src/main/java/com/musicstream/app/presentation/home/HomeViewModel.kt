package com.musicstream.app.presentation.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.UserRepository
import com.musicstream.app.worker.AudioDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var refreshCount = 0

    // Dynamic Pools for Refresh Rotation
    private val trendingQueries = listOf(
        "Top Songs 2026",
        "Latest Punjabi Hits",
        "Global Top Hits",
        "Bollywood Romantic Songs",
        "Urdu Lofi Songs",
        "Acoustic Hits"
    )

    private val newSongsQueries = listOf(
        "New Releases 2026",
        "Fresh Pop Music",
        "Latest Singles",
        "Top Trending Songs",
        "New Punjabi Songs"
    )

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshCount++
            // Slight delay for smooth UI swipe-refresh animation
            delay(500)
            loadData()
            _isRefreshing.value = false
        }
    }

    private var dataJob: Job? = null

    private fun loadData() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            val greeting = when {
                hour < 12 -> "Good Morning 👋"
                hour < 17 -> "Good Afternoon ☀️"
                else -> "Good Evening 🌙"
            }

            // Pick rotated terms on every pull-to-refresh
            val currentTrendingQuery = trendingQueries[refreshCount % trendingQueries.size]
            val currentNewSongsQuery = newSongsQueries[refreshCount % newSongsQueries.size]

            _uiState.update { it.copy(greeting = greeting, isLoading = true) }

            // 1. User Collector
            launch {
                userRepository.getCurrentUser().collect { user ->
                    _uiState.update { it.copy(user = user) }
                }
            }

            // 2. Downloads Progress Tracker
            launch {
                musicRepository.getDownloadingSongs().collect { downloading ->
                    _uiState.update { it.copy(downloadingSongs = downloading) }
                }
            }

            // 3. Trending Songs (Rotated Query)
            launch {
                musicRepository.getTrendingSongs(currentTrendingQuery)
                    .catch { e ->
                        Log.e("HomeViewModel", "Trending error: ${e.message}")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    .collect { trending ->
                        _uiState.update { state ->
                            val featured = if (state.recentlyPlayed.isNotEmpty()) state.recentlyPlayed else trending
                            state.copy(
                                trendingSongs = trending,
                                featuredSongs = featured.take(5),
                                isLoading = false
                            )
                        }
                    }
            }

            // 4. New Songs (Dynamic Rotated Search Query)
            launch {
                musicRepository.searchSongs(currentNewSongsQuery)
                    .catch { e -> Log.e("HomeViewModel", "NewSongs error: ${e.message}") }
                    .collect { songs ->
                        _uiState.update { it.copy(newSongs = songs) }
                    }
            }

            // 5. Recently Played Collector
            launch {
                musicRepository.getRecentlyPlayed().collect { recent ->
                    _uiState.update { state ->
                        val featured = if (recent.isNotEmpty()) recent else state.trendingSongs
                        state.copy(
                            recentlyPlayed = recent,
                            featuredSongs = featured.take(5)
                        )
                    }
                }
            }

            // 6. Playlists Collector
            launch {
                musicRepository.getPlaylists().collect { playlists ->
                    _uiState.update { it.copy(playlists = playlists) }
                }
            }

            // 7. Downloaded Songs Collector
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
        Toast.makeText(context, "Download started: ${song.title}", Toast.LENGTH_SHORT).show()
        AudioDownloadWorker.enqueue(context, song)
    }
}
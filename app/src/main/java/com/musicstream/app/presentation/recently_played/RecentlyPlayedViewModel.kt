package com.musicstream.app.presentation.recently_played

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentlyPlayedUiState(
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentlyPlayedUiState())
    val uiState: StateFlow<RecentlyPlayedUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                musicRepository.getRecentlyPlayed(),
                musicRepository.getPlaylists(),
                musicRepository.getDownloadingSongs()
            ) { recent, playlists, downloading ->
                _uiState.update { it.copy(
                    songs = recent,
                    playlists = playlists,
                    downloadingSongs = downloading,
                    isLoading = false
                ) }
            }.collect()
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // Add a small delay to show the refresh indicator
            kotlinx.coroutines.delay(1500)
            loadData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            musicRepository.deleteDownload(songId)
            loadData()
        }
    }

    fun downloadSong(song: Song) {
        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.service.DownloadService.start(context, song)
    }
}

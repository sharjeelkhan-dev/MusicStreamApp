package com.musicstream.app.presentation.recently_played

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

data class RecentlyPlayedUiState(
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentlyPlayedUiState())
    val uiState: StateFlow<RecentlyPlayedUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            musicRepository.getRecentlyPlayed().collect { songs ->
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
        viewModelScope.launch {
            musicRepository.getPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // Add a small delay to show the refresh indicator
            kotlinx.coroutines.delay(1500)
            loadData()
            _uiState.update { it.copy(isRefreshing = false) }
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

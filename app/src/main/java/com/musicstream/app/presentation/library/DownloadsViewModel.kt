package com.musicstream.app.presentation.library

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

data class DownloadsUiState(
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val downloadingSongsList: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadData() {
        // Collect Downloads
        musicRepository.getDownloads()
            .onEach { downloads ->
                _uiState.update { it.copy(songs = downloads, isLoading = false) }
            }
            .launchIn(viewModelScope)

        // Observe downloading songs
        musicRepository.getDownloadingSongs()
            .onEach { downloading ->
                _uiState.update { it.copy(downloadingSongs = downloading) }
            }
            .launchIn(viewModelScope)

        // Also get playlists for "Add to playlist" functionality
        musicRepository.getPlaylists()
            .onEach { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
            .launchIn(viewModelScope)
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
        }
    }

    fun downloadSong(song: Song) {
        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.service.DownloadService.start(context, song)
    }
}

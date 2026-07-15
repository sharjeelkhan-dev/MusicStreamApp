package com.musicstream.app.presentation.library

import android.content.Context
import androidx.lifecycle.SavedStateHandle
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

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _uiState = MutableStateFlow(PlaylistUiState(isLoading = true))
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylistData()
    }

    private fun loadPlaylistData() {
        viewModelScope.launch {
            combine(
                musicRepository.getPlaylists(),
                musicRepository.getSongsForPlaylist(playlistId),
                musicRepository.getDownloadingSongs()
            ) { playlists, songs, downloading ->
                val playlist = playlists.find { it.id == playlistId }
                PlaylistUiState(
                    playlist = playlist,
                    songs = songs,
                    playlists = playlists,
                    downloadingSongs = downloading,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            musicRepository.deleteDownload(songId)
        }
    }

    fun removeSongFromPlaylist(songId: String) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun downloadSong(song: Song) {
        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.service.DownloadService.start(context, song)
    }
}

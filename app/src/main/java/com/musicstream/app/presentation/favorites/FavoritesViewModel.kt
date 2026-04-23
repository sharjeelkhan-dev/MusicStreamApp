package com.musicstream.app.presentation.favorites
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

data class FavoritesUiState(
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val downloadingSongs: Map<String, Int> = emptyMap()
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
        loadPlaylists()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getFavorites().collect { songs ->
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            repository.getPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            repository.downloadSong(song).collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        _uiState.update { it.copy(
                            downloadingSongs = it.downloadingSongs + (song.id to progress.percent)
                        ) }
                    }
                    is DownloadProgress.Completed -> {
                        _uiState.update { it.copy(
                            downloadingSongs = it.downloadingSongs - song.id
                        ) }
                        // Refresh favorites to update localPath in the UI
                        loadFavorites()
                    }
                    is DownloadProgress.Failed -> {
                        _uiState.update { it.copy(
                            downloadingSongs = it.downloadingSongs - song.id
                        ) }
                    }
                }
            }
        }
    }
}

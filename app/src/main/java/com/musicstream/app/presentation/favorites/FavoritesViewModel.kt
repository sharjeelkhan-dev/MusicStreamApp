package com.musicstream.app.presentation.favorites
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

data class FavoritesUiState(
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val downloadingSongs: Map<String, Int> = emptyMap()
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MusicRepository,
    @ApplicationContext private val context: Context
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
            combine(
                repository.getPlaylists(),
                repository.getDownloadingSongs()
            ) { playlists, downloading ->
                _uiState.update { it.copy(
                    playlists = playlists,
                    downloadingSongs = downloading
                ) }
            }.collect()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // Add a small delay to show the refresh indicator
            kotlinx.coroutines.delay(1500)
            loadFavorites()
            loadPlaylists()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            repository.deleteDownload(songId)
            // Refresh to update localPath in UI
            loadFavorites()
        }
    }

    fun downloadSong(song: Song) {
        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.worker.AudioDownloadWorker.enqueue(context, song)
    }
}

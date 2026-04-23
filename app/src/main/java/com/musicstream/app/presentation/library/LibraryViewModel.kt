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

enum class LibraryTab { Downloads, Playlists, Favorites, Songs }

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.Playlists,
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val downloads: List<Song> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val playlistSongs: List<Song> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val downloadingSongsList: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            musicRepository.getPlaylists().collect { playlists ->
                _uiState.update { currentState ->
                    val updatedSelectedPlaylist = currentState.selectedPlaylist?.let { selected ->
                        playlists.find { it.id == selected.id }
                    }
                    currentState.copy(
                        playlists = playlists,
                        selectedPlaylist = updatedSelectedPlaylist,
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            musicRepository.getTrendingSongs().collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
        viewModelScope.launch {
            musicRepository.getFavorites().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
        viewModelScope.launch {
            musicRepository.getDownloads().collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedPlaylist = null) }
    }

    fun selectPlaylist(playlist: Playlist?) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
        if (playlist != null) {
            viewModelScope.launch {
                musicRepository.getSongsForPlaylist(playlist.id).collect { songs ->
                    _uiState.update { it.copy(playlistSongs = songs) }
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
        }
    }

    fun togglePlaylist(playlistId: String) {
        // This could open a detail screen or do something else
        // For now, let's just log it or update some state if needed
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

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
            loadData()
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
            // Refresh current playlist view
            _uiState.value.selectedPlaylist?.let { current ->
                if (current.id == playlistId) {
                    val updatedSongs = _uiState.value.playlistSongs.filter { it.id != songId }
                    _uiState.update { it.copy(playlistSongs = updatedSongs) }
                }
            }
            loadData()
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            musicRepository.deleteDownload(songId)
            loadData()
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                downloadingSongsList = (it.downloadingSongsList + song).distinctBy { s -> s.id }
            ) }
            musicRepository.downloadSong(song).collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        _uiState.update { it.copy(
                            downloadingSongs = it.downloadingSongs + (song.id to progress.percent)
                        ) }
                    }
                    is DownloadProgress.Completed, is DownloadProgress.Failed -> {
                        _uiState.update { it.copy(
                            downloadingSongs = it.downloadingSongs - song.id,
                            downloadingSongsList = it.downloadingSongsList.filter { s -> s.id != song.id }
                        ) }
                        // Refresh states after download
                        loadData()
                    }
                }
            }
        }
    }
}

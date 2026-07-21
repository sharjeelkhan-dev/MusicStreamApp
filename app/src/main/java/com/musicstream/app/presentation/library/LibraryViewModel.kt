package com.musicstream.app.presentation.library

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

enum class LibraryTab { Downloads, Playlists, Songs }

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
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val initialTab = savedStateHandle.get<String>("tab")?.let { tabName ->
        LibraryTab.entries.find { it.name.lowercase() == tabName.lowercase() }
    } ?: LibraryTab.Playlists

    private val _uiState = MutableStateFlow(LibraryUiState(selectedTab = initialTab))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Collect Playlists with automatic updates
        musicRepository.getPlaylists()
            .onEach { playlists ->
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
            .launchIn(viewModelScope)

        // Collect Trending Songs
        musicRepository.getTrendingSongs()
            .onEach { songs -> _uiState.update { it.copy(songs = songs) } }
            .launchIn(viewModelScope)

        // Collect Downloads (This also includes converted files with localPath)
        musicRepository.getDownloads()
            .onEach { downloads ->
                _uiState.update { it.copy(
                    downloads = downloads,
                    songs = (it.songs + downloads).distinctBy { s -> s.id }.sortedByDescending { s -> s.id }
                ) }
            }
            .launchIn(viewModelScope)

        // Collect Favorites
        musicRepository.getFavorites()
            .onEach { favorites -> _uiState.update { it.copy(favorites = favorites) } }
            .launchIn(viewModelScope)

        // Observe downloading songs map
        musicRepository.getDownloadingSongs()
            .onEach { downloading ->
                _uiState.update { it.copy(downloadingSongs = downloading) }
            }
            .launchIn(viewModelScope)
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedPlaylist = null) }
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

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
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
        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.worker.AudioDownloadWorker.enqueue(context, song)
    }
}

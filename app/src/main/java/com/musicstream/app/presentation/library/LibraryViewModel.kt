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
import kotlin.time.Duration.Companion.milliseconds

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
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val initialTab = savedStateHandle.get<String>("tab")?.let { tabName ->
        LibraryTab.entries.find { it.name.equals(tabName, ignoreCase = true) }
    } ?: LibraryTab.Playlists

    private val _uiState = MutableStateFlow(LibraryUiState(selectedTab = initialTab))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Collect Playlists
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

        // Collect Completed Downloads
        musicRepository.getDownloads()
            .onEach { downloads ->
                _uiState.update { currentState ->
                    val allSongs = (currentState.songs + downloads)
                        .distinctBy { s -> s.id }
                        .sortedByDescending { s -> s.id }

                    // Filtering active downloads if already present in completed downloads
                    val activeDownloadingList = currentState.downloadingSongsList.filterNot { downloading ->
                        downloads.any { downloaded -> downloaded.id == downloading.id }
                    }

                    currentState.copy(
                        downloads = downloads,
                        songs = allSongs,
                        downloadingSongsList = activeDownloadingList
                    )
                }
            }
            .launchIn(viewModelScope)

        // Collect Favorites
        musicRepository.getFavorites()
            .onEach { favorites -> _uiState.update { it.copy(favorites = favorites) } }
            .launchIn(viewModelScope)

        // Observe Downloading Songs Progress Map & Maintain Active Downloading List
        musicRepository.getDownloadingSongs()
            .onEach { downloadingMap ->
                _uiState.update { currentState ->
                    // Get songs matching active progress IDs
                    val activeList = currentState.songs.filter { song ->
                        downloadingMap.containsKey(song.id) &&
                                currentState.downloads.none { downloaded -> downloaded.id == song.id }
                    }

                    currentState.copy(
                        downloadingSongs = downloadingMap,
                        downloadingSongsList = activeList
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab, selectedPlaylist = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            kotlinx.coroutines.delay(1500.milliseconds)
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
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
            _uiState.value.selectedPlaylist?.let { current ->
                if (current.id == playlistId) {
                    val updatedSongs = _uiState.value.playlistSongs.filter { it.id != songId }
                    _uiState.update { it.copy(playlistSongs = updatedSongs) }
                }
            }
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            musicRepository.deleteDownload(songId)
        }
    }

    fun downloadSong(song: Song) {
        // Add to active downloading list immediately on user action
        _uiState.update { currentState ->
            if (currentState.downloadingSongsList.none { it.id == song.id }) {
                currentState.copy(downloadingSongsList = currentState.downloadingSongsList + song)
            } else currentState
        }

        android.widget.Toast.makeText(context, "Download started: ${song.title}", android.widget.Toast.LENGTH_SHORT).show()
        com.musicstream.app.worker.AudioDownloadWorker.enqueue(context, song)
    }
}
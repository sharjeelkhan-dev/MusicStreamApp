package com.musicstream.app.presentation.trending

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

data class TrendingUiState(
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloadingSongs: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                musicRepository.getTrendingSongs(),
                musicRepository.getPlaylists()
            ) { trendingSongs, playlists ->
                TrendingUiState(
                    songs = trendingSongs,
                    playlists = playlists,
                    isLoading = false,
                    isRefreshing = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // Add a small delay to show the refresh indicator
            kotlinx.coroutines.delay(1500)
            loadData()
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

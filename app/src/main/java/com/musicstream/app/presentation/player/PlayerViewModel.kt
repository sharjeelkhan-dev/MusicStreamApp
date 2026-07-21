package com.musicstream.app.presentation.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.service.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleOn: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val playlists: List<Playlist> = emptyList(),
    val isSleepTimerActive: Boolean = false,
    val sleepTimerTimeLeft: Long = 0L
)

enum class RepeatMode { OFF, ONE, ALL }

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = playerManager.uiState

    init {
        loadPlaylists()
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playerManager.playSong(songs[startIndex], songs)
    }

    fun playSong(song: Song) {
        playerManager.playSong(song)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    // Yahan seekTo ko Long (milliseconds) ke sath update kar diya gaya hai
    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun nextSong() {
        playerManager.skipToNext()
    }

    fun previousSong() {
        playerManager.skipToPrevious()
    }

    fun pauseSong() {
        playerManager.togglePlayPause()
    }

    fun stopMusic() {
        playerManager.stop()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song)
            playerManager.updateFavorite(song.id, !song.isFavorite)
        }
    }

    private fun loadPlaylists() {
        musicRepository.getPlaylists()
            .onEach { playlists ->
                playerManager.setPlaylists(playlists)
            }
            .launchIn(viewModelScope)
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            musicRepository.addToRecentlyPlayed(song)
            musicRepository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }
}
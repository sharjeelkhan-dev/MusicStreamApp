package com.musicstream.app.presentation.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.AiRepository
import com.musicstream.app.service.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val recentlyPlayed: List<Song> = emptyList(),
    val isSleepTimerActive: Boolean = false,
    val sleepTimerTimeLeft: Long = 0L,
    val aiSummary: String? = null,
    val aiLyricsExplanation: String? = null,
    val isAiLoading: Boolean = false
)

enum class RepeatMode { OFF, ONE, ALL }

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val aiRepository: AiRepository,
    private val playerManager: MusicPlayerManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _aiState = MutableStateFlow(AiState())

    val uiState: StateFlow<PlayerUiState> = combine(
        playerManager.uiState,
        musicRepository.getPlaylists(),
        musicRepository.getRecentlyPlayed(),
        _aiState
    ) { playerState, playlists, recentlyPlayed, aiState ->
        playerState.copy(
            playlists = playlists,
            recentlyPlayed = recentlyPlayed,
            aiSummary = aiState.summary,
            aiLyricsExplanation = aiState.lyricsExplanation,
            isAiLoading = aiState.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState()
    )

    private data class AiState(
        val summary: String? = null,
        val lyricsExplanation: String? = null,
        val isLoading: Boolean = false
    )

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
        if (uiState.value.isPlaying) {
            playerManager.togglePlayPause()
        }
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
            val updatedFavoriteState = !song.isFavorite
            playerManager.updateFavorite(song.id, updatedFavoriteState)
        }
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

    fun downloadSong(song: Song) {
        // We can reuse the same logic as HomeViewModel or just call a worker
        com.musicstream.app.worker.AudioDownloadWorker.enqueue(context, song)
    }

    fun getAiSummary() {
        val song = uiState.value.currentSong ?: return
        _aiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val summary = aiRepository.getSongSummary(song)
                _aiState.update { it.copy(summary = summary, isLoading = false) }
            } catch (e: Exception) {
                _aiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun getAiLyricsExplanation() {
        val song = uiState.value.currentSong ?: return
        _aiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val explanation = aiRepository.explainLyrics(song)
                _aiState.update { it.copy(lyricsExplanation = explanation, isLoading = false) }
            } catch (e: Exception) {
                _aiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearAiState() {
        _aiState.update { AiState() }
    }
}

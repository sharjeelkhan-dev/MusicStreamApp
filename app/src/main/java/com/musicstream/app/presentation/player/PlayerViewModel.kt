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

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.musicstream.app.domain.model.LyricLine
import com.musicstream.app.domain.repository.LyricsRepository

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
    val isAiLoading: Boolean = false,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val isLyricsLoading: Boolean = false
)

enum class RepeatMode { OFF, ONE, ALL }

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val aiRepository: AiRepository,
    private val lyricsRepository: LyricsRepository,
    private val playerManager: MusicPlayerManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _aiState = MutableStateFlow(AiState())
    private val _syncedLyricsState = MutableStateFlow<List<LyricLine>>(emptyList())
    private val _isLyricsLoadingState = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            playerManager.uiState
                .map { it.currentSong?.id }
                .distinctUntilChanged()
                .collect { songId ->
                    val currentSong = playerManager.uiState.value.currentSong
                    if (currentSong != null) {
                        _isLyricsLoadingState.value = true
                        try {
                            val lyrics = lyricsRepository.getSyncedLyrics(currentSong)
                            _syncedLyricsState.value = lyrics
                        } catch (e: Exception) {
                            _syncedLyricsState.value = emptyList()
                        } finally {
                            _isLyricsLoadingState.value = false
                        }
                    } else {
                        _syncedLyricsState.value = emptyList()
                    }
                }
        }
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        playerManager.uiState,
        musicRepository.getPlaylists(),
        musicRepository.getRecentlyPlayed(),
        _aiState,
        _syncedLyricsState,
        _isLyricsLoadingState
    ) { args: Array<Any?> ->
        val playerState = args[0] as PlayerUiState
        @Suppress("UNCHECKED_CAST")
        val playlists = args[1] as List<Playlist>
        @Suppress("UNCHECKED_CAST")
        val recentlyPlayed = args[2] as List<Song>
        val aiState = args[3] as AiState
        @Suppress("UNCHECKED_CAST")
        val syncedLyrics = args[4] as List<LyricLine>
        val isLyricsLoading = args[5] as Boolean

        playerState.copy(
            playlists = playlists,
            recentlyPlayed = recentlyPlayed,
            aiSummary = aiState.summary,
            aiLyricsExplanation = aiState.lyricsExplanation,
            isAiLoading = aiState.isLoading,
            syncedLyrics = syncedLyrics,
            isLyricsLoading = isLyricsLoading
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

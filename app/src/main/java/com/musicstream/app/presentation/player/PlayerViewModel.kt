package com.musicstream.app.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val currentIndex: Int = 0
)

enum class RepeatMode { OFF, ONE, ALL }

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val exoPlayer: ExoPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    progressJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    nextSong()
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration.coerceAtLeast(1L)
                val stateDur = _uiState.value.duration.coerceAtLeast(1L)
                
                // Audio previews might not report exact duration until loaded
                val effectiveDuration = if (duration > 0) duration else stateDur

                _uiState.update {
                    it.copy(
                        currentPosition = currentPos,
                        progress = (currentPos.toFloat() / effectiveDuration).coerceIn(0f, 1f)
                    )
                }
                delay(100)
            }
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            val currentQueue = _uiState.value.queue
            val existingIndex = currentQueue.indexOfFirst { it.id == song.id }
            
            _uiState.update {
                it.copy(
                    currentSong = song,
                    queue = if (existingIndex >= 0) currentQueue else listOf(song) + currentQueue,
                    currentIndex = if (existingIndex >= 0) existingIndex else 0,
                    currentPosition = 0L,
                    duration = song.duration
                )
            }
            
            musicRepository.addToRecentlyPlayed(song)
            
            if (song.streamUrl.isNotEmpty()) {
                val mediaItem = MediaItem.Builder()
                    .setUri(song.streamUrl)
                    .setMediaId(song.id)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            }
        }
    }

    fun playSongById(songId: String) {
        viewModelScope.launch {
            val queue = _uiState.value.queue
            val index = queue.indexOfFirst { it.id == songId }
            
            val songToPlay = if (index >= 0) {
                queue[index]
            } else {
                musicRepository.getSongById(songId).firstOrNull()
            }

            songToPlay?.let { playSong(it) }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(progress: Float) {
        val duration = exoPlayer.duration.coerceAtLeast(1L)
        val newPos = (progress * duration).toLong()
        exoPlayer.seekTo(newPos)
        _uiState.update {
            it.copy(
                progress = progress,
                currentPosition = newPos
            )
        }
    }

    fun nextSong() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        val nextIndex = (state.currentIndex + 1) % state.queue.size
        val nextSong = state.queue[nextIndex]
        playSong(nextSong)
    }

    fun previousSong() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        val prevIndex = if (state.currentIndex > 0) state.currentIndex - 1 else state.queue.size - 1
        val prevSong = state.queue[prevIndex]
        playSong(prevSong)
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffleOn = !it.isShuffleOn) }
        // Shuffle implementation omitted for brevity in demo
    }

    fun toggleRepeat() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                RepeatMode.ALL
            }
            RepeatMode.ALL -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                RepeatMode.ONE
            }
            RepeatMode.ONE -> {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                RepeatMode.OFF
            }
        }
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
            _uiState.update { state ->
                if (state.currentSong?.id == songId) {
                    state.copy(currentSong = state.currentSong.copy(isFavorite = !state.currentSong.isFavorite))
                } else {
                    state
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        exoPlayer.release()
    }
}

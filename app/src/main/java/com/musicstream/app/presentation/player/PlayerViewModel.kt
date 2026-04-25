package com.musicstream.app.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
    private val settingsRepository: com.musicstream.app.domain.repository.SettingsRepository,
    private val exoPlayer: ExoPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        loadPlaylists()
        observeEqualizerSettings()
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

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val currentSong = _uiState.value.currentSong
                if (currentSong != null) {
                    val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
                    val wasPlayingLocal = currentUri?.scheme == "file"
                    
                    // If local file failed to play, try falling back to online stream
                    if (wasPlayingLocal && currentSong.streamUrl.isNotEmpty()) {
                        playWithUri(currentSong, currentSong.streamUrl)
                    } else {
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val currentPos = exoPlayer.currentPosition
                val exoDuration = exoPlayer.duration
                
                // C.TIME_UNSET check to prevent early "completion"
                val effectiveDuration = if (exoDuration > 0) {
                    exoDuration 
                } else if (_uiState.value.duration > 0) {
                    _uiState.value.duration
                } else {
                    1L // Fallback
                }

                _uiState.update {
                    it.copy(
                        currentPosition = currentPos,
                        progress = (currentPos.toFloat() / effectiveDuration).coerceIn(0f, 1f),
                        // Update total duration if ExoPlayer finally reports it
                        duration = if (exoDuration > 0) exoDuration else it.duration
                    )
                }
                delay(200) // Slightly more stable delay
            }
        }
    }

    fun playSong(song: Song) {
        // If the same song is tapped, restart it from the beginning
        if (_uiState.value.currentSong?.id == song.id) {
            exoPlayer.seekTo(0)
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
            _uiState.update { it.copy(isPlaying = true, currentPosition = 0) }
            return
        }

        playJob?.cancel()
        playJob = viewModelScope.launch {
            // Ensure we have the latest metadata (especially localPath) from the database
            val latestSong = musicRepository.getSongById(song.id).firstOrNull()?.copy(
                isFavorite = song.isFavorite // Preserve favorite status from UI
            ) ?: song

            // Check if local file exists before trying to play it
            val localFileExists = !latestSong.localPath.isNullOrEmpty() && java.io.File(latestSong.localPath!!).exists()
            
            // Debug log to check file path and existence
            println("Offline Playback: songId=${latestSong.id}, path=${latestSong.localPath}, exists=$localFileExists")

            val mediaUri = if (localFileExists) {
                // Use Uri.fromFile to ensure correct file scheme and character escaping
                android.net.Uri.fromFile(java.io.File(latestSong.localPath!!)).toString()
            } else {
                latestSong.streamUrl
            }

            val currentQueue = _uiState.value.queue
            val existingIndex = currentQueue.indexOfFirst { it.id == latestSong.id }
            
            _uiState.update { state ->
                val newQueue = if (existingIndex >= 0) {
                    currentQueue
                } else {
                    // Append instead of Prepend to avoid "up and down" jumping of items
                    currentQueue + latestSong
                }

                val newIndex = if (existingIndex >= 0) {
                    existingIndex
                } else {
                    newQueue.size - 1
                }

                // Keep originalQueue in sync for when shuffle is turned off
                if (!state.isShuffleOn) {
                    originalQueue = newQueue
                } else if (existingIndex == -1) {
                    originalQueue = originalQueue + latestSong
                }

                state.copy(
                    currentSong = latestSong,
                    queue = newQueue,
                    currentIndex = newIndex,
                    currentPosition = 0L,
                    duration = latestSong.duration
                )
            }
            
            musicRepository.addToRecentlyPlayed(latestSong)
            
            if (mediaUri.isNotEmpty()) {
                playWithUri(latestSong, mediaUri)
            }
        }
    }

    private fun playWithUri(song: Song, uri: String) {
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(android.net.Uri.parse(song.coverUrl))
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .setMimeType(if (uri.startsWith("file")) "audio/mpeg" else null)
            .setMediaMetadata(metadata)
            .build()
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun playSongById(songId: String) {
        // If the same song is tapped, restart it from the beginning
        if (_uiState.value.currentSong?.id == songId) {
            exoPlayer.seekTo(0)
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
            _uiState.update { it.copy(isPlaying = true, currentPosition = 0) }
            return
        }
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
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
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

    private var originalQueue: List<Song> = emptyList()

    fun toggleShuffle() {
        _uiState.update { state ->
            val newShuffleState = !state.isShuffleOn
            if (newShuffleState) {
                // Save original order before shuffling
                originalQueue = state.queue
                
                val currentSong = state.currentSong
                val otherSongs = state.queue.filter { it.id != currentSong?.id }.shuffled()
                val shuffledQueue = if (currentSong != null) {
                    listOf(currentSong) + otherSongs
                } else {
                    otherSongs
                }
                
                state.copy(
                    isShuffleOn = true,
                    queue = shuffledQueue,
                    currentIndex = 0
                )
            } else {
                // Restore original order
                val currentSong = state.currentSong
                val restoreIndex = originalQueue.indexOfFirst { it.id == currentSong?.id }
                state.copy(
                    isShuffleOn = false,
                    queue = originalQueue,
                    currentIndex = if (restoreIndex >= 0) restoreIndex else 0
                )
            }
        }
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

    private fun loadPlaylists() {
        musicRepository.getPlaylists()
            .onEach { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
            .launchIn(viewModelScope)
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _uiState.update { it.copy(isSleepTimerActive = false, sleepTimerTimeLeft = 0) }
            return
        }

        val totalMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())
        _uiState.update { it.copy(isSleepTimerActive = true, sleepTimerTimeLeft = totalMillis) }

        sleepTimerJob = viewModelScope.launch {
            var timeLeft = totalMillis
            while (timeLeft > 0) {
                delay(1000)
                timeLeft -= 1000
                _uiState.update { it.copy(sleepTimerTimeLeft = timeLeft) }
            }
            _uiState.update { it.copy(isSleepTimerActive = false, sleepTimerTimeLeft = 0) }
            exoPlayer.pause()
        }
    }

    private fun observeEqualizerSettings() {
        settingsRepository.getEqualizerPreset()
            .onEach { preset ->
                applyEqualizerPreset(preset)
            }
            .launchIn(viewModelScope)
    }

    private fun applyEqualizerPreset(preset: String) {
        val equalizer = android.media.audiofx.Equalizer(0, exoPlayer.audioSessionId)
        if (!equalizer.enabled) equalizer.enabled = true
        
        // Very simplified mapping of presets to bands
        // In a real app, you would define these values more carefully
        val numBands = equalizer.numberOfBands
        val (minLevel, maxLevel) = equalizer.bandLevelRange
        val center = (minLevel + maxLevel) / 2
        
        when (preset) {
            "Flat" -> {
                for (i in 0 until numBands) equalizer.setBandLevel(i.toShort(), center.toShort())
            }
            "Bass Boost" -> {
                if (numBands > 0) equalizer.setBandLevel(0, maxLevel.toShort())
                if (numBands > 1) equalizer.setBandLevel(1, (center + (maxLevel - center) / 2).toShort())
            }
            "Rock" -> {
                if (numBands >= 5) {
                    equalizer.setBandLevel(0, (center + 300).toShort())
                    equalizer.setBandLevel(1, (center + 100).toShort())
                    equalizer.setBandLevel(2, (center - 200).toShort())
                    equalizer.setBandLevel(3, (center + 200).toShort())
                    equalizer.setBandLevel(4, (center + 400).toShort())
                }
            }
            "Pop" -> {
                if (numBands >= 5) {
                    equalizer.setBandLevel(0, (center - 100).toShort())
                    equalizer.setBandLevel(1, (center + 200).toShort())
                    equalizer.setBandLevel(2, (center + 400).toShort())
                    equalizer.setBandLevel(3, (center + 100).toShort())
                    equalizer.setBandLevel(4, (center - 100).toShort())
                }
            }
            // Add more mappings as needed
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        playJob?.cancel()
        sleepTimerJob?.cancel()
        // Do not release exoPlayer here, as it's a singleton used by the Service
    }
}

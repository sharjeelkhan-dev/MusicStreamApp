package com.musicstream.app.presentation.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.service.MusicPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.core.net.toUri

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
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var sleepTimerJob: Job? = null
    
    private var mediaController: MediaController? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                setupPlayerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
        
        loadPlaylists()
    }

    private fun setupPlayerListener() {
        // Sync initial state if already playing
        mediaController?.let { controller ->
            val currentIndex = controller.currentMediaItemIndex
            val queue = _uiState.value.queue
            if (currentIndex >= 0 && currentIndex < queue.size) {
                val currentSong = queue[currentIndex]
                _uiState.update { it.copy(
                    currentSong = currentSong,
                    currentIndex = currentIndex,
                    isPlaying = controller.isPlaying,
                    duration = if (controller.duration > 0) controller.duration else currentSong.duration,
                    currentPosition = controller.currentPosition
                ) }
            }
        }

        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newIndex = mediaController?.currentMediaItemIndex ?: return
                val queue = _uiState.value.queue
                if (newIndex in queue.indices) {
                    val nextSong = queue[newIndex]
                    _uiState.update { it.copy(
                        currentSong = nextSong,
                        currentIndex = newIndex,
                        duration = if (nextSong.duration > 0) nextSong.duration else it.duration,
                        currentPosition = 0L,
                        progress = 0f
                    ) }
                    viewModelScope.launch {
                        musicRepository.addToRecentlyPlayed(nextSong)
                    }
                }
            }

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
                    val currentUri = mediaController?.currentMediaItem?.localConfiguration?.uri
                    val wasPlayingLocal = currentUri?.scheme == "file"
                    
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
            while (mediaController?.isPlaying == true) {
                val currentPos = mediaController?.currentPosition ?: 0L
                val exoDuration = mediaController?.duration ?: 0L
                
                val effectiveDuration = if (exoDuration > 0) {
                    exoDuration 
                } else if (_uiState.value.duration > 0) {
                    _uiState.value.duration
                } else {
                    1L
                }

                _uiState.update {
                    it.copy(
                        currentPosition = currentPos,
                        progress = (currentPos.toFloat() / effectiveDuration).coerceIn(0f, 1f),
                        duration = if (exoDuration > 0) exoDuration else it.duration
                    )
                }
                delay(200)
            }
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _uiState.update { it.copy(
                queue = songs,
                currentIndex = startIndex,
                currentSong = songs[startIndex],
                currentPosition = 0L,
                duration = songs[startIndex].duration,
                isPlaying = true
            ) }
            
            if (!uiState.value.isShuffleOn) {
                originalQueue = songs
            }

            val mediaItems = songs.map { song ->
                val mMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setDisplayTitle(song.title)
                    .setArtworkUri(song.coverUrl.toUri())
                    .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
                
                val uri = if (!song.localPath.isNullOrEmpty() && java.io.File(song.localPath).exists()) {
                    android.net.Uri.fromFile(java.io.File(song.localPath)).toString()
                } else {
                    song.streamUrl
                }

                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(song.id)
                    .setMimeType(if (uri.startsWith("file")) "audio/mpeg" else null)
                    .setMediaMetadata(mMetadata)
                    .build()
            }
            
            mediaController?.let { controller ->
                controller.setMediaItems(mediaItems)
                controller.seekTo(startIndex, 0L)
                controller.prepare()
                controller.play()
            }
            
            musicRepository.addToRecentlyPlayed(songs[startIndex])
        }
    }

    fun playSong(song: Song) {
        if (_uiState.value.currentSong?.id == song.id) {
            mediaController?.seekTo(0)
            if (mediaController?.playbackState == Player.STATE_ENDED) {
                mediaController?.prepare()
            }
            mediaController?.play()
            _uiState.update { it.copy(isPlaying = true, currentPosition = 0) }
            return
        }

        playJob?.cancel()
        playJob = viewModelScope.launch {
            val latestSong = musicRepository.getSongById(song.id).firstOrNull()?.copy(
                isFavorite = song.isFavorite
            ) ?: song

            val localFileExists = !latestSong.localPath.isNullOrEmpty() && java.io.File(latestSong.localPath).exists()
            
            val mediaUri = if (localFileExists) {
                android.net.Uri.fromFile(java.io.File(latestSong.localPath)).toString()
            } else {
                latestSong.streamUrl
            }

            val currentQueue = _uiState.value.queue
            val existingIndex = currentQueue.indexOfFirst { it.id == latestSong.id }
            
            var indexToPlay = 0
            _uiState.update { state ->
                val newQueue = if (existingIndex >= 0) {
                    currentQueue
                } else {
                    currentQueue + latestSong
                }

                val newIndex = if (existingIndex >= 0) {
                    existingIndex
                } else {
                    newQueue.size - 1
                }
                indexToPlay = newIndex

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
                val mediaItems = _uiState.value.queue.map { qSong ->
                    val mMetadata = androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(qSong.title)
                        .setArtist(qSong.artist)
                        .setDisplayTitle(qSong.title)
                        .setArtworkUri(qSong.coverUrl.toUri())
                        .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                    
                    val qUri = if (!qSong.localPath.isNullOrEmpty() && java.io.File(qSong.localPath).exists()) {
                        android.net.Uri.fromFile(java.io.File(qSong.localPath)).toString()
                    } else {
                        qSong.streamUrl
                    }

                    MediaItem.Builder()
                        .setUri(qUri)
                        .setMediaId(qSong.id)
                        .setMimeType(if (qUri.startsWith("file")) "audio/mpeg" else null)
                        .setMediaMetadata(mMetadata)
                        .build()
                }
                
                mediaController?.let { controller ->
                    controller.setMediaItems(mediaItems)
                    controller.seekTo(indexToPlay, 0L)
                    controller.prepare()
                    controller.play()
                }
            }
        }
    }

    private fun playWithUri(song: Song, uri: String) {
        // This is now redundant if we use the queue logic above, 
        // but keeping it for simple cases if needed
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(song.coverUrl.toUri())
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .setMimeType(if (uri.startsWith("file")) "audio/mpeg" else null)
            .setMediaMetadata(metadata)
            .build()
        
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun togglePlayPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        } else {
            if (mediaController?.playbackState == Player.STATE_ENDED) {
                mediaController?.seekTo(0)
            }
            mediaController?.play()
        }
    }

    fun seekTo(progress: Float) {
        val duration = mediaController?.duration?.coerceAtLeast(1L) ?: 1L
        val newPos = (progress * duration).toLong()
        mediaController?.seekTo(newPos)
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

    fun pauseSong() {
        // Agar aap MediaController use kar rahe hain toh:
        mediaController?.pause()
    }

    fun toggleShuffle() {
        _uiState.update { state ->
            val newShuffleState = !state.isShuffleOn
            if (newShuffleState) {
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
                mediaController?.repeatMode = Player.REPEAT_MODE_ALL
                RepeatMode.ALL
            }
            RepeatMode.ALL -> {
                mediaController?.repeatMode = Player.REPEAT_MODE_ONE
                RepeatMode.ONE
            }
            RepeatMode.ONE -> {
                mediaController?.repeatMode = Player.REPEAT_MODE_OFF
                RepeatMode.OFF
            }
        }
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
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
            mediaController?.pause()
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        playJob?.cancel()
        sleepTimerJob?.cancel()
        mediaController?.release()
    }
}

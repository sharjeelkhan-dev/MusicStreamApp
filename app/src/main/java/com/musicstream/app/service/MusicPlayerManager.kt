package com.musicstream.app.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.player.PlayerUiState
import com.musicstream.app.presentation.player.RepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private var pendingAction: (() -> Unit)? = null

    private var isSeeking = false
    private var seekLockJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                updateStateFromController()
                pendingAction?.invoke()
                pendingAction = null
            } catch (e: Exception) {
                Log.e("MusicPlayerManager", "Failed to get MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateStateFromController()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracker() else stopProgressTracker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateStateFromController()
            }
        })
    }

    private fun updateStateFromController() {
        val controller = mediaController ?: return
        if (isSeeking) return

        val currentIndex = controller.currentMediaItemIndex

        _uiState.update { state ->
            val currentSong = if (currentIndex in state.queue.indices) state.queue[currentIndex] else state.currentSong
            val dur = if (controller.duration > 0) controller.duration else (currentSong?.duration ?: 0L)
            val pos = controller.currentPosition
            state.copy(
                currentSong = currentSong,
                currentIndex = currentIndex,
                isPlaying = controller.isPlaying,
                duration = dur,
                currentPosition = pos,
                progress = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                isShuffleOn = controller.shuffleModeEnabled,
                repeatMode = when(controller.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
            )
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val controller = mediaController
        if (controller == null) {
            pendingAction = { playSong(song, queue) }
            return
        }

        val targetQueue = queue.ifEmpty { listOf(song) }
        val index = targetQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        _uiState.update { it.copy(queue = targetQueue, currentIndex = index, currentSong = song) }

        val mediaItems = targetQueue.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems)
        controller.seekTo(index, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun seekTo(position: Long) {
        val controller = mediaController
        if (controller == null) {
            pendingAction = { seekTo(position) }
            return
        }

        isSeeking = true
        seekLockJob?.cancel()

        val dur = _uiState.value.duration.coerceAtLeast(1L)

        _uiState.update {
            it.copy(
                currentPosition = position,
                progress = (position.toFloat() / dur).coerceIn(0f, 1f)
            )
        }

        controller.seekTo(position)

        seekLockJob = managerScope.launch {
            delay(400.milliseconds)
            isSeeking = false
        }
    }

    fun skipToNext() {
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun stop() {
        mediaController?.let {
            it.stop()
            it.clearMediaItems()
        }
        _uiState.update {
            it.copy(
                isPlaying = false,
                currentSong = null,
                queue = emptyList(),
                currentIndex = 0,
                progress = 0f,
                currentPosition = 0L
            )
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        _uiState.update { it.copy(isShuffleOn = controller.shuffleModeEnabled) }
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _uiState.update { it.copy(repeatMode = when(nextMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }) }
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setPlaylists(playlists: List<Playlist>) {
        _uiState.update { it.copy(playlists = playlists) }
    }

    fun updateFavorite(songId: String, isFavorite: Boolean) {
        _uiState.update { state ->
            val updatedQueue = state.queue.map {
                if (it.id == songId) it.copy(isFavorite = isFavorite) else it
            }
            val updatedCurrent = if (state.currentSong?.id == songId)
                state.currentSong.copy(isFavorite = isFavorite)
            else state.currentSong
            state.copy(currentSong = updatedCurrent, queue = updatedQueue)
        }
    }

    private var sleepTimerJob: Job? = null
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _uiState.update { it.copy(isSleepTimerActive = false, sleepTimerTimeLeft = 0) }
            return
        }
        val totalMillis = TimeUnit.MINUTES.toMillis(minutes.toLong())
        _uiState.update { it.copy(isSleepTimerActive = true, sleepTimerTimeLeft = totalMillis) }
        sleepTimerJob = managerScope.launch {
            var timeLeft = totalMillis
            while (timeLeft > 0) {
                delay(1000.milliseconds)
                timeLeft -= 1000
                _uiState.update { it.copy(sleepTimerTimeLeft = timeLeft) }
            }
            _uiState.update { it.copy(isSleepTimerActive = false, sleepTimerTimeLeft = 0) }
            mediaController?.pause()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = managerScope.launch {
            while (isActive) {
                val controller = mediaController ?: break
                if (controller.isPlaying && !isSeeking) {
                    val pos = controller.currentPosition
                    val dur = controller.duration.coerceAtLeast(1L)
                    _uiState.update { it.copy(
                        currentPosition = pos,
                        progress = (pos.toFloat() / dur).coerceIn(0f, 1f)
                    ) }
                }
                delay(500.milliseconds)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(if (coverUrl.isNotBlank()) coverUrl.toUri() else null)
            .build()

        val mediaUri: Uri = when {
            !localPath.isNullOrEmpty() && File(localPath).exists() -> {
                Log.d("MusicPlayerManager", "Playing local file: $localPath")
                Uri.fromFile(File(localPath))
            }
            streamUrl.isNotBlank() -> {
                Log.d("MusicPlayerManager", "Streaming online via Saavn: $streamUrl")
                streamUrl.toUri()
            }
            else -> Uri.EMPTY
        }

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .build()
    }
}
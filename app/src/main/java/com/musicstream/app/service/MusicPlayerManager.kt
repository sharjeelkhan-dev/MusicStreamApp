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
import com.musicstream.app.domain.model.Notification
import com.musicstream.app.domain.model.NotificationType
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.NotificationRepository
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
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val notificationRepository: NotificationRepository
) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private var pendingAction: (() -> Unit)? = null

    @Volatile
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

    private var isFetchingAutoMix = false
    private var isCrossfading = false
    private var fadeOutJob: Job? = null
    private var fadeInJob: Job? = null

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val wasCrossfading = isCrossfading
                fadeOutJob?.cancel()

                // If transition was natural/automatic or during crossfade, smoothly fade in the next track
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || wasCrossfading) {
                    startSmoothFadeIn(1500L)
                } else {
                    cancelFadesAndResetVolume()
                }

                updateStateFromController()
                _uiState.value.currentSong?.let { song ->
                    checkAndTriggerAutoMix(song)
                    managerScope.launch {
                        musicRepository.addToRecentlyPlayed(song)
                        
                        // Add an in-app notification
                        notificationRepository.addNotification(
                            Notification(
                                id = java.util.UUID.randomUUID().toString(),
                                title = "Now Playing",
                                message = "Started listening to '${song.title}' by ${song.artist}",
                                time = "Just now",
                                type = NotificationType.GENERAL
                            )
                        )
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                    cancelFadesAndResetVolume()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateStateFromController()
                if (playbackState == Player.STATE_READY && mediaController?.isPlaying == true) {
                    startProgressTracker()
                }
            }
        })
    }

    private fun updateStateFromController() {
        val controller = mediaController ?: return
        if (isSeeking) return

        val currentIndex = controller.currentMediaItemIndex

        _uiState.update { state ->
            val currentSong = if (currentIndex in state.queue.indices) state.queue[currentIndex] else state.currentSong
            val rawDuration = controller.duration
            val dur = if (rawDuration > 0) rawDuration else (currentSong?.duration ?: 0L)
            val pos = controller.currentPosition.coerceAtLeast(0L)

            val calculatedProgress = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f

            state.copy(
                currentSong = currentSong,
                currentIndex = currentIndex,
                isPlaying = controller.isPlaying,
                duration = dur,
                currentPosition = pos,
                progress = calculatedProgress,
                isShuffleOn = controller.shuffleModeEnabled,
                repeatMode = when (controller.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
            )
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        cancelFadesAndResetVolume()
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

        startProgressTracker()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(position: Long) {
        cancelFadesAndResetVolume()
        val controller = mediaController
        if (controller == null) {
            pendingAction = { seekTo(position) }
            return
        }

        isSeeking = true
        seekLockJob?.cancel()

        val dur = _uiState.value.duration.coerceAtLeast(1L)
        val validPosition = position.coerceIn(0L, dur)
        val calcProgress = (validPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

        _uiState.update {
            it.copy(
                currentPosition = validPosition,
                progress = calcProgress
            )
        }

        controller.seekTo(validPosition)

        seekLockJob = managerScope.launch {
            delay(200.milliseconds)
            isSeeking = false
        }
    }

    fun skipToNext() {
        cancelFadesAndResetVolume()
        mediaController?.seekToNext()
    }

    fun skipToPrevious() {
        cancelFadesAndResetVolume()
        mediaController?.seekToPrevious()
    }

    fun stop() {
        cancelFadesAndResetVolume()
        stopProgressTracker()
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
        _uiState.update {
            it.copy(repeatMode = when (nextMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            })
        }
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
                val controller = mediaController
                if (controller != null && controller.isPlaying && !isSeeking) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val rawDuration = controller.duration
                    val currentSongDur = _uiState.value.currentSong?.duration ?: 0L

                    val dur = if (rawDuration > 0) rawDuration else currentSongDur

                    if (dur > 0) {
                        val calcProgress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                        _uiState.update { it.copy(
                            currentPosition = pos,
                            duration = dur,
                            progress = calcProgress
                        ) }
                        applyCrossfadeIfNecessary(pos, dur)
                    }
                }
                delay(250.milliseconds) // Refresh rate smoothly 250ms set kar diya gaya hai
            }
        }
    }

    private fun checkAndTriggerAutoMix(currentSong: Song?) {
        if (currentSong == null || isFetchingAutoMix) return
        val currentQueue = _uiState.value.queue
        val currentIndex = mediaController?.currentMediaItemIndex ?: 0

        if (currentQueue.isNotEmpty() && currentIndex >= currentQueue.size - 2) {
            isFetchingAutoMix = true
            managerScope.launch {
                try {
                    musicRepository.getTrendingSongs(query = currentSong.artist)
                        .collect { recommendations ->
                            val existingIds = currentQueue.map { it.id }.toSet()
                            val newSongs = recommendations.filter { it.id !in existingIds }
                            if (newSongs.isNotEmpty()) {
                                val updatedQueue = currentQueue + newSongs
                                _uiState.update { it.copy(queue = updatedQueue) }
                                mediaController?.let { controller ->
                                    val mediaItems = newSongs.map { it.toMediaItem() }
                                    controller.addMediaItems(mediaItems)
                                }
                            }
                            isFetchingAutoMix = false
                        }
                } catch (e: Exception) {
                    Log.e("MusicPlayerManager", "Failed to fetch auto mix songs", e)
                    isFetchingAutoMix = false
                }
            }
        }
    }

    private fun applyCrossfadeIfNecessary(pos: Long, dur: Long) {
        val controller = mediaController ?: return
        val remainingMs = dur - pos
        val crossfadeWindowMs = 3000L

        // Only trigger crossfade if there actually is a next track or repeat is enabled
        val currentIndex = controller.currentMediaItemIndex
        val hasNextTrack = (currentIndex < controller.mediaItemCount - 1) ||
                (controller.repeatMode != Player.REPEAT_MODE_OFF)

        if (hasNextTrack && remainingMs in 1..crossfadeWindowMs && dur > 10000L) {
            if (!isCrossfading) {
                isCrossfading = true
                startSmoothFadeOut(dur, crossfadeWindowMs)
            }
        } else if (isCrossfading && (remainingMs !in 1..crossfadeWindowMs)) {
            cancelFadesAndResetVolume()
        }
    }

    private fun startSmoothFadeOut(dur: Long, windowMs: Long) {
        fadeOutJob?.cancel()
        fadeInJob?.cancel()
        fadeOutJob = managerScope.launch {
            val controller = mediaController ?: return@launch
            while (isActive && isCrossfading) {
                val currentPos = controller.currentPosition
                val remaining = dur - currentPos
                if (remaining <= 0) break
                val ratio = (remaining.toFloat() / windowMs.toFloat()).coerceIn(0f, 1f)
                // Natural audio fade out curve (equal-power)
                controller.volume = (ratio * ratio).coerceIn(0.05f, 1.0f)
                delay(40.milliseconds)
            }
        }
    }

    private fun startSmoothFadeIn(durationMs: Long = 1500L) {
        fadeInJob?.cancel()
        fadeOutJob?.cancel()
        val controller = mediaController ?: return
        isCrossfading = true
        controller.volume = 0f
        fadeInJob = managerScope.launch {
            val steps = 30
            val interval = durationMs / steps
            for (i in 1..steps) {
                if (!isActive) break
                val factor = (i.toFloat() / steps).coerceIn(0f, 1f)
                controller.volume = (factor * factor).coerceIn(0f, 1f)
                delay(interval.milliseconds)
            }
            controller.volume = 1.0f
            isCrossfading = false
        }
    }

    private fun cancelFadesAndResetVolume() {
        fadeOutJob?.cancel()
        fadeInJob?.cancel()
        isCrossfading = false
        mediaController?.volume = 1.0f
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
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
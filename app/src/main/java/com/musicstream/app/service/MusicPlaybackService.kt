package com.musicstream.app.service

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import javax.inject.Inject
import androidx.core.net.toUri

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicRepository: MusicRepository

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CUSTOM_COMMAND_TOGGLE_FAVORITE = "CUSTOM_COMMAND_TOGGLE_FAVORITE"
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("MusicPlaybackService", "onCreate: Service starting...")

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicPlaybackService", "Player Error: ${error.message}", error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when(playbackState) {
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    Player.STATE_IDLE -> "IDLE"
                    else -> "UNKNOWN"
                }
                android.util.Log.d("MusicPlaybackService", "Playback State Changed: $stateName")
            }
        })

        val sessionCallback = object : MediaSession.Callback {

            // 1. Controller Permissions Setup (Taaki player commands trigger ho sakein)
            @OptIn(UnstableApi::class)
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
                    .build()

                val availablePlayerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_PREPARE)
                    .add(Player.COMMAND_SET_MEDIA_ITEMS_METADATA)
                    .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_STOP)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableSessionCommands)
                    .setAvailablePlayerCommands(availablePlayerCommands)
                    .build()
            }

            // 2. Dynamic Media Resolution (Asynchronous URL Retrieval for YouTube tracks)
            @OptIn(UnstableApi::class)
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {

                // Guava ListenableFuture wrapper use karke coroutine resolve karna
                return serviceScope.future {
                    val resolvedList = mediaItems.map { item ->
                        val currentUri = item.localConfiguration?.uri?.toString() ?: ""

                        // Agar URL YouTube format mein hai, tabhi resolution run karein
                        if (currentUri.startsWith("youtube://") || item.mediaId.isNotEmpty() && (currentUri.isEmpty() || currentUri.startsWith("youtube://"))) {
                            val cleanId = if (currentUri.startsWith("youtube://")) {
                                currentUri.substringAfter("youtube://")
                            } else {
                                item.mediaId
                            }

                            android.util.Log.d("MusicPlaybackService", "Resolving YouTube audio stream for: $cleanId")
                            val directStreamUrl = musicRepository.getYouTubeAudioStreamUrl(cleanId)

                            if (!directStreamUrl.isNullOrEmpty()) {
                                android.util.Log.d("MusicPlaybackService", "Success resolving stream: $directStreamUrl")
                                item.buildUpon()
                                    .setUri(directStreamUrl.toUri())
                                    .build()
                            } else {
                                android.util.Log.e("MusicPlaybackService", "Resolution failed, returning fallback item")
                                item
                            }
                        } else {
                            // Local file ya pehle se resolved URL ko bypass karein
                            item
                        }
                    }.toMutableList()

                    resolvedList
                }
            }
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(sessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            exoPlayer.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
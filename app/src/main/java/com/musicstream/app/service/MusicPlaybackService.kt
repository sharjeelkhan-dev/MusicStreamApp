package com.musicstream.app.service

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicRepository: MusicRepository

    private var mediaSession: MediaSession? = null

    companion object {
        const val CUSTOM_COMMAND_TOGGLE_FAVORITE = "CUSTOM_COMMAND_TOGGLE_FAVORITE"
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("MusicPlaybackService", "onCreate: Service starting...")

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicPlaybackService", "EXOPLAYER ERROR CODE: ${error.errorCodeName}")
                android.util.Log.e("MusicPlaybackService", "EXOPLAYER ERROR CAUSE: ${error.cause?.message}", error)
            }
        })

        val sessionCallback = object : MediaSession.Callback {

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

            @OptIn(UnstableApi::class)
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                val updatedItems = mediaItems.map { item ->
                    val rawUriString = item.localConfiguration?.uri?.toString() ?: ""

                    // File path Extraction
                    val cleanPath = when {
                        rawUriString.startsWith("file://") -> rawUriString.substring(7)
                        else -> rawUriString
                    }

                    val isLocalPath = cleanPath.startsWith("/data/") || cleanPath.startsWith("/storage/")
                    val localFile = File(cleanPath)

                    if (isLocalPath && localFile.exists()) {
                        android.util.Log.d("MusicPlaybackService", "Successfully matched offline file: ${localFile.absolutePath}")

                        // Properly build MediaItem with MediaId & File URI
                        item.buildUpon()
                            .setMediaId(item.mediaId.ifEmpty { cleanPath })
                            .setUri(Uri.fromFile(localFile))
                            .setMediaMetadata(item.mediaMetadata)
                            .build()
                    } else {
                        if (isLocalPath) {
                            android.util.Log.e("MusicPlaybackService", "File path marked local but missing on disk: $cleanPath")
                        }
                        item
                    }
                }.toMutableList()

                return Futures.immediateFuture(updatedItems)
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
        super.onDestroy()
    }
}
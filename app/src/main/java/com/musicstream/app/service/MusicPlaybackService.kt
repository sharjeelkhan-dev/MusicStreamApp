package com.musicstream.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.DefaultMediaNotificationProvider
import com.google.common.util.concurrent.ListenableFuture
import com.musicstream.app.MainActivity
import com.musicstream.app.R
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicRepository: MusicRepository

    @Inject
    lateinit var settingsRepository: com.musicstream.app.domain.repository.SettingsRepository

    private var mediaSession: MediaSession? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val TAG = "MusicPlaybackService"
    private val CHANNEL_ID = "music_stream_v7"

    companion object {
        private const val CUSTOM_COMMAND_TOGGLE_FAVORITE = "ACTION_TOGGLE_FAVORITE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Starting Service")
        
        createNotificationChannel()
        observeEqualizerSettings()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()
            
        val baseProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            
        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                session: MediaSession,
                customLayout: com.google.common.collect.ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                val mediaNotification = baseProvider.createNotification(
                    session, customLayout, actionFactory, onNotificationChangedCallback
                )
                
                // Extra visibility for Lock Screen (Essential for realme/oppo)
                mediaNotification.notification.visibility = android.app.Notification.VISIBILITY_PUBLIC
                mediaNotification.notification.category = android.app.Notification.CATEGORY_TRANSPORT
                
                return mediaNotification
            }

            override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean = false
        })

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                setCustomLayout()
            }
        })
        
        setCustomLayout()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Stream Playback"
            val descriptionText = "Music playback controls"
            // Importance DEFAULT is required for lock screen visibility on many devices
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun observeEqualizerSettings() {
        settingsRepository.getEqualizerPreset()
            .onEach { preset ->
                applyEqualizerPreset(preset)
            }
            .launchIn(serviceScope)
    }

    private fun applyEqualizerPreset(preset: String) {
        try {
            val equalizer = android.media.audiofx.Equalizer(0, exoPlayer.audioSessionId)
            if (!equalizer.enabled) equalizer.enabled = true
            
            val numBands = equalizer.numberOfBands
            val (minLevel, maxLevel) = equalizer.bandLevelRange
            val center = (minLevel + maxLevel) / 2
            
            when (preset) {
                "Flat" -> {
                    for (i in 0 until numBands) equalizer.setBandLevel(i.toShort(), center.toShort())
                }
                "Bass Boost" -> {
                    if (numBands > 0) equalizer.setBandLevel(0.toShort(), maxLevel)
                    if (numBands > 1) equalizer.setBandLevel(1.toShort(), (center + (maxLevel - center) / 2).toShort())
                }
                "Rock" -> {
                    if (numBands >= 5) {
                        equalizer.setBandLevel(0.toShort(), (center + 300).toShort())
                        equalizer.setBandLevel(1.toShort(), (center + 100).toShort())
                        equalizer.setBandLevel(2.toShort(), (center - 200).toShort())
                        equalizer.setBandLevel(3.toShort(), (center + 200).toShort())
                        equalizer.setBandLevel(4.toShort(), (center + 400).toShort())
                    }
                }
                "Pop" -> {
                    if (numBands >= 5) {
                        equalizer.setBandLevel(0.toShort(), (center - 100).toShort())
                        equalizer.setBandLevel(1.toShort(), (center + 200).toShort())
                        equalizer.setBandLevel(2.toShort(), (center + 400).toShort())
                        equalizer.setBandLevel(3.toShort(), (center + 100).toShort())
                        equalizer.setBandLevel(4.toShort(), (center - 100).toShort())
                    }
                }
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Error applying equalizer: ${e.message}")
        }
    }

    private fun setCustomLayout() {
        val mediaItem = exoPlayer.currentMediaItem
        val songId = mediaItem?.mediaId
        
        serviceScope.launch {
            val isFavorite = if (songId != null) {
                musicRepository.getSongById(songId).first()?.isFavorite ?: false
            } else {
                false
            }

            val favoriteButton = CommandButton.Builder()
                .setDisplayName(if (isFavorite) "Remove from Favorites" else "Add to Favorites")
                .setSessionCommand(SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
                .setIconResId(if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart)
                .build()

            mediaSession?.setCustomLayout(com.google.common.collect.ImmutableList.of(favoriteButton))
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
                .build()
                
            val availablePlayerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_STOP)
                .build()
                
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setAvailablePlayerCommands(availablePlayerCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_COMMAND_TOGGLE_FAVORITE) {
                val mediaId = exoPlayer.currentMediaItem?.mediaId
                if (mediaId != null) {
                    return serviceScope.future {
                        musicRepository.toggleFavorite(mediaId)
                        setCustomLayout()
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    }
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null) {
            if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
                stopSelf()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

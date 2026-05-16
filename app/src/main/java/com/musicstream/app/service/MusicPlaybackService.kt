package com.musicstream.app.service

import android.app.Notification
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
    private val CHANNEL_ID = "music_stream_v13"

    companion object {
        private const val CUSTOM_COMMAND_TOGGLE_FAVORITE = "ACTION_TOGGLE_FAVORITE"
    }

    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: android.media.audiofx.BassBoost? = null
    private var virtualizer: android.media.audiofx.Virtualizer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing Service")
        
        createNotificationChannel()
        
        initializeAudioEffects()
        observeAudioEffectsSettings()

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
            
        // Use standard Media3 notification management
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            
        setMediaNotificationProvider(notificationProvider)

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                setCustomLayout()
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged: $playbackState")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "onIsPlayingChanged: $isPlaying")
            }
        })
        
        setCustomLayout()
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // Here we can inject the visibility flags into the notification before it's posted
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: $intent")
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Stream"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Music playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeAudioEffects() {
        val sessionId = exoPlayer.audioSessionId
        try {
            equalizer = android.media.audiofx.Equalizer(0, sessionId).apply { enabled = true }
            bassBoost = android.media.audiofx.BassBoost(0, sessionId).apply { enabled = true }
            virtualizer = android.media.audiofx.Virtualizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio effects: ${e.message}")
        }
    }

    private fun observeAudioEffectsSettings() {
        // Observe Preset
        settingsRepository.getEqualizerPreset()
            .onEach { preset ->
                if (preset != "Custom") {
                    applyEqualizerPreset(preset)
                }
            }
            .launchIn(serviceScope)

        // Observe Bass Boost
        settingsRepository.getBassBoostLevel()
            .onEach { level ->
                try {
                    bassBoost?.setStrength(level.toShort())
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting bass boost: ${e.message}")
                }
            }
            .launchIn(serviceScope)

        // Observe Virtualizer
        settingsRepository.getVirtualizerLevel()
            .onEach { level ->
                try {
                    virtualizer?.setStrength(level.toShort())
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting virtualizer: ${e.message}")
                }
            }
            .launchIn(serviceScope)

        // Observe Custom Band Levels
        settingsRepository.getEqualizerBandLevels()
            .onEach { bandLevels ->
                settingsRepository.getEqualizerPreset().first().let { preset ->
                    if (preset == "Custom") {
                        bandLevels.forEach { (band, level) ->
                            try {
                                equalizer?.setBandLevel(band.toShort(), level.toShort())
                            } catch (e: Exception) {
                                Log.e(TAG, "Error setting band $band: ${e.message}")
                            }
                        }
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun applyEqualizerPreset(preset: String) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            
            // Standard preset mapping
            val presetLevels = when (preset) {
                "Flat" -> List(numBands.toInt()) { 0 }
                "Bass Boost" -> listOf(1000, 500, 0, 0, 0)
                "Rock" -> listOf(400, 200, -200, 300, 500)
                "Pop" -> listOf(-200, 100, 300, 100, -200)
                "Electronic" -> listOf(400, 200, 0, 200, 400)
                "Classical" -> listOf(0, 0, 0, -200, -200)
                "Jazz" -> listOf(0, 0, 200, 200, 0)
                "Dance" -> listOf(600, 0, 200, 400, 0)
                else -> List(numBands.toInt()) { 0 }
            }

            for (i in 0 until numBands) {
                val level = if (i < presetLevels.size) presetLevels[i] else 0
                eq.setBandLevel(i.toShort(), level.toShort())
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
            player.pause()
            player.stop()
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

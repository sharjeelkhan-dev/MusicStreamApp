package com.musicstream.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.math.abs
import kotlinx.coroutines.Job
import androidx.core.app.NotificationCompat
import com.musicstream.app.R
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.DownloadProgress
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var musicRepository: MusicRepository
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val activeDownloads = mutableMapOf<String, Pair<Job, Song>>()

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val EXTRA_SONG = "extra_song"
        private const val FOREGROUND_NOTIFICATION_ID = 9999

        fun start(context: Context, song: Song) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_SONG, song)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadService", "Failed to start service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val song = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_SONG, Song::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_SONG)
        }

        if (song != null) {
            val isFirstDownload = activeDownloads.isEmpty()
            activeDownloads[song.id]?.first?.cancel()

            if (isFirstDownload) {
                val initialNotification = createNotification(song, 0)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            FOREGROUND_NOTIFICATION_ID,
                            initialNotification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(FOREGROUND_NOTIFICATION_ID, initialNotification)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DownloadService",
                        "Foreground error", e)
                }
            }
            downloadSong(song)
        } else if (activeDownloads.isEmpty()) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun downloadSong(song: Song) {
        val notificationId = abs(song.id.hashCode()) + 1000
        val job = serviceScope.launch {
            try {
                musicRepository.downloadSong(song).collect { progress ->
                    withContext(Dispatchers.Main) {
                        when (progress) {
                            is DownloadProgress.Progress -> {
                                updateNotification(song, progress.percent, notificationId)
                            }
                            is DownloadProgress.Completed -> {
                                showCompletionNotification(song)
                                onDownloadFinished(song.id)
                            }
                            is DownloadProgress.Failed -> {
                                showFailureNotification(song, progress.error)
                                onDownloadFinished(song.id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onDownloadFinished(song.id) }
            }
        }
        activeDownloads[song.id] = job to song
    }

    private fun onDownloadFinished(songId: String) {
        activeDownloads.remove(songId)
        notificationManager.cancel(abs(songId.hashCode()) + 1000)

        if (activeDownloads.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            val nextEntry = activeDownloads.values.firstOrNull()
            if (nextEntry != null) {
                notificationManager.notify(FOREGROUND_NOTIFICATION_ID,
                    createNotification(nextEntry.second, 0))
            }
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Downloads",
                NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun createNotification(song: Song, progress: Int): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.media3_notification_small_icon)
            .setContentTitle("Downloading Track")
            .setContentText("${song.title} - ${song.artist}")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification(song: Song, progress: Int, notificationId: Int) {
        notificationManager.notify(notificationId,
            createNotification(song, progress))
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID,
            createNotification(song, progress))
    }

    private fun showCompletionNotification(song: Song) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(this,
            "Finished: ${song.title}",
            Toast.LENGTH_SHORT).show() }
        val notification = NotificationCompat.Builder(this,
            CHANNEL_ID)
            .setSmallIcon(R.drawable.check_mark_line_icon)
            .setContentTitle("Download Complete")
            .setContentText(song.title)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(abs(song.id.hashCode()) + 2000, notification)
    }

    private fun showFailureNotification(song: Song, error: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(this,
            "Failed: ${song.title}",
            Toast.LENGTH_LONG).show() }
        val notification = NotificationCompat.Builder(this,
            CHANNEL_ID)
            .setSmallIcon(R.drawable.close_line_icon)
            .setContentTitle("Download Failed")
            .setContentText("${song.title}: $error")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(abs(song.id.hashCode()) + 3000, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}

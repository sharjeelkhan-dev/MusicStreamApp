package com.musicstream.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.musicstream.app.R
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.DownloadProgress
import com.musicstream.app.domain.repository.MusicRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@HiltWorker
class AudioDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val musicRepository: MusicRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_SONG_ID = "key_song_id"
        const val KEY_SONG_TITLE = "key_song_title"
        const val KEY_SONG_ARTIST = "key_song_artist"
        const val KEY_SONG_COVER = "key_song_cover"
        const val TAG_DOWNLOAD = "tag_download_"
        const val CHANNEL_ID = "download_channel"

        fun enqueue(context: Context, song: Song) {
            val data = Data.Builder()
                .putString(KEY_SONG_ID, song.id)
                .putString(KEY_SONG_TITLE, song.title)
                .putString(KEY_SONG_ARTIST, song.artist)
                .putString(KEY_SONG_COVER, song.coverUrl)
                .build()

            val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(TAG_DOWNLOAD + song.id)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_${song.id}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private val notificationManager by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationId by lazy {
        val songId = inputData.getString(KEY_SONG_ID) ?: ""
        abs(songId.hashCode()) + 1000
    }

    override suspend fun doWork(): Result {
        val songId = inputData.getString(KEY_SONG_ID) ?: return Result.failure()
        val songTitle = inputData.getString(KEY_SONG_TITLE) ?: "Unknown Track"
        val songArtist = inputData.getString(KEY_SONG_ARTIST) ?: "Unknown Artist"
        val songCover = inputData.getString(KEY_SONG_COVER) ?: ""

        Log.d("AudioDownloadWorker", "Download work started for: $songTitle ($songId)")

        createNotificationChannel()

        val song = Song(
            id = songId,
            title = songTitle,
            artist = songArtist,
            coverUrl = songCover
        )

        return try {
            // Service status ko foreground notify karne ke liye initial notification bind karein
            val initialForegroundInfo = createForegroundInfo(song, 0)
            setForeground(initialForegroundInfo)

            var isCompletedSuccessfully = false

            musicRepository.downloadSong(song).collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        setProgress(workDataOf("progress" to progress.percent))
                        try {
                            setForeground(createForegroundInfo(song, progress.percent))
                        } catch (e: Exception) {
                            Log.e("AudioDownloadWorker", "Notification Update Error: ${e.message}")
                        }
                    }
                    is DownloadProgress.Completed -> {
                        isCompletedSuccessfully = true
                        showCompletionNotification(song)
                    }
                    is DownloadProgress.Failed -> {
                        Log.e("AudioDownloadWorker", "Download Failed for $songId: ${progress.error}")
                        showFailureNotification(song, progress.error)
                    }
                }
            }

            if (isCompletedSuccessfully) Result.success() else Result.failure()
        } catch (e: Exception) {
            Log.e("AudioDownloadWorker", "Worker execution exception for $songId: ${e.message}")
            showFailureNotification(song, e.localizedMessage ?: "Unknown Error")
            Result.failure()
        }
    }

    private fun createForegroundInfo(song: Song, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.media3_notification_small_icon)
            .setContentTitle("Downloading Track")
            .setContentText("${song.title} - ${song.artist}")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun showCompletionNotification(song: Song) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "Finished: ${song.title}", Toast.LENGTH_SHORT).show()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.check_mark_line_icon)
            .setContentTitle("Download Complete")
            .setContentText(song.title)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val completionNotificationId = abs(song.id.hashCode()) + 2000
        notificationManager.notify(completionNotificationId, notification)
    }

    private fun showFailureNotification(song: Song, error: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "Failed: ${song.title}", Toast.LENGTH_LONG).show()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.close_line_icon)
            .setContentTitle("Download Failed")
            .setContentText("${song.title}: $error")
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val failureNotificationId = abs(song.id.hashCode()) + 3000
        notificationManager.notify(failureNotificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Track Downloads"
            val descriptionText = "Shows progress and completion status for downloading music"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
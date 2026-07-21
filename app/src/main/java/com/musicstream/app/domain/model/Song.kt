package com.musicstream.app.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L, // in milliseconds
    val coverUrl: String = "",
    val streamUrl: String = "",
    val localPath: String? = null,
    val albumId: String = "",
    val isrc: String? = null,
    val quality: String = "320kbps",
    val isFavorite: Boolean = false,
    val isExplicit: Boolean = false,
    val playCount: Long = 0L,
    val gradientIndex: Int = 0 // for gradient thumb selection
) : Parcelable {
    val durationFormatted: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}

package com.musicstream.app.data.remote.dto

import com.musicstream.app.domain.model.Song
import com.squareup.moshi.Json

data class SongDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "artist") val artist: String,
    @Json(name = "album") val album: String?,
    @Json(name = "duration") val duration: Long,
    @Json(name = "playCount") val playCount: Long,
    @Json(name = "gradientIndex") val gradientIndex: Int
) {
    fun toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album ?: "Unknown Album",
        duration = duration,
        playCount = playCount,
        gradientIndex = gradientIndex,
        isFavorite = false
    )

    fun toEntity() = com.musicstream.app.data.local.entity.SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album ?: "Unknown Album",
        duration = duration,
        playCount = playCount,
        gradientIndex = gradientIndex
    )
}

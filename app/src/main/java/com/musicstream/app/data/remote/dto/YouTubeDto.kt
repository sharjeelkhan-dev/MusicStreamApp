package com.musicstream.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponseDto(
    @Json(name = "items") val items: List<YouTubeSearchItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItemDto(
    @Json(name = "url") val url: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "uploaderName") val uploaderName: String? = null,
    @Json(name = "duration") val duration: Long? = null,
    @Json(name = "type") val type: String? = null
) {
    val videoId: String?
        get() {
            if (url == null) return null
            val pattern = "(?:v=|/v/|/vi/|youtu\\.be/|/embed/|/watch\\?v=)([a-zA-Z0-9_-]{11})".toRegex()
            return pattern.find(url)?.groupValues?.get(1)
        }
}

@JsonClass(generateAdapter = true)
data class YouTubeStreamDto(
    @Json(name = "audioStreams") val audioStreams: List<YouTubeAudioStreamDto>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeAudioStreamDto(
    @Json(name = "url") val url: String? = null,
    @Json(name = "bitrate") val bitrate: Int? = null,
    @Json(name = "mimeType") val mimeType: String? = null
)

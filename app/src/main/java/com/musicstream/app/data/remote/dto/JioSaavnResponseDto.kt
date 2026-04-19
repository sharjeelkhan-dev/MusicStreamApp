package com.musicstream.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JioSaavnResponseDto(
    @Json(name = "status") val status: String?,
    @Json(name = "data") val data: JioSaavnDataDto?
)

@JsonClass(generateAdapter = true)
data class JioSaavnDataDto(
    @Json(name = "results") val results: List<JioSaavnSongDto>?
)

@JsonClass(generateAdapter = true)
data class JioSaavnSongDto(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "primaryArtists") val primaryArtists: String?,
    @Json(name = "album") val album: JioSaavnAlbumDto?,
    @Json(name = "duration") val duration: String?,
    @Json(name = "image") val image: List<JioSaavnImageDto>?,
    @Json(name = "downloadUrl") val downloadUrl: List<JioSaavnDownloadDto>?
)

@JsonClass(generateAdapter = true)
data class JioSaavnAlbumDto(
    @Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class JioSaavnImageDto(
    @Json(name = "quality") val quality: String?,
    @Json(name = "link") val link: String?
)

@JsonClass(generateAdapter = true)
data class JioSaavnDownloadDto(
    @Json(name = "quality") val quality: String?,
    @Json(name = "link") val link: String?
)

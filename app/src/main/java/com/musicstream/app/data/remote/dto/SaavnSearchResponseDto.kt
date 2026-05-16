package com.musicstream.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SaavnSearchResponseDto(
    @Json(name = "status") val status: String?,
    @Json(name = "success") val success: Boolean?,
    @Json(name = "data") val data: Any? // Can be List<SaavnSongDto> or SaavnSearchDataDto
)

@JsonClass(generateAdapter = true)
data class SaavnSearchDataDto(
    @Json(name = "results") val results: List<SaavnSongDto>?
)

@JsonClass(generateAdapter = true)
data class SaavnSongDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "primaryArtists") val primaryArtists: String?,
    @Json(name = "image") val image: List<SaavnImageDto>?,
    @Json(name = "downloadUrl") val downloadUrl: List<SaavnDownloadUrlDto>?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "isExplicit") val isExplicit: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SaavnImageDto(
    @Json(name = "quality") val quality: String,
    @Json(name = "link") val link: String
)

@JsonClass(generateAdapter = true)
data class SaavnDownloadUrlDto(
    @Json(name = "quality") val quality: String,
    @Json(name = "link") val link: String
)

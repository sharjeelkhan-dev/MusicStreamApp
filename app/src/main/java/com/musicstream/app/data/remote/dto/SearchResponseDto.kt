package com.musicstream.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponseDto(
    @Json(name = "resultCount") val resultCount: Int,
    @Json(name = "results") val results: List<TrackDto>
)

@JsonClass(generateAdapter = true)
data class TrackDto(
    @Json(name = "trackId") val trackId: Long?,
    @Json(name = "trackName") val trackName: String?,
    @Json(name = "artistName") val artistName: String?,
    @Json(name = "collectionName") val collectionName: String?,
    @Json(name = "previewUrl") val previewUrl: String?,
    @Json(name = "artworkUrl100") val artworkUrl100: String?,
    @Json(name = "trackTimeMillis") val trackTimeMillis: Long?,
    @Json(name = "primaryGenreName") val primaryGenreName: String?
)

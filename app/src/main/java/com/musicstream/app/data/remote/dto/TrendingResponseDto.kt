package com.musicstream.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HomeModulesResponseDto(
    @Json(name = "status") val status: String?,
    @Json(name = "success") val success: Boolean?,
    @Json(name = "data") val data: Any? // Can be HomeModulesDataDto or List<SaavnSongDto>
)

@JsonClass(generateAdapter = true)
data class HomeModulesDataDto(
    @Json(name = "trending") val trending: TrendingModuleDto?
)

@JsonClass(generateAdapter = true)
data class TrendingModuleDto(
    @Json(name = "songs") val songs: List<SaavnSongDto>?
)

@JsonClass(generateAdapter = true)
data class TrendingResponseDto(
    @Json(name = "status") val status: String?,
    @Json(name = "success") val success: Boolean?,
    @Json(name = "data") val data: TrendingDataDto?
)

@JsonClass(generateAdapter = true)
data class TrendingDataDto(
    @Json(name = "trending") val trending: TrendingItemsDto?
)

@JsonClass(generateAdapter = true)
data class TrendingItemsDto(
    @Json(name = "songs") val songs: List<SaavnSongDto>?
)

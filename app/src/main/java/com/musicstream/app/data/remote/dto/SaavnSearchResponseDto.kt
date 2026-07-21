package com.musicstream.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// 1. Search Songs Response DTO (/api/search/songs)
@JsonClass(generateAdapter = true)
data class SaavnSearchResponseDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: SaavnSearchDataDto? = null
)

@JsonClass(generateAdapter = true)
data class SaavnSearchDataDto(
    @Json(name = "total") val total: Int? = 0,
    @Json(name = "start") val start: Int? = 0,
    @Json(name = "results") val results: List<SaavnSongDto>? = emptyList()
)

// 2. Song Details Response DTO (/api/songs)
@JsonClass(generateAdapter = true)
data class SaavnSongDetailsResponseDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: Any? = null // Can be List<SaavnSongDto> or Single Object
)

// 3. Artist Object DTO (Flexible for primaryArtists field)
@JsonClass(generateAdapter = true)
data class SaavnArtistDto(
    @Json(name = "id") val id: String? = "",
    @Json(name = "name") val name: String? = "",
    @Json(name = "role") val role: String? = ""
)

// 4. Main Song Object DTO
@JsonClass(generateAdapter = true)
data class SaavnSongDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String? = "",
    @Json(name = "title") val title: String? = "", // Fallback title
    @Json(name = "primaryArtists") val primaryArtists: Any? = null, // String OR List<Artist>
    @Json(name = "image") val image: Any? = null, // String OR List<SaavnImageDto>
    @Json(name = "downloadUrl") val downloadUrl: Any? = null, // String OR List<SaavnDownloadUrlDto>
    @Json(name = "duration") val duration: Any? = null,
    @Json(name = "isExplicit") val isExplicit: Boolean? = false
) {
    // Helper property to get display title
    val songTitle: String
        get() = name?.ifBlank { null } ?: title ?: "Unknown Song"

    // Helper property to extract artist names safely
    val artistNameFormatted: String
        get() = when (primaryArtists) {
            is String -> primaryArtists
            is List<*> -> {
                primaryArtists.filterIsInstance<Map<String, Any>>()
                    .mapNotNull { it["name"] as? String }
                    .joinToString(", ")
            }
            else -> "Unknown Artist"
        }

    // Helper property to extract highest quality stream link (320kbps)
    val highestQualityStreamUrl: String
        get() {
            return when (downloadUrl) {
                is String -> downloadUrl
                is List<*> -> {
                    val lastItem = downloadUrl.lastOrNull()
                    if (lastItem is Map<*, *>) {
                        lastItem["link"] as? String ?: ""
                    } else ""
                }
                else -> ""
            }
        }

    // Helper property to extract highest quality cover art (500x500)
    val highestQualityImageUrl: String
        get() {
            return when (image) {
                is String -> image
                is List<*> -> {
                    val lastItem = image.lastOrNull()
                    if (lastItem is Map<*, *>) {
                        lastItem["link"] as? String ?: ""
                    } else ""
                }
                else -> ""
            }
        }
}

@JsonClass(generateAdapter = true)
data class SaavnImageDto(
    @Json(name = "quality") val quality: String? = "",
    @Json(name = "link") val link: String? = ""
)

@JsonClass(generateAdapter = true)
data class SaavnDownloadUrlDto(
    @Json(name = "quality") val quality: String? = "",
    @Json(name = "link") val link: String? = ""
)
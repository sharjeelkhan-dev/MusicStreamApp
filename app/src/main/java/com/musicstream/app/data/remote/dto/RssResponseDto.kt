package com.musicstream.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RssResponseDto(
    @Json(name = "feed") val feed: FeedDto?
)

@JsonClass(generateAdapter = true)
data class FeedDto(
    @Json(name = "entry") val entry: List<EntryDto>?
)

@JsonClass(generateAdapter = true)
data class EntryDto(
    @Json(name = "id") val id: RssIdDto?,
    @Json(name = "im:name") val name: LabelDto?,
    @Json(name = "im:artist") val artist: LabelDto?,
    @Json(name = "im:image") val images: List<ImageDto>?,
    @Json(name = "link") val link: List<LinkDto>?
)

@JsonClass(generateAdapter = true)
data class RssIdDto(
    @Json(name = "attributes") val attributes: IdAttributesDto?
)

@JsonClass(generateAdapter = true)
data class IdAttributesDto(
    @Json(name = "im:id") val id: String?
)

@JsonClass(generateAdapter = true)
data class LabelDto(
    @Json(name = "label") val label: String?
)

@JsonClass(generateAdapter = true)
data class ImageDto(
    @Json(name = "label") val label: String?,
    @Json(name = "attributes") val attributes: ImageAttributesDto?
)

@JsonClass(generateAdapter = true)
data class ImageAttributesDto(
    @Json(name = "height") val height: String?
)

@JsonClass(generateAdapter = true)
data class LinkDto(
    @Json(name = "attributes") val attributes: LinkAttributesDto?
)

@JsonClass(generateAdapter = true)
data class LinkAttributesDto(
    @Json(name = "rel") val rel: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "href") val href: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "im:assetType") val assetType: String?
)

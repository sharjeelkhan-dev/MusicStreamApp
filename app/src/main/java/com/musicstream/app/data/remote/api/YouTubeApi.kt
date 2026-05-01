package com.musicstream.app.data.remote.api

import com.musicstream.app.data.remote.dto.YouTubeSearchItemDto
import com.musicstream.app.data.remote.dto.YouTubeSearchResponseDto
import com.musicstream.app.data.remote.dto.YouTubeStreamDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YouTubeApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "music_videos"
    ): YouTubeSearchResponseDto

    @GET("streams/{videoId}")
    suspend fun getStreamInfo(
        @Path("videoId") videoId: String
    ): YouTubeStreamDto

    @GET("trending")
    suspend fun getTrending(
        @Query("region") region: String? = null
    ): List<YouTubeSearchItemDto>

    companion object {
        const val BASE_URL = "https://piped-api.lilly.garden/"
    }
}

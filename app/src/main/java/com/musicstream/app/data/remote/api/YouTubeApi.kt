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
        @Query("filter") filter: String = "all",
        @Query("limit") limit: Int = 100
    ): YouTubeSearchResponseDto

    @GET("streams/{videoId}")
    suspend fun getStreamInfo(
        @Path("videoId") videoId: String
    ): YouTubeStreamDto

    companion object {
        const val BASE_URL = "https://pipedapi.in.projectsegfau.lt/" // Default to India
    }
}

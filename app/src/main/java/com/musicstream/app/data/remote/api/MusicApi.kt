package com.musicstream.app.data.remote.api

import com.musicstream.app.data.remote.dto.SaavnSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): SaavnSearchResponseDto

    @GET("search/songs")
    suspend fun getTrendingSongs(
        @Query("query") query: String = "trending",
        @Query("limit") limit: Int = 20
    ): SaavnSearchResponseDto
}

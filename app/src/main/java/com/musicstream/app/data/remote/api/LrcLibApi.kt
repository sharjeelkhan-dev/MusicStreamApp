package com.musicstream.app.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class LrcLibResponse(
    val id: Long?,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Double?,
    val instrumental: Boolean?,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

interface LrcLibApi {
    @GET("get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("duration") durationInSeconds: Int? = null
    ): Response<LrcLibResponse>

    @GET("search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): Response<List<LrcLibResponse>>
}

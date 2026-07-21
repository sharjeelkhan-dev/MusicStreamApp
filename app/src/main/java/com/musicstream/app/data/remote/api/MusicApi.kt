package com.musicstream.app.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 40
    ): ResponseBody

    @GET("modules")
    suspend fun getTrending(
        @Query("language") language: String = "hindi,english,punjabi,urdu"
    ): ResponseBody

    @GET("search/songs")
    suspend fun getTrendingSongs(
        @Query("query") query: String = "Top Songs",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 40
    ): ResponseBody

    @GET("songs")
    suspend fun getSongDetails(
        @Query("id") id: String
    ): ResponseBody
}
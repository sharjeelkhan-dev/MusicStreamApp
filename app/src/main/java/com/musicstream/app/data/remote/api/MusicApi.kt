package com.musicstream.app.data.remote.api
import com.musicstream.app.data.remote.dto.SaavnSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query", encoded = true) query: String,
        @Query("limit") limit: Int = 50
    ): SaavnSearchResponseDto

    @GET("songs")
    suspend fun getSongDetails(
        @Query("id") id: String
    ): SaavnSearchResponseDto

    @GET("modules")
    suspend fun getTrending(
        @Query("language") language: String = "hindi,punjabi,english"
    ): com.musicstream.app.data.remote.dto.HomeModulesResponseDto

    @GET("search/songs")
    suspend fun getTrendingSongs(
        @Query("query") query: String = "Trending Songs",
        @Query("limit") limit: Int = 30
    ): SaavnSearchResponseDto

    @GET("modules")
    suspend fun getHomeModules(
        @Query("language") language: String = "hindi,punjabi,english"
    ): retrofit2.Response<okhttp3.ResponseBody>
}

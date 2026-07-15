package com.musicstream.app.di

import com.musicstream.app.data.remote.api.MusicApi
import com.musicstream.app.data.remote.api.YouTubeApi
import com.musicstream.app.data.remote.interceptor.PipedInstanceInterceptor
import com.musicstream.app.data.remote.interceptor.SaavnMirrorInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    @Named("baseClient")
    fun provideBaseOkHttpClient(
        pipedInterceptor: PipedInstanceInterceptor,
        saavnInterceptor: SaavnMirrorInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(saavnInterceptor)
            .addInterceptor(pipedInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("downloadClient")
    fun provideDownloadOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMusicApi(@Named("baseClient") okHttpClient: OkHttpClient, moshi: Moshi): MusicApi {
        return Retrofit.Builder()
            .baseUrl("https://saavn.dev/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MusicApi::class.java)
    }

    @Provides
    @Singleton
    fun provideYouTubeApi(@Named("baseClient") okHttpClient: OkHttpClient, moshi: Moshi): YouTubeApi {
        return Retrofit.Builder()
            .baseUrl(YouTubeApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeApi::class.java)
    }
}

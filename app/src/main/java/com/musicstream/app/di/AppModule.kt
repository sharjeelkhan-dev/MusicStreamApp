package com.musicstream.app.di

import android.content.Context
import androidx.room.Room
import com.musicstream.app.data.local.MusicDatabase
import com.musicstream.app.data.local.dao.FavoriteDao
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.musicstream.app.data.local.dao.PlaylistDao
import com.musicstream.app.data.local.dao.SongDao
import com.musicstream.app.data.remote.api.MusicApi
import com.musicstream.app.data.remote.api.YouTubeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        pipedInterceptor: com.musicstream.app.data.remote.interceptor.PipedInstanceInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(pipedInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideMusicApi(okHttpClient: OkHttpClient): MusicApi {
        return Retrofit.Builder()
            .baseUrl("https://saavn.dev/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(MusicApi::class.java)
    }

    @Provides
    @Singleton
    fun provideYouTubeApi(okHttpClient: OkHttpClient): YouTubeApi {
        return Retrofit.Builder()
            .baseUrl(YouTubeApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(YouTubeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_stream_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSongDao(db: MusicDatabase): SongDao = db.songDao()

    @Provides
    fun providePlaylistDao(db: MusicDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideFavoriteDao(db: MusicDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideSearchDao(db: MusicDatabase): com.musicstream.app.data.local.dao.SearchDao = db.searchDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }
}

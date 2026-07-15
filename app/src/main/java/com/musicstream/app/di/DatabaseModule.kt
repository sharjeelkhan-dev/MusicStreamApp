package com.musicstream.app.di

import android.content.Context
import androidx.room.Room
import com.musicstream.app.data.local.MusicDatabase
import com.musicstream.app.data.local.dao.FavoriteDao
import com.musicstream.app.data.local.dao.PlaylistDao
import com.musicstream.app.data.local.dao.SearchDao
import com.musicstream.app.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: MusicDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: MusicDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideSearchDao(database: MusicDatabase): SearchDao {
        return database.searchDao()
    }
}

package com.musicstream.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musicstream.app.data.local.dao.SongDao
import com.musicstream.app.data.local.dao.PlaylistDao
import com.musicstream.app.data.local.dao.FavoriteDao
import com.musicstream.app.data.local.dao.SearchDao
import com.musicstream.app.data.local.entity.SongEntity
import com.musicstream.app.data.local.entity.PlaylistEntity
import com.musicstream.app.data.local.entity.FavoriteEntity
import com.musicstream.app.data.local.entity.RecentlyPlayedEntity
import com.musicstream.app.data.local.entity.PlaylistSongCrossRef
import com.musicstream.app.data.local.entity.SearchHistoryEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        FavoriteEntity::class,
        RecentlyPlayedEntity::class,
        PlaylistSongCrossRef::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchDao(): SearchDao
}

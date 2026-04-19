package com.musicstream.app.data.local.dao

import androidx.room.*
import com.musicstream.app.data.local.entity.PlaylistEntity
import com.musicstream.app.data.local.entity.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("UPDATE playlists SET songCount = songCount + 1 WHERE id = :playlistId")
    suspend fun incrementSongCount(playlistId: String)

    @Query("UPDATE playlists SET songCount = CASE WHEN songCount > 0 THEN songCount - 1 ELSE 0 END WHERE id = :playlistId")
    suspend fun decrementSongCount(playlistId: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Delete
    suspend fun deletePlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)
}

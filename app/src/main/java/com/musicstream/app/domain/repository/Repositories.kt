package com.musicstream.app.domain.repository

import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getFeaturedSong(): Flow<Song>
    fun getTrendingSongs(): Flow<List<Song>>
    fun getRecentlyPlayed(): Flow<List<Song>>
    fun getPlaylists(): Flow<List<Playlist>>
    fun getGenres(): Flow<List<Genre>>
    fun getTrendingSearches(): Flow<List<String>>
    fun searchSongs(query: String): Flow<List<Song>>
    fun getFavorites(): Flow<List<Song>>
    fun getDownloads(): Flow<List<Song>>
    fun getSongById(songId: String): Flow<Song?>
    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>>
    suspend fun toggleFavorite(songId: String)
    suspend fun createPlaylist(name: String)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun deleteAllPlaylists()
    suspend fun addSongToPlaylist(playlistId: String, songId: String)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    suspend fun deleteDownload(songId: String)
    suspend fun addToRecentlyPlayed(song: Song)
    suspend fun downloadSong(song: Song): Flow<DownloadProgress>
}

sealed class DownloadProgress {
    data class Progress(val percent: Int) : DownloadProgress()
    object Completed : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}

interface UserRepository {
    fun getCurrentUser(): Flow<User>
    suspend fun updateUser(user: User)
    suspend fun signOut()
}

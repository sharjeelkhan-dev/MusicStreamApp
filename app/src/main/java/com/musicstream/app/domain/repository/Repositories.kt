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
    suspend fun toggleFavorite(songId: String)
    suspend fun createPlaylist(name: String)
    suspend fun addToRecentlyPlayed(song: Song)
}

interface UserRepository {
    fun getCurrentUser(): Flow<User>
    suspend fun updateUser(user: User)
    suspend fun signOut()
}

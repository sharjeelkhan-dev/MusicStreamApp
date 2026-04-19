package com.musicstream.app.data.repository

import com.musicstream.app.data.MockData
import com.musicstream.app.data.local.dao.FavoriteDao
import com.musicstream.app.data.local.dao.PlaylistDao
import com.musicstream.app.data.local.dao.SongDao
import com.musicstream.app.data.local.entity.FavoriteEntity
import com.musicstream.app.data.local.entity.PlaylistEntity
import com.musicstream.app.data.local.entity.PlaylistSongCrossRef
import com.musicstream.app.data.local.entity.RecentlyPlayedEntity
import com.musicstream.app.data.remote.api.MusicApi
import com.musicstream.app.data.remote.dto.SaavnSongDto
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyList

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val musicApi: MusicApi
) : MusicRepository {

    override fun getFeaturedSong(): Flow<Song> = flow {
        try {
            val response = musicApi.getTrendingSongs(limit = 1)
            val saavnSong = response.data?.results?.firstOrNull()
            if (saavnSong != null) {
                val song = saavnSong.toDomain()
                songDao.insertSong(song.toEntity())
                emit(song)
            } else {
                emit(MockData.featuredSong)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            songDao.getAllSongs().map { it.firstOrNull()?.toDomain() ?: MockData.featuredSong }.let { emitAll(it) }
        }
    }

    override fun getTrendingSongs(): Flow<List<Song>> = flow {
        try {
            val response = musicApi.getTrendingSongs()
            val songs = response.data?.results?.map { it.toDomain() } ?: emptyList()
            if (songs.isNotEmpty()) {
                songDao.insertSongs(songs.map { it.toEntity() })
                emit(songs)
            } else {
                emit(MockData.trendingSongs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            songDao.getAllSongs().map { entities ->
                if (entities.isEmpty()) MockData.trendingSongs else entities.map { it.toDomain() }
            }.let { emitAll(it) }
        }
    }

    override fun getRecentlyPlayed(): Flow<List<Song>> = songDao.getRecentlyPlayed().combine(favoriteDao.getAllFavorites()) { entities, favorites ->
        val favIds = favorites.map { it.songId }.toSet()
        entities.map { it.toDomain().copy(isFavorite = favIds.contains(it.id)) }
    }

    override fun getPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getGenres(): Flow<List<Genre>> = flow {
        emit(MockData.genres) 
    }

    override fun getTrendingSearches(): Flow<List<String>> = flow {
        emit(MockData.trendingSearches)
    }

    override fun searchSongs(query: String): Flow<List<Song>> = flow {
        try {
            val response = musicApi.searchSongs(query)
            val results = response.data?.results?.map { it.toDomain() } ?: emptyList()
            emit(results)
        } catch (e: Exception) {
            e.printStackTrace()
            emitAll(songDao.searchSongs(query).map { entities -> entities.map { it.toDomain() } })
        }
    }

    override fun getFavorites(): Flow<List<Song>> = favoriteDao.getAllFavorites().combine(songDao.getAllSongs()) { favorites, allSongs ->
        val favIds = favorites.map { it.songId }.toSet()
        allSongs.filter { favIds.contains(it.id) }.map { it.toDomain().copy(isFavorite = true) }
    }

    override fun getDownloads(): Flow<List<Song>> = songDao.getAllSongs().map { songs ->
        songs.filter { it.localPath != null }.map { it.toDomain() }
    }

    override fun getSongById(songId: String): Flow<Song?> = flow {
        val localSong = songDao.getSongById(songId)
        emit(localSong?.toDomain())
    }

    override suspend fun toggleFavorite(songId: String) {
        if (favoriteDao.isFavorite(songId)) {
            favoriteDao.removeFavorite(songId)
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(songId = songId))
        }
    }

    override suspend fun createPlaylist(name: String) {
        val id = UUID.randomUUID().toString()
        val gradientIndex = (name.hashCode() and Integer.MAX_VALUE) % 5
        playlistDao.insertPlaylist(
            PlaylistEntity(id = id, name = name, songCount = 0, gradientIndex = gradientIndex)
        )
    }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        playlistDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
        playlistDao.incrementSongCount(playlistId)
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.deletePlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
        playlistDao.decrementSongCount(playlistId)
    }

    override suspend fun addToRecentlyPlayed(song: Song) {
        songDao.insertSong(song.toEntity())
        songDao.insertRecentlyPlayed(
            RecentlyPlayedEntity(songId = song.id)
        )
        songDao.incrementPlayCount(song.id)
    }

    // --- Mappers ---

    private fun SaavnSongDto.toDomain(): Song {
        val highQualImage = image?.find { it.quality == "500x500" }?.link 
            ?: image?.lastOrNull()?.link ?: ""
        
        val highQualDownload = downloadUrl?.find { it.quality == "320kbps" }?.link 
            ?: downloadUrl?.lastOrNull()?.link ?: ""

        val songName = name ?: "Unknown Title"

        return Song(
            id = id,
            title = songName,
            artist = primaryArtists ?: "Unknown Artist",
            album = "",
            // Provide a default duration in case duration comes as null.
            duration = (duration ?: 0) * 1000L,
            coverUrl = highQualImage,
            streamUrl = highQualDownload,
            isFavorite = false,
            gradientIndex = (songName.hashCode() and Integer.MAX_VALUE) % 5,
            playCount = 0L
        )
    }

    private fun com.musicstream.app.data.local.entity.SongEntity.toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        coverUrl = coverUrl,
        streamUrl = streamUrl,
        isFavorite = false,
        gradientIndex = gradientIndex,
        playCount = playCount
    )

    private fun com.musicstream.app.data.local.entity.PlaylistEntity.toDomain() = Playlist(
        id = id,
        name = name,
        songCount = songCount,
        gradientIndex = gradientIndex
    )

    private fun Song.toEntity() = com.musicstream.app.data.local.entity.SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        coverUrl = coverUrl,
        streamUrl = streamUrl,
        playCount = playCount,
        gradientIndex = gradientIndex
    )
}

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val _user = MutableStateFlow(MockData.currentUser)

    override fun getCurrentUser(): Flow<User> = _user

    override suspend fun updateUser(user: User) {
        _user.value = user
    }

    override suspend fun signOut() {
    }
}

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
import com.musicstream.app.domain.repository.DownloadProgress
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.UserRepository
import kotlinx.coroutines.*
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
    private val musicApi: MusicApi,
    private val okHttpClient: okhttp3.OkHttpClient,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : MusicRepository {

    override fun getFeaturedSong(): Flow<Song> = songDao.getAllSongs().combine(favoriteDao.getAllFavorites()) { songs, favorites ->
        val favIds = favorites.map { it.songId }.toSet()
        val featured = songs.firstOrNull()?.toDomain() ?: MockData.featuredSong
        featured.copy(isFavorite = favIds.contains(featured.id))
    }.onStart {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = musicApi.getTrendingSongs(limit = 1)
                response.data?.results?.firstOrNull()?.let { saavnSong ->
                    val remote = saavnSong.toDomain()
                    val local = songDao.getSongById(remote.id)
                    songDao.insertSong(remote.toEntity().copy(localPath = local?.localPath))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun getTrendingSongs(): Flow<List<Song>> = songDao.getAllSongs().combine(favoriteDao.getAllFavorites()) { songs, favorites ->
        val favIds = favorites.map { it.songId }.toSet()
        if (songs.isEmpty()) MockData.trendingSongs else songs.map { it.toDomain().copy(isFavorite = favIds.contains(it.id)) }
    }.onStart {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = musicApi.getTrendingSongs()
                response.data?.results?.let { results ->
                    results.forEach { saavnSong ->
                        val remote = saavnSong.toDomain()
                        val local = songDao.getSongById(remote.id)
                        songDao.insertSong(remote.toEntity().copy(localPath = local?.localPath))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun getRecentlyPlayed(): Flow<List<Song>> = songDao.getRecentlyPlayed().combine(favoriteDao.getAllFavorites()) { entities, favorites ->
        val favIds = favorites.map { it.songId }.toSet()
        entities.map { entity ->
            val domain = entity.toDomain()
            // If the song is already downloaded, it will have a localPath in the 'songs' table
            // However, the getRecentlyPlayed query joins with the songs table, so toDomain() 
            // will already pick up the localPath if it exists.
            domain.copy(isFavorite = favIds.contains(entity.id))
        }
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
            val remoteResults = response.data?.results?.map { it.toDomain() } ?: emptyList()
            
            // Merge with local data
            val allLocal = songDao.getAllSongs().first()
            val allFavs = favoriteDao.getAllFavorites().first().map { it.songId }.toSet()
            
            val mergedResults = remoteResults.map { remote ->
                val local = allLocal.find { it.id == remote.id }
                remote.copy(
                    localPath = local?.localPath,
                    isFavorite = allFavs.contains(remote.id)
                )
            }
            emit(mergedResults)
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

    override fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> =
        playlistDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
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

    override suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun deleteAllPlaylists() {
        playlistDao.deleteAllPlaylists()
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

    override suspend fun deleteDownload(songId: String) {
        val song = songDao.getSongById(songId)
        if (song?.localPath != null) {
            val file = java.io.File(song.localPath)
            if (file.exists()) {
                file.delete()
            }
            songDao.insertSong(song.copy(localPath = null))
        }
    }

    override suspend fun addToRecentlyPlayed(song: Song) {
        songDao.insertSong(song.toEntity())
        songDao.insertRecentlyPlayed(
            RecentlyPlayedEntity(songId = song.id)
        )
        songDao.incrementPlayCount(song.id)
    }

    override suspend fun downloadSong(song: Song): Flow<DownloadProgress> = flow {
        try {
            emit(DownloadProgress.Progress(0))
            val request = okhttp3.Request.Builder().url(song.streamUrl).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful && response.body != null) {
                val totalBytes = response.body!!.contentLength()
                val file = java.io.File(context.filesDir, "downloads/${song.id}.mp3")
                file.parentFile?.mkdirs()
                
                response.body!!.byteStream().use { input ->
                    java.io.FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val percent = ((totalRead * 100) / totalBytes).toInt()
                                emit(DownloadProgress.Progress(percent))
                            }
                        }
                    }
                }
                
                val songEntity = song.toEntity().copy(
                    localPath = file.absolutePath
                )
                songDao.insertSong(songEntity)
                emit(DownloadProgress.Completed)
            } else {
                emit(DownloadProgress.Failed("Response failed"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(DownloadProgress.Failed(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

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
        localPath = localPath,
        isFavorite = false, // This will be updated by the combine/merge logic
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
        localPath = localPath,
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

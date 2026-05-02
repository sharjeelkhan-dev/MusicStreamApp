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
import com.musicstream.app.data.remote.api.YouTubeApi
import com.musicstream.app.data.remote.dto.SaavnSongDto
import com.musicstream.app.data.remote.dto.YouTubeSearchItemDto
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.repository.DownloadProgress
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.UserRepository
import com.musicstream.app.domain.repository.SettingsRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.emptyList
import androidx.core.net.toUri

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val musicApi: MusicApi,
    private val youTubeApi: YouTubeApi,
    private val okHttpClient: okhttp3.OkHttpClient,
    @param:dagger.hilt.android.qualifiers.ApplicationContext
    private val context: android.content.Context
) : MusicRepository {

    override fun getFeaturedSong(): Flow<Song> = 
        songDao.getRecentlyPlayed()
            .combine(favoriteDao.getAllFavorites()) { recentlyPlayed, favorites ->
                val favIds = favorites.map { it.songId }.toSet()
                val latest = recentlyPlayed.firstOrNull()?.toDomain() ?: MockData.featuredSong
                latest.copy(isFavorite = favIds.contains(latest.id))
            }

    override fun getTrendingSongs(): Flow<List<Song>> = flow {
        val allFavs = favoriteDao.getAllFavorites().first().map { it.songId }.toSet()
        val allLocal = songDao.getAllSongs().first()
        val results = mutableListOf<Song>()

        // 1. Try Saavn Trending (Global/Region specific charts)
        try {
            val saavnResponse = musicApi.getTrendingSongs()
            val saavnResults = saavnResponse.data?.results?.map { it.toDomain() } ?: emptyList()
            results.addAll(saavnResults)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try YouTube Trending (Real-time viral hits)
        try {
            val ytTrending = youTubeApi.getTrending()
            val ytResults = ytTrending.filter { it.type == "stream" }.map { it.toDomain() }
            
            val existingNames = results.map { "${it.title.lowercase()}${it.artist.lowercase()}" }.toSet()
            val uniqueYtResults = ytResults.filter { 
                "${it.title.lowercase()}${it.artist.lowercase()}" !in existingNames
            }
            results.addAll(uniqueYtResults)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (results.isNotEmpty()) {
            val merged = results.map { remote ->
                val local = allLocal.find { it.id == remote.id }
                remote.copy(
                    localPath = local?.localPath,
                    isFavorite = allFavs.contains(remote.id)
                )
            }
            emit(merged)
        } else {
            emit(MockData.trendingSongs)
        }
    }.flowOn(Dispatchers.IO)

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
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        
        val allFavs = favoriteDao.getAllFavorites().first().map { it.songId }.toSet()
        val allLocal = songDao.getAllSongs().first()

        val results = mutableListOf<Song>()
        
        // 1. Try Saavn first for high-quality official studio tracks
        try {
            val saavnResponse = musicApi.searchSongs(query, limit = 15)
            val saavnResults = saavnResponse.data?.results?.map { it.toDomain() } ?: emptyList()
            results.addAll(saavnResults)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try YouTube for everything else (covers, remixes, rare songs)
        try {
            val ytResponse = youTubeApi.search(query)
            val ytResults = ytResponse.items?.filter { it.type == "stream" }?.map { it.toDomain() } ?: emptyList()
            
            // Avoid duplicates (simplified check by title + artist)
            val existingNames = results.map { "${it.title.lowercase()}${it.artist.lowercase()}" }.toSet()
            val uniqueYtResults = ytResults.filter { 
                "${it.title.lowercase()}${it.artist.lowercase()}" !in existingNames
            }
            results.addAll(uniqueYtResults)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (results.isNotEmpty()) {
            // Merge with local download status and favorites
            val mergedResults = results.map { remote ->
                val local = allLocal.find { it.id == remote.id }
                remote.copy(
                    localPath = local?.localPath,
                    isFavorite = allFavs.contains(remote.id)
                )
            }
            emit(mergedResults)
        } else {
            // Last resort: search local DB
            emitAll(songDao.searchSongs(query).map { entities -> 
                entities.map { it.toDomain().copy(isFavorite = allFavs.contains(it.id)) } 
            })
        }
    }.flowOn(Dispatchers.IO)

    override fun getFavorites(): Flow<List<Song>> =
        favoriteDao.getAllFavorites()
            .combine(songDao
                .getAllSongs())
            { favorites, allSongs ->
        val favIds = favorites.map { it.songId }.toSet()
        allSongs.filter { favIds.contains(it.id) }.map { it.toDomain().copy(isFavorite = true) }
    }

    override fun getDownloads(): Flow<List<Song>> = songDao.getAllSongs().map { songs ->
        songs.filter { it.localPath != null }.map { it.toDomain() }
    }

    override fun getSongById(songId: String): Flow<Song?> = flow {
        // First check local DB
        val localSong = songDao.getSongById(songId)?.toDomain()
        
        // If it's a YouTube placeholder, resolve it
        if (localSong?.streamUrl?.startsWith("youtube://") == true || songId.length == 11) {
            try {
                val videoId = if (localSong?.streamUrl?.startsWith("youtube://") == true) {
                    localSong.streamUrl.substringAfter("youtube://")
                } else songId
                
                val streamInfo = youTubeApi.getStreamInfo(videoId)
                val audioUrl = streamInfo.audioStreams?.maxByOrNull { it.bitrate ?: 0 }?.url
                
                if (audioUrl != null) {
                    emit((localSong ?: Song(id = videoId, title = "YouTube Song", artist = "YouTube")).copy(streamUrl = audioUrl))
                } else {
                    emit(localSong)
                }
            } catch (_: Exception) {
                emit(localSong)
            }
        } else {
            emit(localSong)
        }
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
            PlaylistEntity(id = id, name = name,
                songCount = 0,
                gradientIndex = gradientIndex)
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
            
            // Get user's preferred audio quality

            // Try to find the URL for the selected quality if available in the DTO or meta
            // For now we use streamUrl which is already set to 320kbps in mapping if available
            // In a real app we might need to re-fetch or use a different property

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

    private fun YouTubeSearchItemDto.toDomain(): Song {
        val id = videoId ?: ""
        val songName = title ?: "Unknown"
        return Song(
            id = id,
            title = songName,
            artist = uploaderName ?: "Unknown Artist",
            coverUrl = thumbnail ?: "",
            // Use a specific pattern for YouTube streams that we can resolve later
            streamUrl = "youtube://$id",
            duration = (duration ?: 0) * 1000L,
            gradientIndex = (songName.hashCode() and Integer.MAX_VALUE) % 5
        )
    }

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

    private fun PlaylistEntity.toDomain() = Playlist(
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
class UserRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val dataStore: DataStore<Preferences>,
    @param:dagger.hilt.android.qualifiers.ApplicationContext
    private val context: android.content.Context // Context added for file saving
) : UserRepository {

    private object UserKeys {
        val ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val AVATAR = stringPreferencesKey("user_avatar")
        val BANNER = stringPreferencesKey("user_banner")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val REGISTERED_EMAILS = stringSetPreferencesKey("registered_emails")
    }

    // Direct flow from DataStore so UI updates automatically when data changes
    override fun getCurrentUser(): Flow<User> = dataStore.data.map { preferences ->
        User(
            id = preferences[UserKeys.ID] ?: UUID.randomUUID().toString(),
            name = preferences[UserKeys.NAME] ?: "Guest User",
            email = preferences[UserKeys.EMAIL] ?: "",
            avatarUrl = preferences[UserKeys.AVATAR] ?: "",
            bannerUrl = preferences[UserKeys.BANNER] ?: ""
        )
    }

    override fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[UserKeys.IS_LOGGED_IN] ?: false
    }

    override suspend fun isEmailRegistered(email: String): Boolean {
        val preferences = dataStore.data.first()
        val emails = preferences[UserKeys.REGISTERED_EMAILS] ?: emptySet()
        return emails.contains(email)
    }

    override suspend fun updateUser(user: User) {
        dataStore.edit { preferences ->
            // Pehle check karein ke kya hamare paas pehle se koi Avatar ya Banner save hai
            val existingAvatar = preferences[UserKeys.AVATAR] ?: ""
            val existingBanner = preferences[UserKeys.BANNER] ?: ""

            // Agar naya user object khali hai (Login ke waqt aksar hota hai),
            // toh purana wala hi use karein.
            val finalAvatar = if (user.avatarUrl.isEmpty()) existingAvatar else {
                if (user.avatarUrl.startsWith("content://")) {
                    saveImageToInternal(user.avatarUrl, "avatar_${user.id}.jpg")
                } else user.avatarUrl
            }

            val finalBanner = if (user.bannerUrl.isEmpty()) existingBanner else {
                if (user.bannerUrl.startsWith("content://")) {
                    saveImageToInternal(user.bannerUrl, "banner_${user.id}.jpg")
                } else user.bannerUrl
            }

            // Data save karein
            preferences[UserKeys.ID] = user.id
            preferences[UserKeys.NAME] = user.name
            preferences[UserKeys.EMAIL] = user.email
            preferences[UserKeys.AVATAR] = finalAvatar
            preferences[UserKeys.BANNER] = finalBanner
            preferences[UserKeys.IS_LOGGED_IN] = true

            val currentEmails = preferences[UserKeys.REGISTERED_EMAILS] ?: emptySet()
            preferences[UserKeys.REGISTERED_EMAILS] = currentEmails + user.email
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            // Hum favorites aur playlists delete kar rahe hain as per your original code
            favoriteDao.deleteAllFavorites()
            playlistDao.deleteAllPlaylists()

            // CRITICAL: Hum sirf Login status remove karenge.
            // Name, Email, Avatar aur Banner ko remove nahi karenge taake wo "Hamesha Save" rahe.
            dataStore.edit { preferences ->
                preferences[UserKeys.IS_LOGGED_IN] = false
                // ID, NAME, EMAIL, AVATAR, BANNER ko delete nahi kiya taake persistence bani rahe
            }
        }
    }

    // Helper function to save image permanently
    private fun saveImageToInternal(uriString: String, fileName: String): String {
        return try {
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = java.io.File(context.filesDir, fileName)

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath // Ab ye permanent path hai
        } catch (e: Exception) {
            e.printStackTrace()
            uriString // Error par wapis wahi bhej do
        }
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val LANGUAGE = stringPreferencesKey("language")
        val EQUALIZER = stringPreferencesKey("equalizer")
    }

    override fun getAudioQuality(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUDIO_QUALITY] ?: "High (320kbps)"
    }

    override suspend fun setAudioQuality(quality: String) {
        dataStore.edit { it[PreferencesKeys.AUDIO_QUALITY] = quality }
    }

    override fun getTheme(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME] ?: "System Default"
    }

    override suspend fun setTheme(theme: String) {
        dataStore.edit { it[PreferencesKeys.THEME] = theme }
    }

    override fun getNotificationsEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS] ?: true
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATIONS] = enabled }
    }

    override fun getLanguage(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LANGUAGE] ?: "English"
    }

    override suspend fun setLanguage(language: String) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE] = language }
    }

    override fun getEqualizerPreset(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EQUALIZER] ?: "Custom"
    }

    override suspend fun setEqualizerPreset(preset: String) {
        dataStore.edit { it[PreferencesKeys.EQUALIZER] = preset }
    }
}

package com.musicstream.app.data.repository
import com.musicstream.app.data.MockData
import com.musicstream.app.data.local.dao.FavoriteDao
import com.musicstream.app.data.local.dao.PlaylistDao
import com.musicstream.app.data.local.dao.SearchDao
import com.musicstream.app.data.local.dao.SongDao
import com.musicstream.app.data.local.entity.FavoriteEntity
import com.musicstream.app.data.local.entity.PlaylistEntity
import com.musicstream.app.data.local.entity.PlaylistSongCrossRef
import com.musicstream.app.data.local.entity.RecentlyPlayedEntity
import com.musicstream.app.data.local.entity.SearchHistoryEntity
import com.musicstream.app.data.local.entity.SongEntity
import com.musicstream.app.data.remote.api.MusicApi
import com.musicstream.app.data.remote.api.YouTubeApi
import com.musicstream.app.data.remote.dto.SaavnDownloadUrlDto
import com.musicstream.app.data.remote.dto.SaavnImageDto
import com.musicstream.app.data.remote.dto.SaavnSongDto
import com.musicstream.app.data.remote.extractor.YouTubeExtractor
import com.musicstream.app.data.remote.interceptor.PipedInstanceInterceptor
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
    private val searchDao: SearchDao,
    private val musicApi: MusicApi,
    private val youTubeApi: YouTubeApi,
    private val youTubeExtractor: YouTubeExtractor,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val pipedInterceptor: PipedInstanceInterceptor,
    @param:dagger.hilt.android.qualifiers.ApplicationContext
    private val context: android.content.Context,
) : MusicRepository {

    override fun getFeaturedSong(): Flow<Song> = 
        songDao.getRecentlyPlayed()
            .combine(favoriteDao.getAllFavorites()) { recentlyPlayed, favorites ->
                val favIds = favorites.asSequence().map { it.songId }.toSet()
                val latest = recentlyPlayed.firstOrNull()?.toDomain() ?: Song(
                    id = "initial_discover",
                    title = "Discover New Music",
                    artist = "Start Playing",
                    coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800"
                )
                latest.copy(isFavorite = favIds.contains(latest.id))
            }

    override fun getTrendingSongs(query: String): Flow<List<Song>> {
        val remoteFlow = flow {
            var trendingResults = emptyList<Song>()
            
            android.util.Log.d("MusicRepo", "--- TRENDING FETCH START ---")
            // Try Saavn Trending (/modules endpoint)
            try {
                val response = withTimeoutOrNull(10000) { musicApi.getTrending() }
                android.util.Log.d("MusicRepo", "Modules Response raw data: ${response?.data}")

                // Manual extraction for polymorphism
                val rawData = response?.data
                val songs = when (rawData) {
                    is Map<*, *> -> {
                        // case: data -> trending -> songs
                        val trending = rawData["trending"] as? Map<*, *>
                        val results = trending?.get("songs") as? List<*>
                        results?.mapNotNull { if (it is Map<*, *>) parseSaavnSongFromMap(it) else null }
                    }
                    is List<*> -> {
                        // case: data is a direct list
                        rawData.mapNotNull { if (it is Map<*, *>) parseSaavnSongFromMap(it) else null }
                    }
                    else -> null
                }

                if (!songs.isNullOrEmpty()) {
                    trendingResults = songs.map { it.toDomain() }
                    android.util.Log.d("MusicRepo", "Saavn Trending Success: ${trendingResults.size} songs")
                    emit(trendingResults)
                } else {
                    android.util.Log.w("MusicRepo", "Saavn modules parsing returned null or empty")
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicRepo", "Saavn Trending Exception: ${e.message}", e)
            }

            // Supplement with YouTube
            try {
                android.util.Log.d("MusicRepo", "Trying YouTube Trending fallback...")
                val ytResults = withTimeoutOrNull(10000) {
                    youTubeExtractor.search("Latest Indian Trending Songs 2026")
                }
                if (!ytResults.isNullOrEmpty()) {
                    trendingResults = (trendingResults + ytResults).distinctBy { it.id }
                    android.util.Log.d("MusicRepo", "YouTube Trending Success: ${ytResults.size} songs")
                    emit(trendingResults)
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicRepo", "YouTube Trending Exception: ${e.message}", e)
            }

            if (trendingResults.isEmpty()) {
                android.util.Log.d("MusicRepo", "All Trending sources failed, using Mock Data")
                emit(MockData.trendingSongs)
            }
        }

        // CRITICAL FIX: Single parallel combine with onStart backup to break database deadlock
        return combine(
            remoteFlow,
            songDao.getAllSongs().onStart { emit(emptyList()) },
            favoriteDao.getAllFavorites().onStart { emit(emptyList()) }
        ) { remote, local, favorites ->
            val favIds = favorites.map { it.songId }.toSet()
            remote.map { song ->
                val localMatch = local.find { it.id == song.id }
                song.copy(
                    localPath = localMatch?.localPath,
                    isFavorite = favIds.contains(song.id)
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRecentlyPlayed(): Flow<List<Song>> = songDao.getRecentlyPlayed().combine(favoriteDao.getAllFavorites()) { entities, favorites ->
        val favIds = favorites.map { it.songId }.toSet()
        entities.map { entity ->
            val domain = entity.toDomain()
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

    override fun getSearchHistory(): Flow<List<String>> = 
        searchDao.getSearchHistory().map { entities -> entities.map { it.query } }

    override suspend fun addSearchHistory(query: String) {
        if (query.isNotBlank()) {
            searchDao.insertSearchHistory(SearchHistoryEntity(query = query))
        }
    }

    override suspend fun deleteSearchHistory(query: String) {
        searchDao.deleteSearchHistory(query)
    }

    override suspend fun clearSearchHistory() {
        searchDao.clearSearchHistory()
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        if (query.isBlank()) return flowOf(emptyList())

        val remoteFlow = flow {
            coroutineScope {
                android.util.Log.d("MusicRepo", "Starting Search for: $query")
                val saavnDeferred = async {
                    try {
                        withTimeoutOrNull(8000) {
                            val response = musicApi.searchSongs(query, limit = 50)
                            android.util.Log.d("MusicRepo", "Saavn Search raw data type: ${response.data?.javaClass?.name}")

                            // Manual extraction to handle polymorphisim/nested issues
                            val resultsList = when (val data = response.data) {
                                is Map<*, *> -> {
                                    (data["results"] as? List<*>)?.mapNotNull { item ->
                                        if (item is Map<*, *>) {
                                            // Handle Map to DTO if Moshi didn't parse nested objects
                                            parseSaavnSongFromMap(item)
                                        } else item as? SaavnSongDto
                                    } ?: emptyList()
                                }
                                is List<*> -> data.filterIsInstance<SaavnSongDto>()
                                else -> emptyList()
                            }

                            resultsList.map { it.toDomain() }.also {
                                android.util.Log.d("MusicRepo", "Saavn Search found: ${it.size} songs")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicRepo", "Saavn Search Failed: ${e.message}", e)
                        null
                    }
                }

                val ytDeferred = async {
                    try {
                        withTimeoutOrNull(12000) {
                            youTubeExtractor.search(query).also {
                                android.util.Log.d("MusicRepo", "YouTube Search found: ${it.size} songs")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicRepo", "YouTube Search Failed: ${e.message}", e)
                        null
                    }
                }

                val currentResults = mutableListOf<Song>()

                val saavnRes = saavnDeferred.await()
                if (!saavnRes.isNullOrEmpty()) {
                    currentResults.addAll(saavnRes)
                    emit(currentResults.toList())
                }

                val ytRes = ytDeferred.await()
                if (!ytRes.isNullOrEmpty()) {
                    val combined = (currentResults + ytRes).distinctBy { it.id }
                    if (combined.size > currentResults.size) {
                        emit(combined)
                    }
                }

                if (currentResults.isEmpty() && ytRes.isNullOrEmpty()) {
                    android.util.Log.w("MusicRepo", "All search sources failed for: $query")
                    // Final fallback to local
                    val localResults = songDao.searchSongs(query).first().map { it.toDomain() }
                    emit(localResults)
                }
            }
        }

        // CRITICAL FIX: Single parallel combine with onStart backup to break database deadlock
        return combine(
            remoteFlow,
            songDao.getAllSongs().onStart { emit(emptyList()) },
            favoriteDao.getAllFavorites().onStart { emit(emptyList()) }
        ) { remote, local, favorites ->
            val favIds = favorites.map { it.songId }.toSet()
            remote.map { song ->
                val localMatch = local.find { it.id == song.id }
                song.copy(
                    localPath = localMatch?.localPath,
                    isFavorite = favIds.contains(song.id)
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun parseSaavnSongFromMap(map: Map<*, *>): SaavnSongDto {
        android.util.Log.d("MusicRepo", "Manually parsing map: $map")
        val id = (map["id"] as? String) ?: (map["songid"] as? String) ?: ""
        val name = (map["name"] as? String) ?: (map["songname"] as? String) ?: (map["title"] as? String)
        val artists = (map["primaryArtists"] as? String) ?: (map["singers"] as? String) ?: (map["artist"] as? String)
        
        // Handle images
        val imageList = (map["image"] as? List<*>)?.mapNotNull { img ->
            when (img) {
                is Map<*, *> -> SaavnImageDto((img["quality"] as? String) ?: "", (img["link"] as? String) ?: "")
                is String -> SaavnImageDto("unknown", img)
                else -> null
            }
        } ?: listOfNotNull(
            (map["image"] as? String)?.let { SaavnImageDto("high", it) }
        )

        // Handle download URLs
        val downloadList = (map["downloadUrl"] as? List<*>)?.mapNotNull { url ->
            when (url) {
                is Map<*, *> -> SaavnDownloadUrlDto((url["quality"] as? String) ?: "", (url["link"] as? String) ?: "")
                is String -> SaavnDownloadUrlDto("high", url)
                else -> null
            }
        } ?: listOfNotNull(
            (map["download_url"] as? String)?.let { SaavnDownloadUrlDto("high", it) },
            (map["media_url"] as? String)?.let { SaavnDownloadUrlDto("high", it) }
        )

        val duration = when (val dur = map["duration"]) {
            is Number -> dur.toInt()
            is String -> dur.toIntOrNull()
            else -> null
        }

        return SaavnSongDto(
            id = id,
            name = name,
            primaryArtists = artists,
            duration = duration,
            image = imageList.takeIf { it.isNotEmpty() },
            downloadUrl = downloadList.takeIf { it.isNotEmpty() }
        )
    }

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
        val localSong = songDao.getSongById(songId)?.toDomain()
        
        val isYouTube = localSong?.streamUrl?.startsWith("youtube://") == true || (localSong == null && songId.length == 11)
        
        if (isYouTube) {
            val videoId = if (localSong?.streamUrl?.startsWith("youtube://") == true) {
                localSong.streamUrl.substringAfter("youtube://")
            } else songId
            
            android.util.Log.d("MusicRepo", "Resolving YouTube URL for: $videoId")
            val audioUrl = try {
                youTubeExtractor.getAudioUrl(videoId)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("MusicRepo", "YouTube resolution failed: ${e.message}")
                null
            }

            if (audioUrl != null) {
                android.util.Log.d("MusicRepo", "YouTube resolution success: $audioUrl")
                emit((localSong ?: Song(id = videoId, title = "YouTube Song", artist = "YouTube")).copy(streamUrl = audioUrl))
            } else {
                android.util.Log.w("MusicRepo", "YouTube resolution returned null")
                // If it's a youtube URI, don't emit it to the player as is
                if (localSong?.streamUrl?.startsWith("youtube://") == true) {
                    emit(localSong.copy(streamUrl = ""))
                } else {
                    emit(localSong)
                }
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
            if (file.exists()) file.delete()
            songDao.insertSong(song.copy(localPath = null))
        }
    }

    override suspend fun addToRecentlyPlayed(song: Song) {
        songDao.insertSong(song.toEntity())
        songDao.insertRecentlyPlayed(RecentlyPlayedEntity(songId = song.id))
        songDao.incrementPlayCount(song.id)
    }

    override suspend fun addLocalSong(song: Song) {
        songDao.insertSong(song.toEntity())
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
                val songEntity = song.toEntity().copy(localPath = file.absolutePath)
                songDao.insertSong(songEntity)
                emit(DownloadProgress.Completed)
            } else emit(DownloadProgress.Failed("Response failed"))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(DownloadProgress.Failed(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    // --- Mappers ---

    private fun SaavnSongDto.toDomain(): Song {
        val rawImage = image?.find { it.quality == "500x500" }?.link
            ?: image?.find { it.quality == "150x150" }?.link
            ?: image?.lastOrNull()?.link ?: ""
        
        var secureImage = rawImage.replace("http://", "https://").trim()
        
        // Upgrade image quality if it's a known pattern but smaller size
        if (secureImage.contains("saavncdn.com") || secureImage.contains("cdn-images")) {
            if (secureImage.contains("150x150")) {
                secureImage = secureImage.replace("150x150", "500x500")
            } else if (secureImage.contains("50x50")) {
                secureImage = secureImage.replace("50x50", "500x500")
            }
        }

        val highQualDownload = downloadUrl?.find { it.quality == "320kbps" }?.link
            ?: downloadUrl?.lastOrNull()?.link ?: ""
        val songName = name?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown Track"
        val artistName = primaryArtists?.trim()?.takeIf { it.isNotBlank() } ?: "Various Artists"
        return Song(
            id = id,
            title = songName,
            artist = artistName,
            album = "",
            duration = (duration ?: 0) * 1000L,
            coverUrl = secureImage,
            streamUrl = highQualDownload,
            isFavorite = false,
            gradientIndex = (songName.hashCode() and Integer.MAX_VALUE) % 5,
            playCount = 0L
        )
    }

    private fun SongEntity.toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        coverUrl = coverUrl,
        streamUrl = streamUrl,
        localPath = localPath,
        isFavorite = false,
        gradientIndex = gradientIndex,
        playCount = playCount
    )

    private fun PlaylistEntity.toDomain() = Playlist(
        id = id,
        name = name,
        songCount = songCount,
        gradientIndex = gradientIndex
    )

    private fun Song.toEntity() = SongEntity(
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
    private val context: android.content.Context
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
            val existingAvatar = preferences[UserKeys.AVATAR] ?: ""
            val existingBanner = preferences[UserKeys.BANNER] ?: ""
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
            favoriteDao.deleteAllFavorites()
            playlistDao.deleteAllPlaylists()
            dataStore.edit { preferences ->
                preferences[UserKeys.IS_LOGGED_IN] = false
            }
        }
    }

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
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uriString
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
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        fun bandKey(band: Int) = intPreferencesKey("eq_band_$band")
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
        preferences[PreferencesKeys.EQUALIZER] ?: "Flat"
    }

    override suspend fun setEqualizerPreset(preset: String) {
        dataStore.edit { it[PreferencesKeys.EQUALIZER] = preset }
    }

    override fun getBassBoostLevel(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BASS_BOOST] ?: 0
    }

    override suspend fun setBassBoostLevel(level: Int) {
        dataStore.edit { it[PreferencesKeys.BASS_BOOST] = level }
    }

    override fun getVirtualizerLevel(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIRTUALIZER] ?: 0
    }

    override suspend fun setVirtualizerLevel(level: Int) {
        dataStore.edit { it[PreferencesKeys.VIRTUALIZER] = level }
    }

    override fun getEqualizerBandLevels(): Flow<Map<Int, Int>> = dataStore.data.map { preferences ->
        val map = mutableMapOf<Int, Int>()
        for (i in 0 until 10) {
            preferences[PreferencesKeys.bandKey(i)]?.let { map[i] = it }
        }
        map
    }

    override suspend fun setEqualizerBandLevel(band: Int, level: Int) {
        dataStore.edit { it[PreferencesKeys.bandKey(band)] = level }
    }
}

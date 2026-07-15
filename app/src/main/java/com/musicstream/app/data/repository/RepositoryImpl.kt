package com.musicstream.app.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.musicstream.app.data.MockData
import com.musicstream.app.data.local.MusicDatabase
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
import com.musicstream.app.domain.repository.SettingsRepository
import com.musicstream.app.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val searchDao: SearchDao,
    private val musicApi: MusicApi,
    private val youTubeApi: YouTubeApi,
    private val youTubeExtractor: YouTubeExtractor,
    @Named("downloadClient")
    private val downloadHttpClient: OkHttpClient,
    private val pipedInterceptor: PipedInstanceInterceptor,
    @ApplicationContext private val context: Context,
) : MusicRepository {

    fun getContext() = context

    private val _downloadingSongs = MutableStateFlow<Map<String, Int>>(emptyMap())
    override fun getDownloadingSongs(): Flow<Map<String, Int>> = _downloadingSongs.asStateFlow()

    override fun getFeaturedSong(): Flow<Song> = 
        songDao.getRecentlyPlayed()
            .combine(favoriteDao.getAllFavorites()) { recentlyPlayed, favorites ->
                val favIds = favorites.map { it.songId }.toSet()
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
            
            // 1. Try Saavn via Interceptor (Mirror rotation handled)
            try {
                val response = withTimeoutOrNull(15000) { musicApi.getTrending() }
                val rawData = response?.data
                val songs = when (rawData) {
                    is Map<*, *> -> {
                        // Check for TrendingModuleDto structure
                        val trending = rawData["trending"] as? Map<*, *>
                        val results = trending?.get("songs") as? List<*>
                        results?.mapNotNull { if (it is Map<*, *>) parseSaavnSongFromMap(it) else null }
                    }
                    is List<*> -> {
                        // Sometimes data is directly a list of songs
                        rawData.mapNotNull { if (it is Map<*, *>) parseSaavnSongFromMap(it) else null }
                    }
                    else -> null
                }
                
                if (!songs.isNullOrEmpty()) {
                    trendingResults = songs.map { it.toDomain() }
                    emit(trendingResults)
                }
            } catch (e: Exception) { 
                Log.e("MusicRepo", "Saavn Trending Failed: ${e.message}") 
            }

            // 2. Direct YouTube Scraping Fallback
            try {
                val ytResults = withTimeoutOrNull(20000) { youTubeExtractor.search(query.ifBlank { "Top Hits 2026" }) }
                if (!ytResults.isNullOrEmpty()) {
                    trendingResults = (trendingResults + ytResults).distinctBy { it.id }
                    emit(trendingResults)
                }
            } catch (e: Exception) { 
                Log.e("MusicRepo", "YT Trending Failed: ${e.message}") 
            }

            // 3. Final Mock Fallback (NEVER show blank UI)
            if (trendingResults.isEmpty()) {
                Log.w("MusicRepo", "All Trending methods failed, using Mock data")
                emit(MockData.trendingSongs)
            }
        }

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
        entities.map { it.toDomain().copy(isFavorite = favIds.contains(it.id)) }
    }

    override fun getPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities -> entities.map { it.toDomain() } }
    override fun getGenres(): Flow<List<Genre>> = flow { emit(MockData.genres) }
    override fun getTrendingSearches(): Flow<List<String>> = flow { emit(MockData.trendingSearches) }
    override fun getSearchHistory(): Flow<List<String>> = searchDao.getSearchHistory().map { entities -> entities.map { it.query } }
    override suspend fun addSearchHistory(query: String) { if (query.isNotBlank()) searchDao.insertSearchHistory(SearchHistoryEntity(query = query)) }
    override suspend fun deleteSearchHistory(query: String) = searchDao.deleteSearchHistory(query)
    override suspend fun clearSearchHistory() = searchDao.clearSearchHistory()

    override fun searchSongs(query: String): Flow<List<Song>> {
        if (query.isBlank()) return flowOf(emptyList())
        val remoteFlow = flow {
            coroutineScope {
                val saavnDeferred = async {
                    try {
                        withTimeoutOrNull(12000) {
                            val response = musicApi.searchSongs(query, limit = 50)
                            val data = response.data
                            val resultsList = when (data) {
                                is Map<*, *> -> {
                                    val results = (data["results"] as? List<*>)
                                    if (results != null) {
                                        results.mapNotNull { item -> 
                                            if (item is Map<*, *>) parseSaavnSongFromMap(item) else null 
                                        }
                                    } else {
                                        // Some API versions return data directly as a map with results
                                        val subResults = (data["data"] as? Map<*, *>)?.get("results") as? List<*>
                                        subResults?.mapNotNull { item ->
                                             if (item is Map<*, *>) parseSaavnSongFromMap(item) else null
                                        } ?: emptyList()
                                    }
                                }
                                is List<*> -> data.mapNotNull { if (it is Map<*, *>) parseSaavnSongFromMap(it) else null }
                                else -> emptyList()
                            }
                            resultsList.map { it.toDomain() }
                        }
                    } catch (e: Exception) { null }
                }
                val ytDeferred = async { 
                    try { 
                        withTimeoutOrNull(20000) { 
                            val results = youTubeExtractor.search(query) 
                            if (results.isEmpty()) {
                                // Fallback to Piped Search API
                                val pipedResponse = youTubeApi.search(query, filter = "music")
                                pipedResponse.items?.mapNotNull { item ->
                                    val videoId = item.videoId ?: return@mapNotNull null
                                    Song(
                                        id = videoId,
                                        title = item.title ?: "Unknown",
                                        artist = item.uploaderName ?: "YouTube",
                                        coverUrl = item.thumbnail ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                                        streamUrl = "youtube://$videoId",
                                        duration = (item.duration ?: 0) * 1000L,
                                        gradientIndex = (item.title.hashCode() and Integer.MAX_VALUE) % 5
                                    )
                                }
                            } else results
                        } 
                    } catch (e: Exception) { null } 
                }

                val currentResults = mutableListOf<Song>()
                val saavnRes = saavnDeferred.await()
                if (!saavnRes.isNullOrEmpty()) { currentResults.addAll(saavnRes); emit(currentResults.toList()) }
                val ytRes = ytDeferred.await()
                if (!ytRes.isNullOrEmpty()) { val combined = (currentResults + ytRes).distinctBy { it.id }; emit(combined) }
                
                // If everything fails, show local DB search results
                if (currentResults.isEmpty() && ytRes.isNullOrEmpty()) {
                    val localResults = songDao.searchSongs(query).first().map { it.toDomain() }
                    if (localResults.isNotEmpty()) {
                        emit(localResults)
                    } else {
                        // LAST RESORT: Mock results based on query
                        emit(MockData.trendingSongs.filter { it.title.contains(query, ignoreCase = true) })
                    }
                }
            }
        }
        return combine(remoteFlow, songDao.getAllSongs().onStart { emit(emptyList()) }, favoriteDao.getAllFavorites().onStart { emit(emptyList()) }) { remote, local, favorites ->
            val favIds = favorites.map { it.songId }.toSet()
            remote.map { song -> val localMatch = local.find { it.id == song.id }; song.copy(localPath = localMatch?.localPath, isFavorite = favIds.contains(song.id)) }
        }.flowOn(Dispatchers.IO)
    }

    private fun parseSaavnSongFromMap(map: Map<*, *>): SaavnSongDto {
        val id = (map["id"] as? String) ?: (map["songid"] as? String) ?: ""
        val name = (map["name"] as? String) ?: (map["songname"] as? String) ?: (map["title"] as? String) ?: "Unknown"
        val artists = (map["primaryArtists"] as? String) ?: (map["singers"] as? String) ?: (map["artist"] as? String) ?: "Various Artists"
        val imageList = (map["image"] as? List<*>)?.mapNotNull { img -> when (img) { is Map<*, *> -> SaavnImageDto((img["quality"] as? String) ?: "", (img["link"] as? String) ?: ""); is String -> SaavnImageDto("high", img); else -> null } } ?: listOfNotNull((map["image"] as? String)?.let { SaavnImageDto("high", it) })
        val downloadList = (map["downloadUrl"] as? List<*>)?.mapNotNull { url -> when (url) { is Map<*, *> -> SaavnDownloadUrlDto((url["quality"] as? String) ?: "", (url["link"] as? String) ?: ""); is String -> SaavnDownloadUrlDto("high", url); else -> null } } ?: listOfNotNull((map["download_url"] as? String)?.let { SaavnDownloadUrlDto("high", it) }, (map["media_url"] as? String)?.let { SaavnDownloadUrlDto("high", it) })
        val duration = when (val dur = map["duration"]) { is Number -> dur.toInt(); is String -> dur.toIntOrNull(); else -> 0 }
        return SaavnSongDto(id = id, name = name, primaryArtists = artists, duration = duration, image = imageList, downloadUrl = downloadList)
    }

    override fun getFavorites(): Flow<List<Song>> = favoriteDao.getAllFavorites().combine(songDao.getAllSongs()) { favorites, allSongs ->
        val favIds = favorites.map { it.songId }.toSet()
        allSongs.filter { favIds.contains(it.id) }.map { it.toDomain().copy(isFavorite = true) }
    }
    override fun getDownloads(): Flow<List<Song>> = songDao.getAllSongs().map { songs -> songs.filter { it.localPath != null }.map { it.toDomain() } }

    override fun getSongById(songId: String): Flow<Song?> = flow {
        // 1. Get from DB immediately
        val localSong = songDao.getSongById(songId)?.toDomain()
        
        // 2. Priority 1: Valid Offline File
        if (localSong?.localPath != null) {
            val file = File(localSong.localPath!!)
            if (file.exists() && file.length() > 0) {
                Log.d("MusicRepo", "Found valid offline file for $songId")
                emit(localSong)
                return@flow
            }
        }
        
        // 3. Online Resolution Flow
        // Emission 1: Placeholder from DB (if exists)
        if (localSong != null) emit(localSong)

        // 4. Force Resolve Fresh Playable Link with Rapid Mirror Rotation
        Log.d("MusicRepo", "Rapid Mirror Resolution for $songId...")
        
        var resolved: Song? = null
        
        // Attempt 1: Normal resolution
        resolved = try { 
            withTimeoutOrNull(15000) { fetchFreshSong(localSong ?: Song(id = songId, title = "Resolving...", artist = "Music Stream")) } 
        } catch (e: Exception) { null }

        // Attempt 2: If failed, rotate and try again immediately
        if (resolved == null || !resolved.streamUrl.startsWith("http")) {
            Log.w("MusicRepo", "Mirror 1 failed for $songId, rotating Piped instance...")
            pipedInterceptor.rotateInstance()
            resolved = try { 
                withTimeoutOrNull(20000) { fetchFreshSong(localSong ?: Song(id = songId, title = "Resolving...", artist = "Music Stream")) } 
            } catch (e: Exception) { null }
        }
        
        if (resolved != null && resolved.streamUrl.startsWith("http")) {
            Log.d("MusicRepo", "Resolution successful for $songId")
            emit(resolved)
        } else {
            Log.e("MusicRepo", "All resolution mirrors failed for $songId")
            emit(localSong)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getYouTubeAudioStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val newPipeUrl = youTubeExtractor.getAudioUrl(videoId)
                if (!newPipeUrl.isNullOrBlank()) return@withContext newPipeUrl
                val pipedInfo = withTimeoutOrNull(15000) { youTubeApi.getStreamInfo(videoId) }
                pipedInfo?.audioStreams?.maxByOrNull { it.bitrate ?: 0 }?.url
            } catch (e: Exception) { null }
        }
    }

    override fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> = playlistDao.getSongsForPlaylist(playlistId).map { entities -> entities.map { it.toDomain() } }
    override suspend fun toggleFavorite(song: Song) { songDao.insertSong(song.toEntity()); if (favoriteDao.isFavorite(song.id)) favoriteDao.removeFavorite(song.id) else favoriteDao.insertFavorite(FavoriteEntity(songId = song.id)) }
    override suspend fun createPlaylist(name: String) { val id = UUID.randomUUID().toString(); playlistDao.insertPlaylist(PlaylistEntity(id = id, name = name, songCount = 0, gradientIndex = (name.hashCode() and Integer.MAX_VALUE) % 5)) }
    override suspend fun deletePlaylist(playlistId: String) = playlistDao.deletePlaylist(playlistId)
    override suspend fun deleteAllPlaylists() = playlistDao.deleteAllPlaylists()
    override suspend fun addSongToPlaylist(playlistId: String, songId: String) { playlistDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId = playlistId, songId = songId)); playlistDao.incrementSongCount(playlistId) }
    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) { playlistDao.deletePlaylistSongCrossRef(PlaylistSongCrossRef(playlistId = playlistId, songId = songId)); playlistDao.decrementSongCount(playlistId) }
    override suspend fun deleteDownload(songId: String) { val song = songDao.getSongById(songId); if (song?.localPath != null) { val file = File(song.localPath!!); if (file.exists()) file.delete(); songDao.insertSong(song.copy(localPath = null)) } }
    override suspend fun addToRecentlyPlayed(song: Song) { songDao.insertSong(song.toEntity()); songDao.insertRecentlyPlayed(RecentlyPlayedEntity(songId = song.id)); songDao.incrementPlayCount(song.id) }
    override suspend fun addLocalSong(song: Song) = songDao.insertSong(song.toEntity())

    override suspend fun downloadSong(song: Song): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Progress(0))
        _downloadingSongs.update { it + (song.id to 0) }

        try {
            val resolvedSong = withTimeoutOrNull(30000) { fetchFreshSong(song) } ?: song
            var downloadUrl = resolvedSong.streamUrl

            if (downloadUrl.startsWith("youtube://") || (downloadUrl.isEmpty() && song.id.length == 11)) {
                val videoId = if (downloadUrl.startsWith("youtube://")) downloadUrl.substringAfter("youtube://") else song.id
                val resolvedUrl = withTimeoutOrNull(25000) { youTubeExtractor.getAudioUrl(videoId) }
                if (resolvedUrl != null) downloadUrl = resolvedUrl
            }

            if (downloadUrl.isBlank() || !downloadUrl.startsWith("http")) {
                throw Exception("Invalid streaming transport endpoint")
            }

            Log.d("DownloadPipeline", "Starting download for ${song.title} from: $downloadUrl")

            // REVOLUTIONARY: Pre-flight Head Request to validate endpoint
            val isReady = withContext(Dispatchers.IO) {
                try {
                    val headRequest = Request.Builder()
                        .url(downloadUrl)
                        .head()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                        .build()
                    downloadHttpClient.newCall(headRequest).execute().use { it.isSuccessful }
                } catch (e: Exception) { false }
            }

            if (!isReady) {
                Log.w("DownloadPipeline", "Endpoint failed pre-flight. Force rotating mirror...")
                pipedInterceptor.rotateInstance()
                val retryUrl = getYouTubeAudioStreamUrl(if (song.id.length == 11) song.id else song.streamUrl.substringAfter("youtube://"))
                if (retryUrl != null) downloadUrl = retryUrl
                else throw Exception("Invalid streaming transport endpoint - All routes blocked")
            }

            // Setup Final Request with Ultra-Consistent Identity
            val finalRequest = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Connection", "keep-alive")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity") 
                .build()

            downloadHttpClient.newCall(finalRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "Server HTTP Error: ${response.code} ${response.message}"
                    Log.e("DownloadPipeline", errorMsg)
                    throw Exception(errorMsg)
                }
                
                val body = response.body ?: throw Exception("Empty stream package descriptor")
                val totalBytes = body.contentLength()
                Log.d("DownloadPipeline", "Total bytes to download: $totalBytes")

                // Robust Path Selection: Use internal storage MusicStream folder
                val downloadDir = File(context.filesDir, "MusicStream")
                if (!downloadDir.exists()) {
                    val created = downloadDir.mkdirs()
                    Log.d("DownloadPipeline", "Created music directory: $created at ${downloadDir.absolutePath}")
                }
                
                val fileName = "${song.id.filter { it.isLetterOrDigit() }}.mp3"
                val tempFile = File(downloadDir, "$fileName.tmp")
                val finalFile = File(downloadDir, fileName)

                var totalRead = 0L
                var lastProgressEmit = -1
                val buffer = ByteArray(256 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break

                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            if (totalBytes > 0) {
                                val percent = ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (percent >= lastProgressEmit + 5 || percent == 100) {
                                    _downloadingSongs.update { it + (song.id to percent) }
                                    emit(DownloadProgress.Progress(percent))
                                    lastProgressEmit = percent
                                }
                            }
                        }
                        output.flush()
                        try { output.fd.sync() } catch (_: Exception) {}
                    }
                }

                if (tempFile.exists() && (totalBytes <= 0 || tempFile.length() > 0)) {
                    if (finalFile.exists()) finalFile.delete()
                    if (tempFile.renameTo(finalFile)) {
                        Log.d("DownloadPipeline", "Successfully renamed temp to final: ${finalFile.absolutePath}")
                        val entity = resolvedSong.toEntity().copy(localPath = finalFile.absolutePath)
                        songDao.insertSong(entity)
                        _downloadingSongs.update { it - song.id }
                        emit(DownloadProgress.Completed)
                    } else {
                        Log.e("DownloadPipeline", "Failed to rename temp file to final file")
                        throw Exception("Disk atomic renaming operation faulted")
                    }
                } else {
                    Log.e("DownloadPipeline", "Download failed: temp file empty or missing")
                    throw Exception("Payload corrupted during structural download execution")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("DownloadPipeline", "Execution stopped completely: ${e.message}")
            _downloadingSongs.update { it - song.id }
            emit(DownloadProgress.Failed(e.message ?: "Network transport failure"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchFreshSong(song: Song): Song {
        return try {
            if (song.id.length == 11 && !song.id.contains(" ")) {
                val audioUrl = getYouTubeAudioStreamUrl(song.id)
                if (audioUrl != null) song.copy(streamUrl = audioUrl) else song
            } else {
                val response = musicApi.getSongDetails(song.id)
                val data = response.data
                val songDto = when (data) {
                    is List<*> -> (data.firstOrNull() as? Map<*, *>)?.let { parseSaavnSongFromMap(it) } ?: (data.firstOrNull() as? SaavnSongDto)
                    is Map<*, *> -> (data["results"] as? List<*>)?.let { (it.firstOrNull() as? Map<*, *>)?.let { m -> parseSaavnSongFromMap(m) } ?: (it.firstOrNull() as? SaavnSongDto) } ?: parseSaavnSongFromMap(data)
                    else -> null
                }
                songDto?.toDomain()?.copy(localPath = song.localPath, isFavorite = song.isFavorite) ?: song
            }
        } catch (e: Exception) { song }
    }

    private fun SaavnSongDto.toDomain(): Song {
        val rawImage = image?.find { it.quality == "500x500" }?.link ?: image?.lastOrNull()?.link ?: ""
        val secureImage = rawImage.replace("http://", "https://").trim()
        val highQualUrl = (downloadUrl?.find { it.quality == "320kbps" }?.link ?: downloadUrl?.lastOrNull()?.link ?: "").trim()
        return Song(id = id, title = name ?: "Unknown", artist = primaryArtists ?: "Various", album = "", duration = (duration ?: 0) * 1000L, coverUrl = secureImage, streamUrl = highQualUrl, isFavorite = false, gradientIndex = ((name?.hashCode() ?: 0) and Integer.MAX_VALUE) % 5, playCount = 0L)
    }

    private fun SongEntity.toDomain() = Song(id = id, title = title, artist = artist, album = album, duration = duration, coverUrl = coverUrl, streamUrl = streamUrl, localPath = localPath, isFavorite = false, gradientIndex = gradientIndex, playCount = playCount)
    private fun PlaylistEntity.toDomain() = Playlist(id = id, name = name, songCount = songCount, gradientIndex = gradientIndex)
    private fun Song.toEntity() = SongEntity(id = id, title = title, artist = artist, album = album, duration = duration, coverUrl = coverUrl, streamUrl = streamUrl, localPath = localPath, playCount = playCount, gradientIndex = gradientIndex)
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : UserRepository {
    private object UserKeys {
        val ID = stringPreferencesKey("user_id"); val NAME = stringPreferencesKey("user_name"); val EMAIL = stringPreferencesKey("user_email"); val AVATAR = stringPreferencesKey("user_avatar"); val BANNER = stringPreferencesKey("user_banner"); val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in"); val REGISTERED_EMAILS = stringSetPreferencesKey("registered_emails")
    }
    override fun getCurrentUser(): Flow<User> = dataStore.data.map { p -> User(p[UserKeys.ID] ?: UUID.randomUUID().toString(), p[UserKeys.NAME] ?: "Guest User", p[UserKeys.EMAIL] ?: "", p[UserKeys.AVATAR] ?: "", p[UserKeys.BANNER] ?: "") }
    override fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { it[UserKeys.IS_LOGGED_IN] ?: false }
    override suspend fun isEmailRegistered(email: String): Boolean = (dataStore.data.first()[UserKeys.REGISTERED_EMAILS] ?: emptySet()).contains(email)
    override suspend fun updateUser(user: User) { dataStore.edit { p -> p[UserKeys.ID] = user.id; p[UserKeys.NAME] = user.name; p[UserKeys.EMAIL] = user.email; p[UserKeys.IS_LOGGED_IN] = true; p[UserKeys.REGISTERED_EMAILS] = (p[UserKeys.REGISTERED_EMAILS] ?: emptySet()) + user.email } }
    override suspend fun signOut() { withContext(Dispatchers.IO) { favoriteDao.deleteAllFavorites(); playlistDao.deleteAllPlaylists(); dataStore.edit { it[UserKeys.IS_LOGGED_IN] = false } } }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    private object PreferencesKeys {
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality"); val THEME = stringPreferencesKey("theme"); val NOTIFICATIONS = booleanPreferencesKey("notifications"); val LANGUAGE = stringPreferencesKey("language"); val EQUALIZER = stringPreferencesKey("equalizer"); val BASS_BOOST = intPreferencesKey("bass_boost"); val VIRTUALIZER = intPreferencesKey("virtualizer"); fun bandKey(band: Int) = intPreferencesKey("eq_band_$band")
    }
    override fun getAudioQuality() = dataStore.data.map { it[PreferencesKeys.AUDIO_QUALITY] ?: "High (320kbps)" }
    override suspend fun setAudioQuality(quality: String) { dataStore.edit { it[PreferencesKeys.AUDIO_QUALITY] = quality } }
    override fun getTheme() = dataStore.data.map { it[PreferencesKeys.THEME] ?: "System Default" }
    override suspend fun setTheme(theme: String) { dataStore.edit { it[PreferencesKeys.THEME] = theme } }
    override fun getNotificationsEnabled() = dataStore.data.map { it[PreferencesKeys.NOTIFICATIONS] ?: true }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { dataStore.edit { it[PreferencesKeys.NOTIFICATIONS] = enabled } }
    override fun getLanguage() = dataStore.data.map { it[PreferencesKeys.LANGUAGE] ?: "English" }
    override suspend fun setLanguage(language: String) { dataStore.edit { it[PreferencesKeys.LANGUAGE] = language } }
    override fun getEqualizerPreset() = dataStore.data.map { it[PreferencesKeys.EQUALIZER] ?: "Flat" }
    override suspend fun setEqualizerPreset(preset: String) { dataStore.edit { it[PreferencesKeys.EQUALIZER] = preset } }
    override fun getBassBoostLevel() = dataStore.data.map { it[PreferencesKeys.BASS_BOOST] ?: 0 }
    override suspend fun setBassBoostLevel(level: Int) { dataStore.edit { it[PreferencesKeys.BASS_BOOST] = level } }
    override fun getVirtualizerLevel() = dataStore.data.map { it[PreferencesKeys.VIRTUALIZER] ?: 0 }
    override suspend fun setVirtualizerLevel(level: Int) { dataStore.edit { it[PreferencesKeys.VIRTUALIZER] = level } }
    override fun getEqualizerBandLevels() = dataStore.data.map { p -> (0 until 10).mapNotNull { i -> p[PreferencesKeys.bandKey(i)]?.let { i to it } }.toMap() }
    override suspend fun setEqualizerBandLevel(band: Int, level: Int) { dataStore.edit { it[PreferencesKeys.bandKey(band)] = level } }
}

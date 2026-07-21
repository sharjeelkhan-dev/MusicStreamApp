package com.musicstream.app.data.repository

import android.content.Context
import android.util.Log
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
import com.musicstream.app.data.remote.dto.SaavnDownloadUrlDto
import com.musicstream.app.data.remote.dto.SaavnImageDto
import com.musicstream.app.data.remote.dto.SaavnSongDto
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.DownloadProgress
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val searchDao: SearchDao,
    private val musicApi: MusicApi,
    private val mp3Scraper: Mp3ScraperRepository,
    @Named("downloadClient")
    private val downloadHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context,
) : MusicRepository {

    companion object {
        private const val DEFAULT_COVER_IMAGE = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500"
    }

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
                    coverUrl = DEFAULT_COVER_IMAGE
                )
                latest.copy(isFavorite = favIds.contains(latest.id))
            }

    override fun getTrendingSongs(query: String): Flow<List<Song>> {
        val remoteFlow = flow {
            val trendingResults = mutableListOf<Song>()

            val dynamicPool = listOf(
                "Top Songs 2026",
                "Latest Punjabi Hits",
                "Global Top 50",
                "Bollywood Romantic",
                "Urdu Lofi",
                "Acoustic Pop",
                "Trending Hits"
            )

            val searchQueries = if (query.isNotBlank()) {
                listOf(query)
            } else {
                dynamicPool.shuffled().take(3)
            }

            val randomPage = (1..3).random()

            for (q in searchQueries) {
                try {
                    Log.d("MusicRepo", "Fetching catalog for query: $q (Page: $randomPage)")
                    val response = withTimeoutOrNull(7000.milliseconds) {
                        musicApi.searchSongs(q, page = randomPage, limit = 20)
                    }
                    val songs = parseSongsFromResponse(response)

                    if (!songs.isNullOrEmpty()) {
                        trendingResults.addAll(songs.map { it.toDomain() })
                    }
                } catch (e: Exception) {
                    Log.d("MusicRepo", "Query '$q' failed: ${e.message}")
                }

                if (trendingResults.size >= 40) break
            }

            if (trendingResults.isEmpty()) {
                try {
                    Log.d("MusicRepo", "Executing ultimate fallback query: Top Songs")
                    val response = withTimeoutOrNull(7000.milliseconds) {
                        musicApi.searchSongs("Top Songs", page = 1, limit = 30)
                    }
                    val songs = parseSongsFromResponse(response)
                    if (!songs.isNullOrEmpty()) {
                        trendingResults.addAll(songs.map { it.toDomain() })
                    }
                } catch (e: Exception) {
                    Log.e("MusicRepo", "Fallback fetch failed: ${e.message}")
                }
            }

            val finalMixedList = trendingResults.distinctBy { it.id }.shuffled()

            if (finalMixedList.isNotEmpty()) {
                Log.d("MusicRepo", "Successfully loaded ${finalMixedList.size} diverse songs")
                emit(finalMixedList)
            } else {
                Log.w("MusicRepo", "All remote attempts failed, returning local Mock data")
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

    override fun getGenres(): Flow<List<Genre>> = flow {
        val genres = listOf(
            Genre("1", "Pop", "https://images.unsplash.com/photo-1548778052-311f4bc2b502?q=80&w=500", "#1E3243"),
            Genre("2", "Hip-Hop", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500", "#1E3243"),
            Genre("3", "Punjabi", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500", "#1E3243"),
            Genre("4", "Hindi", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=500", "#1E3243"),
            Genre("5", "Rock", "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500", "#1E3243"),
            Genre("6", "Classical", "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=500", "#1E3243"),
            Genre("7", "Jazz", "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=500", "#1E3243"),
            Genre("8", "Lo-fi", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500", "#1E3243")
        )
        emit(genres)
    }

    override fun getTrendingSearches(): Flow<List<String>> = flow { emit(MockData.trendingSearches) }
    override fun getSearchHistory(): Flow<List<String>> = searchDao.getSearchHistory().map { entities -> entities.map { it.query } }
    override suspend fun addSearchHistory(query: String) { if (query.isNotBlank()) searchDao.insertSearchHistory(SearchHistoryEntity(query = query)) }
    override suspend fun deleteSearchHistory(query: String) = searchDao.deleteSearchHistory(query)
    override suspend fun clearSearchHistory() = searchDao.clearSearchHistory()

    override fun searchSongs(query: String): Flow<List<Song>> {
        if (query.isBlank()) return flowOf(emptyList())
        val remoteFlow = flow {
            coroutineScope {
                Log.d("MusicRepo", "Starting remote search for: $query")
                val saavnDeferred = async {
                    try {
                        withTimeoutOrNull(15000.milliseconds) {
                            val response = musicApi.searchSongs(query, limit = 50)
                            val songs = parseSongsFromResponse(response)
                            songs?.map { it.toDomain() }
                        }
                    } catch (e: Exception) {
                        Log.e("MusicRepo", "Search failed: ${e.message}")
                        null
                    }
                }

                val saavnRes = saavnDeferred.await()
                if (!saavnRes.isNullOrEmpty()) {
                    emit(saavnRes)
                } else {
                    val localResults = songDao.searchSongs(query).firstOrNull()?.map { it.toDomain() } ?: emptyList()
                    if (localResults.isNotEmpty()) {
                        emit(localResults)
                    } else {
                        emit(MockData.trendingSongs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) })
                    }
                }
            }
        }
        return combine(remoteFlow, songDao.getAllSongs().onStart { emit(emptyList()) }, favoriteDao.getAllFavorites().onStart { emit(emptyList()) }) { remote, local, favorites ->
            val favIds = favorites.map { it.songId }.toSet()
            remote.map { song -> val localMatch = local.find { it.id == song.id }; song.copy(localPath = localMatch?.localPath, isFavorite = favIds.contains(song.id)) }
        }.flowOn(Dispatchers.IO)
    }

    private fun parseSongsFromResponse(response: Any?): List<SaavnSongDto>? {
        val rawJson = when (response) {
            is Response<*> -> (response.body() as? ResponseBody)?.string() ?: (response.errorBody() as? ResponseBody)?.string()
            is ResponseBody -> response.string()
            is String -> response
            else -> response?.toString()
        } ?: return null

        val trimmed = rawJson.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            Log.e("MusicRepo", "Server returned non-JSON format: ${trimmed.take(100)}")
            return null
        }

        return try {
            val parsedSongs = mutableListOf<SaavnSongDto>()
            if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                val dataObj = jsonObject.optJSONObject("data")

                val songsArray = dataObj?.optJSONArray("results")
                    ?: dataObj?.optJSONArray("songs")
                    ?: jsonObject.optJSONArray("results")
                    ?: jsonObject.optJSONArray("songs")
                    ?: jsonObject.optJSONArray("data")

                if (songsArray != null) {
                    for (i in 0 until songsArray.length()) {
                        val item = songsArray.optJSONObject(i)
                        if (item != null) {
                            parsedSongs.add(parseSaavnSongFromJsonObject(item))
                        }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i)
                    if (item != null) {
                        parsedSongs.add(parseSaavnSongFromJsonObject(item))
                    }
                }
            }
            parsedSongs.ifEmpty { null }
        } catch (e: Exception) {
            Log.e("MusicRepo", "JSON Parsing Exception: ${e.message}")
            null
        }
    }

    private fun parseSaavnSongFromJsonObject(map: JSONObject): SaavnSongDto {
        val id = map.optString("id").ifEmpty { map.optString("songid", UUID.randomUUID().toString()) }
        val name = map.optString("name").ifEmpty { map.optString("songname", map.optString("title", "Unknown Track")) }

        val primaryArtists = when {
            map.has("primaryArtists") -> map.optString("primaryArtists")
            map.has("singers") -> map.optString("singers")
            map.has("artist") -> map.optString("artist")
            else -> "Various Artists"
        }

        val imageList = mutableListOf<SaavnImageDto>()
        val imgArray = map.optJSONArray("image")
        if (imgArray != null) {
            for (i in 0 until imgArray.length()) {
                val imgObj = imgArray.optJSONObject(i)
                if (imgObj != null) {
                    val quality = imgObj.optString("quality", "high")
                    val url = imgObj.optString("link", imgObj.optString("url"))
                    if (url.isNotBlank()) imageList.add(SaavnImageDto(quality, url))
                } else {
                    val str = imgArray.optString(i)
                    if (str.isNotBlank()) imageList.add(SaavnImageDto("high", str))
                }
            }
        } else {
            val imgStr = map.optString("image")
            if (imgStr.isNotBlank()) imageList.add(SaavnImageDto("high", imgStr))
        }

        val downloadList = mutableListOf<SaavnDownloadUrlDto>()
        val dlArray = map.optJSONArray("downloadUrl")
            ?: map.optJSONArray("media_url")
        if (dlArray != null) {
            for (i in 0 until dlArray.length()) {
                val dlObj = dlArray.optJSONObject(i)
                if (dlObj != null) {
                    downloadList.add(SaavnDownloadUrlDto(dlObj.optString("quality", "high"), dlObj.optString("link", dlObj.optString("url"))))
                } else {
                    val str = dlArray.optString(i)
                    if (str.isNotBlank()) downloadList.add(SaavnDownloadUrlDto("high", str))
                }
            }
        } else {
            val dlStr = map.optString("downloadUrl", map.optString("media_url"))
            if (dlStr.isNotBlank()) downloadList.add(SaavnDownloadUrlDto("high", dlStr))
        }

        val duration = map.optLong("duration", 0L)

        return SaavnSongDto(id = id, name = name, primaryArtists = primaryArtists, duration = duration, image = imageList, downloadUrl = downloadList)
    }

    override fun getFavorites(): Flow<List<Song>> = favoriteDao.getAllFavorites().combine(songDao.getAllSongs()) { favorites, allSongs ->
        val favIds = favorites.map { it.songId }.toSet()
        allSongs.filter { favIds.contains(it.id) }.map { it.toDomain().copy(isFavorite = true) }
    }

    override fun getDownloads(): Flow<List<Song>> = songDao.getAllSongs().map { songs ->
        songs.filter { it.localPath != null && File(it.localPath).exists() }.map { it.toDomain() }
    }

    override fun getSongById(songId: String): Flow<Song?> = flow {
        val localSong = songDao.getSongById(songId)?.toDomain()
        if (localSong?.localPath != null && File(localSong.localPath).exists()) {
            emit(localSong)
            return@flow
        }
        if (localSong != null) emit(localSong)

        var resolved: Song? = try {
            withTimeoutOrNull(15000.milliseconds) {
                val response = musicApi.getSongDetails(songId)
                val songs = parseSongsFromResponse(response)
                songs?.firstOrNull()?.toDomain()?.copy(localPath = localSong?.localPath, isFavorite = localSong?.isFavorite ?: false)
            }
        } catch (e: Exception) { null }

        if (resolved == null || resolved.streamUrl.isBlank() || !resolved.streamUrl.startsWith("http")) {
            val query = (localSong?.title ?: resolved?.title ?: "").let { if (it.isNotBlank()) "$it ${localSong?.artist ?: resolved?.artist ?: ""}" else "" }
            val scraperUrl = if (query.isNotBlank()) mp3Scraper.resolveMp3Link(query) else null
            if (!scraperUrl.isNullOrEmpty()) {
                val cleanUrl = scraperUrl.replace("http://", "https://")
                resolved = (resolved ?: localSong ?: Song(id = songId, title = "Resolving...", artist = "Music Stream")).copy(streamUrl = cleanUrl)
            }
        }
        emit(resolved ?: localSong)
    }.flowOn(Dispatchers.IO)

    override suspend fun getYouTubeAudioStreamUrl(videoId: String, title: String?, artist: String?): String? {
        val song = songDao.getSongById(videoId)?.toDomain() ?: if (title != null && artist != null) Song(id = videoId, title = title, artist = artist) else null
        return if (song != null) resolveHighQualityStream(song) else null
    }

    private suspend fun resolveHighQualityStream(song: Song): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (song.streamUrl.isNotBlank() && song.streamUrl.startsWith("http") && !song.streamUrl.contains("googlevideo")) {
                    return@withContext song.streamUrl.replace("http://", "https://")
                }
                val response = musicApi.searchSongs("${song.title} ${song.artist}", limit = 5)
                val songs = parseSongsFromResponse(response)
                val firstItem = songs?.firstOrNull()

                if (firstItem != null) {
                    val downloadList = extractDownloadUrls(firstItem.downloadUrl)
                    val bestUrl = downloadList.find { it.quality == "320kbps" }?.link
                        ?: downloadList.find { it.quality == "160kbps" }?.link
                        ?: downloadList.lastOrNull()?.link
                    if (!bestUrl.isNullOrEmpty()) return@withContext bestUrl.replace("http://", "https://")
                }
                mp3Scraper.resolveMp3Link("${song.title} ${song.artist}")?.replace("http://", "https://")
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
    override suspend fun deleteDownload(songId: String) { val song = songDao.getSongById(songId); if (song?.localPath != null) { val file = File(song.localPath); if (file.exists()) file.delete(); songDao.insertSong(song.copy(localPath = null)) } }
    override suspend fun addToRecentlyPlayed(song: Song) { songDao.insertSong(song.toEntity()); songDao.insertRecentlyPlayed(RecentlyPlayedEntity(songId = song.id)); songDao.incrementPlayCount(song.id) }
    override suspend fun addLocalSong(song: Song) = songDao.insertSong(song.toEntity())

    override suspend fun downloadSong(song: Song): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Progress(0))
        _downloadingSongs.update { it + (song.id to 0) }
        try {
            val resolvedSong = getSongById(song.id).firstOrNull() ?: song
            var dlUrl = resolvedSong.streamUrl
            if (dlUrl.isBlank() || !dlUrl.startsWith("http")) {
                dlUrl = resolveHighQualityStream(song) ?: throw Exception("No downloadable link found")
            }

            val safeUrl = dlUrl.replace("http://", "https://")
            val request = Request.Builder()
                .url(safeUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            downloadHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP Error Code: ${response.code}")
                val body = response.body ?: throw Exception("Null response body")
                val total = body.contentLength()
                val downloadDir = File(context.filesDir, "MusicStream").apply { if (!exists()) mkdirs() }

                val cleanId = song.id.filter { it.isLetterOrDigit() }.ifEmpty { "song_${System.currentTimeMillis()}" }
                val finalFile = File(downloadDir, "$cleanId.mp3")
                val tempFile = File(downloadDir, "$cleanId.tmp")

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(1024 * 64)
                        var read = 0L
                        while (true) {
                            val bytes = input.read(buffer)
                            if (bytes == -1) break
                            output.write(buffer, 0, bytes)
                            read += bytes
                            if (total > 0) {
                                val p = ((read * 100) / total).toInt()
                                _downloadingSongs.update { it + (song.id to p) }
                                emit(DownloadProgress.Progress(p))
                            }
                        }
                        output.flush()
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (finalFile.exists()) finalFile.delete()
                    if (tempFile.renameTo(finalFile)) {
                        songDao.insertSong(resolvedSong.copy(streamUrl = safeUrl).toEntity().copy(localPath = finalFile.absolutePath))
                        _downloadingSongs.update { it - song.id }
                        emit(DownloadProgress.Completed)
                    } else {
                        throw Exception("File rename operation failed")
                    }
                } else {
                    throw Exception("Downloaded file size is 0 bytes")
                }
            }
        } catch (e: Exception) {
            _downloadingSongs.update { it - song.id }
            Log.e("MusicRepo", "Download failed: ${e.message}")
            emit(DownloadProgress.Failed(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun extractImageUrls(rawImage: Any?): List<SaavnImageDto> {
        return when (rawImage) {
            is List<*> -> rawImage.mapNotNull { item ->
                when (item) {
                    is SaavnImageDto -> item
                    is Map<*, *> -> SaavnImageDto((item["quality"] as? String) ?: "", (item["link"] as? String) ?: (item["url"] as? String) ?: "")
                    is String -> SaavnImageDto("high", item)
                    else -> null
                }
            }
            is String -> if (rawImage.isNotBlank()) listOf(SaavnImageDto("high", rawImage)) else emptyList()
            else -> emptyList()
        }
    }

    private fun extractDownloadUrls(rawDownload: Any?): List<SaavnDownloadUrlDto> {
        return when (rawDownload) {
            is List<*> -> rawDownload.mapNotNull { item ->
                when (item) {
                    is SaavnDownloadUrlDto -> item
                    is Map<*, *> -> SaavnDownloadUrlDto((item["quality"] as? String) ?: "", (item["link"] as? String) ?: (item["url"] as? String) ?: "")
                    is String -> SaavnDownloadUrlDto("high", item)
                    else -> null
                }
            }
            is String -> if (rawDownload.isNotBlank()) listOf(SaavnDownloadUrlDto("high", rawDownload)) else emptyList()
            else -> emptyList()
        }
    }

    // Process and resolve high resolution cover image URL
    private fun resolveCoverUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return DEFAULT_COVER_IMAGE

        var cleanUrl = rawUrl.trim().replace("http://", "https://")

        // Format JioSaavn image size strings if present
        if (cleanUrl.contains("50x50") || cleanUrl.contains("150x150")) {
            cleanUrl = cleanUrl.replace("50x50", "500x500").replace("150x150", "500x500")
        }

        return cleanUrl
    }

    private fun SaavnSongDto.toDomain(): Song {
        val images = extractImageUrls(this.image)
        val downloads = extractDownloadUrls(this.downloadUrl)

        val rawImage = images.find { it.quality == "500x500" }?.link
            ?: images.find { it.quality == "high" }?.link
            ?: images.lastOrNull()?.link
            ?: ""

        val rawStream = (downloads.find { it.quality == "320kbps" }?.link
            ?: downloads.find { it.quality == "160kbps" }?.link
            ?: downloads.lastOrNull()?.link
            ?: "").trim()

        val safeStreamUrl = rawStream.replace("http://", "https://")

        val artistName = when (val art = this.primaryArtists) {
            is String -> art
            is List<*> -> art.joinToString(", ") { if (it is Map<*, *>) (it["name"] as? String) ?: "" else it.toString() }
            else -> "Various Artists"
        }

        val durationInMs = when (val dur = this.duration) {
            is Number -> {
                val valLong = dur.toLong()
                if (valLong < 100000L) valLong * 1000L else valLong
            }
            else -> 0L
        }

        return Song(
            id = id,
            title = name ?: "Unknown Track",
            artist = artistName,
            album = "",
            duration = durationInMs,
            coverUrl = resolveCoverUrl(rawImage),
            streamUrl = safeStreamUrl,
            albumId = "",
            isrc = null,
            quality = if (safeStreamUrl.contains("320")) "320kbps" else "160kbps",
            isFavorite = false,
            gradientIndex = ((name?.hashCode() ?: 0) and Integer.MAX_VALUE) % 5,
            playCount = 0L
        )
    }

    private fun SongEntity.toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        coverUrl = resolveCoverUrl(coverUrl),
        streamUrl = streamUrl,
        localPath = localPath,
        albumId = albumId,
        isrc = isrc,
        quality = quality,
        isFavorite = false,
        gradientIndex = gradientIndex,
        playCount = playCount
    )

    private fun Song.toEntity() = SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        coverUrl = resolveCoverUrl(coverUrl),
        streamUrl = streamUrl,
        localPath = localPath,
        albumId = albumId,
        isrc = isrc,
        quality = quality,
        playCount = playCount,
        gradientIndex = gradientIndex
    )

    private fun PlaylistEntity.toDomain() = Playlist(id = id, name = name, songCount = songCount, gradientIndex = gradientIndex)
}
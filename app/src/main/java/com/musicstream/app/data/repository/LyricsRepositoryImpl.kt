package com.musicstream.app.data.repository

import android.util.Log
import com.musicstream.app.data.remote.api.LrcLibApi
import com.musicstream.app.data.remote.api.MusicApi
import com.musicstream.app.domain.model.LyricLine
import com.musicstream.app.domain.model.LrcParser
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.LyricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    private val lrcLibApi: LrcLibApi,
    private val musicApi: MusicApi
) : LyricsRepository {

    private val cache = mutableMapOf<String, Pair<List<LyricLine>, String?>>()

    override suspend fun getSyncedLyrics(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        if (cache.containsKey(song.id)) {
            val cachedResult = cache[song.id]?.first
            if (!cachedResult.isNullOrEmpty()) return@withContext cachedResult
        }

        val cleanTitle = LrcParser.cleanTitle(song.title)
        val cleanArtist = LrcParser.cleanArtist(song.artist)
        val durationSec = if (song.duration > 0) (song.duration / 1000).toInt() else null

        // Strategy 1: Direct LrcLib Exact Search with Clean Title & Artist
        try {
            val response = lrcLibApi.getLyrics(
                trackName = cleanTitle,
                artistName = cleanArtist,
                durationInSeconds = durationSec
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val rawLrc = body.syncedLyrics ?: body.plainLyrics
                val parsed = LrcParser.parse(rawLrc, song.duration)
                if (parsed.isNotEmpty()) {
                    cache[song.id] = Pair(parsed, body.plainLyrics ?: body.syncedLyrics)
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            Log.d("LyricsRepo", "LrcLib direct get failed: ${e.message}")
        }

        // Strategy 2: LrcLib Search Endpoint with Clean Title & Artist
        try {
            val queryStr = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle
            val searchRes = lrcLibApi.searchLyrics(queryStr)
            if (searchRes.isSuccessful && !searchRes.body().isNullOrEmpty()) {
                val matches = searchRes.body()!!
                val bestMatch = matches.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                    ?: matches.firstOrNull { !it.plainLyrics.isNullOrBlank() }
                    ?: matches.first()

                val rawLrc = bestMatch.syncedLyrics ?: bestMatch.plainLyrics
                val parsed = LrcParser.parse(rawLrc, song.duration)
                if (parsed.isNotEmpty()) {
                    cache[song.id] = Pair(parsed, bestMatch.plainLyrics ?: bestMatch.syncedLyrics)
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            Log.d("LyricsRepo", "LrcLib search failed: ${e.message}")
        }

        // Strategy 3: JioSaavn API Lyrics Endpoint Fallback
        try {
            val saavnRes = musicApi.getSaavnLyrics(song.id)
            val jsonStr = saavnRes.string()
            if (jsonStr.isNotBlank()) {
                val json = JSONObject(jsonStr)
                val lyricsText = if (json.has("data")) {
                    json.optJSONObject("data")?.optString("lyrics") ?: json.optJSONObject("data")?.optString("snippet")
                } else {
                    json.optString("lyrics")
                }

                if (!lyricsText.isNullOrBlank()) {
                    val parsed = LrcParser.parsePlainToTimed(lyricsText, song.duration)
                    if (parsed.isNotEmpty()) {
                        cache[song.id] = Pair(parsed, lyricsText)
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("LyricsRepo", "Saavn lyrics fetch failed: ${e.message}")
        }

        // Strategy 4: Fallback to Raw Uncleaned Title LrcLib Search
        if (cleanTitle != song.title) {
            try {
                val searchRes = lrcLibApi.searchLyrics(song.title)
                if (searchRes.isSuccessful && !searchRes.body().isNullOrEmpty()) {
                    val match = searchRes.body()!!.first()
                    val rawLrc = match.syncedLyrics ?: match.plainLyrics
                    val parsed = LrcParser.parse(rawLrc, song.duration)
                    if (parsed.isNotEmpty()) {
                        cache[song.id] = Pair(parsed, match.plainLyrics ?: match.syncedLyrics)
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.d("LyricsRepo", "Raw title search failed: ${e.message}")
            }
        }

        emptyList()
    }

    override suspend fun getPlainLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        if (cache.containsKey(song.id)) {
            return@withContext cache[song.id]?.second
        }
        getSyncedLyrics(song)
        cache[song.id]?.second
    }
}
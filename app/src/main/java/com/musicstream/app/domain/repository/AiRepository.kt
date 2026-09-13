package com.musicstream.app.domain.repository

import com.musicstream.app.domain.model.Song

interface AiRepository {
    suspend fun getRecommendations(history: List<Song>, likedSongs: List<Song>): List<Song>
    suspend fun generatePlaylist(prompt: String): List<Song>
    suspend fun detectMood(input: String): String
    suspend fun getAiAssistantResponse(query: String): String
    suspend fun explainLyrics(song: Song): String
    suspend fun translateLyrics(song: Song, targetLanguage: String): String
    suspend fun getSongSummary(song: Song): String
    suspend fun getArtistInfo(artistName: String): String
    suspend fun getContextAwareRecommendations(activity: String): List<Song>
}

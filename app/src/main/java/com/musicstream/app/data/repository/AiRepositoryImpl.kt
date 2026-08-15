package com.musicstream.app.data.repository

import com.google.firebase.ai.GenerativeModel
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.AiRepository
import com.musicstream.app.domain.repository.MusicRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val musicRepository: MusicRepository
) : AiRepository {

    override suspend fun getRecommendations(history: List<Song>, likedSongs: List<Song>): List<Song> {
        val historyStr = history.take(5).joinToString { "${it.title} by ${it.artist}" }
        val likedStr = likedSongs.take(5).joinToString { "${it.title} by ${it.artist}" }
        
        val prompt = """
            Based on my listening history: [$historyStr] 
            And my liked songs: [$likedStr]
            
            Recommend 10 similar songs. 
            Return the result ONLY as a list where each line is in the format: "Song Title - Artist Name".
            Do not include numbering or extra text.
        """.trimIndent()
        
        val response = generativeModel.generateContent(prompt)
        return parseAndSearchSongs(response.text)
    }

    override suspend fun generatePlaylist(prompt: String): List<Song> {
        val fullPrompt = """
            Create a music playlist for: "$prompt".
            Suggest 15 songs that fit this mood or context.
            Return the result ONLY as a list where each line is in the format: "Song Title - Artist Name".
            Do not include numbering or extra text.
        """.trimIndent()
        
        val response = generativeModel.generateContent(fullPrompt)
        return parseAndSearchSongs(response.text)
    }

    override suspend fun detectMood(input: String): String {
        val prompt = "Analyze the mood of the following text and return only ONE word representing it (e.g., Happy, Sad, Energetic, Relaxed, Romantic): \"$input\""
        return generativeModel.generateContent(prompt).text?.trim() ?: "Neutral"
    }

    override suspend fun getAiAssistantResponse(query: String): String {
        val systemPrompt = "You are a helpful AI Music Assistant. Answer the user's query about music, artists, or suggest songs. Query: "
        return generativeModel.generateContent(systemPrompt + query).text ?: "I'm sorry, I couldn't process that."
    }

    override suspend fun explainLyrics(song: Song): String {
        val prompt = "Explain the lyrical meaning and deeper theme of the song '${song.title}' by '${song.artist}'."
        return generativeModel.generateContent(prompt).text ?: "Explanation unavailable."
    }

    override suspend fun translateLyrics(song: Song, targetLanguage: String): String {
        val prompt = "Translate the main essence and some key lyrics of '${song.title}' by '${song.artist}' into $targetLanguage."
        return generativeModel.generateContent(prompt).text ?: "Translation unavailable."
    }

    override suspend fun getSongSummary(song: Song): String {
        val prompt = "Provide a summary of the song '${song.title}' by '${song.artist}', including its genre, mood, and instrumentation."
        return generativeModel.generateContent(prompt).text ?: "Summary unavailable."
    }

    override suspend fun getArtistInfo(artistName: String): String {
        val prompt = "Give a summary of the artist '$artistName', their musical style, and their most famous work."
        return generativeModel.generateContent(prompt).text ?: "Artist info unavailable."
    }

    override suspend fun getContextAwareRecommendations(activity: String): List<Song> {
        val prompt = """
            Suggest 10 songs suitable for the activity: "$activity".
            Return the result ONLY as a list where each line is in the format: "Song Title - Artist Name".
            Do not include numbering or extra text.
        """.trimIndent()
        
        val response = generativeModel.generateContent(prompt)
        return parseAndSearchSongs(response.text)
    }

    private suspend fun parseAndSearchSongs(text: String?): List<Song> {
        if (text.isNullOrBlank()) return emptyList()
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.contains(" - ") || it.contains("-") }
        
        val results = mutableListOf<Song>()
        for (line in lines) {
            val delimiter = if (line.contains(" - ")) " - " else "-"
            val parts = line.split(delimiter)
            if (parts.size >= 2) {
                val searchTitle = parts[0].trim().removePrefix("-").trim()
                val artistName = parts[1].trim()
                // Search for the song in our database/API
                val songsFound = musicRepository.searchSongs("$searchTitle $artistName").first()
                if (songsFound.isNotEmpty()) {
                    results.add(songsFound[0])
                }
            }
        }
        return results.distinctBy { it.id }
    }
}

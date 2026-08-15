package com.musicstream.app.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.AiRepository
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val recommendedSongs: List<Song> = emptyList(),
    val detectedMood: String? = null
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        val userMessage = ChatMessage(text = query, isUser = true)
        _uiState.update { it.copy(
            messages = it.messages + userMessage,
            isLoading = true
        ) }

        viewModelScope.launch {
            try {
                val response = aiRepository.getAiAssistantResponse(query)
                val aiMessage = ChatMessage(text = response, isUser = false)
                
                // If the user asks for songs, also try to get recommendations
                if (query.contains("recommend", ignoreCase = true) || query.contains("suggest", ignoreCase = true)) {
                    val recommended = aiRepository.generatePlaylist(query)
                    _uiState.update { it.copy(recommendedSongs = recommended) }
                }

                _uiState.update { it.copy(
                    messages = it.messages + aiMessage,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(text = "Error: ${e.message}", isUser = false)
                _uiState.update { it.copy(
                    messages = it.messages + errorMessage,
                    isLoading = false
                ) }
            }
        }
    }

    fun generatePlaylistFromMood(moodInput: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val mood = aiRepository.detectMood(moodInput)
                val songs = aiRepository.generatePlaylist("Songs for $mood mood based on $moodInput")
                _uiState.update { it.copy(
                    detectedMood = mood,
                    recommendedSongs = songs,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun getSongExplanation(song: Song) {
        viewModelScope.launch {
            val explanation = aiRepository.explainLyrics(song)
            val message = ChatMessage(text = "Explanation for ${song.title}: $explanation", isUser = false)
            _uiState.update { it.copy(messages = it.messages + message) }
        }
    }
}

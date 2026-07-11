package com.attendance.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.domain.repository.AiChatMessage
import com.attendance.app.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiAssistantState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage("Hello! I'm your AI Assistant. How can I help you today?", isUser = false)
    ),
    val isLoading: Boolean = false,
    val currentInput: String = ""
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

fun ChatMessage.toDomain() = AiChatMessage(text, isUser)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AiAssistantState())
    val state: StateFlow<AiAssistantState> = _state.asStateFlow()

    fun onInputChange(input: String) {
        _state.update { it.copy(currentInput = input) }
    }

    fun sendMessage() {
        val input = _state.value.currentInput.trim()
        if (input.isEmpty()) return

        val userMessage = ChatMessage(input, isUser = true)
        val history = _state.value.messages.map { it.toDomain() }
        
        _state.update { 
            it.copy(
                messages = it.messages + userMessage,
                currentInput = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            aiRepository.processAiCommand(input, history).collect { response ->
                _state.update { 
                    it.copy(
                        messages = it.messages + ChatMessage(response, isUser = false),
                        isLoading = false
                    )
                }
            }
        }
    }
}

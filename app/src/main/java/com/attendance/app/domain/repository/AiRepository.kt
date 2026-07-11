package com.attendance.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Represents a simplified chat message for the repository.
 */
data class AiChatMessage(
    val text: String,
    val isUser: Boolean
)

/**
 * Repository interface for AI-related operations.
 */
interface AiRepository {
    /**
     * Fetches attendance insights based on provided summary data.
     */
    fun getAttendanceInsights(summaryData: String): Flow<String>

    /**
     * Process a conversational command with history.
     */
    fun processAiCommand(
        prompt: String,
        history: List<AiChatMessage> = emptyList()
    ): Flow<String>
}

package com.attendance.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for AI-related operations.
 */
interface AiRepository {
    /**
     * Fetches attendance insights based on provided summary data.
     * @param summaryData A string representation of attendance data to analyze.
     * @return A Flow emitting the AI-generated insights as a string.
     */
    fun getAttendanceInsights(summaryData: String): Flow<String>

    /**
     * Process a general user command or query.
     * @param prompt User input.
     * @return A Flow emitting the AI response.
     */
    fun processAiCommand(prompt: String): Flow<String>
}

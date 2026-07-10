package com.attendance.app.data.repository

import android.util.Log
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.domain.repository.AiRepository
import com.google.ai.client.generativeai.GenerativeModel as GoogleGenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAiRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager
) : AiRepository {

    // Helper function patterned exactly like your Smart To-Do App with exponential backoff retry
    private suspend fun generateContentWithRetry(prompt: String, maxRetries: Int = 3): String? = withContext(Dispatchers.IO) {
        val userApiKey = preferencesManager.aiApiKeyFlow.first()?.trim()
        var currentDelay = 1500L // 1.5 Seconds initial delay to clear brief quota window

        for (attempt in 0 until maxRetries) {
            try {
                if (userApiKey.isNullOrBlank()) {
                    Log.d("FirebaseAiRepo", "Using default Firebase-managed Gemini model (Attempt ${attempt + 1})")
                    val firebaseModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel("gemini-3.5-flash")

                    return@withContext firebaseModel.generateContent(prompt).text
                } else {
                    Log.d("FirebaseAiRepo", "Using custom API key with Google AI SDK (Attempt ${attempt + 1})")
                    val googleModel = GoogleGenerativeModel(
                        modelName = "gemini-3.5-flash",
                        apiKey = userApiKey
                    )
                    return@withContext googleModel.generateContent(prompt).text
                }
            } catch (e: Exception) {
                Log.e("FirebaseAiRepo", "Error on attempt ${attempt + 1}", e)
                val errorMsg = e.message?.lowercase() ?: ""
                val isQuotaError = errorMsg.contains("429") || errorMsg.contains("quota") || errorMsg.contains("exhausted")

                if (isQuotaError && attempt < maxRetries - 1) {
                    Log.w("FirebaseAiRepo", "Quota hit (RESOURCE_EXHAUSTED). Retrying in $currentDelay ms...")
                    delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff (1.5s -> 3s -> 6s)
                } else {
                    // Critical error or last attempt exhausted, map standard exceptions safely
                    val errorMessage = when {
                        errorMsg.contains("403") -> "Access Forbidden (403). Check App Check or API restrictions."
                        errorMsg.contains("api_key_invalid") -> "Invalid API Key. Please check your key in Settings."
                        isQuotaError -> "Rate limit reached (RESOURCE_EXHAUSTED). Please wait a few moments."
                        else -> "AI Engine Error: ${e.message}"
                    }
                    throw Exception(errorMessage)
                }
            }
        }
        null
    }

    override fun getAttendanceInsights(summaryData: String): Flow<String> = flow {
        try {
            Log.d("FirebaseAiRepo", "Requesting attendance insights")
            // Input data optimization to keep token payload strict and clean
            val optimizedData = if (summaryData.length > 1500) summaryData.take(1500) else summaryData

            val prompt = """
                You are an expert educational consultant. Analyze the following student attendance data and provide 3 short, actionable insights or recommendations for the teacher to improve attendance or identify risks.
                
                Data:
                $optimizedData
                
                Requirements:
                1. Use a professional and encouraging tone.
                2. Keep each insight concise (under 20 words).
                3. Focus on patterns or specific concerns.
                4. Output only the 3 bullet points.
            """.trimIndent()

            val responseText = generateContentWithRetry(prompt) ?: throw Exception("Empty AI response")
            emit(responseText)
        } catch (e: Exception) {
            Log.e("FirebaseAiRepo", "Error during insights generation", e)
            emit("Could not generate insights: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override fun processAiCommand(prompt: String): Flow<String> = flow {
        try {
            Log.d("FirebaseAiRepo", "Processing AI command: $prompt")
            val systemPrompt = """
                You are a smart AI Assistant for a Student Attendance App. 
                You can help the user manage classes, students, and attendance records.
                Currently, you are in conversational mode.
                
                Guidelines:
                1. Be helpful, concise, and professional.
                2. If the user asks about app features, explain them.
                3. If the user wants to perform a task, acknowledge it.
                
                App Capabilities:
                - Take attendance for specific dates.
                - View attendance reports and statistics.
                - Add or import students.
                - Manage different classes and sections.
                
                User Query: $prompt
            """.trimIndent()

            val responseText = generateContentWithRetry(systemPrompt) ?: throw Exception("Empty AI response")
            emit(responseText)
        } catch (e: Exception) {
            Log.e("FirebaseAiRepo", "Error during command processing", e)
            emit("Error processing command: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
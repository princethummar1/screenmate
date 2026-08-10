package com.screenmate.app.core.network

import io.ktor.client.*
import android.util.Log
import io.ktor.client.call.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class UsageSummary(
    val totalMinutes: Int,
    val topApp: String,
    val topAppMinutes: Int,
    val yesterdayMinutes: Int,
    val unlockCount: Int
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatRequest(val model: String, val messages: List<ChatMessage>)

@Serializable
data class ChatChoice(val message: ChatMessage)

@Serializable
data class ChatResponse(val choices: List<ChatChoice>)

class OpenRouterApi {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 20000
            socketTimeoutMillis = 60000
        }
    }

    suspend fun generateCommentary(apiKey: String, model: String, summary: UsageSummary): Result<String> {
        if (apiKey.isBlank()) return Result.failure(Exception("OpenRouter API key is blank"))

        val prompt = "My screen time today: ${summary.totalMinutes} mins. Top app: ${summary.topApp} for ${summary.topAppMinutes} mins. Unlocks: ${summary.unlockCount}. Yesterday was ${summary.yesterdayMinutes} mins."

        return try {
            Log.d("OpenRouterApi", "generateCommentary model=$model totalMinutes=${summary.totalMinutes}")
            val response: ChatResponse = client.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", "You are a casual, playful, internet-native AI. Give a short 1-2 sentence reaction to the user's screen time data. Include light roasting. Be concise."),
                        ChatMessage("user", prompt)
                    )
                ))
            }.body()
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            Log.d("OpenRouterApi", "generateCommentary success length=${content.length}")
            Result.success(content)
        } catch (e: Exception) {
            Log.e("OpenRouterApi", "generateCommentary failed: ${e.message}", e)
            Result.failure(Exception(e.message ?: "OpenRouter request failed"))
        }
    }
}

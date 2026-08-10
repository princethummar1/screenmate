package com.screenmate.app.core.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.UnknownHostException
import java.net.SocketTimeoutException

@Serializable
data class TmdbResult(
    val id: Int,
    val media_type: String = "movie",
    val title: String? = null,
    val name: String? = null,
    val poster_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
    val overview: String? = null
) {
    val displayTitle get() = title ?: name ?: "Unknown"
    val releaseYear get() = (release_date ?: first_air_date)?.take(4)?.toIntOrNull()
}

@Serializable
data class TmdbResponse(
    val results: List<TmdbResult>
)

class TmdbApi {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "https://api.themoviedb.org/3"

    suspend fun testConnection(token: String): Result<Boolean> {
        if (token.isBlank()) return Result.failure(Exception("Token is empty"))
        return try {
            val response = client.get("$baseUrl/movie/11") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
            }
            logRequest("/movie/11", response.status)
            when (response.status) {
                HttpStatusCode.OK -> Result.success(true)
                HttpStatusCode.Unauthorized -> Result.failure(Exception("401 Unauthorized - Invalid or expired token."))
                else -> Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: UnknownHostException) {
            logError("/movie/11", "DNS/Host resolution failure", e)
            Result.failure(Exception("DNS/Network Failure: Cannot resolve api.themoviedb.org"))
        } catch (e: SocketTimeoutException) {
            logError("/movie/11", "Timeout", e)
            Result.failure(Exception("Network Timeout"))
        } catch (e: Exception) {
            logError("/movie/11", "Unknown error", e)
            Result.failure(Exception("Network Error: ${e.message}"))
        }
    }

    suspend fun testSearch(token: String): Result<Boolean> {
        if (token.isBlank()) return Result.failure(Exception("Token is empty"))
        return try {
            val response = client.get("$baseUrl/search/movie") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                url { parameters.append("query", "Interstellar") }
            }
            logRequest("/search/movie", response.status)
            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.body<TmdbResponse>()
                    if (body.results.isNotEmpty()) Result.success(true)
                    else Result.failure(Exception("200 OK, but no search results returned."))
                }
                HttpStatusCode.Unauthorized -> Result.failure(Exception("401 Unauthorized - Invalid or expired token."))
                else -> Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            logError("/search/movie", "Search failed", e)
            Result.failure(Exception("Search Network Error: ${e.message}"))
        }
    }

    suspend fun searchMulti(query: String, token: String): List<TmdbResult> {
        if (token.isBlank() || query.isBlank()) return emptyList()
        return try {
            val response = client.get("$baseUrl/search/multi") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
                url { parameters.append("query", query) }
            }
            logRequest("/search/multi", response.status)
            if (response.status == HttpStatusCode.OK) {
                val body = response.body<TmdbResponse>()
                body.results.filter { it.media_type == "movie" || it.media_type == "tv" }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logError("/search/multi", "SearchMulti failed", e)
            emptyList()
        }
    }

    private fun logRequest(endpoint: String, status: HttpStatusCode) {
        Log.d("TmdbApi", "[TMDb Request] $endpoint -> ${status.value} ${status.description}")
    }

    private fun logError(endpoint: String, message: String, e: Exception) {
        Log.e("TmdbApi", "[TMDb Error] $endpoint -> $message", e)
    }
}

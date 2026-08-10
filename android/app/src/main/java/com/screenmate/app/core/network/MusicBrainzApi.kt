package com.screenmate.app.core.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*

@Serializable
data class RecordingItem(
    val id: String,
    val title: String,
    @SerialName("length") val length: Int? = null,
    @SerialName("artist-credit") val artistCredit: List<ArtistCredit>? = null
)

@Serializable
data class ArtistCredit(val name: String? = null)

@Serializable
data class RecordingResponse(@SerialName("recordings") val recordings: List<JsonObjectWrapper>? = null)

@Serializable
data class JsonObjectWrapper(val id: String? = null)

data class RecordingResult(
    val id: String,
    val title: String,
    val artist: String?
)

class MusicBrainzApi(appName: String = "ScreenMatePersonal", contact: String = "contact") {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private val userAgent = "$appName ( $contact )"
    private val mutex = Mutex()
    private var lastRequestMillis = 0L

    // Simple rate limiter: ensure at least 1 second between requests
    private suspend fun throttle() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val diff = now - lastRequestMillis
            if (diff < 1000L) {
                val wait = 1000L - diff
                kotlinx.coroutines.delay(wait)
            }
            lastRequestMillis = System.currentTimeMillis()
        }
    }

    suspend fun searchRecordings(query: String): List<RecordingResult> {
        if (query.isBlank()) return emptyList()
        try {
            throttle()
            val url = Url("https://musicbrainz.org/ws/2/recording/")
            val response: HttpResponse = client.get(url) {
                header(HttpHeaders.UserAgent, userAgent)
                url { parameters.append("query", query); parameters.append("fmt", "json") }
            }
            val text = response.bodyAsText()
            // Lightweight parse: look for recordings[].id and title + artist-credit
            // To avoid heavy serialization, parse via kotlinx.json dynamic parsing
            val json = Json.parseToJsonElement(text).jsonObject
            val recs = json["recordings"]?.jsonArray ?: return emptyList()
            return recs.mapNotNull { elem ->
                val obj = elem.jsonObject
                val id = obj["id"]?.toString()?.trim('"') ?: return@mapNotNull null
                val title = obj["title"]?.toString()?.trim('"') ?: ""
                val artist = obj["artist-credit"]?.jsonArray?.firstOrNull()?.jsonObject?.get("name")?.toString()?.trim('"')
                RecordingResult(id = id, title = title, artist = artist)
            }
        } catch (e: Exception) {
            Log.e("MusicBrainzApi", "search failed: ${e.message}")
            return emptyList()
        }
    }
}

package com.screenmate.app.core.network

import io.ktor.client.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.body
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BooksVolumeInfo(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    @SerialName("publishedDate") val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null,
    val language: String? = null,
    val industryIdentifiers: List<IndustryIdentifier>? = null,
    val imageLinks: ImageLinks? = null
)

@Serializable
data class IndustryIdentifier(val type: String? = null, val identifier: String? = null)

@Serializable
data class ImageLinks(val smallThumbnail: String? = null, val thumbnail: String? = null)

@Serializable
data class BooksVolume(val id: String, val volumeInfo: BooksVolumeInfo)

@Serializable
data class BooksResponse(val items: List<BooksVolume>? = null)

data class BookResult(
    val id: String,
    val title: String,
    val subtitle: String?,
    val authors: List<String>?,
    val publisher: String?,
    val publishedDate: String?,
    val description: String?,
    val pageCount: Int?,
    val categories: List<String>?,
    val language: String?,
    val isbn: String?,
    val thumbnail: String?
)

class BooksApi {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun searchBooks(query: String, apiKey: String): List<BookResult> {
        if (query.isBlank()) return emptyList()
        return try {
            val url = Url("https://www.googleapis.com/books/v1/volumes")
            val response: BooksResponse = client.get(url) {
                url { parameters.append("q", query) ; parameters.append("key", apiKey) }
            }.body()

            response.items?.map { v ->
                val info = v.volumeInfo
                val isbn = info.industryIdentifiers?.firstOrNull { it.type?.contains("ISBN") == true }?.identifier
                val thumb = info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail
                BookResult(
                    id = v.id,
                    title = info.title ?: "",
                    subtitle = info.subtitle,
                    authors = info.authors,
                    publisher = info.publisher,
                    publishedDate = info.publishedDate,
                    description = info.description,
                    pageCount = info.pageCount,
                    categories = info.categories,
                    language = info.language,
                    isbn = isbn,
                    thumbnail = thumb
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

package com.keeftalk.chat.data.cloud

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream

@Serializable
data class PickerSessionRequest(
    val pickingConfig: PickingConfig
)

@Serializable
data class PickingConfig(
    val maxMediaItems: Int = 100
)

@Serializable
data class PickerSessionResponse(
    val id: String,
    val pickerUri: String,
    val mediaItemsSet: Boolean = false
)

@Serializable
data class MediaItemsResponse(
    val mediaItems: List<MediaItem>? = null,
    val nextPageToken: String? = null
)

@Serializable
data class MediaItem(
    val id: String,
    val baseUrl: String,
    val mimeType: String,
    val filename: String? = null
)

class GooglePhotosService(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createSession(accessToken: String): Result<PickerSessionResponse> {
        return try {
            val response: HttpResponse = httpClient.post("https://photospicker.googleapis.com/v1/sessions") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(PickerSessionRequest(PickingConfig()))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to create session: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSession(sessionId: String, accessToken: String): Result<PickerSessionResponse> {
        return try {
            val response: HttpResponse = httpClient.get("https://photospicker.googleapis.com/v1/sessions/$sessionId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get session: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listMediaItems(sessionId: String, accessToken: String): Result<List<MediaItem>> {
        return try {
            val response: HttpResponse = httpClient.get("https://photospicker.googleapis.com/v1/mediaItems") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("sessionId", sessionId)
            }
            if (response.status.isSuccess()) {
                val body: MediaItemsResponse = response.body()
                Result.success(body.mediaItems ?: emptyList())
            } else {
                Result.failure(Exception("Failed to list items: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadMedia(url: String, targetFile: File): Result<Unit> {
        return try {
            // Append =d to get the original bytes
            val downloadUrl = if (url.contains("?")) "$url&d" else "$url=d"
            val response: HttpResponse = httpClient.get(downloadUrl)
            if (response.status.isSuccess()) {
                val channel: ByteReadChannel = response.bodyAsChannel()
                targetFile.outputStream().use { output: OutputStream ->
                    channel.copyTo(output)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to download media: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

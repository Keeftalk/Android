package com.keeftalk.chat.data.cloud

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.Serializable
import java.io.File
import java.io.OutputStream

@Serializable
data class DriveFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: String? = null
)

class GoogleDriveService(private val httpClient: HttpClient) {

    suspend fun getFileMetadata(fileId: String, accessToken: String): Result<DriveFileMetadata> {
        return try {
            val response: HttpResponse = httpClient.get("https://www.googleapis.com/drive/v3/files/$fileId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("fields", "id, name, mimeType, size")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get Drive metadata: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(fileId: String, accessToken: String, targetFile: File): Result<Unit> {
        return try {
            val response: HttpResponse = httpClient.get("https://www.googleapis.com/drive/v3/files/$fileId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("alt", "media")
            }
            if (response.status.isSuccess()) {
                val channel: ByteReadChannel = response.bodyAsChannel()
                targetFile.outputStream().use { output: OutputStream ->
                    channel.copyTo(output)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to download Drive file: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

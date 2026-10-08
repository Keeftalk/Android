package tn.keeftalk.chat.util

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.OutputStream

@Serializable
data class UploadTargetResponse(
    val url: String,
    @SerialName("objectKey") val objectKey: String,
    val bucket: String,
    @SerialName("countryCode") val countryCode: String,
    val region: String
)

@Serializable
data class DownloadTargetResponse(
    val url: String,
    @SerialName("objectKey") val objectKey: String
)

@Serializable
data class StorageOperationResponse(
    val success: Boolean = false,
    val size: Long = 0L,
    @SerialName("newPath") val newPath: String? = null,
    val error: String? = null,
    val code: String? = null
)

class InfrastructureStorageException(message: String) : Exception(message)
class StorageUploadException(message: String) : Exception(message)
class StorageDownloadException(message: String) : Exception(message)

class R2StorageClient(
    private val supabaseProvider: suspend () -> SupabaseClient,
    private val httpClient: HttpClient = HttpClient(OkHttp)
) {
    private val TAG = "R2StorageClient"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getUploadUrl(
        fileId: String,
        category: String = "files",
        subpath: String = "original",
        contentType: String? = null
    ): Result<UploadTargetResponse> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "get-upload-url")
                put("fileId", fileId)
                put("category", category)
                put("subpath", subpath)
                if (contentType != null) put("contentType", contentType)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val responseText = response.bodyAsText()

            if (responseText.contains("INFRASTRUCTURE_ERROR") || responseText.contains("BUCKET_NOT_FOUND")) {
                val opResp = runCatching { json.decodeFromString<StorageOperationResponse>(responseText) }.getOrNull()
                val errorMsg = opResp?.error ?: "INFRASTRUCTURE_ERROR: Target R2 bucket not provisioned"
                Log.e(TAG, "INFRASTRUCTURE_ERROR | $errorMsg")
                return@withContext Result.failure(InfrastructureStorageException(errorMsg))
            }

            val target = json.decodeFromString<UploadTargetResponse>(responseText)
            Log.d(TAG, "GET_UPLOAD_URL_SUCCESS | key=${target.objectKey} | bucket=${target.bucket}")
            Result.success(target)
        } catch (e: Exception) {
            Log.e(TAG, "GET_UPLOAD_URL_FAILED | fileId=$fileId | message=${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadByteArray(
        presignedUrl: String,
        bytes: ByteArray,
        contentType: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = httpClient.put(presignedUrl) {
                if (contentType != null) {
                    header("Content-Type", contentType)
                }
                setBody(bytes)
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created || response.status == HttpStatusCode.NoContent) {
                Result.success(Unit)
            } else {
                val errorMsg = "Upload failed with status: ${response.status.value}"
                Log.e(TAG, "UPLOAD_FAILED | $errorMsg")
                Result.failure(StorageUploadException(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "UPLOAD_STREAM_FAILED | message=${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadStream(
        presignedUrl: String,
        channel: ByteReadChannel,
        contentLength: Long,
        onProgress: (uploaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = httpClient.put(presignedUrl) {
                header("Content-Length", contentLength.toString())
                setBody(channel)
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created || response.status == HttpStatusCode.NoContent) {
                Result.success(Unit)
            } else {
                val errorMsg = "Upload failed with status: ${response.status.value}"
                Log.e(TAG, "UPLOAD_FAILED | $errorMsg")
                Result.failure(StorageUploadException(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "UPLOAD_STREAM_FAILED | message=${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun finalizeUpload(fileId: String, objectKey: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "finalize-upload")
                put("fileId", fileId)
                put("path", objectKey)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val opResp = json.decodeFromString<StorageOperationResponse>(response.bodyAsText())

            if (opResp.success) {
                Result.success(opResp.size)
            } else {
                Result.failure(StorageUploadException(opResp.error ?: "Finalize upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDownloadUrl(
        fileId: String? = null,
        path: String? = null,
        category: String = "files",
        subpath: String = "original"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "get-download-url")
                if (fileId != null) put("fileId", fileId)
                if (path != null) put("path", path)
                put("category", category)
                put("subpath", subpath)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val target = json.decodeFromString<DownloadTargetResponse>(response.bodyAsText())
            Result.success(target.url)
        } catch (e: Exception) {
            Log.e(TAG, "GET_DOWNLOAD_URL_FAILED | message=${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadToChannel(presignedUrl: String): Result<ByteReadChannel> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = httpClient.get(presignedUrl)
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.bodyAsChannel())
            } else {
                Result.failure(StorageDownloadException("Download failed with status ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadToStream(
        presignedUrl: String,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = httpClient.get(presignedUrl)
            if (response.status == HttpStatusCode.OK) {
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L
                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L

                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    outputStream.write(buffer, 0, read)
                    totalBytesRead += read
                    if (contentLength > 0) {
                        onProgress(totalBytesRead.toFloat() / contentLength)
                    }
                }
                outputStream.flush()
                Result.success(Unit)
            } else {
                Result.failure(StorageDownloadException("Download failed with status ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveToTrash(fileId: String, path: String? = null): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "move-to-trash")
                put("fileId", fileId)
                if (path != null) put("path", path)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val opResp = json.decodeFromString<StorageOperationResponse>(response.bodyAsText())
            if (opResp.success) {
                Result.success(opResp.newPath)
            } else {
                Result.failure(Exception(opResp.error ?: "Move to trash failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromTrash(fileId: String, path: String? = null): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "restore-from-trash")
                put("fileId", fileId)
                if (path != null) put("path", path)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val opResp = json.decodeFromString<StorageOperationResponse>(response.bodyAsText())
            if (opResp.success) {
                Result.success(opResp.newPath)
            } else {
                Result.failure(Exception(opResp.error ?: "Restore from trash failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteObject(fileId: String? = null, path: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "delete-object")
                if (fileId != null) put("fileId", fileId)
                if (path != null) put("path", path)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val opResp = json.decodeFromString<StorageOperationResponse>(response.bodyAsText())
            if (opResp.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(opResp.error ?: "Delete object failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

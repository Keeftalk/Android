package tn.keeftalk.chat.extension.data

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

@Serializable
data class RemotePublisher(
    val id: String = "",
    @SerialName("display_name") val displayName: String = "Keeftalk",
    @SerialName("is_verified") val isVerified: Boolean = true
)

@Serializable
data class RemoteStoreExtension(
    val id: String,
    val slug: String = "",
    val name: String,
    val type: String = "TOOL",
    @SerialName("publisher_id") val publisherId: String = "",
    val publisher: RemotePublisher? = null,
    @SerialName("short_description") val shortDescription: String = "",
    @SerialName("long_description") val longDescription: String = "",
    val category: String = "tools",
    @SerialName("age_rating") val ageRating: String = "4+",
    val languages: List<String> = emptyList(),
    @SerialName("compatible_modules") val compatibleModules: List<String> = emptyList(),
    @SerialName("min_app_version") val minAppVersion: String = "3.0.0",
    @SerialName("current_version") val currentVersion: String = "1.0.0",
    val status: String = "PUBLISHED",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("download_count") val downloadCount: Long = 0L,
    @SerialName("install_count") val installCount: Long = 0L,
    @SerialName("rating_avg") val ratingAvg: Double = 0.0,
    @SerialName("rating_count") val ratingCount: Int = 0,
    @SerialName("package_size_bytes") val packageSizeBytes: Long = 0L,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("screenshot01") val screenshot01: String? = null,
    @SerialName("screenshot02") val screenshot02: String? = null,
    @SerialName("screenshot03") val screenshot03: String? = null,
    @SerialName("screenshot04") val screenshot04: String? = null,
    @SerialName("screenshot05") val screenshot05: String? = null,
    @SerialName("screenshot06") val screenshot06: String? = null,
    @SerialName("screenshot07") val screenshot07: String? = null
) {
    val screenshots: List<String>
        get() = listOfNotNull(
            screenshot01, screenshot02, screenshot03,
            screenshot04, screenshot05, screenshot06, screenshot07
        ).filter { it.isNotBlank() }
}

@Serializable
data class RemoteStoreRelease(
    val id: String = "",
    @SerialName("extension_id") val extensionId: String,
    val version: String = "1.0.0",
    @SerialName("package_object_key") val packageObjectKey: String,
    @SerialName("package_hash_sha256") val packageHashSha256: String = "",
    @SerialName("package_size_bytes") val packageSizeBytes: Long = 0L
)

@Serializable
data class RemoteReview(
    val id: String = "",
    @SerialName("extension_id") val extensionId: String,
    @SerialName("author_name") val authorName: String = "Keeftalk User",
    val rating: Int = 5,
    val title: String = "",
    @SerialName("review_text") val reviewText: String = "",
    @SerialName("helpful_count") val helpfulCount: Int = 0,
    @SerialName("developer_reply") val developerReply: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CatalogResponse(
    val success: Boolean = false,
    val category: String = "all",
    val country: String = "GLOBAL",
    val extensions: List<RemoteStoreExtension> = emptyList()
)

@Serializable
data class DownloadResponse(
    val success: Boolean = false,
    val downloadUrl: String = "",
    val version: String = "1.0.0",
    val sha256: String = "",
    val sizeBytes: Long = 0L
)

class StoreRepository(
    private val context: Context,
    private val httpClient: HttpClient = HttpClient(OkHttp)
) {
    private val TAG = "StoreRepository"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val storeApiUrl = "https://lhlylcyuvfpauwvuxped.supabase.co/functions/v1/store-api"
    private val supabaseRestUrl = "https://lhlylcyuvfpauwvuxped.supabase.co/rest/v1"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxobHlsY3l1dmZwYXV3dnV4cGVkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTUzNDMsImV4cCI6MjA5Nzg3MTM0M30.vu5Zga2iX37NlDV9v0IgQp_PjDNWJmmqqhrvLJ9Un9E"

    suspend fun fetchCatalog(
        category: String = "all",
        query: String = "",
        limit: Int = 30
    ): Result<List<RemoteStoreExtension>> = withContext(Dispatchers.IO) {
        try {
            val url = "$storeApiUrl/catalog?category=$category&q=$query&limit=$limit"
            val response = httpClient.get(url) {
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $supabaseAnonKey")
            }
            if (response.status == HttpStatusCode.OK) {
                val parsed = json.decodeFromString<CatalogResponse>(response.bodyAsText())
                if (parsed.extensions.isNotEmpty()) {
                    return@withContext Result.success(parsed.extensions)
                }
            }

            fetchCatalogFromRest(category, query, limit)
        } catch (e: Exception) {
            Log.w(TAG, "Edge function query failed (${e.message}). Falling back to PostgREST table query...")
            fetchCatalogFromRest(category, query, limit)
        }
    }

    private suspend fun fetchCatalogFromRest(
        category: String,
        query: String,
        limit: Int
    ): Result<List<RemoteStoreExtension>> = withContext(Dispatchers.IO) {
        try {
            var restUrl = "$supabaseRestUrl/store_extensions?select=*,publisher:store_publishers(*)&status=eq.PUBLISHED&limit=$limit"
            if (category != "all" && category.isNotBlank()) {
                restUrl += "&category=eq.$category"
            }
            if (query.isNotBlank()) {
                restUrl += "&name=ilike.*$query*"
            }

            val response = httpClient.get(restUrl) {
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $supabaseAnonKey")
            }

            if (response.status == HttpStatusCode.OK) {
                val bodyText = response.bodyAsText()
                val extensions = json.decodeFromString<List<RemoteStoreExtension>>(bodyText)
                Result.success(extensions)
            } else {
                Result.failure(Exception("PostgREST catalog query failed with status ${response.status.value}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching catalog from PostgREST: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchRankings(
        mode: String = "global",
        countryCode: String = "TN"
    ): Result<List<RemoteStoreExtension>> = withContext(Dispatchers.IO) {
        try {
            val url = "$storeApiUrl/rankings?mode=$mode&country=$countryCode"
            val response = httpClient.get(url) {
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $supabaseAnonKey")
            }
            if (response.status == HttpStatusCode.OK) {
                val parsed = json.decodeFromString<CatalogResponse>(response.bodyAsText())
                if (parsed.extensions.isNotEmpty()) {
                    return@withContext Result.success(parsed.extensions)
                }
            }

            fetchCatalogFromRest("all", "", 30)
        } catch (e: Exception) {
            Log.w(TAG, "Rankings query failed (${e.message}). Falling back to PostgREST table query...")
            fetchCatalogFromRest("all", "", 30)
        }
    }

    suspend fun downloadPackageToCache(extensionId: String, version: String? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            var downloadUrl: String? = null
            var pkgVersion = version ?: "1.0.0"

            try {
                val url = "$storeApiUrl/download?id=$extensionId" + if (version != null) "&version=$version" else ""
                val response = httpClient.get(url) {
                    header("apikey", supabaseAnonKey)
                    header("Authorization", "Bearer $supabaseAnonKey")
                }

                if (response.status == HttpStatusCode.OK) {
                    val dlResp = json.decodeFromString<DownloadResponse>(response.bodyAsText())
                    if (dlResp.success && dlResp.downloadUrl.isNotBlank()) {
                        downloadUrl = dlResp.downloadUrl
                        pkgVersion = dlResp.version
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Edge function download request failed (${e.message}). Trying PostgREST query...")
            }

            if (downloadUrl.isNullOrBlank()) {
                val restUrl = "$supabaseRestUrl/store_releases?extension_id=eq.$extensionId&status=eq.PUBLISHED&select=*&order=version.desc&limit=1"
                val response = httpClient.get(restUrl) {
                    header("apikey", supabaseAnonKey)
                    header("Authorization", "Bearer $supabaseAnonKey")
                }

                if (response.status == HttpStatusCode.OK) {
                    val releases = json.decodeFromString<List<RemoteStoreRelease>>(response.bodyAsText())
                    val release = releases.firstOrNull()
                    if (release != null) {
                        downloadUrl = release.packageObjectKey
                        pkgVersion = release.version
                    }
                }
            }

            if (downloadUrl.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Unable to resolve package download URL for '$extensionId'"))
            }

            val pkgResponse = httpClient.get(downloadUrl)
            if (pkgResponse.status != HttpStatusCode.OK) {
                return@withContext Result.failure(Exception("Package download failed with HTTP ${pkgResponse.status.value}"))
            }

            val cacheDir = File(context.cacheDir, "store_downloads")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val targetFile = File(cacheDir, "$extensionId-v$pkgVersion.ktxp")
            val bytes = pkgResponse.body<ByteArray>()
            targetFile.writeBytes(bytes)

            recordDownloadCount(extensionId)

            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading package '$extensionId': ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchReviews(extensionId: String): Result<List<RemoteReview>> = withContext(Dispatchers.IO) {
        try {
            val restUrl = "$supabaseRestUrl/store_ratings?extension_id=eq.$extensionId&order=created_at.desc&limit=20"
            val response = httpClient.get(restUrl) {
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $supabaseAnonKey")
            }
            if (response.status == HttpStatusCode.OK) {
                val reviews = json.decodeFromString<List<RemoteReview>>(response.bodyAsText())
                Result.success(reviews)
            } else {
                Result.failure(Exception("Failed to fetch reviews: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching reviews for '$extensionId': ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun submitReview(
        extensionId: String,
        rating: Int,
        title: String,
        reviewText: String,
        authorName: String = "Keeftalk User"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val restUrl = "$supabaseRestUrl/store_ratings"
            val payload = buildJsonObject {
                put("extension_id", extensionId)
                put("rating", rating)
                put("title", title)
                put("review_text", reviewText)
                put("author_name", authorName)
            }.toString()

            val response = httpClient.post(restUrl) {
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $supabaseAnonKey")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created || response.status == HttpStatusCode.NoContent) {
                Result.success(true)
            } else {
                Result.failure(Exception("Submit review failed with HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting review for '$extensionId': ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun recordDownloadCount(extensionId: String) = withContext(Dispatchers.IO) {
        try {
            val url = "$storeApiUrl/download_ping"
            httpClient.post(url) {
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $supabaseAnonKey")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("extension_id", extensionId) }.toString())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Download ping failed in background: ${e.message}")
        }
    }

    suspend fun recordInstallationEvent(extensionId: String, version: String) = withContext(Dispatchers.IO) {
        try {
            val restUrl = "$supabaseRestUrl/store_installations"
            val payload = buildJsonObject {
                put("extension_id", extensionId)
                put("installed_version", version)
                put("status", "INSTALLED")
            }.toString()

            httpClient.post(restUrl) {
                header("apikey", supabaseAnonKey)
                header("Authorization", "Bearer $supabaseAnonKey")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Installation record failed in background: ${e.message}")
        }
    }
}

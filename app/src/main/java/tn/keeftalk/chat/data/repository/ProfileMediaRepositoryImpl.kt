package tn.keeftalk.chat.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import tn.keeftalk.chat.data.local.dao.ChatDao
import tn.keeftalk.chat.data.local.dao.ProfileMediaDao
import tn.keeftalk.chat.data.local.dao.UserDao
import tn.keeftalk.chat.data.local.entities.ProfileMediaEntity
import tn.keeftalk.chat.data.local.entities.ProfileMediaLikeEntity
import tn.keeftalk.chat.domain.model.MediaLiker
import tn.keeftalk.chat.domain.model.MediaStatus
import tn.keeftalk.chat.domain.model.MediaType
import tn.keeftalk.chat.domain.model.ProfileMedia
import tn.keeftalk.chat.domain.repository.ProfileMediaRepository
import java.time.Instant

class ProfileMediaRepositoryImpl(
    private val supabaseProvider: suspend () -> SupabaseClient,
    private val mediaDao: ProfileMediaDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao
) : ProfileMediaRepository {

    private val TAG = "ProfileMediaRepo"
    private val json = Json { ignoreUnknownKeys = true }

    override fun getMediaHistory(userId: String, mediaType: MediaType): Flow<List<ProfileMedia>> {
        return mediaDao.getMediaHistory(userId, mediaType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun String.isValidUuid(): Boolean {
        if (this.isBlank() || this.startsWith("fallback_") || this.startsWith("content://") || this.startsWith("file://")) return false
        return try {
            java.util.UUID.fromString(this)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun fetchMediaHistoryFromRemote(userId: String, mediaType: MediaType): Result<List<ProfileMedia>> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val currentUserId = supabase.auth.currentUserOrNull()?.id

            val result = supabase.postgrest["profile_media"]
                .select() {
                    filter {
                        eq("user_id", userId)
                        eq("media_type", mediaType.name)
                        neq("status", "DETENTION")
                    }
                }

            val mediaList = mutableListOf<ProfileMedia>()
            val rawArray = result.data.let { json.parseToJsonElement(it).jsonArray }

            for (elem in rawArray) {
                val obj = elem.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: continue
                val typeStr = obj["media_type"]?.jsonPrimitive?.content ?: mediaType.name
                val origKey = obj["original_object_key"]?.jsonPrimitive?.content ?: ""
                val compKey = obj["compressed_object_key"]?.jsonPrimitive?.content
                val origCdn = obj["original_cdn_url"]?.jsonPrimitive?.content ?: ""
                val compCdn = obj["compressed_cdn_url"]?.jsonPrimitive?.content
                val statusStr = obj["status"]?.jsonPrimitive?.content ?: "ACTIVE"
                val isActive = obj["is_active"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                val createdAtStr = obj["created_at"]?.jsonPrimitive?.content
                val activatedAtStr = obj["activated_at"]?.jsonPrimitive?.content

                val createdAt = parseTimestamp(createdAtStr)
                val activatedAt = parseTimestamp(activatedAtStr)

                // Fetch likes count and current user like status
                val likesRes = supabase.postgrest["profile_media_likes"]
                    .select(columns = Columns.list("user_id")) {
                        filter { eq("media_id", id) }
                    }
                val likesArray = likesRes.data.let { json.parseToJsonElement(it).jsonArray }
                val likesCount = likesArray.size
                val isLikedByMe = currentUserId != null && likesArray.any {
                    it.jsonObject["user_id"]?.jsonPrimitive?.content == currentUserId
                }

                val media = ProfileMedia(
                    id = id,
                    userId = userId,
                    mediaType = try { MediaType.valueOf(typeStr) } catch (_: Exception) { mediaType },
                    originalObjectKey = origKey,
                    compressedObjectKey = compKey,
                    originalCdnUrl = origCdn,
                    compressedCdnUrl = compCdn,
                    status = try { MediaStatus.valueOf(statusStr) } catch (_: Exception) { MediaStatus.ACTIVE },
                    isActive = isActive,
                    createdAt = createdAt,
                    activatedAt = activatedAt,
                    likesCount = likesCount,
                    isLikedByMe = isLikedByMe
                )

                mediaList.add(media)
            }

            mediaList.sortByDescending { it.activatedAt }

            // Save to local Room cache
            val entities = mediaList.map { it.toEntity() }
            mediaDao.insertOrUpdateMedia(entities)

            Result.success(mediaList)
        } catch (e: Exception) {
            Log.e(TAG, "fetchMediaHistoryFromRemote failed", e)
            Result.failure(e)
        }
    }

    override suspend fun likeMedia(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        var targetMediaId = mediaId
        val cached = mediaDao.getMediaById(mediaId)

        if (!targetMediaId.isValidUuid()) {
            Log.w(TAG, "likeMedia called with non-UUID mediaId '$mediaId'. Attempting resolution...")
            val supabase = supabaseProvider()
            val userId = supabase.auth.currentUserOrNull()?.id

            if (userId != null) {
                val origUrl = cached?.originalCdnUrl ?: ""
                val compUrl = cached?.compressedCdnUrl ?: origUrl
                val typeStr = cached?.mediaType ?: "AVATAR"
                val type = try { MediaType.valueOf(typeStr) } catch (_: Exception) { MediaType.AVATAR }

                val activeMedia = mediaDao.getActiveMedia(userId, type.name)?.takeIf { it.id.isValidUuid() }

                if (activeMedia != null) {
                    targetMediaId = activeMedia.id
                    Log.d(TAG, "Resolved fallback media '$mediaId' to active media UUID '${targetMediaId}'")
                } else if (origUrl.isNotBlank() && !origUrl.startsWith("file://") && !origUrl.startsWith("content://")) {
                    val regResult = registerProfileMedia(
                        userId = userId,
                        mediaType = type,
                        originalObjectKey = origUrl,
                        compressedObjectKey = compUrl,
                        originalCdnUrl = origUrl,
                        compressedCdnUrl = compUrl
                    )
                    regResult.onSuccess { newMedia ->
                        if (mediaId != newMedia.id) {
                            mediaDao.deleteMedia(mediaId)
                        }
                        targetMediaId = newMedia.id
                        Log.d(TAG, "Registered fallback media to remote. Real UUID: ${newMedia.id}")
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to register fallback media before liking: ${e.message}")
                    }
                }
            }
        }

        if (!targetMediaId.isValidUuid()) {
            val errMsg = "Cannot like media: invalid input syntax for type uuid: '$targetMediaId'"
            Log.e(TAG, "likeMedia failed: $errMsg")
            return@withContext Result.failure(Exception(errMsg))
        }

        val updatedCached = mediaDao.getMediaById(targetMediaId) ?: cached
        val initialCount = updatedCached?.likesCount ?: 0

        // Optimistic UI update
        mediaDao.updateLikesInfo(targetMediaId, initialCount + 1, true)

        try {
            val supabase = supabaseProvider()
            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")

            supabase.postgrest["profile_media_likes"].insert(
                buildJsonObject {
                    put("media_id", targetMediaId)
                    put("user_id", userId)
                }
            )

            mediaDao.insertLike(ProfileMediaLikeEntity(mediaId = targetMediaId, userId = userId))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "likeMedia failed, rolling back", e)
            // Rollback on error
            mediaDao.updateLikesInfo(targetMediaId, initialCount, false)
            Result.failure(e)
        }
    }

    override suspend fun unlikeMedia(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        var targetMediaId = mediaId
        if (!targetMediaId.isValidUuid()) {
            val cached = mediaDao.getMediaById(mediaId)
            val typeStr = cached?.mediaType ?: "AVATAR"
            val type = try { MediaType.valueOf(typeStr) } catch (_: Exception) { MediaType.AVATAR }
            val supabase = supabaseProvider()
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                val activeMedia = mediaDao.getActiveMedia(userId, type.name)?.takeIf { it.id.isValidUuid() }
                if (activeMedia != null) {
                    targetMediaId = activeMedia.id
                }
            }
        }

        if (!targetMediaId.isValidUuid()) {
            val errMsg = "Cannot unlike media: invalid input syntax for type uuid: '$targetMediaId'"
            Log.e(TAG, "unlikeMedia failed: $errMsg")
            return@withContext Result.failure(Exception(errMsg))
        }

        val cached = mediaDao.getMediaById(targetMediaId)
        val initialCount = cached?.likesCount ?: 1
        val newCount = (initialCount - 1).coerceAtLeast(0)

        // Optimistic UI update
        mediaDao.updateLikesInfo(targetMediaId, newCount, false)

        try {
            val supabase = supabaseProvider()
            val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")

            supabase.postgrest["profile_media_likes"].delete {
                filter {
                    eq("media_id", targetMediaId)
                    eq("user_id", userId)
                }
            }

            mediaDao.deleteLike(targetMediaId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "unlikeMedia failed, rolling back", e)
            // Rollback on error
            mediaDao.updateLikesInfo(mediaId, initialCount, true)
            Result.failure(e)
        }
    }

    override suspend fun deleteMediaToDetention(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "move-media-to-detention")
                put("mediaId", mediaId)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val text = response.bodyAsText()

            if (text.contains("error") && !text.contains("\"success\":true")) {
                val err = json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content ?: "Failed to move media to detention"
                return@withContext Result.failure(Exception(err))
            }

            mediaDao.markAsDetained(mediaId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteMediaToDetention failed", e)
            Result.failure(e)
        }
    }

    override suspend fun restoreMediaFromDetention(mediaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "restore-media-from-detention")
                put("mediaId", mediaId)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val text = response.bodyAsText()

            if (text.contains("error") && !text.contains("\"success\":true")) {
                val err = json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content ?: "Failed to restore media"
                return@withContext Result.failure(Exception(err))
            }

            mediaDao.markAsRestored(mediaId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "restoreMediaFromDetention failed", e)
            Result.failure(e)
        }
    }

    override suspend fun setActiveMedia(userId: String, mediaId: String, mediaType: MediaType): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()

            val payload = buildJsonObject {
                put("p_user_id", userId)
                put("p_media_id", mediaId)
            }

            supabase.postgrest.rpc("set_active_profile_media", payload)

            val media = mediaDao.getMediaById(mediaId)
            val activeUrl = media?.compressedCdnUrl ?: media?.originalCdnUrl

            if (activeUrl != null) {
                val profileUpdate = if (mediaType == MediaType.AVATAR) {
                    mapOf("avatar_url" to activeUrl)
                } else {
                    mapOf("cover_url" to activeUrl)
                }
                supabase.postgrest["profiles"].update(profileUpdate) {
                    filter { eq("id", userId) }
                }
            }

            mediaDao.setActiveMedia(userId, mediaType.name, mediaId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "setActiveMedia failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getMediaLikers(mediaId: String): Result<List<MediaLiker>> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val likesRes = supabase.postgrest["profile_media_likes"]
                .select(columns = Columns.list("user_id")) {
                    filter { eq("media_id", mediaId) }
                }

            val likesArray = json.parseToJsonElement(likesRes.data).jsonArray
            val userIds = likesArray.mapNotNull { it.jsonObject["user_id"]?.jsonPrimitive?.content }

            if (userIds.isEmpty()) return@withContext Result.success(emptyList())

            val profilesRes = supabase.postgrest["profiles"]
                .select() {
                    filter {
                        isIn("id", userIds)
                    }
                }

            val profilesArray = json.parseToJsonElement(profilesRes.data).jsonArray
            val likers = mutableListOf<MediaLiker>()

            for (p in profilesArray) {
                val obj = p.jsonObject
                val uId = obj["id"]?.jsonPrimitive?.content ?: continue
                val username = obj["username"]?.jsonPrimitive?.content
                val fullName = obj["full_name"]?.jsonPrimitive?.content
                val avatarUrl = obj["avatar_url"]?.jsonPrimitive?.content

                val isLocalContact = userDao.getUserById(uId) != null || chatDao.getChatById(uId) != null
                likers.add(
                    MediaLiker(
                        userId = uId,
                        username = username,
                        fullName = fullName,
                        avatarUrl = avatarUrl,
                        isContact = isLocalContact
                    )
                )
            }

            Result.success(likers)
        } catch (e: Exception) {
            Log.e(TAG, "getMediaLikers failed", e)
            Result.failure(e)
        }
    }

    override suspend fun registerProfileMedia(
        userId: String,
        mediaType: MediaType,
        originalObjectKey: String,
        compressedObjectKey: String?,
        originalCdnUrl: String,
        compressedCdnUrl: String?
    ): Result<ProfileMedia> = withContext(Dispatchers.IO) {
        try {
            val supabase = supabaseProvider()
            val payload = buildJsonObject {
                put("action", "register-profile-media")
                put("mediaType", mediaType.name)
                put("originalObjectKey", originalObjectKey)
                if (compressedObjectKey != null) put("compressedObjectKey", compressedObjectKey)
                put("originalCdnUrl", originalCdnUrl)
                if (compressedCdnUrl != null) put("compressedCdnUrl", compressedCdnUrl)
            }

            val response = supabase.functions.invoke("storage", body = payload)
            val text = response.bodyAsText()

            val responseObj = json.parseToJsonElement(text).jsonObject
            val mediaObj = responseObj["media"]?.jsonObject ?: throw Exception("Registration response missing media payload")

            val id = mediaObj["id"]?.jsonPrimitive?.content ?: throw Exception("Missing media ID")
            val media = ProfileMedia(
                id = id,
                userId = userId,
                mediaType = mediaType,
                originalObjectKey = originalObjectKey,
                compressedObjectKey = compressedObjectKey,
                originalCdnUrl = originalCdnUrl,
                compressedCdnUrl = compressedCdnUrl,
                status = MediaStatus.ACTIVE,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                activatedAt = System.currentTimeMillis()
            )

            mediaDao.setActiveMedia(userId, mediaType.name, id)
            mediaDao.insertOrUpdateMedia(media.toEntity())

            try {
                fetchMediaHistoryFromRemote(userId, mediaType)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }

            Result.success(media)
        } catch (e: Exception) {
            Log.e(TAG, "registerProfileMedia failed", e)
            Result.failure(e)
        }
    }

    private fun parseTimestamp(str: String?): Long {
        if (str.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(str).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun ProfileMediaEntity.toDomain() = ProfileMedia(
        id = id,
        userId = userId,
        mediaType = try { MediaType.valueOf(mediaType) } catch (_: Exception) { MediaType.AVATAR },
        originalObjectKey = originalObjectKey,
        compressedObjectKey = compressedObjectKey,
        originalCdnUrl = originalCdnUrl,
        compressedCdnUrl = compressedCdnUrl,
        status = try { MediaStatus.valueOf(status) } catch (_: Exception) { MediaStatus.ACTIVE },
        isActive = isActive,
        createdAt = createdAt,
        activatedAt = activatedAt,
        deletedAt = deletedAt,
        scheduledPurgeAt = scheduledPurgeAt,
        likesCount = likesCount,
        isLikedByMe = isLikedByMe
    )

    private fun ProfileMedia.toEntity() = ProfileMediaEntity(
        id = id,
        userId = userId,
        mediaType = mediaType.name,
        originalObjectKey = originalObjectKey,
        compressedObjectKey = compressedObjectKey,
        originalCdnUrl = originalCdnUrl,
        compressedCdnUrl = compressedCdnUrl,
        status = status.name,
        isActive = isActive,
        createdAt = createdAt,
        activatedAt = activatedAt,
        deletedAt = deletedAt,
        scheduledPurgeAt = scheduledPurgeAt,
        likesCount = likesCount,
        isLikedByMe = isLikedByMe
    )
}

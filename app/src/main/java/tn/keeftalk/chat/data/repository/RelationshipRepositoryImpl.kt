package tn.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.Relationship
import tn.keeftalk.chat.domain.model.RelationshipSummary
import tn.keeftalk.chat.domain.model.FirstContactPolicy
import tn.keeftalk.chat.domain.model.CanMessageResult
import tn.keeftalk.chat.domain.repository.RelationshipRepository
import tn.keeftalk.chat.domain.repository.ChatRepository
import tn.keeftalk.chat.domain.repository.PrivacyRepository
import tn.keeftalk.chat.data.local.dao.RelationshipDao
import tn.keeftalk.chat.data.local.entities.toDomain
import tn.keeftalk.chat.data.local.entities.toEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RelationshipRepositoryImpl(
    private val context: Context,
    private val chatRepository: ChatRepository,
    private val privacyRepository: PrivacyRepository,
    private val relationshipDao: RelationshipDao
) : RelationshipRepository {

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    init {
        // Automatically invalidate all cache when blocked users change
        repositoryScope.launch {
            privacyRepository.getBlockedUsers().collect {
                invalidateAll()
            }
        }
    }

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    override suspend fun getRelationship(targetUserId: String): Result<Relationship> = try {
        val supabase = getSupabase()
        val response = supabase.postgrest.rpc(
            "get_relationship_summary",
            buildJsonObject { put("target_user_id", targetUserId) }
        ).decodeAs<RelationshipSummary>()

        // Fetch mutual previews if any
        val mutuals = if (response.mutualPreviewIds.isNotEmpty()) {
            chatRepository.getProfilesByIds(response.mutualPreviewIds)
        } else emptyList()

        val relationship = Relationship(
            targetUserId = targetUserId,
            distance = response.distance,
            isDirectConnection = response.distance == 1,
            mutualConnectionCount = response.mutualCount,
            mutualConnectionPreview = mutuals,
            densityLabel = response.densityLabel,
            strengthLabel = response.strengthLabel
        )

        relationshipDao.insertRelationship(relationship.toEntity())
        Result.success(relationship)
    } catch (e: Exception) {
        Log.e("RelationshipRepo", "Failed to fetch relationship for $targetUserId", e)
        Result.failure(e)
    }

    override suspend fun getRelationshipPaths(targetUserId: String): Result<List<List<String>>> = try {
        val supabase = getSupabase()
        val response = supabase.postgrest.rpc(
            "get_connection_paths",
            buildJsonObject { put("target_user_id", targetUserId) }
        ).decodeAs<List<List<String>>?>() ?: emptyList()
        Result.success(response)
    } catch (e: Exception) {
        Log.e("RelationshipRepo", "Failed to fetch paths for $targetUserId", e)
        Result.failure(e)
    }

    override suspend fun getFirstContactPolicy(): Result<FirstContactPolicy> = try {
        val supabase = getSupabase()
        val response = supabase.postgrest.rpc(
            "get_active_first_contact_policy",
            buildJsonObject {}
        ).decodeAs<FirstContactPolicy>()
        Result.success(response)
    } catch (e: Exception) {
        Log.e("RelationshipRepo", "Failed to fetch first contact policy", e)
        Result.success(FirstContactPolicy())
    }

    override suspend fun checkCanMessage(targetUserId: String): Result<CanMessageResult> = try {
        val supabase = getSupabase()
        val response = supabase.postgrest.rpc(
            "check_can_message",
            buildJsonObject { put("target_user_id", targetUserId) }
        ).decodeAs<CanMessageResult>()
        Result.success(response)
    } catch (e: Exception) {
        Log.e("RelationshipRepo", "Failed to check can message for $targetUserId", e)
        Result.failure(e)
    }

    override suspend fun replyToMessageRequest(requestId: String): Result<Unit> = try {
        val supabase = getSupabase()
        supabase.postgrest.rpc(
            "reply_to_message_request",
            buildJsonObject { put("p_request_id", requestId) }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("RelationshipRepo", "Failed to reply to message request $requestId", e)
        Result.failure(e)
    }

    override fun getCachedRelationship(targetUserId: String): Flow<Relationship?> = 
        relationshipDao.getRelationship(targetUserId).map { it?.toDomain() }

    override suspend fun invalidateRelationship(targetUserId: String) {
        relationshipDao.deleteRelationship(targetUserId)
    }

    override suspend fun invalidateAll() {
        relationshipDao.clearAll()
    }

    override fun shutdown() {
        Log.i("RelationshipRepo", "Shutting down RelationshipRepository")
        repositoryScope.cancel()
    }
}

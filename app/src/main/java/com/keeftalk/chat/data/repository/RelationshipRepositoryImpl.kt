package com.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.Relationship
import com.keeftalk.chat.domain.model.RelationshipSummary
import com.keeftalk.chat.domain.repository.RelationshipRepository
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.repository.PrivacyRepository
import com.keeftalk.chat.data.local.dao.RelationshipDao
import com.keeftalk.chat.data.local.entities.toDomain
import com.keeftalk.chat.data.local.entities.toEntity
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
            strengthLabel = response.strengthLabel,
            nudgesReceived = response.nudgesReceived
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

    override suspend fun sendNudge(targetUserId: String, count: Int): Result<Unit> = try {
        val supabase = getSupabase()
        supabase.postgrest.rpc(
            "send_nudge",
            buildJsonObject { 
                put("target_user_id", targetUserId)
                put("nudge_increment", count)
            }
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("RelationshipRepo", "Failed to send nudge to $targetUserId", e)
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

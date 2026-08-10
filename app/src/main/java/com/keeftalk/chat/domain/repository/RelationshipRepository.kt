package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.Relationship
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    suspend fun getRelationship(targetUserId: String): Result<Relationship>
    suspend fun getRelationshipPaths(targetUserId: String): Result<List<List<String>>>
    suspend fun sendNudge(targetUserId: String, count: Int): Result<Unit>
    fun getCachedRelationship(targetUserId: String): Flow<Relationship?>
    suspend fun invalidateRelationship(targetUserId: String)
    suspend fun invalidateAll()
    fun shutdown()
}

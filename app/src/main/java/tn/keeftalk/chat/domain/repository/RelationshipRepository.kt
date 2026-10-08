package tn.keeftalk.chat.domain.repository

import tn.keeftalk.chat.domain.model.Relationship
import tn.keeftalk.chat.domain.model.FirstContactPolicy
import tn.keeftalk.chat.domain.model.CanMessageResult
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    suspend fun getRelationship(targetUserId: String): Result<Relationship>
    suspend fun getRelationshipPaths(targetUserId: String): Result<List<List<String>>>
    suspend fun getFirstContactPolicy(): Result<FirstContactPolicy>
    suspend fun checkCanMessage(targetUserId: String): Result<CanMessageResult>
    suspend fun replyToMessageRequest(requestId: String): Result<Unit>
    fun getCachedRelationship(targetUserId: String): Flow<Relationship?>
    suspend fun invalidateRelationship(targetUserId: String)
    suspend fun invalidateAll()
    fun shutdown()
}

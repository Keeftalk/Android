package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.RelationshipCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationship_cache WHERE targetUserId = :userId")
    fun getRelationship(userId: String): Flow<RelationshipCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: RelationshipCacheEntity)

    @Query("DELETE FROM relationship_cache WHERE targetUserId = :userId")
    suspend fun deleteRelationship(userId: String)

    @Query("DELETE FROM relationship_cache")
    suspend fun clearAll()
}

package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.keeftalk.chat.domain.model.Relationship

@Entity(tableName = "relationship_cache")
data class RelationshipCacheEntity(
    @PrimaryKey val targetUserId: String,
    val distance: Int?,
    val mutualConnectionCount: Int,
    val strengthLabel: String?,
    val densityLabel: String?,
    val nudgesReceived: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

fun RelationshipCacheEntity.toDomain() = Relationship(
    targetUserId = targetUserId,
    distance = distance,
    isDirectConnection = distance == 1,
    mutualConnectionCount = mutualConnectionCount,
    strengthLabel = strengthLabel,
    densityLabel = densityLabel,
    nudgesReceived = nudgesReceived
)

fun Relationship.toEntity() = RelationshipCacheEntity(
    targetUserId = targetUserId,
    distance = distance,
    mutualConnectionCount = mutualConnectionCount,
    strengthLabel = strengthLabel,
    densityLabel = densityLabel,
    nudgesReceived = nudgesReceived
)

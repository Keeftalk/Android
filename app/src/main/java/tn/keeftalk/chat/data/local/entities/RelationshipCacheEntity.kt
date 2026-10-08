package tn.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import tn.keeftalk.chat.domain.model.Relationship
import tn.keeftalk.chat.domain.model.Profile

@Entity(tableName = "relationship_cache")
data class RelationshipCacheEntity(
    @PrimaryKey val targetUserId: String,
    val distance: Int?,
    val mutualConnectionCount: Int,
    val mutualConnectionPreview: List<Profile> = emptyList(),
    val strengthLabel: String?,
    val densityLabel: String?,
    val updatedAt: Long = System.currentTimeMillis()
)

fun RelationshipCacheEntity.toDomain() = Relationship(
    targetUserId = targetUserId,
    distance = distance,
    isDirectConnection = distance == 1,
    mutualConnectionCount = mutualConnectionCount,
    mutualConnectionPreview = mutualConnectionPreview,
    strengthLabel = strengthLabel,
    densityLabel = densityLabel
)

fun Relationship.toEntity() = RelationshipCacheEntity(
    targetUserId = targetUserId,
    distance = distance,
    mutualConnectionCount = mutualConnectionCount,
    mutualConnectionPreview = mutualConnectionPreview,
    strengthLabel = strengthLabel,
    densityLabel = densityLabel
)

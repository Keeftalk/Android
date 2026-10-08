package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Relationship(
    val targetUserId: String,
    val distance: Int?, // null for infinity
    val isDirectConnection: Boolean = false,
    val mutualConnectionCount: Int = 0,
    val mutualConnectionPreview: List<Profile> = emptyList(),
    val connectionSummary: String? = null,
    val strengthLabel: String? = null,
    val densityLabel: String? = null
)

@Serializable
data class RelationshipSummary(
    val distance: Int?,
    val mutualCount: Int,
    val mutualPreviewIds: List<String> = emptyList(),
    val densityLabel: String? = null,
    val strengthLabel: String? = null
)

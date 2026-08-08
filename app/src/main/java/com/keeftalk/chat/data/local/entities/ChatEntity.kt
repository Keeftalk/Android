package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    indices = [
        Index(value = ["isArchived", "isPinned", "lastTimestamp"], name = "idx_chats_visibility_prio_time"),
        Index(value = ["lastTimestamp"], name = "idx_chats_last_timestamp")
    ]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val avatarUrl: String? = null,
    val type: String, // "ONE_TO_ONE", "GROUP"
    val lastMessage: String?,
    val lastTimestamp: Long,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val snippetType: String? = null, // Denormalized: TEXT, IMAGE, VIDEO, etc.
    val snippetUri: String? = null,  // Denormalized: local URI for thumbnail
    val autoDeleteTimer: Long? = null,
    val containerId: String? = null,
    val peerId: String? = null,
    val lastMessageStatus: String? = null,
    val lastMessageSenderId: String? = null,
    val lastActivityAt: Long = System.currentTimeMillis(),
    val autoTranslateEnabled: Boolean = false,
    val notificationSettingsJson: String? = null,
    val themeId: String? = null,
    val lastDecryptedMessage: String? = null,
    val metadata: ByteArray? = null // Phase 3: Metadata BLOB
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChatEntity
        if (id != other.id) return false
        if (name != other.name) return false
        if (avatarUrl != other.avatarUrl) return false
        if (type != other.type) return false
        if (lastMessage != other.lastMessage) return false
        if (lastTimestamp != other.lastTimestamp) return false
        if (unreadCount != other.unreadCount) return false
        if (isPinned != other.isPinned) return false
        if (isMuted != other.isMuted) return false
        if (isArchived != other.isArchived) return false
        if (isFavorite != other.isFavorite) return false
        if (snippetType != other.snippetType) return false
        if (snippetUri != other.snippetUri) return false
        if (autoDeleteTimer != other.autoDeleteTimer) return false
        if (containerId != other.containerId) return false
        if (peerId != other.peerId) return false
        if (lastMessageStatus != other.lastMessageStatus) return false
        if (lastMessageSenderId != other.lastMessageSenderId) return false
        if (lastActivityAt != other.lastActivityAt) return false
        if (autoTranslateEnabled != other.autoTranslateEnabled) return false
        if (notificationSettingsJson != other.notificationSettingsJson) return false
        if (themeId != other.themeId) return false
        if (metadata != null) {
            if (other.metadata == null) return false
            if (!metadata.contentEquals(other.metadata)) return false
        } else if (other.metadata != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (avatarUrl?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + (lastMessage?.hashCode() ?: 0)
        result = 31 * result + lastTimestamp.hashCode()
        result = 31 * result + unreadCount
        result = 31 * result + isPinned.hashCode()
        result = 31 * result + isMuted.hashCode()
        result = 31 * result + isArchived.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + (snippetType?.hashCode() ?: 0)
        result = 31 * result + (snippetUri?.hashCode() ?: 0)
        result = 31 * result + (autoDeleteTimer?.hashCode() ?: 0)
        result = 31 * result + (containerId?.hashCode() ?: 0)
        result = 31 * result + (peerId?.hashCode() ?: 0)
        result = 31 * result + (lastMessageStatus?.hashCode() ?: 0)
        result = 31 * result + (lastMessageSenderId?.hashCode() ?: 0)
        result = 31 * result + lastActivityAt.hashCode()
        result = 31 * result + autoTranslateEnabled.hashCode()
        result = 31 * result + (notificationSettingsJson?.hashCode() ?: 0)
        result = 31 * result + (themeId?.hashCode() ?: 0)
        result = 31 * result + (metadata?.contentHashCode() ?: 0)
        return result
    }
}

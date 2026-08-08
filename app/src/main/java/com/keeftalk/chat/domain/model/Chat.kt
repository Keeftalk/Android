package com.keeftalk.chat.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.keeftalk.chat.util.AvatarUtils

import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Stable
data class ChatNotificationSettings(
    val muteUntil: Long? = null,
    val customSoundUri: String? = null,
    val vibration: String? = null,
    val showPreview: Boolean? = null,
    val highPriority: Boolean = false,
    val pinnedNotification: Boolean = false,
    val ignoreMentions: Boolean = false,
    val ringtone: String? = null
)

@Immutable
@Stable
data class Chat(
    val id: String,
    val name: String?,
    val avatarUrl: String?,
    val type: ChatType,
    val lastMessage: String?,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val autoDeleteTimer: Long? = null,
    val containerId: String? = null,
    val peerId: String? = null,
    val lastMessageStatus: MessageStatus? = null,
    val lastMessageSenderId: String? = null,
    val autoTranslateEnabled: Boolean = false,
    val notificationSettings: ChatNotificationSettings = ChatNotificationSettings(),
    val themeId: String? = null,
    val snippetType: String? = null,
    val snippetUri: String? = null,
    val latestAuthorName: String? = null
) {
    val initials: String by lazy { AvatarUtils.getInitials(displayName) }

    val displayName: String
        get() {
            if (name == null || name == "Unknown" || name.equals("Direct Chat", ignoreCase = true)) {
                return latestAuthorName ?: "Unknown"
            }
            return name
        }

    fun toUiModel() = ChatListItemUiModel(
        id = id,
        name = displayName,
        avatarUrl = avatarUrl,
        initials = initials,
        lastMessage = lastMessage,
        lastTimestamp = lastTimestamp,
        formattedTimestamp = com.keeftalk.chat.ui.screens.formatTimestamp(lastTimestamp),
        unreadCount = unreadCount,
        isPinned = isPinned,
        isMuted = isMuted,
        lastMessageStatus = lastMessageStatus,
        lastMessageSenderId = lastMessageSenderId,
        isArchived = isArchived,
        isFavorite = isFavorite,
        peerId = peerId,
        type = type,
        snippetType = snippetType,
        snippetUri = snippetUri,
        latestAuthorName = latestAuthorName
    )
}

@Serializable
@Immutable
@Stable
data class ChatListItemUiModel(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val initials: String,
    val lastMessage: String?,
    val lastTimestamp: Long,
    val formattedTimestamp: String,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val lastMessageStatus: MessageStatus?,
    val lastMessageSenderId: String?,
    val isArchived: Boolean,
    val isFavorite: Boolean = false,
    val peerId: String?,
    val type: ChatType,
    val isTyping: Boolean = false,
    val snippetType: String? = null,
    val snippetUri: String? = null,
    val latestAuthorName: String? = null
)


enum class ChatType {
    ONE_TO_ONE, GROUP
}

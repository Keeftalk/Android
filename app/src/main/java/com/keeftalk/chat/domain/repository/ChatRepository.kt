package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.Chat
import com.keeftalk.chat.domain.model.Message
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.model.CallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface ChatRepository {
    val allChats: StateFlow<List<Chat>>
    fun getChats(): Flow<List<Chat>>
    fun getChats(limit: Int, offset: Int): Flow<List<Chat>>
    fun getArchivedChats(): Flow<List<Chat>>
    fun getChat(chatId: String): Flow<Chat?>
    fun getChatPager(filter: String, query: String = ""): Flow<androidx.paging.PagingData<Chat>>
    fun getMessage(messageId: String): Flow<Message?>
    fun getMessages(chatId: String): Flow<List<Message>>
    fun getMessages(chatId: String, limit: Int, offset: Int): Flow<List<Message>>
    fun getMessagePager(chatId: String): Flow<androidx.paging.PagingData<Message>>
    fun getMediaMessagePager(chatId: String): Flow<androidx.paging.PagingData<Message>>
    suspend fun getMediaMessageIds(chatId: String): List<String>
    suspend fun loadOlderMessages(chatId: String, limit: Int, offset: Int)
    suspend fun loadOlderMessages(chatId: String)
    fun getContacts(): Flow<List<User>>
    fun getRecentChatUsers(): Flow<List<User>>
    fun getBlockedUsers(): Flow<List<User>>
    fun getContact(userId: String): Flow<User?>
    fun getChatMembers(chatId: String): Flow<List<User>>
    suspend fun reportProfileView(viewedUserId: String)
    suspend fun syncBroadcasts()
    suspend fun createReminderNotification(chatId: String, senderName: String, count: Int)
    suspend fun sendMessage(chatId: String, content: String, type: com.keeftalk.chat.domain.model.MessageType = com.keeftalk.chat.domain.model.MessageType.TEXT, filePath: String? = null, replyToId: String? = null, onProgress: ((Float) -> Unit)? = null)
    suspend fun createGroup(name: String, memberIds: List<String>, avatarUrl: String? = null): String
    suspend fun syncMessages(peerId: String, lastSyncTimestamp: Long): Long
    suspend fun syncChats(limit: Int, offset: Int)
    suspend fun syncFullChatHistory()
    suspend fun syncAllRecentContent()
    suspend fun syncChat(chatId: String)
    suspend fun syncNotifications()
    suspend fun refreshChatMetadata(chatIds: List<String>)
    suspend fun markAsRead(chatId: String)
    suspend fun sendReadReceipt(chatId: String, messageId: String)
    suspend fun sendDeliveryReceipt(chatId: String, messageId: String)
    fun getChatMembersFlow(chatId: String): Flow<List<com.keeftalk.chat.data.local.entities.ChatMemberEntity>>
    fun getSearchableProfiles(): Flow<List<com.keeftalk.chat.domain.model.Profile>>
    suspend fun getProfilesByIds(ids: List<String>): List<com.keeftalk.chat.domain.model.Profile>
    suspend fun syncAllProfiles()
    suspend fun searchUsers(query: String): Result<List<com.keeftalk.chat.domain.model.Profile>>
    suspend fun searchUserByPhone(phone: String): Result<com.keeftalk.chat.domain.model.Profile?>
    suspend fun getOrCreateOneToOneChat(otherUserId: String): Result<String>
    suspend fun updateMessageStatus(messageId: String, status: com.keeftalk.chat.domain.model.MessageStatus)
    suspend fun togglePinChat(chatId: String, isPinned: Boolean)
    suspend fun toggleFavorite(chatId: String, isFavorite: Boolean)
    suspend fun toggleMuteChat(chatId: String, isMuted: Boolean)
    suspend fun archiveChat(chatId: String, isArchived: Boolean)
    suspend fun blockUser(userId: String, isBlocked: Boolean)
    suspend fun clearChat(chatId: String)
    suspend fun deleteChat(chatId: String)
    suspend fun updateChatTheme(chatId: String, themeId: String?)
    suspend fun deleteMessage(messageId: String)
    suspend fun setAutoDeleteTimer(chatId: String, timer: Long?)
    suspend fun toggleAutoTranslate(chatId: String, enabled: Boolean)
    suspend fun updateChatNotificationSettings(chatId: String, settings: com.keeftalk.chat.domain.model.ChatNotificationSettings)
    suspend fun translateMessage(messageId: String)
    suspend fun preloadTranslationModel(langTag: String)
    suspend fun updateMessageTranslation(messageId: String, translatedContent: String, sourceLanguage: String)
    suspend fun exportChat(chatId: String): String
    fun getTypingStatus(chatId: String): Flow<Set<String>>
    fun getLastSeenMessageId(chatId: String, senderId: String): Flow<String?>
    fun getAllTypingStatuses(): Flow<Map<String, Set<String>>>
    suspend fun setTypingStatus(chatId: String, isTyping: Boolean)

    // Reactions & Context Menu Actions
    suspend fun addReaction(messageId: String, emoji: String)
    suspend fun removeReaction(messageId: String)
    suspend fun bumpChat(chatId: String)
    suspend fun forwardMessage(messageId: String, targetChatIds: List<String>)
    suspend fun reportMessage(messageId: String, reason: String)
    suspend fun toggleMediaLock(messageId: String, locked: Boolean)
    suspend fun downloadMedia(message: Message, onProgress: ((Float) -> Unit)? = null): Result<DownloadResult>
    suspend fun ensureMediaLocal(message: Message): Result<File>

    // Call Actions
    suspend fun startCall(chatId: String, type: String, callId: String = java.util.UUID.randomUUID().toString()): String // Returns Call ID
    suspend fun acceptCall(callId: String)
    suspend fun endCall(callId: String)
    suspend fun updateCallState(callId: String, state: CallState)
    suspend fun sendCallSignaling(callId: String, type: String, payload: String)
    fun getCallSignaling(callId: String): Flow<CallSignaling>
    fun getCallSession(callId: String): Flow<CallSession?>
    fun getIncomingCalls(): Flow<CallSession>

    // Notifications
    fun getNotifications(): Flow<List<com.keeftalk.chat.domain.model.AppNotification>>
    fun getAppNotifications(): Flow<List<com.keeftalk.chat.domain.model.AppNotification>>
    fun getUnreadNotificationCount(): Flow<Int>
    suspend fun insertNotification(notification: com.keeftalk.chat.domain.model.AppNotification)
    suspend fun pushNotification(recipientId: String, notification: com.keeftalk.chat.domain.model.AppNotification)
    suspend fun markNotificationAsRead(id: String)
    suspend fun markAllNotificationsAsRead()
    suspend fun deleteNotification(id: String)
    suspend fun clearAllNotifications()

    /**
     * Starts background tasks like Realtime, Sync, and Cache Hydration.
     * This should be called from a background thread during app startup.
     */
    fun startBackgroundTasks()

    /**
     * Pre-fetches and warms the recipient cache for the given user IDs.
     * Helps avoid N+1 queries during initial list loading.
     */
    suspend fun warmRecipientCache(userIds: List<String>)

    suspend fun syncContacts()
    suspend fun syncCallLogs()

    suspend fun saveLocalContact(
        name: String, 
        phone: String,
        secondaryPhone: String? = null,
        secondaryPhoneLabel: String? = null,
        tertiaryPhone: String? = null,
        tertiaryPhoneLabel: String? = null,
        email: String? = null,
        birthday: Long? = null,
        callingCard: String? = null,
        avatarUrl: String? = null
    )

    suspend fun shareEmailToChat(emailMessageId: String, chatId: String)
    suspend fun shareVaultFileToChat(vaultItemId: String, chatId: String)
    suspend fun shareAgendaToChat(calendarItemId: String, chatId: String)
    suspend fun shareNoteToChat(noteId: String, chatId: String, accessLevel: String, canInviteOthers: Boolean)

    fun shutdown()
}

data class CallSession(
    val id: String,
    val chatId: String,
    val callerId: String,
    val receiverId: String,
    val type: String,
    val state: CallState,
    val createdAt: Long
)

data class CallSignaling(
    val id: String,
    val callId: String,
    val senderId: String,
    val type: String,
    val payload: String
)

data class DownloadResult(
    val file: File,
    val alreadyExisted: Boolean
)

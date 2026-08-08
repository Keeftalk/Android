package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.ChatIdCache
import com.keeftalk.chat.data.local.entities.ChatEntity
import com.keeftalk.chat.data.local.entities.ChatMemberEntity
import com.keeftalk.chat.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastTimestamp DESC")
    abstract fun getActiveChatsPaging(): androidx.paging.PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chats WHERE isArchived = 0 AND unreadCount > 0 ORDER BY isPinned DESC, lastTimestamp DESC")
    abstract fun getUnreadChatsPaging(): androidx.paging.PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chats WHERE isArchived = 0 AND type = 'GROUP' ORDER BY isPinned DESC, lastTimestamp DESC")
    abstract fun getGroupChatsPaging(): androidx.paging.PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastTimestamp DESC")
    abstract fun getArchivedChatsPaging(): androidx.paging.PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chats WHERE isArchived = 0 AND isFavorite = 1 ORDER BY isPinned DESC, lastTimestamp DESC")
    abstract fun getFavoriteChatsPaging(): androidx.paging.PagingSource<Int, ChatEntity>

    @Query(
        """
        SELECT * FROM chats 
        WHERE isArchived = 0 AND (name LIKE :query OR lastMessage LIKE :query) 
        ORDER BY isPinned DESC, lastTimestamp DESC
        """
    )
    abstract fun searchChatsPaging(query: String): androidx.paging.PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastTimestamp DESC LIMIT :limit OFFSET :offset")
    abstract fun getActiveChats(limit: Int, offset: Int): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastTimestamp DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getActiveChatsOnce(limit: Int, offset: Int): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastTimestamp DESC LIMIT :limit OFFSET :offset")
    abstract fun getArchivedChats(limit: Int, offset: Int): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastTimestamp DESC")
    abstract fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastTimestamp DESC")
    abstract fun getActiveChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats ORDER BY lastTimestamp DESC")
    abstract fun getAllChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertChatInternal(chat: ChatEntity)

    @Transaction
    open suspend fun insertChat(chat: ChatEntity) {
        insertChatInternal(chat)
        ChatIdCache.putChat(chat)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertChatsInternal(chats: List<ChatEntity>)

    @Transaction
    open suspend fun insertChats(chats: List<ChatEntity>) {
        insertChatsInternal(chats)
        chats.forEach { ChatIdCache.putChat(it) }
    }

    @Query("SELECT * FROM chats WHERE id = :id")
    protected abstract suspend fun getChatByIdInternal(id: String): ChatEntity?

    open suspend fun getChatById(id: String): ChatEntity? {
        return ChatIdCache.getChat(id) ?: getChatByIdInternal(id)?.also { ChatIdCache.putChat(it) }
    }

    @Query("SELECT * FROM chats WHERE id = :id")
    abstract fun getChatFlowById(id: String): Flow<ChatEntity?>

    @Transaction
    @Query(
        """
        SELECT users.* FROM users 
        JOIN chat_members ON users.id = chat_members.userId 
        WHERE chat_members.chatId = :chatId
        """
    )
    abstract fun getChatMembers(chatId: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertChatMember(chatMember: ChatMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertChatMembers(chatMembers: List<ChatMemberEntity>)

    @Query("UPDATE chat_members SET lastReadMessageId = :messageId, lastReadAt = :timestamp WHERE chatId = :chatId AND userId = :userId")
    abstract suspend fun updateReadPointer(chatId: String, userId: String, messageId: String, timestamp: Long)

    @Query("UPDATE chat_members SET lastDeliveredMessageId = :messageId, lastDeliveredAt = :timestamp WHERE chatId = :chatId AND userId = :userId")
    abstract suspend fun updateDeliveryPointer(chatId: String, userId: String, messageId: String, timestamp: Long)

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    abstract suspend fun getChatMember(chatId: String, userId: String): ChatMemberEntity?

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId")
    abstract fun getChatMembersFlow(chatId: String): Flow<List<ChatMemberEntity>>

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    abstract suspend fun updatePinnedInternal(chatId: String, isPinned: Boolean)

    @Transaction
    open suspend fun updatePinned(chatId: String, isPinned: Boolean) {
        updatePinnedInternal(chatId, isPinned)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    abstract suspend fun updateMutedInternal(chatId: String, isMuted: Boolean)

    @Transaction
    open suspend fun updateMuted(chatId: String, isMuted: Boolean) {
        updateMutedInternal(chatId, isMuted)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    abstract suspend fun updateArchivedInternal(chatId: String, isArchived: Boolean)

    @Transaction
    open suspend fun updateArchived(chatId: String, isArchived: Boolean) {
        updateArchivedInternal(chatId, isArchived)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET isFavorite = :isFavorite WHERE id = :chatId")
    abstract suspend fun updateFavoriteInternal(chatId: String, isFavorite: Boolean)

    @Transaction
    open suspend fun updateFavorite(chatId: String, isFavorite: Boolean) {
        updateFavoriteInternal(chatId, isFavorite)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET autoDeleteTimer = :timer WHERE id = :chatId")
    abstract suspend fun updateAutoDeleteTimerInternal(chatId: String, timer: Long?)

    @Transaction
    open suspend fun updateAutoDeleteTimer(chatId: String, timer: Long?) {
        updateAutoDeleteTimerInternal(chatId, timer)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE id = :chatId")
    abstract suspend fun incrementUnreadCountInternal(chatId: String)

    @Transaction
    open suspend fun incrementUnreadCount(chatId: String) {
        incrementUnreadCountInternal(chatId)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET unreadCount = (CASE WHEN unreadCount > 0 THEN unreadCount - 1 ELSE 0 END) WHERE id = :chatId")
    abstract suspend fun decrementUnreadCountInternal(chatId: String)

    @Transaction
    open suspend fun decrementUnreadCount(chatId: String) {
        decrementUnreadCountInternal(chatId)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    abstract suspend fun resetUnreadCountInternal(chatId: String)

    @Transaction
    open suspend fun resetUnreadCount(chatId: String) {
        resetUnreadCountInternal(chatId)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET autoTranslateEnabled = :enabled WHERE id = :chatId")
    abstract suspend fun updateAutoTranslateInternal(chatId: String, enabled: Boolean)

    @Transaction
    open suspend fun updateAutoTranslate(chatId: String, enabled: Boolean) {
        updateAutoTranslateInternal(chatId, enabled)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET themeId = :themeId WHERE id = :chatId")
    abstract suspend fun updateThemeIdInternal(chatId: String, themeId: String?)

    @Transaction
    open suspend fun updateThemeId(chatId: String, themeId: String?) {
        updateThemeIdInternal(chatId, themeId)
        ChatIdCache.remove(chatId)
    }

    @Query("UPDATE chats SET notificationSettingsJson = :json WHERE id = :chatId")
    abstract suspend fun updateNotificationSettingsInternal(chatId: String, json: String)

    @Transaction
    open suspend fun updateNotificationSettings(chatId: String, json: String) {
        updateNotificationSettingsInternal(chatId, json)
        ChatIdCache.remove(chatId)
    }

    @Query("DELETE FROM chats WHERE id = :chatId")
    protected abstract suspend fun deleteChatInternal(chatId: String)

    @Transaction
    open suspend fun deleteChat(chatId: String) {
        deleteChatInternal(chatId)
        ChatIdCache.remove(chatId)
    }

    @Query("DELETE FROM chats")
    protected abstract suspend fun clearChatsInternal()

    @Query("DELETE FROM chat_members")
    abstract suspend fun clearMembers()

    @Transaction
    open suspend fun clearAll() {
        clearChatsInternal()
        clearMembers()
        ChatIdCache.clear()
    }
}

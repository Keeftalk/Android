package com.keeftalk.chat.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isKeeftalkUser: Boolean = false,
    val avatarUrl: String? = null,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val draft: String? = null,
    val recipientId: String? = null
)

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val type: Int, // 1 for inbox, 2 for sent
    val read: Int,
    val status: Int = -1,
    val isMms: Boolean = false,
    val attachments: List<SmsAttachment> = emptyList(),
    val deliveryStatus: Int = -1
)

data class SmsAttachment(
    val uri: String,
    val mimeType: String,
    val name: String? = null
)

interface SmsRepository {
    fun getConversations(showArchived: Boolean = false): Flow<List<SmsConversation>>
    fun getMessages(threadId: Long): Flow<PagingData<SmsMessage>>
    fun getMessagesList(threadId: Long): Flow<List<SmsMessage>>
    suspend fun sendSms(address: String, body: String): Result<Unit>
    suspend fun sendMms(address: String, body: String, attachments: List<SmsAttachment>): Result<Unit>
    suspend fun markAsRead(threadId: Long)
    suspend fun deleteConversation(threadId: Long)
    suspend fun toggleArchive(threadId: Long, archived: Boolean)
    suspend fun togglePin(threadId: Long, pinned: Boolean)
    suspend fun toggleMute(threadId: Long, muted: Boolean)
    fun searchConversations(query: String): Flow<List<SmsConversation>>
}

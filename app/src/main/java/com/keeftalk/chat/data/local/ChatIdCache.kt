package com.keeftalk.chat.data.local

import android.util.LruCache
import com.keeftalk.chat.data.local.entities.ChatEntity

/**
 * In-memory cache for Chat IDs and Chat Entities to minimize database lookups.
 */
object ChatIdCache {
    private const val MAX_SIZE = 1000
    
    // peerId -> chatId
    private val peerIdToChatId = LruCache<String, String>(MAX_SIZE)
    
    // chatId -> ChatEntity
    private val chatIdToEntity = LruCache<String, ChatEntity>(MAX_SIZE)

    fun getChatId(peerId: String): String? = synchronized(peerIdToChatId) {
        peerIdToChatId[peerId]
    }

    fun putChatId(peerId: String, chatId: String): Unit = synchronized(peerIdToChatId) {
        peerIdToChatId.put(peerId, chatId)
    }

    fun getChat(chatId: String): ChatEntity? = synchronized(chatIdToEntity) {
        chatIdToEntity[chatId]
    }

    fun putChat(chat: ChatEntity) = synchronized(chatIdToEntity) {
        chatIdToEntity.put(chat.id, chat)
        chat.peerId?.let { peerIdToChatId.put(it, chat.id) }
    }

    fun remove(chatId: String) = synchronized(chatIdToEntity) {
        val chat = chatIdToEntity.remove(chatId)
        chat?.peerId?.let { peerIdToChatId.remove(it) }
    }

    fun clear() {
        synchronized(peerIdToChatId) { peerIdToChatId.evictAll() }
        synchronized(chatIdToEntity) { chatIdToEntity.evictAll() }
    }
}

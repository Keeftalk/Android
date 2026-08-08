package com.keeftalk.chat.security.crypto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ConversationKeyEntity)

    @Query("SELECT * FROM conversation_keys WHERE conversationId = :conversationId AND user_id = :userId")
    suspend fun getKeyForConversation(conversationId: String, userId: String): ConversationKeyEntity?

    @Query("DELETE FROM conversation_keys WHERE conversationId = :conversationId AND user_id = :userId")
    suspend fun deleteKeyForConversation(conversationId: String, userId: String)

    @Query("DELETE FROM conversation_keys")
    suspend fun deleteAll()
}

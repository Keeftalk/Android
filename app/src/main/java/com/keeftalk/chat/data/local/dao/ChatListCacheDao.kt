package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.ChatListCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatListCacheDao {
    @Query("SELECT * FROM chat_list_cache ORDER BY sortOrder ASC LIMIT :limit")
    suspend fun getTopChats(limit: Int): List<ChatListCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatListCacheEntity>)

    @Query("DELETE FROM chat_list_cache")
    suspend fun clearAll()
}

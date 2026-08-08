package com.keeftalk.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keeftalk.chat.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun getUsersByIds(ids: List<String>): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserFlowById(id: String): Flow<UserEntity?>

    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :userId")
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean, lastSeen: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE isContact = 1")
    fun getContacts(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isBlocked = 1")
    fun getBlockedUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET isBlocked = :isBlocked WHERE id = :userId")
    suspend fun updateBlocked(userId: String, isBlocked: Boolean)

    @Query(
        """
        SELECT users.* FROM users 
        JOIN chats ON users.id = chats.peerId 
        WHERE chats.type = 'ONE_TO_ONE' 
        ORDER BY chats.lastTimestamp DESC
        """
    )
    fun getRecentChatUsers(): Flow<List<UserEntity>>

    @Query("DELETE FROM users")
    suspend fun clearAll()

    @Query("SELECT * FROM users WHERE isContact = 1 AND cloudSyncStatus != 1")
    suspend fun getUnsyncedContacts(): List<UserEntity>

    @Query("UPDATE users SET cloudSyncStatus = :status WHERE id = :userId")
    suspend fun updateSyncStatus(userId: String, status: Int)
}

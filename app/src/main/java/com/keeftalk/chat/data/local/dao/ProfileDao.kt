package com.keeftalk.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keeftalk.chat.data.local.entities.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfile(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getProfileFlow(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE phone = :phone LIMIT 1")
    suspend fun getProfileByPhone(phone: String): ProfileEntity?

    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE username LIKE '%' || :query || '%' OR full_name LIKE '%' || :query || '%'")
    suspend fun searchProfiles(query: String): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Query("SELECT * FROM profiles WHERE id IN (:ids)")
    suspend fun getProfilesByIds(ids: List<String>): List<ProfileEntity>

    @Query("DELETE FROM profiles")
    suspend fun clearProfiles()
}

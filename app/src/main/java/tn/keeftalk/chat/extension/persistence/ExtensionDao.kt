package tn.keeftalk.chat.extension.persistence

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM installed_extensions ORDER BY installedTimeMillis DESC")
    fun getAllExtensionsFlow(): Flow<List<ExtensionEntity>>

    @Query("SELECT * FROM installed_extensions WHERE id = :id LIMIT 1")
    suspend fun getExtensionById(id: String): ExtensionEntity?

    @Query("SELECT * FROM installed_extensions WHERE isEnabled = 1")
    suspend fun getEnabledExtensions(): List<ExtensionEntity>

    @Query("SELECT * FROM installed_extensions WHERE isEnabled = 1")
    fun getEnabledExtensionsSync(): List<ExtensionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(extension: ExtensionEntity)

    @Query("UPDATE installed_extensions SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: String, isEnabled: Boolean)

    @Query("DELETE FROM installed_extensions WHERE id = :id")
    suspend fun deleteById(id: String)
}

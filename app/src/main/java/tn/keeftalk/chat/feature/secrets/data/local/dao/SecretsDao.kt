package tn.keeftalk.chat.feature.secrets.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretBackupCodeEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretCredentialEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretPasswordHistoryEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretSettingsEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretTotpAccountEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretVaultItemEntity

@Dao
interface SecretsDao {

    // Credentials
    @Query("SELECT * FROM secret_credentials WHERE user_id = :userId AND is_deleted = 0 ORDER BY created_at DESC")
    fun getCredentials(userId: String): Flow<List<SecretCredentialEntity>>

    @Query("SELECT * FROM secret_credentials WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getCredentialById(id: String, userId: String): SecretCredentialEntity?

    @Query("SELECT * FROM secret_credentials WHERE user_id = :userId AND is_deleted = 0")
    suspend fun getCredentialsOnce(userId: String): List<SecretCredentialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: SecretCredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: List<SecretCredentialEntity>)

    @Update
    suspend fun updateCredential(credential: SecretCredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: SecretCredentialEntity)

    // Password History
    @Query("SELECT * FROM secret_password_history WHERE credential_id = :credentialId ORDER BY set_at DESC")
    fun getPasswordHistory(credentialId: String): Flow<List<SecretPasswordHistoryEntity>>

    @Query("SELECT * FROM secret_password_history WHERE credential_id = :credentialId ORDER BY set_at DESC")
    suspend fun getPasswordHistoryOnce(credentialId: String): List<SecretPasswordHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasswordHistory(historyItem: SecretPasswordHistoryEntity)

    @Delete
    suspend fun deletePasswordHistoryItem(historyItem: SecretPasswordHistoryEntity)

    // TOTP Accounts
    @Query("SELECT * FROM secret_totp_accounts WHERE user_id = :userId ORDER BY created_at DESC")
    fun getTotpAccounts(userId: String): Flow<List<SecretTotpAccountEntity>>

    @Query("SELECT * FROM secret_totp_accounts WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getTotpAccountsOnce(userId: String): List<SecretTotpAccountEntity>

    @Query("SELECT * FROM secret_totp_accounts WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getTotpAccountById(id: String, userId: String): SecretTotpAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTotpAccount(account: SecretTotpAccountEntity)

    @Update
    suspend fun updateTotpAccount(account: SecretTotpAccountEntity)

    @Delete
    suspend fun deleteTotpAccount(account: SecretTotpAccountEntity)

    // Backup Codes
    @Query("SELECT * FROM secret_backup_codes WHERE user_id = :userId ORDER BY created_at DESC")
    fun getBackupCodes(userId: String): Flow<List<SecretBackupCodeEntity>>

    @Query("SELECT * FROM secret_backup_codes WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getBackupCodesOnce(userId: String): List<SecretBackupCodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupCodes(codes: List<SecretBackupCodeEntity>)

    @Query("DELETE FROM secret_backup_codes WHERE user_id = :userId")
    suspend fun clearBackupCodes(userId: String)

    @Update
    suspend fun updateBackupCode(code: SecretBackupCodeEntity)

    // Vault Items
    @Query("SELECT * FROM secret_vault_items WHERE user_id = :userId ORDER BY created_at DESC")
    fun getVaultItems(userId: String): Flow<List<SecretVaultItemEntity>>

    @Query("SELECT * FROM secret_vault_items WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getVaultItemsOnce(userId: String): List<SecretVaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: SecretVaultItemEntity)

    @Delete
    suspend fun deleteVaultItem(item: SecretVaultItemEntity)

    // Settings
    @Query("SELECT * FROM secret_settings WHERE user_id = :userId LIMIT 1")
    fun getSettingsFlow(userId: String): Flow<SecretSettingsEntity?>

    @Query("SELECT * FROM secret_settings WHERE user_id = :userId LIMIT 1")
    suspend fun getSettings(userId: String): SecretSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SecretSettingsEntity)
}

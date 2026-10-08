package tn.keeftalk.chat.feature.secrets.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretSettingsEntity
import tn.keeftalk.chat.feature.secrets.model.BackupCode
import tn.keeftalk.chat.feature.secrets.model.PasswordHistoryItem
import tn.keeftalk.chat.feature.secrets.model.SecretCredential
import tn.keeftalk.chat.feature.secrets.model.SecretSettings
import tn.keeftalk.chat.feature.secrets.model.SecretVaultItem
import tn.keeftalk.chat.feature.secrets.model.SecurityScoreSummary
import tn.keeftalk.chat.feature.secrets.model.TotpAccount
import tn.keeftalk.chat.feature.secrets.util.CsvImportResult

interface SecretsRepository {
    // Credentials
    fun getCredentials(userId: String): Flow<List<SecretCredential>>
    suspend fun getCredentialById(id: String, userId: String): SecretCredential?
    suspend fun saveCredential(credential: SecretCredential): Result<Unit>
    suspend fun updatePassword(credentialId: String, userId: String, newPassword: String): Result<Unit>
    suspend fun restoreOldPassword(credentialId: String, userId: String, historyItem: PasswordHistoryItem): Result<Unit>
    suspend fun deleteCredential(credential: SecretCredential): Result<Unit>

    // Password History
    fun getPasswordHistory(credentialId: String): Flow<List<PasswordHistoryItem>>

    // TOTP Accounts
    fun getTotpAccounts(userId: String): Flow<List<TotpAccount>>
    suspend fun saveTotpAccount(account: TotpAccount): Result<Unit>
    suspend fun deleteTotpAccount(account: TotpAccount): Result<Unit>

    // Backup Codes
    fun getBackupCodes(userId: String): Flow<List<BackupCode>>
    suspend fun generateBackupCodes(userId: String): Result<List<String>>

    // Secret Vault Items
    fun getVaultItems(userId: String): Flow<List<SecretVaultItem>>
    suspend fun saveVaultItem(item: SecretVaultItem): Result<Unit>
    suspend fun deleteVaultItem(item: SecretVaultItem): Result<Unit>

    // Security Settings
    fun getSettings(userId: String): Flow<SecretSettings>
    suspend fun updateSettings(settings: SecretSettings): Result<Unit>

    // Analytics & Security Score
    fun getSecuritySummary(userId: String): Flow<SecurityScoreSummary>

    // CSV Import / Export
    suspend fun importFromCsv(csvText: String, userId: String): Result<CsvImportResult>
    suspend fun exportToCsv(userId: String): Result<String>
}

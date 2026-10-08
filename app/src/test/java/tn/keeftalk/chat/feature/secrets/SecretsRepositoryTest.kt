package tn.keeftalk.chat.feature.secrets

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretBackupCodeEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretCredentialEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretPasswordHistoryEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretSettingsEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretTotpAccountEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretVaultItemEntity
import tn.keeftalk.chat.feature.secrets.model.SecretCredential
import tn.keeftalk.chat.feature.secrets.repository.SecretsRepositoryImpl

class SecretsRepositoryTest {

    private lateinit var fakeDao: FakeSecretsDao
    private lateinit var repository: SecretsRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeSecretsDao()
        repository = SecretsRepositoryImpl(fakeDao)
    }

    @Test
    fun saveAndGetCredentials_encryptsAndDecryptsSuccessfully() = runBlocking {
        val cred = SecretCredential(
            id = "cred-1",
            userId = "user-1",
            serviceName = "GitHub",
            website = "https://github.com",
            username = "dev-user",
            password = "MySuperSecretPassword123!"
        )

        val saveResult = repository.saveCredential(cred)
        assertTrue(saveResult.isSuccess)

        val list = repository.getCredentials("user-1").first()
        assertEquals(1, list.size)
        assertEquals("GitHub", list[0].serviceName)
        assertEquals("MySuperSecretPassword123!", list[0].password)
    }

    @Test
    fun updatePassword_archivesOldPasswordToHistory() = runBlocking {
        val cred = SecretCredential(
            id = "cred-2",
            userId = "user-1",
            serviceName = "Google",
            username = "you@mail.com",
            password = "OldPassword123!"
        )

        repository.saveCredential(cred)
        repository.updatePassword("cred-2", "user-1", "NewPassword456!")

        val updated = repository.getCredentialById("cred-2", "user-1")
        assertNotNull(updated)
        assertEquals("NewPassword456!", updated?.password)

        val history = repository.getPasswordHistory("cred-2").first()
        assertEquals(1, history.size)
        assertEquals("OldPassword123!", history[0].password)
    }
}

class FakeSecretsDao : tn.keeftalk.chat.feature.secrets.data.local.dao.SecretsDao {
    private val credentials = mutableMapOf<String, SecretCredentialEntity>()
    private val history = mutableMapOf<String, SecretPasswordHistoryEntity>()
    private val totpAccounts = mutableMapOf<String, SecretTotpAccountEntity>()
    private val backupCodes = mutableMapOf<String, SecretBackupCodeEntity>()
    private val vaultItems = mutableMapOf<String, SecretVaultItemEntity>()
    private val settingsMap = mutableMapOf<String, SecretSettingsEntity>()

    override fun getCredentials(userId: String) = kotlinx.coroutines.flow.flowOf(
        credentials.values.filter { it.userId == userId && !it.isDeleted }
    )

    override suspend fun getCredentialById(id: String, userId: String) = credentials[id]

    override suspend fun getCredentialsOnce(userId: String) = credentials.values.filter { it.userId == userId && !it.isDeleted }

    override suspend fun insertCredential(credential: SecretCredentialEntity) {
        credentials[credential.id] = credential
    }

    override suspend fun insertCredentials(credentials: List<SecretCredentialEntity>) {
        credentials.forEach { this.credentials[it.id] = it }
    }

    override suspend fun updateCredential(credential: SecretCredentialEntity) {
        credentials[credential.id] = credential
    }

    override suspend fun deleteCredential(credential: SecretCredentialEntity) {
        credentials.remove(credential.id)
    }

    override fun getPasswordHistory(credentialId: String) = kotlinx.coroutines.flow.flowOf(
        history.values.filter { it.credentialId == credentialId }
    )

    override suspend fun getPasswordHistoryOnce(credentialId: String) = history.values.filter { it.credentialId == credentialId }

    override suspend fun insertPasswordHistory(historyItem: SecretPasswordHistoryEntity) {
        history[historyItem.id] = historyItem
    }

    override suspend fun deletePasswordHistoryItem(historyItem: SecretPasswordHistoryEntity) {
        history.remove(historyItem.id)
    }

    override fun getTotpAccounts(userId: String) = kotlinx.coroutines.flow.flowOf(totpAccounts.values.filter { it.userId == userId })

    override suspend fun getTotpAccountsOnce(userId: String) = totpAccounts.values.filter { it.userId == userId }

    override suspend fun getTotpAccountById(id: String, userId: String) = totpAccounts[id]

    override suspend fun insertTotpAccount(account: SecretTotpAccountEntity) {
        totpAccounts[account.id] = account
    }

    override suspend fun updateTotpAccount(account: SecretTotpAccountEntity) {
        totpAccounts[account.id] = account
    }

    override suspend fun deleteTotpAccount(account: SecretTotpAccountEntity) {
        totpAccounts.remove(account.id)
    }

    override fun getBackupCodes(userId: String) = kotlinx.coroutines.flow.flowOf(backupCodes.values.filter { it.userId == userId })

    override suspend fun getBackupCodesOnce(userId: String) = backupCodes.values.filter { it.userId == userId }

    override suspend fun insertBackupCodes(codes: List<SecretBackupCodeEntity>) {
        codes.forEach { backupCodes[it.id] = it }
    }

    override suspend fun clearBackupCodes(userId: String) {
        backupCodes.entries.removeIf { it.value.userId == userId }
    }

    override suspend fun updateBackupCode(code: SecretBackupCodeEntity) {
        backupCodes[code.id] = code
    }

    override fun getVaultItems(userId: String) = kotlinx.coroutines.flow.flowOf(vaultItems.values.filter { it.userId == userId })

    override suspend fun getVaultItemsOnce(userId: String) = vaultItems.values.filter { it.userId == userId }

    override suspend fun insertVaultItem(item: SecretVaultItemEntity) {
        vaultItems[item.id] = item
    }

    override suspend fun deleteVaultItem(item: SecretVaultItemEntity) {
        vaultItems.remove(item.id)
    }

    override fun getSettingsFlow(userId: String) = kotlinx.coroutines.flow.flowOf(settingsMap[userId])

    override suspend fun getSettings(userId: String) = settingsMap[userId]

    override suspend fun saveSettings(settings: SecretSettingsEntity) {
        settingsMap[settings.userId] = settings
    }
}

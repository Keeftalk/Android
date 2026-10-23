package tn.keeftalk.chat.feature.secrets.repository

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tn.keeftalk.chat.feature.secrets.crypto.PasswordStrengthEvaluator
import tn.keeftalk.chat.feature.secrets.data.local.dao.SecretsDao
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretBackupCodeEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretCredentialEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretPasswordHistoryEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretSettingsEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretTotpAccountEntity
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretVaultItemEntity
import tn.keeftalk.chat.feature.secrets.model.BackupCode
import tn.keeftalk.chat.feature.secrets.model.PasswordHistoryItem
import tn.keeftalk.chat.feature.secrets.model.SecretCredential
import tn.keeftalk.chat.feature.secrets.model.SecretSettings
import tn.keeftalk.chat.feature.secrets.model.SecretVaultItem
import tn.keeftalk.chat.feature.secrets.model.SecurityScoreSummary
import tn.keeftalk.chat.feature.secrets.model.TotpAccount
import tn.keeftalk.chat.feature.secrets.model.VaultSecretType
import tn.keeftalk.chat.feature.secrets.util.CsvImportResult
import tn.keeftalk.chat.feature.secrets.util.CsvImporterExporter
import tn.keeftalk.chat.security.crypto.EncryptedObject
import tn.keeftalk.chat.security.crypto.KeyManager
import tn.keeftalk.chat.security.crypto.StorageCryptoService
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKey

class SecretsRepositoryImpl(
    private val dao: SecretsDao
) : SecretsRepository {

    private companion object {
        private const val TAG = "SecretsRepositoryImpl"
    }

    private fun getEncryptionKey(): SecretKey {
        return KeyManager.getVaultKey()
    }

    private fun encryptText(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val key = getEncryptionKey()
        val encObj = StorageCryptoService.encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
        return "${encObj.version}:${encObj.keyId}:${encObj.iv}:${encObj.ciphertext}"
    }

    private fun decryptText(ciphertext: String): String {
        if (ciphertext.isEmpty()) return ""
        val parts = ciphertext.split(":")
        if (parts.size != 4) return ciphertext // Not formatted as EncryptedObject, return as-is
        return try {
            val key = getEncryptionKey()
            val encObj = EncryptedObject(
                version = parts[0].toIntOrNull() ?: 1,
                keyId = parts[1],
                iv = parts[2],
                ciphertext = parts[3]
            )
            val bytes = StorageCryptoService.decrypt(encObj, key)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt payload", e)
            "••••••••••••"
        }
    }

    override fun getCredentials(userId: String): Flow<List<SecretCredential>> {
        return dao.getCredentials(userId).map { list ->
            list.map { entity ->
                val decryptedPassword = decryptText(entity.encryptedPassword)
                val historyEntities = dao.getPasswordHistoryOnce(entity.id)
                val history = historyEntities.map { h ->
                    PasswordHistoryItem(
                        id = h.id,
                        credentialId = h.credentialId,
                        password = decryptText(h.encryptedPassword),
                        setAt = h.setAt,
                        replacedAt = h.replacedAt
                    )
                }
                SecretCredential(
                    id = entity.id,
                    userId = entity.userId,
                    serviceName = entity.serviceName,
                    website = entity.website,
                    username = entity.username,
                    password = decryptedPassword,
                    category = entity.category,
                    passwordStrength = entity.passwordStrength,
                    hasTwoFactor = entity.hasTwoFactor,
                    lastChangedAt = entity.lastChangedAt,
                    createdAt = entity.createdAt,
                    notes = entity.notes,
                    history = history
                )
            }
        }
    }

    override suspend fun getCredentialById(id: String, userId: String): SecretCredential? {
        val entity = dao.getCredentialById(id, userId) ?: return null
        val decryptedPassword = decryptText(entity.encryptedPassword)
        val historyEntities = dao.getPasswordHistoryOnce(entity.id)
        val history = historyEntities.map { h ->
            PasswordHistoryItem(
                id = h.id,
                credentialId = h.credentialId,
                password = decryptText(h.encryptedPassword),
                setAt = h.setAt,
                replacedAt = h.replacedAt
            )
        }
        return SecretCredential(
            id = entity.id,
            userId = entity.userId,
            serviceName = entity.serviceName,
            website = entity.website,
            username = entity.username,
            password = decryptedPassword,
            category = entity.category,
            passwordStrength = entity.passwordStrength,
            hasTwoFactor = entity.hasTwoFactor,
            lastChangedAt = entity.lastChangedAt,
            createdAt = entity.createdAt,
            notes = entity.notes,
            history = history
        )
    }

    override suspend fun saveCredential(credential: SecretCredential): Result<Unit> = runCatching {
        val eval = PasswordStrengthEvaluator.evaluate(credential.password)
        val encryptedPw = encryptText(credential.password)
        val entity = SecretCredentialEntity(
            id = credential.id.ifBlank { UUID.randomUUID().toString() },
            userId = credential.userId,
            serviceName = credential.serviceName,
            website = credential.website,
            username = credential.username,
            encryptedPassword = encryptedPw,
            category = credential.category,
            passwordStrength = eval.score,
            hasTwoFactor = credential.hasTwoFactor,
            lastChangedAt = credential.lastChangedAt,
            createdAt = credential.createdAt,
            notes = credential.notes,
            isDeleted = false
        )
        dao.insertCredential(entity)
    }

    override suspend fun updatePassword(credentialId: String, userId: String, newPassword: String): Result<Unit> = runCatching {
        val existing = dao.getCredentialById(credentialId, userId) ?: throw IllegalArgumentException("Credential not found")
        val currentEncryptedPw = existing.encryptedPassword

        // 1. Archive current password into history
        val historyItem = SecretPasswordHistoryEntity(
            id = UUID.randomUUID().toString(),
            credentialId = credentialId,
            userId = userId,
            encryptedPassword = currentEncryptedPw,
            setAt = existing.lastChangedAt,
            replacedAt = System.currentTimeMillis()
        )
        dao.insertPasswordHistory(historyItem)

        // 2. Update credential with new password and recalculated strength
        val eval = PasswordStrengthEvaluator.evaluate(newPassword)
        val updatedEntity = existing.copy(
            encryptedPassword = encryptText(newPassword),
            passwordStrength = eval.score,
            lastChangedAt = System.currentTimeMillis()
        )
        dao.updateCredential(updatedEntity)
    }

    override suspend fun restoreOldPassword(credentialId: String, userId: String, historyItem: PasswordHistoryItem): Result<Unit> = runCatching {
        val existing = dao.getCredentialById(credentialId, userId) ?: throw IllegalArgumentException("Credential not found")
        val currentEncryptedPw = existing.encryptedPassword

        // Archive current before restoring
        val archiveItem = SecretPasswordHistoryEntity(
            id = UUID.randomUUID().toString(),
            credentialId = credentialId,
            userId = userId,
            encryptedPassword = currentEncryptedPw,
            setAt = existing.lastChangedAt,
            replacedAt = System.currentTimeMillis()
        )
        dao.insertPasswordHistory(archiveItem)

        // Remove restored item from history
        dao.deletePasswordHistoryItem(
            SecretPasswordHistoryEntity(
                id = historyItem.id,
                credentialId = historyItem.credentialId,
                userId = userId,
                encryptedPassword = encryptText(historyItem.password),
                setAt = historyItem.setAt,
                replacedAt = historyItem.replacedAt
            )
        )

        // Restore credential password
        val eval = PasswordStrengthEvaluator.evaluate(historyItem.password)
        val updated = existing.copy(
            encryptedPassword = encryptText(historyItem.password),
            passwordStrength = eval.score,
            lastChangedAt = System.currentTimeMillis()
        )
        dao.updateCredential(updated)
    }

    override suspend fun deleteCredential(credential: SecretCredential): Result<Unit> = runCatching {
        val entity = SecretCredentialEntity(
            id = credential.id,
            userId = credential.userId,
            serviceName = credential.serviceName,
            website = credential.website,
            username = credential.username,
            encryptedPassword = encryptText(credential.password),
            category = credential.category,
            passwordStrength = credential.passwordStrength,
            hasTwoFactor = credential.hasTwoFactor,
            lastChangedAt = credential.lastChangedAt,
            createdAt = credential.createdAt,
            notes = credential.notes,
            isDeleted = true
        )
        dao.updateCredential(entity)
    }

    override fun getPasswordHistory(credentialId: String): Flow<List<PasswordHistoryItem>> {
        return dao.getPasswordHistory(credentialId).map { list ->
            list.map { h ->
                PasswordHistoryItem(
                    id = h.id,
                    credentialId = h.credentialId,
                    password = decryptText(h.encryptedPassword),
                    setAt = h.setAt,
                    replacedAt = h.replacedAt
                )
            }
        }
    }

    override fun getTotpAccounts(userId: String): Flow<List<TotpAccount>> {
        return dao.getTotpAccounts(userId).map { list ->
            list.map { account ->
                TotpAccount(
                    id = account.id,
                    userId = account.userId,
                    serviceName = account.serviceName,
                    username = account.username,
                    secretKey = decryptText(account.encryptedSecretKey),
                    issuer = account.issuer,
                    type = account.type,
                    algorithm = account.algorithm,
                    digits = account.digits,
                    period = account.period,
                    counter = account.counter,
                    pin = account.pin,
                    colorHex = account.colorHex,
                    createdAt = account.createdAt
                )
            }
        }
    }

    override suspend fun saveTotpAccount(account: TotpAccount): Result<Unit> = runCatching {
        val entity = SecretTotpAccountEntity(
            id = account.id.ifBlank { UUID.randomUUID().toString() },
            userId = account.userId,
            serviceName = account.serviceName,
            username = account.username,
            encryptedSecretKey = encryptText(account.secretKey),
            issuer = account.issuer,
            type = account.type,
            algorithm = account.algorithm,
            digits = account.digits,
            period = account.period,
            counter = account.counter,
            pin = account.pin,
            colorHex = account.colorHex,
            createdAt = account.createdAt
        )
        dao.insertTotpAccount(entity)

        // Mark associated credentials as having 2FA
        val credentials = dao.getCredentialsOnce(account.userId)
        credentials.filter { it.serviceName.equals(account.serviceName, ignoreCase = true) }.forEach { cred ->
            if (!cred.hasTwoFactor) {
                dao.updateCredential(cred.copy(hasTwoFactor = true))
            }
        }
    }

    override suspend fun deleteTotpAccount(account: TotpAccount): Result<Unit> = runCatching {
        val entity = SecretTotpAccountEntity(
            id = account.id,
            userId = account.userId,
            serviceName = account.serviceName,
            username = account.username,
            encryptedSecretKey = encryptText(account.secretKey),
            issuer = account.issuer,
            type = account.type,
            algorithm = account.algorithm,
            digits = account.digits,
            period = account.period,
            counter = account.counter,
            pin = account.pin,
            colorHex = account.colorHex,
            createdAt = account.createdAt
        )
        dao.deleteTotpAccount(entity)
    }

    override fun getBackupCodes(userId: String): Flow<List<BackupCode>> {
        return dao.getBackupCodes(userId).map { list ->
            list.map { code ->
                BackupCode(
                    id = code.id,
                    userId = code.userId,
                    code = decryptText(code.encryptedCode),
                    isUsed = code.isUsed,
                    usedAt = code.usedAt,
                    createdAt = code.createdAt
                )
            }
        }
    }

    override suspend fun generateBackupCodes(userId: String): Result<List<String>> = runCatching {
        dao.clearBackupCodes(userId)
        val secureRandom = SecureRandom()
        val plainCodes = mutableListOf<String>()
        val entities = mutableListOf<SecretBackupCodeEntity>()

        repeat(8) {
            val num1 = String.format(Locale.US, "%04d", secureRandom.nextInt(10000))
            val num2 = String.format(Locale.US, "%04d", secureRandom.nextInt(10000))
            val codeStr = "$num1-$num2"
            plainCodes.add(codeStr)

            entities.add(
                SecretBackupCodeEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    encryptedCode = encryptText(codeStr),
                    isUsed = false,
                    usedAt = null,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        dao.insertBackupCodes(entities)
        plainCodes
    }

    override fun getVaultItems(userId: String): Flow<List<SecretVaultItem>> {
        return dao.getVaultItems(userId).map { list ->
            list.map { item ->
                val type = runCatching { VaultSecretType.valueOf(item.itemType) }.getOrDefault(VaultSecretType.PRIVATE_NOTE)
                SecretVaultItem(
                    id = item.id,
                    userId = item.userId,
                    itemType = type,
                    title = item.title,
                    content = decryptText(item.encryptedContent),
                    subtitle = item.subtitle,
                    isLocked = item.isLocked,
                    createdAt = item.createdAt
                )
            }
        }
    }

    override suspend fun saveVaultItem(item: SecretVaultItem): Result<Unit> = runCatching {
        val entity = SecretVaultItemEntity(
            id = item.id.ifBlank { UUID.randomUUID().toString() },
            userId = item.userId,
            itemType = item.itemType.name,
            title = item.title,
            encryptedContent = encryptText(item.content),
            subtitle = item.subtitle,
            isLocked = item.isLocked,
            createdAt = item.createdAt
        )
        dao.insertVaultItem(entity)
    }

    override suspend fun deleteVaultItem(item: SecretVaultItem): Result<Unit> = runCatching {
        val entity = SecretVaultItemEntity(
            id = item.id,
            userId = item.userId,
            itemType = item.itemType.name,
            title = item.title,
            encryptedContent = encryptText(item.content),
            subtitle = item.subtitle,
            isLocked = item.isLocked,
            createdAt = item.createdAt
        )
        dao.deleteVaultItem(entity)
    }

    override fun getSettings(userId: String): Flow<SecretSettings> {
        return dao.getSettingsFlow(userId).map { entity ->
            if (entity == null) {
                SecretSettings(userId = userId)
            } else {
                SecretSettings(
                    userId = entity.userId,
                    autoLockMinutes = entity.autoLockMinutes,
                    blockScreenshots = entity.blockScreenshots,
                    clearClipboardSeconds = entity.clearClipboardSeconds,
                    wipeAfterFailedTries = entity.wipeAfterFailedTries,
                    encryptedBackupEnabled = entity.encryptedBackupEnabled,
                    biometricRequired = entity.biometricRequired
                )
            }
        }
    }

    override suspend fun updateSettings(settings: SecretSettings): Result<Unit> = runCatching {
        val entity = SecretSettingsEntity(
            userId = settings.userId,
            autoLockMinutes = settings.autoLockMinutes,
            blockScreenshots = settings.blockScreenshots,
            clearClipboardSeconds = settings.clearClipboardSeconds,
            wipeAfterFailedTries = settings.wipeAfterFailedTries,
            encryptedBackupEnabled = settings.encryptedBackupEnabled,
            biometricRequired = settings.biometricRequired
        )
        dao.saveSettings(entity)
    }

    override fun getSecuritySummary(userId: String): Flow<SecurityScoreSummary> {
        return dao.getCredentials(userId).map { list ->
            val credentials = list.map { entity ->
                val decryptedPassword = decryptText(entity.encryptedPassword)
                SecretCredential(
                    id = entity.id,
                    userId = entity.userId,
                    serviceName = entity.serviceName,
                    website = entity.website,
                    username = entity.username,
                    password = decryptedPassword,
                    category = entity.category,
                    passwordStrength = entity.passwordStrength,
                    hasTwoFactor = entity.hasTwoFactor,
                    lastChangedAt = entity.lastChangedAt,
                    createdAt = entity.createdAt,
                    notes = entity.notes
                )
            }

            val weakList = credentials.filter { it.passwordStrength < 55 }
            val missing2faList = credentials.filter { !it.hasTwoFactor }

            // Group by password value to detect reuse
            val passwordCounts = credentials.groupBy { it.password }
            val reusedList = credentials.filter { (passwordCounts[it.password]?.size ?: 0) > 1 }

            val healthyCount = credentials.count { it.passwordStrength >= 55 && it.hasTwoFactor }

            val scorePenalty = (weakList.size * 7) + (missing2faList.size * 4) + (reusedList.size * 6)
            val computedScore = (100 - scorePenalty).coerceIn(0, 100)

            SecurityScoreSummary(
                score = computedScore,
                weakCredentials = weakList,
                missingTwoFactorCredentials = missing2faList,
                reusedCredentials = reusedList,
                healthyCredentialsCount = healthyCount,
                verifiedBreachesCount = 0
            )
        }
    }

    override suspend fun importFromCsv(csvText: String, userId: String): Result<CsvImportResult> = runCatching {
        val existingEntities = dao.getCredentialsOnce(userId)
        val result = CsvImporterExporter.importFromCsv(csvText, existingEntities)

        if (result.importedItems.isNotEmpty()) {
            val entities = result.importedItems.map { item ->
                val eval = PasswordStrengthEvaluator.evaluate(item.password)
                SecretCredentialEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    serviceName = item.serviceName,
                    website = item.website,
                    username = item.username,
                    encryptedPassword = encryptText(item.password),
                    category = item.category,
                    passwordStrength = eval.score,
                    hasTwoFactor = false,
                    lastChangedAt = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                    notes = item.notes,
                    isDeleted = false
                )
            }
            dao.insertCredentials(entities)
        }

        result
    }

    override suspend fun exportToCsv(userId: String): Result<String> = runCatching {
        val entities = dao.getCredentialsOnce(userId)
        val pairs = entities.map { entity ->
            val plainPw = decryptText(entity.encryptedPassword)
            Pair(entity, plainPw)
        }
        CsvImporterExporter.exportToCsv(pairs)
    }
}

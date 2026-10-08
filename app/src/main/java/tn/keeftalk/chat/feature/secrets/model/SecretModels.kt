package tn.keeftalk.chat.feature.secrets.model

data class SecretCredential(
    val id: String,
    val userId: String,
    val serviceName: String,
    val website: String = "",
    val username: String = "",
    val password: String = "", // Decrypted in memory
    val category: String = "Personal", // Personal, Work, Social, Finance
    val passwordStrength: Int = 0, // 0 to 100
    val hasTwoFactor: Boolean = false,
    val lastChangedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val history: List<PasswordHistoryItem> = emptyList()
)

data class PasswordHistoryItem(
    val id: String,
    val credentialId: String,
    val password: String, // Decrypted in memory
    val setAt: Long = System.currentTimeMillis(),
    val replacedAt: Long = System.currentTimeMillis()
)

data class TotpAccount(
    val id: String,
    val userId: String,
    val serviceName: String,
    val username: String = "",
    val secretKey: String = "", // Decrypted in memory
    val issuer: String = "",
    val type: String = "TOTP", // TOTP, HOTP, STEAM, YANDEX, MOTP
    val algorithm: String = "SHA1", // SHA1, SHA256, SHA512
    val digits: Int = 6,
    val period: Int = 30,
    val counter: Long = 0L,
    val pin: String = "",
    val colorHex: String = "#7D5CFF",
    val createdAt: Long = System.currentTimeMillis()
)

data class BackupCode(
    val id: String,
    val userId: String,
    val code: String = "", // Decrypted in memory
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class VaultSecretType {
    PASSPORT_ID,
    PRIVATE_NOTE,
    ENCRYPTION_KEY,
    CONTRACT,
    RECOVERY_PHRASE,
    SENSITIVE_FILE
}

data class SecretVaultItem(
    val id: String,
    val userId: String,
    val itemType: VaultSecretType = VaultSecretType.PRIVATE_NOTE,
    val title: String,
    val content: String = "", // Decrypted in memory
    val subtitle: String = "",
    val isLocked: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class SecretSettings(
    val userId: String,
    val autoLockMinutes: Int = 5,
    val blockScreenshots: Boolean = true,
    val clearClipboardSeconds: Int = 30,
    val wipeAfterFailedTries: Boolean = true,
    val encryptedBackupEnabled: Boolean = false,
    val biometricRequired: Boolean = true
)

data class SecurityScoreSummary(
    val score: Int,
    val weakCredentials: List<SecretCredential>,
    val missingTwoFactorCredentials: List<SecretCredential>,
    val reusedCredentials: List<SecretCredential>,
    val healthyCredentialsCount: Int,
    val verifiedBreachesCount: Int = 0
)

data class SecurityIssue(
    val id: String,
    val title: String,
    val description: String,
    val affectedItemsText: String,
    val severity: String, // "danger", "warn", "secure"
    val actionLabel: String,
    val actionType: String // "UPDATE_PASSWORD", "ADD_2FA", "GENERATE_BACKUP_CODES", "EXPORT_GOOGLE"
)

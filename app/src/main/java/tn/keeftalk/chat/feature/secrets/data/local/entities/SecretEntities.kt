package tn.keeftalk.chat.feature.secrets.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "secret_credentials",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["category"])
    ]
)
data class SecretCredentialEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "service_name") val serviceName: String,
    val website: String = "",
    val username: String = "",
    @ColumnInfo(name = "encrypted_password") val encryptedPassword: String,
    val category: String = "Personal",
    @ColumnInfo(name = "password_strength") val passwordStrength: Int = 0,
    @ColumnInfo(name = "has_two_factor") val hasTwoFactor: Boolean = false,
    @ColumnInfo(name = "last_changed_at") val lastChangedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)

@Entity(
    tableName = "secret_password_history",
    foreignKeys = [
        ForeignKey(
            entity = SecretCredentialEntity::class,
            parentColumns = ["id"],
            childColumns = ["credential_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["credential_id"]),
        Index(value = ["user_id"])
    ]
)
data class SecretPasswordHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "credential_id") val credentialId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "encrypted_password") val encryptedPassword: String,
    @ColumnInfo(name = "set_at") val setAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "replaced_at") val replacedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "secret_totp_accounts",
    indices = [
        Index(value = ["user_id"])
    ]
)
data class SecretTotpAccountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "service_name") val serviceName: String,
    val username: String = "",
    @ColumnInfo(name = "encrypted_secret_key") val encryptedSecretKey: String,
    val issuer: String = "",
    val type: String = "TOTP",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val counter: Long = 0L,
    val pin: String = "",
    @ColumnInfo(name = "color_hex") val colorHex: String = "#7D5CFF",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "secret_backup_codes",
    indices = [
        Index(value = ["user_id"])
    ]
)
data class SecretBackupCodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "encrypted_code") val encryptedCode: String,
    @ColumnInfo(name = "is_used") val isUsed: Boolean = false,
    @ColumnInfo(name = "used_at") val usedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "secret_vault_items",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["item_type"])
    ]
)
data class SecretVaultItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "item_type") val itemType: String,
    val title: String,
    @ColumnInfo(name = "encrypted_content") val encryptedContent: String,
    val subtitle: String = "",
    @ColumnInfo(name = "is_locked") val isLocked: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "secret_settings")
data class SecretSettingsEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "auto_lock_minutes") val autoLockMinutes: Int = 5,
    @ColumnInfo(name = "block_screenshots") val blockScreenshots: Boolean = true,
    @ColumnInfo(name = "clear_clipboard_seconds") val clearClipboardSeconds: Int = 30,
    @ColumnInfo(name = "wipe_after_failed_tries") val wipeAfterFailedTries: Boolean = true,
    @ColumnInfo(name = "encrypted_backup_enabled") val encryptedBackupEnabled: Boolean = false,
    @ColumnInfo(name = "biometric_required") val biometricRequired: Boolean = true
)

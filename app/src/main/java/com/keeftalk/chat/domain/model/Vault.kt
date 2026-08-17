package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VaultItemType {
    PHOTO, VIDEO, AUDIO, DOCUMENT, FOLDER, OTHER
}

@Serializable
data class VaultItem(
    val id: String,
    val userId: String,
    val file: File? = null,
    val folderId: String? = null,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val locked: Boolean = false,
    val metadata: Map<String, String> = emptyMap(),
    val tags: List<String> = emptyList(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

data class UploadProgress(
    val id: String,
    val fileName: String,
    val progress: Float,
    val uploadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class VaultFolder(
    val id: String,
    val userId: String = "",
    val name: String,
    val color: Int? = null,
    val icon: String? = null,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val totalSize: Long = 0
)

@Serializable
data class VaultTag(
    val id: String,
    val name: String,
    val color: Int
)

data class VaultStorageInfo(
    val cloudBytesUsed: Long,
    val cloudBytesLimit: Long,
    val localCacheBytesUsed: Long,
    val trashBytesUsed: Long,
    val itemsCount: Int,
    val categories: Map<VaultItemType, Long>,
    // For backward compatibility while migrating UI
    val totalUsed: Long = cloudBytesUsed,
    val cloudUsed: Long = cloudBytesUsed,
    val localCacheUsed: Long = localCacheBytesUsed
)

enum class StorageTipAction {
    NAVIGATE_TRASH,
    FILTER_LARGE_FILES,
    CLEAR_CACHE,
    UPGRADE_PLAN
}

data class VaultStorageTip(
    val id: String,
    val title: String,
    val description: String,
    val actionLabel: String,
    val action: StorageTipAction,
    val severity: TipSeverity = TipSeverity.INFO
)

enum class TipSeverity {
    INFO, WARNING, CRITICAL
}

val VAULT_STORAGE_LIMIT = SubscriptionPlan.FREE.storageLimit

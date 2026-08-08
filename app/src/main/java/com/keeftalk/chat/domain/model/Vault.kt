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

@Serializable
data class VaultFolder(
    val id: String,
    val name: String,
    val color: Int? = null,
    val icon: String? = null,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class VaultTag(
    val id: String,
    val name: String,
    val color: Int
)

data class VaultStorageInfo(
    val totalUsed: Long,
    val cloudUsed: Long,
    val localCacheUsed: Long,
    val itemsCount: Int,
    val categories: Map<VaultItemType, Long>
)

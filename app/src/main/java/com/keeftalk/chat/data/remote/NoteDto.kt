package com.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String?,
    @SerialName("content") val content: String?,
    @SerialName("color") val color: String?,
    @SerialName("pinned") val pinned: Boolean?,
    @SerialName("archived") val archived: Boolean?,
    @SerialName("container") val container: String?,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("can_others_add") val canOthersAdd: Boolean?,
    @SerialName("created_at") val createdAt: String?, // Supabase uses ISO8601 strings
    @SerialName("updated_at") val updatedAt: String?,
    @SerialName("ciphertext") val ciphertext: String? = null,
    @SerialName("iv") val iv: String? = null,
    @SerialName("crypto_version") val cryptoVersion: Int? = 0
)

@Serializable
data class NoteShareDto(
    @SerialName("id") val id: String,
    @SerialName("note_id") val noteId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("access") val access: String,
    @SerialName("created_at") val createdAt: String?
)

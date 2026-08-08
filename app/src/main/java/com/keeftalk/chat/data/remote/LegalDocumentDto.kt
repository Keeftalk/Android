package com.keeftalk.chat.data.remote

import com.keeftalk.chat.domain.model.LegalDocument
import com.keeftalk.chat.util.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LegalDocumentDto(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val version: Int,
    @SerialName("is_active")
    val isActive: Boolean,
    @Serializable(with = TimestampSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Long
)

fun LegalDocumentDto.toDomain() = LegalDocument(
    id = id,
    type = type,
    title = title,
    content = content,
    version = version,
    updatedAt = updatedAt,
    isFromCache = false
)

package com.keeftalk.chat.domain.model

data class LegalDocument(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val version: Int,
    val updatedAt: Long,
    val isFromCache: Boolean = false
)

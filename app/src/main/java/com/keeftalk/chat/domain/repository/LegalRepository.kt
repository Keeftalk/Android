package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.LegalDocument

interface LegalRepository {
    suspend fun getLegalDocument(type: String): Result<LegalDocument>
}

package tn.keeftalk.chat.domain.repository

import tn.keeftalk.chat.domain.model.LegalDocument

interface LegalRepository {
    suspend fun getLegalDocument(type: String): Result<LegalDocument>
}

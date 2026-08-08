package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.DocumentFolder
import com.keeftalk.chat.domain.model.DocumentModel
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getRecentDocuments(): Flow<List<DocumentModel>>
    fun getSharedDocuments(): Flow<List<DocumentModel>>
    fun getFolders(): Flow<List<DocumentFolder>>
    fun getDocumentsInFolder(bucketId: String): Flow<List<DocumentModel>>
    suspend fun resolveMetadata(uri: android.net.Uri): DocumentModel?
    fun isFullAccessUnlocked(): Boolean
    fun setFullAccessUri(uri: android.net.Uri?)
}

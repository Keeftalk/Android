package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.Note
import com.keeftalk.chat.domain.model.User
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(container: String, searchQuery: String): Flow<List<Note>>
    fun getNote(id: String): Flow<Note?>
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(id: String)
    suspend fun archiveNote(id: String, archived: Boolean)
    suspend fun pinNote(id: String, pinned: Boolean)
    suspend fun addShare(noteId: String, userId: String, access: String)
    suspend fun removeShare(noteId: String, userId: String)
    fun getSharedUsers(noteId: String): Flow<List<User>>
    suspend fun syncNotes()
    suspend fun processSyncQueue()
    suspend fun uploadMedia(uri: String): String // Returns fileId
    suspend fun downloadToCache(fileId: String): String
    fun observeNotesRealtime(): Flow<Unit>
    fun shutdown()
}

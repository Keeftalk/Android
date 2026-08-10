package com.keeftalk.chat.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.keeftalk.chat.data.local.KeeftalkDatabase
import com.keeftalk.chat.data.local.dao.NoteDao
import com.keeftalk.chat.data.local.dao.SyncQueueDao
import com.keeftalk.chat.data.local.entities.*
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.data.remote.NoteDto
import com.keeftalk.chat.data.remote.NoteShareDto
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.domain.repository.NoteRepository
import com.keeftalk.chat.util.NotesLogger
import com.keeftalk.chat.security.crypto.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NoteRepositoryImpl(
    private val context: Context,
    private val database: KeeftalkDatabase,
    private val syncQueueDao: SyncQueueDao,
    private val fileDao: com.keeftalk.chat.data.local.dao.FileDao,
    private val fileUploadManager: com.keeftalk.chat.util.FileUploadManager,
    private val prefs: UserPreferencesRepository
) : NoteRepository {

    private val cryptoManager get() = AppModule.provideCryptoManager(context)

    private val noteDao: NoteDao by lazy { database.noteDao() }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        NotesLogger.i("REPO", "NoteRepositoryImpl initialized")
        com.keeftalk.chat.util.StartupOrchestrator.enqueue(com.keeftalk.chat.util.StartupOrchestrator.Tier.TIER_3_POST_RENDER) {
            scope.launch {
                while (isActive) {
                    processSyncQueue()
                    delay(30.seconds)
                }
            }
        }
    }

    private fun Note.toDto() = NoteDto(
        id = id,
        title = title,
        content = content,
        color = color,
        pinned = pinned,
        archived = archived,
        container = container,
        ownerId = ownerId,
        canOthersAdd = canOthersAdd,
        createdAt = isoFormatter.format(Date(createdAt)),
        updatedAt = isoFormatter.format(Date(updatedAt))
    )

    override fun getNotes(container: String, searchQuery: String): Flow<List<Note>> {
        val baseFlow: Flow<List<NoteWithShares>> = when (container) {
            "Archived" -> noteDao.getArchivedNotes()
            "Shared" -> noteDao.getActiveNotesByContainer("All").map { list ->
                list.filter { it.shares.isNotEmpty() }
            }
            else -> noteDao.getActiveNotesByContainer(container)
        }

        return baseFlow.map { entities ->
            entities.map { it.toDomainInternal() }.filter {
                (it.title.contains(searchQuery, ignoreCase = true)) ||
                        (it.content.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    override fun getNote(id: String): Flow<Note?> {
        return noteDao.getNoteById(id).map { it?.toDomainInternal() }
    }

    override suspend fun saveNote(note: Note) {
        if (note.ownerId.isEmpty()) return
        
        // --- E2E ENCRYPTION ---
        val sensitiveData = buildJsonObject {
            put("title", note.title)
            put("content", note.content)
        }

        val encrypted = cryptoManager.encryptForStorage(
            sensitiveData.toString().toByteArray(Charsets.UTF_8),
            com.keeftalk.chat.security.crypto.CryptoManager.StoragePurpose.NOTES
        )

        val encryptedNoteEntity = note.toEntity().copy(
            ciphertext = encrypted.ciphertext,
            iv = encrypted.iv,
            cryptoVersion = encrypted.version,
            title = "[Encrypted]",
            content = "" 
        )

        database.withTransaction {
            noteDao.insertNote(encryptedNoteEntity)
            
            // Handle attachments
            note.attachments.forEach { file ->
                fileDao.insertNoteAttachment(NoteAttachmentEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = note.id,
                    fileId = file.id
                ))
            }
        }
        
        val encryptedNoteDto = note.toDto().copy(
            title = "[Encrypted]",
            content = "",
            ciphertext = encrypted.ciphertext,
            iv = encrypted.iv,
            cryptoVersion = encrypted.version
        )
        
        val payload = Json.encodeToString(encryptedNoteDto)
        syncQueueDao.insert(SyncQueueEntity(noteId = note.id, operation = "SAVE", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun deleteNote(id: String) {
        val noteWithShares = noteDao.getNoteByIdSync(id) ?: return
        val attachments = fileDao.getFilesForNote(id)
        
        noteDao.deleteNote(noteWithShares.note)
        syncQueueDao.insert(SyncQueueEntity(noteId = id, operation = "DELETE", payload = null))
        
        val referenceManager = AppModule.provideFileReferenceManager(context)
        attachments.forEach { file ->
            referenceManager.removeReference(file.id)
        }
        
        scope.launch { processSyncQueue() }
    }

    override suspend fun archiveNote(id: String, archived: Boolean) {
        val noteWithShares = noteDao.getNoteByIdSync(id) ?: return
        val updated = noteWithShares.note.copy(archived = archived, updatedAt = System.currentTimeMillis())
        noteDao.updateNote(updated)
        
        val payload = Json.encodeToString(mapOf("archived" to archived.toString(), "updated_at" to isoFormatter.format(Date(updated.updatedAt!!))))
        syncQueueDao.insert(SyncQueueEntity(noteId = id, operation = "ARCHIVE", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun pinNote(id: String, pinned: Boolean) {
        val noteWithShares = noteDao.getNoteByIdSync(id) ?: return
        val updated = noteWithShares.note.copy(pinned = pinned, updatedAt = System.currentTimeMillis())
        noteDao.updateNote(updated)
        
        val payload = Json.encodeToString(mapOf("pinned" to pinned.toString(), "updated_at" to isoFormatter.format(Date(updated.updatedAt!!))))
        syncQueueDao.insert(SyncQueueEntity(noteId = id, operation = "PIN", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun addShare(noteId: String, userId: String, access: String) {
        val shareId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val shareEntity = NoteShareEntity(id = shareId, noteId = noteId, userId = userId, access = access, createdAt = createdAt)
        noteDao.insertShare(shareEntity)
        
        val payload = Json.encodeToString(NoteShareDto(shareId, noteId, userId, access, isoFormatter.format(Date(createdAt))))
        syncQueueDao.insert(SyncQueueEntity(noteId = noteId, operation = "SHARE", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun removeShare(noteId: String, userId: String) {
        noteDao.deleteShare(noteId, userId)
        val payload = Json.encodeToString(mapOf("noteId" to noteId, "userId" to userId))
        syncQueueDao.insert(SyncQueueEntity(noteId = noteId, operation = "REMOVE_SHARE", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun processSyncQueue() = withContext(Dispatchers.IO) {
        val pending = syncQueueDao.getPendingItems()
        if (pending.isEmpty()) return@withContext
        
        val supabase = getSupabase()
        pending.forEach { item ->
            try {
                when (item.operation) {
                    "SAVE" -> supabase.postgrest["notes"].upsert(Json.decodeFromString<NoteDto>(item.payload!!))
                    "DELETE" -> supabase.postgrest["notes"].delete { filter { eq("id", item.noteId) } }
                    "ARCHIVE" -> {
                        val data = Json.decodeFromString<Map<String, String>>(item.payload!!)
                        supabase.postgrest["notes"].update({
                            set("archived", data["archived"]!!.toBoolean())
                            set("updated_at", data["updated_at"]!!)
                        }) { filter { eq("id", item.noteId) } }
                    }
                    "PIN" -> {
                        val data = Json.decodeFromString<Map<String, String>>(item.payload!!)
                        supabase.postgrest["notes"].update({
                            set("pinned", data["pinned"]!!.toBoolean())
                            set("updated_at", data["updated_at"]!!)
                        }) { filter { eq("id", item.noteId) } }
                    }
                    "SHARE" -> supabase.postgrest["note_shares"].upsert(Json.decodeFromString<NoteShareDto>(item.payload!!))
                    "REMOVE_SHARE" -> {
                        val data = Json.decodeFromString<Map<String, String>>(item.payload!!)
                        supabase.postgrest["note_shares"].delete {
                            filter {
                                eq("note_id", data["noteId"]!!)
                                eq("user_id", data["userId"]!!)
                            }
                        }
                    }
                }
                syncQueueDao.delete(item)
            } catch (e: Exception) {
                if (item.retryCount < 5) syncQueueDao.update(item.copy(retryCount = item.retryCount + 1))
                else syncQueueDao.update(item.copy(status = "FAILED"))
            }
        }
    }

    override suspend fun syncNotes() {
        val lastSync = prefs.userPreferencesFlow.first().lastNotesSyncTimestamp
        if (syncMutex.isLocked) return
        
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastSync < 5000) return@withLock
            
            try {
                val supabase = getSupabase()
                val lastUpdateStr = isoFormatter.format(Date(lastSync))
                val notes = supabase.postgrest["notes"].select { filter { gt("updated_at", lastUpdateStr) } }.decodeList<NoteDto>()
                val shares = supabase.postgrest["note_shares"].select().decodeList<NoteShareDto>()

                database.withTransaction {
                    if (notes.isNotEmpty()) {
                        noteDao.insertNotes(notes.map { it.toDomain().toEntity() })
                    }
                    if (shares.isNotEmpty()) {
                        noteDao.insertShares(shares.map { NoteShareEntity(it.id, it.noteId, it.userId, it.access, isoFormatter.parse(it.createdAt ?: "")?.time) })
                    }
                }
                
                val allUserIds = (notes.map { it.ownerId } + shares.map { it.userId }).distinct()
                syncProfilesByIds(allUserIds)
                prefs.updateLastNotesSyncTimestamp(now)
            } catch (e: Exception) {}
        }
    }

    private suspend fun syncProfilesByIds(userIds: List<String>) {
        if (userIds.isEmpty()) return
        try {
            val supabase = getSupabase()
            val profiles = supabase.postgrest["profiles"].select { filter { isIn("id", userIds) } }.decodeList<Profile>()
            if (profiles.isNotEmpty()) {
                database.withTransaction {
                    noteDao.insertUsers(profiles.map { profile ->
                        val name = (profile.fullName ?: profile.username).ifBlank { "User" }
                        UserEntity(id = profile.id, name = name, username = profile.username, avatarUrl = profile.avatarUrl, isActive = true)
                    })
                }
            }
        } catch (e: Exception) {}
    }

    override fun getSharedUsers(noteId: String): Flow<List<User>> {
        return noteDao.getSharedUsers(noteId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun uploadMedia(uri: String): String = withContext(Dispatchers.IO) {
        val androidUri = Uri.parse(uri)
        val file = File(context.cacheDir, "note_media_temp_${UUID.randomUUID()}")
        context.contentResolver.openInputStream(androidUri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val currentUserId = getSupabase().auth.currentUserOrNull()?.id ?: "anon"
        val result = fileUploadManager.uploadFile(file, SourceType.NOTE, currentUserId)
        
        if (result.isSuccess) {
            result.getOrThrow().id
        } else {
            throw result.exceptionOrNull() ?: Exception("Upload failed")
        }
    }

    override suspend fun downloadToCache(fileId: String): String = withContext(Dispatchers.IO) {
        try {
            val fileEntity = fileDao.getFileById(fileId) ?: return@withContext ""
            val file = fileEntity.toDomain()
            val fileRepo = AppModule.provideFileRepository(context)
            val result = fileRepo.ensureMediaLocal(file, null)
            result.getOrNull()?.absolutePath ?: ""
        } catch (e: Exception) { "" }
    }

    @OptIn(FlowPreview::class)
    override fun observeNotesRealtime(): Flow<Unit> = flow {
        val supabase = getSupabase()
        val channel = supabase.realtime.channel("notes_realtime")
        val notesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "notes" }
        val realtimeFlow = callbackFlow {
            val job = scope.launch {
                notesFlow.debounce(1000.milliseconds).collect {
                    syncNotes()
                    trySend(Unit)
                }
            }
            launch { channel.subscribe() }
            awaitClose {
                job.cancel()
                launch { channel.unsubscribe() }
            }
        }
        emitAll(realtimeFlow)
    }

    override fun shutdown() {
        NotesLogger.i("REPO", "Shutting down NoteRepository")
        scope.cancel()
    }

    private fun NoteDto.toDomain() = Note(
        id = id, title = title ?: "", content = content ?: "",
        color = color ?: "#6C63FF", pinned = pinned ?: false, archived = archived ?: false,
        container = container ?: "All", ownerId = ownerId, canOthersAdd = canOthersAdd ?: false,
        createdAt = isoFormatter.parse(createdAt ?: "")?.time ?: System.currentTimeMillis(),
        updatedAt = isoFormatter.parse(updatedAt ?: "")?.time ?: System.currentTimeMillis()
    ).let { decryptNote(it, ciphertext, iv, cryptoVersion ?: 0) }

    private fun NoteWithShares.toDomainInternal(): Note {
        val noteDomain = Note(
            id = note.id, title = note.title ?: "", content = note.content ?: "[]", color = note.color ?: "#6C63FF", 
            pinned = note.pinned ?: false, archived = note.archived ?: false, container = note.container ?: "All", 
            ownerId = note.ownerId, canOthersAdd = note.canOthersAdd ?: false, createdAt = note.createdAt ?: System.currentTimeMillis(), 
            updatedAt = note.updatedAt ?: System.currentTimeMillis(), sharedUsers = shares.map { it.toDomain() },
            attachments = attachments.map { it.toDomain() }
        )
        return decryptNote(noteDomain, note.ciphertext, note.iv, note.cryptoVersion ?: 0)
    }

    private fun decryptNote(note: Note, ciphertext: String?, iv: String?, version: Int): Note {
        if (ciphertext == null || iv == null) return note
        return try {
            val encryptedObj = EncryptedObject(version = version, keyId = "root", iv = iv, ciphertext = ciphertext)
            val decryptedBytes = cryptoManager.decryptFromStorage(encryptedObj, com.keeftalk.chat.security.crypto.CryptoManager.StoragePurpose.NOTES)
            val decryptedStr = String(decryptedBytes, Charsets.UTF_8)
            val json = Json.parseToJsonElement(decryptedStr).jsonObject
            note.copy(
                title = json["title"]?.jsonPrimitive?.content ?: note.title,
                content = json["content"]?.jsonPrimitive?.content ?: note.content
            )
        } catch (e: Exception) {
            note
        }
    }
}

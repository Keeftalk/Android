package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: String): FileEntity?

    @Query("SELECT * FROM files WHERE file_hash = :hash")
    suspend fun getFileByHash(hash: String): FileEntity?

    @Query("SELECT * FROM files WHERE status = 'ACTIVE'")
    fun getAllActiveFiles(): Flow<List<FileEntity>>

    @Query("UPDATE files SET reference_count = reference_count + 1 WHERE id = :fileId")
    suspend fun incrementReferenceCount(fileId: String)

    @Query("UPDATE files SET reference_count = reference_count - 1 WHERE id = :fileId")
    suspend fun decrementReferenceCount(fileId: String)

    @Query("UPDATE files SET status = :status, deleted_at = :deletedAt WHERE id = :fileId")
    suspend fun updateFileStatus(fileId: String, status: String, deletedAt: Long?)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    @Query("SELECT * FROM files WHERE status = 'PENDING_DELETE'")
    suspend fun getFilesPendingDelete(): List<FileEntity>

    // Attachment related queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageAttachment(attachment: MessageAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteAttachment(attachment: NoteAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgendaAttachment(attachment: AgendaAttachmentEntity)

    @Query("DELETE FROM message_attachment WHERE message_id = :messageId AND file_id = :fileId")
    suspend fun deleteMessageAttachment(messageId: String, fileId: String)

    @Query("SELECT files.* FROM files INNER JOIN message_attachment ON files.id = message_attachment.file_id WHERE message_attachment.message_id = :messageId")
    suspend fun getFilesForMessage(messageId: String): List<FileEntity>
    
    @Query("SELECT files.* FROM files INNER JOIN note_attachment ON files.id = note_attachment.file_id WHERE note_attachment.note_id = :noteId")
    suspend fun getFilesForNote(noteId: String): List<FileEntity>

    @Query("SELECT files.* FROM files INNER JOIN agenda_attachment ON files.id = agenda_attachment.file_id WHERE agenda_attachment.agenda_event_id = :eventId")
    suspend fun getFilesForAgendaEvent(eventId: String): List<FileEntity>

    @Query("SELECT COUNT(*) FROM message_attachment WHERE file_id = :fileId")
    suspend fun countMessageAttachments(fileId: String): Int

    @Query("SELECT COUNT(*) FROM note_attachment WHERE file_id = :fileId")
    suspend fun countNoteAttachments(fileId: String): Int

    @Query("SELECT COUNT(*) FROM agenda_attachment WHERE file_id = :fileId")
    suspend fun countAgendaAttachments(fileId: String): Int

    @Query("SELECT COUNT(*) FROM vault_items WHERE file_id = :fileId")
    suspend fun countVaultItems(fileId: String): Int

    @Query("UPDATE files SET reference_count = :count WHERE id = :fileId")
    suspend fun updateReferenceCount(fileId: String, count: Int)

    @Query("UPDATE files SET local_path = :path WHERE id = :id")
    suspend fun updateLocalPath(id: String, path: String)

    @Query("UPDATE files SET thumbnail_local_path = :path WHERE id = :id")
    suspend fun updateThumbnailLocalPath(id: String, path: String)

    @Query("UPDATE files SET thumbnail_remote_path = :path, thumbnail_size = :size, thumbnail_width = :width, thumbnail_height = :height WHERE id = :id")
    suspend fun updateThumbnailRemoteMetadata(id: String, path: String, size: Long, width: Int?, height: Int?)

    @Query("UPDATE files SET file_name = :fileName, mime_type = :mimeType WHERE id = :id")
    suspend fun updateFileDetails(id: String, fileName: String?, mimeType: String?)

    @Query("SELECT id FROM files")
    suspend fun getAllFileIds(): List<String>

    @Query("SELECT SUM(file_size + IFNULL(thumbnail_size, 0)) FROM files WHERE owner_id = :userId AND status != 'DELETED'")
    fun getAccountCloudBytesFlow(userId: String): Flow<Long?>
}

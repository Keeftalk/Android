package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.NoteEntity
import com.keeftalk.chat.data.local.entities.NoteShareEntity
import com.keeftalk.chat.data.local.entities.NoteWithShares
import com.keeftalk.chat.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE archived = 0 AND container = :container ORDER BY pinned DESC, updated_at DESC")
    fun getActiveNotesByContainer(container: String): Flow<List<NoteWithShares>>

    @Transaction
    @Query("SELECT * FROM notes WHERE archived = 1 ORDER BY updated_at DESC")
    fun getArchivedNotes(): Flow<List<NoteWithShares>>

    @Transaction
    @Query("SELECT notes.* FROM notes JOIN note_shares ON notes.id = note_shares.note_id WHERE note_shares.user_id = :userId AND notes.archived = 0")
    fun getSharedWithMe(userId: String): Flow<List<NoteWithShares>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<NoteShareEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: String): Flow<NoteWithShares?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteByIdSync(id: String): NoteWithShares?

    @Query("SELECT MAX(updated_at) FROM notes")
    suspend fun getLastUpdatedNoteTime(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: NoteShareEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM note_shares WHERE note_id = :noteId AND user_id = :userId")
    suspend fun deleteShare(noteId: String, userId: String)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesOnce(): List<NoteEntity>

    @Query("SELECT users.* FROM users JOIN note_shares ON users.id = note_shares.user_id WHERE note_shares.note_id = :noteId")
    fun getSharedUsers(noteId: String): Flow<List<UserEntity>>
}

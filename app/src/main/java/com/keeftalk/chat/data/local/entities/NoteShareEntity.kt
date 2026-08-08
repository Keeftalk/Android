package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.UUID

@Entity(tableName = "note_shares")
data class NoteShareEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val access: String, // "read" or "write"
    @ColumnInfo(name = "created_at") val createdAt: Long? = System.currentTimeMillis()
)

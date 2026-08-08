package com.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "message_attachment",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["file_id"]
        )
    ]
)
data class MessageAttachmentEntity(
    @PrimaryKey val id: String,
    @SerialName("message_id") @ColumnInfo(name = "message_id") val messageId: String,
    @SerialName("file_id") @ColumnInfo(name = "file_id") val fileId: String,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "note_attachment",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["file_id"]
        )
    ]
)
data class NoteAttachmentEntity(
    @PrimaryKey val id: String,
    @SerialName("note_id") @ColumnInfo(name = "note_id") val noteId: String,
    @SerialName("file_id") @ColumnInfo(name = "file_id") val fileId: String,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "agenda_attachment",
    foreignKeys = [
        ForeignKey(
            entity = CalendarItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["agenda_event_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["file_id"]
        )
    ]
)
data class AgendaAttachmentEntity(
    @PrimaryKey val id: String,
    @SerialName("agenda_event_id") @ColumnInfo(name = "agenda_event_id") val agendaEventId: String,
    @SerialName("file_id") @ColumnInfo(name = "file_id") val fileId: String,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

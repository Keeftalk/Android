package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.ColumnInfo
import androidx.room.Junction
import com.keeftalk.chat.domain.model.*

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val content: String?,
    val color: String?,
    val pinned: Boolean?,
    val archived: Boolean?,
    val container: String?,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "can_others_add") val canOthersAdd: Boolean?,
    @ColumnInfo(name = "created_at") val createdAt: Long?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long?,
    val ciphertext: String? = null,
    val iv: String? = null,
    @ColumnInfo(name = "crypto_version") val cryptoVersion: Int? = 0
)

data class NoteWithShares(
    @Embedded val note: NoteEntity,
    @Relation(
        entity = NoteShareEntity::class,
        parentColumn = "id",
        entityColumn = "note_id"
    )
    val shares: List<NoteShareWithUser>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            NoteAttachmentEntity::class,
            parentColumn = "note_id",
            entityColumn = "file_id"
        )
    )
    val attachments: List<FileEntity>
)

data class NoteShareWithUser(
    @Embedded val share: NoteShareEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "id"
    )
    val user: UserEntity?
)

fun NoteWithShares.toDomain() = Note(
    id = note.id,
    title = note.title ?: "",
    content = note.content ?: "[]",
    color = note.color ?: "#6C63FF",
    pinned = note.pinned ?: false,
    archived = note.archived ?: false,
    container = note.container ?: "All",
    ownerId = note.ownerId,
    canOthersAdd = note.canOthersAdd ?: false,
    createdAt = note.createdAt ?: System.currentTimeMillis(),
    updatedAt = note.updatedAt ?: System.currentTimeMillis(),
    sharedUsers = shares.map { it.toDomain() },
    attachments = attachments.map { it.toDomain() }
)

fun NoteShareWithUser.toDomain() = NoteShare(
    user = (user ?: UserEntity(
        id = share.userId,
        name = "Unknown User",
        username = "unknown",
        avatarUrl = null
    )).toDomain(),
    access = share.access
)

fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    color = color,
    pinned = pinned,
    archived = archived,
    container = container,
    ownerId = ownerId,
    canOthersAdd = canOthersAdd,
    createdAt = createdAt,
    updatedAt = updatedAt
)

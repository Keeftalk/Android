package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "docs",
    indices = [
        Index(value = ["is_archived"]),
        Index(value = ["is_deleted"])
    ]
)
data class DocEntity(
    @PrimaryKey val id: String,
    val title: String,
    @SerialName("author_or_source") @ColumnInfo(name = "author_or_source") val authorOrSource: String = "",
    @SerialName("total_pages_or_sections") @ColumnInfo(name = "total_pages_or_sections") val totalPagesOrSections: Int = 1,
    val format: String = "PDF", // PDF, MARKDOWN, HTML, TXT, DOCX, EPUB, CODE, OTHER
    @SerialName("file_path") @ColumnInfo(name = "file_path") val filePath: String = "",
    @SerialName("file_size") @ColumnInfo(name = "file_size") val fileSize: Long = 0L,
    @SerialName("mime_type") @ColumnInfo(name = "mime_type") val mimeType: String = "",
    @SerialName("cover_url") @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    @SerialName("custom_cover_json") @ColumnInfo(name = "custom_cover_json") val customCoverJson: String? = null,
    @SerialName("percent_progress") @ColumnInfo(name = "percent_progress") val percentProgress: Int = 0,
    @SerialName("last_read_page") @ColumnInfo(name = "last_read_page") val lastReadPage: Int = 1,
    @SerialName("last_read_position_json") @ColumnInfo(name = "last_read_position_json") val lastReadPositionJson: String? = null,
    @SerialName("is_favorite") @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_archived") @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @SerialName("is_deleted") @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @SerialName("content_hash") @ColumnInfo(name = "content_hash") val contentHash: String? = null,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "doc_bookmarks",
    primaryKeys = ["doc_id", "page_or_position"],
    indices = [Index(value = ["doc_id"])]
)
data class DocBookmarkEntity(
    @SerialName("doc_id") @ColumnInfo(name = "doc_id") val docId: String,
    @SerialName("page_or_position") @ColumnInfo(name = "page_or_position") val pageOrPosition: Int,
    @SerialName("chapter_title") @ColumnInfo(name = "chapter_title") val chapterTitle: String = "",
    val excerpt: String = "",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "doc_highlights",
    indices = [Index(value = ["doc_id"])]
)
data class DocHighlightEntity(
    @PrimaryKey val id: String,
    @SerialName("doc_id") @ColumnInfo(name = "doc_id") val docId: String,
    @SerialName("page_or_position") @ColumnInfo(name = "page_or_position") val pageOrPosition: Int,
    @SerialName("start_offset") @ColumnInfo(name = "start_offset") val startOffset: Int = 0,
    @SerialName("end_offset") @ColumnInfo(name = "end_offset") val endOffset: Int = 0,
    @SerialName("selected_text") @ColumnInfo(name = "selected_text") val selectedText: String,
    @SerialName("color_hex") @ColumnInfo(name = "color_hex") val colorHex: String = "#2ECC71",
    @SerialName("note_text") @ColumnInfo(name = "note_text") val noteText: String = "",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "doc_sync_queue")
data class DocSyncQueueEntity(
    @PrimaryKey val id: String,
    val action: String, // INSERT, UPDATE, DELETE, UPSERT
    @SerialName("entity_type") @ColumnInfo(name = "entity_type") val entityType: String,
    @SerialName("payload_json") @ColumnInfo(name = "payload_json") val payloadJson: String,
    val status: String = "PENDING",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["is_archived"]),
        Index(value = ["is_deleted"])
    ]
)
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    @SerialName("total_pages") @ColumnInfo(name = "total_pages") val totalPages: Int,
    val format: String = "PDF", // PDF or EPUB
    @SerialName("file_path") @ColumnInfo(name = "file_path") val filePath: String = "",
    @SerialName("cover_url") @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    @SerialName("custom_cover_json") @ColumnInfo(name = "custom_cover_json") val customCoverJson: String? = null,
    @SerialName("percent_progress") @ColumnInfo(name = "percent_progress") val percentProgress: Int = 0,
    @SerialName("last_read_page") @ColumnInfo(name = "last_read_page") val lastReadPage: Int = 1,
    @SerialName("hours_read") @ColumnInfo(name = "hours_read") val hoursRead: Float = 0f,
    @SerialName("pages_per_day") @ColumnInfo(name = "pages_per_day") val pagesPerDay: Int = 10,
    @SerialName("speed_pages_per_hour") @ColumnInfo(name = "speed_pages_per_hour") val speedPagesPerHour: Int = 30,
    @SerialName("insight_text") @ColumnInfo(name = "insight_text") val insightText: String = "",
    @SerialName("is_archived") @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @SerialName("is_deleted") @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_favorite") @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @SerialName("marked_done") @ColumnInfo(name = "marked_done") val markedDone: Boolean = false,
    @SerialName("original_progress") @ColumnInfo(name = "original_progress") val originalProgress: Int = 0,
    @SerialName("original_insight") @ColumnInfo(name = "original_insight") val originalInsight: String = "",
    @SerialName("content_hash") @ColumnInfo(name = "content_hash") val contentHash: String? = null,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "book_pages_progress",
    primaryKeys = ["book_id", "page_number"],
    indices = [Index(value = ["book_id"])]
)
data class BookPageProgressEntity(
    @SerialName("book_id") @ColumnInfo(name = "book_id") val bookId: String,
    @SerialName("page_number") @ColumnInfo(name = "page_number") val pageNumber: Int,
    @SerialName("is_read") @ColumnInfo(name = "is_read") val isRead: Boolean = false,
    @SerialName("phrase_dock_index") @ColumnInfo(name = "phrase_dock_index") val phraseDockIndex: Int? = null,
    @SerialName("free_x") @ColumnInfo(name = "free_x") val freeX: Float? = null,
    @SerialName("free_y") @ColumnInfo(name = "free_y") val freeY: Float? = null,
    @SerialName("updated_at") @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "book_highlights",
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["page_number"])
    ]
)
data class BookHighlightEntity(
    @PrimaryKey val id: String,
    @SerialName("book_id") @ColumnInfo(name = "book_id") val bookId: String,
    @SerialName("page_number") @ColumnInfo(name = "page_number") val pageNumber: Int,
    @SerialName("start_offset") @ColumnInfo(name = "start_offset") val startOffset: Int,
    @SerialName("end_offset") @ColumnInfo(name = "end_offset") val endOffset: Int,
    @SerialName("selected_text") @ColumnInfo(name = "selected_text") val selectedText: String,
    @SerialName("color_hex") @ColumnInfo(name = "color_hex") val colorHex: String,
    @SerialName("is_quote") @ColumnInfo(name = "is_quote") val isQuote: Boolean = false,
    @SerialName("note_text") @ColumnInfo(name = "note_text") val noteText: String = "",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "book_bookmarks",
    primaryKeys = ["book_id", "page_number"],
    indices = [Index(value = ["book_id"])]
)
data class BookBookmarkEntity(
    @SerialName("book_id") @ColumnInfo(name = "book_id") val bookId: String,
    @SerialName("page_number") @ColumnInfo(name = "page_number") val pageNumber: Int,
    @SerialName("chapter_title") @ColumnInfo(name = "chapter_title") val chapterTitle: String = "",
    val excerpt: String = "",
    @SerialName("is_user") @ColumnInfo(name = "is_user") val isUser: Boolean = true,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "book_reading_sessions",
    indices = [Index(value = ["book_id"])]
)
data class BookReadingSessionEntity(
    @PrimaryKey val id: String,
    @SerialName("book_id") @ColumnInfo(name = "book_id") val bookId: String,
    @SerialName("start_time") @ColumnInfo(name = "start_time") val startTime: Long,
    @SerialName("end_time") @ColumnInfo(name = "end_time") val endTime: Long,
    @SerialName("pages_read") @ColumnInfo(name = "pages_read") val pagesRead: Int,
    @SerialName("duration_seconds") @ColumnInfo(name = "duration_seconds") val durationSeconds: Long,
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "book_sync_queue")
data class BookSyncQueueEntity(
    @PrimaryKey val id: String,
    val action: String, // INSERT, UPDATE, DELETE
    @SerialName("entity_type") @ColumnInfo(name = "entity_type") val entityType: String,
    @SerialName("payload_json") @ColumnInfo(name = "payload_json") val payloadJson: String,
    val status: String = "PENDING",
    @SerialName("created_at") @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

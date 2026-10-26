package tn.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.domain.model.*

@Serializable
data class BookSyncDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val author: String = "",
    @SerialName("total_pages") val totalPages: Int = 1,
    val format: String = "PDF",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("custom_cover_json") val customCoverJson: String? = null,
    @SerialName("percent_progress") val percentProgress: Int = 0,
    @SerialName("last_read_page") val lastReadPage: Int = 1,
    @SerialName("hours_read") val hoursRead: Float = 0f,
    @SerialName("pages_per_day") val pagesPerDay: Int = 10,
    @SerialName("speed_pages_per_hour") val speedPagesPerHour: Int = 30,
    @SerialName("insight_text") val insightText: String = "",
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("marked_done") val markedDone: Boolean = false,
    @SerialName("original_progress") val originalProgress: Int = 0,
    @SerialName("original_insight") val originalInsight: String = "",
    @SerialName("finished_at") val finishedAt: Long? = null,
    @SerialName("content_hash") val contentHash: String? = null,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(localFilePath: String = ""): BookEntity {
        return BookEntity(
            id = id,
            title = title,
            author = author,
            totalPages = totalPages,
            format = format,
            filePath = localFilePath,
            coverUrl = coverUrl,
            customCoverJson = customCoverJson,
            percentProgress = percentProgress,
            lastReadPage = lastReadPage,
            hoursRead = hoursRead,
            pagesPerDay = pagesPerDay,
            speedPagesPerHour = speedPagesPerHour,
            insightText = insightText,
            isArchived = isArchived,
            isDeleted = isDeleted,
            isFavorite = isFavorite,
            markedDone = markedDone,
            originalProgress = originalProgress,
            originalInsight = originalInsight,
            finishedAt = finishedAt,
            contentHash = contentHash,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: BookEntity, userId: String): BookSyncDto {
            return BookSyncDto(
                id = entity.id,
                userId = userId,
                title = entity.title,
                author = entity.author,
                totalPages = entity.totalPages,
                format = entity.format,
                coverUrl = entity.coverUrl,
                customCoverJson = entity.customCoverJson,
                percentProgress = entity.percentProgress,
                lastReadPage = entity.lastReadPage,
                hoursRead = entity.hoursRead,
                pagesPerDay = entity.pagesPerDay,
                speedPagesPerHour = entity.speedPagesPerHour,
                insightText = entity.insightText,
                isArchived = entity.isArchived,
                isDeleted = entity.isDeleted,
                isFavorite = entity.isFavorite,
                markedDone = entity.markedDone,
                originalProgress = entity.originalProgress,
                originalInsight = entity.originalInsight,
                finishedAt = entity.finishedAt,
                contentHash = entity.contentHash,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

@Serializable
data class BookProgressSyncDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("book_id") val bookId: String,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("phrase_dock_index") val phraseDockIndex: Int? = null,
    @SerialName("free_x") val freeX: Float? = null,
    @SerialName("free_y") val freeY: Float? = null,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): BookPageProgressEntity {
        return BookPageProgressEntity(
            bookId = bookId,
            pageNumber = pageNumber,
            isRead = isRead,
            phraseDockIndex = phraseDockIndex,
            freeX = freeX,
            freeY = freeY,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: BookPageProgressEntity, userId: String): BookProgressSyncDto {
            return BookProgressSyncDto(
                userId = userId,
                bookId = entity.bookId,
                pageNumber = entity.pageNumber,
                isRead = entity.isRead,
                phraseDockIndex = entity.phraseDockIndex,
                freeX = entity.freeX,
                freeY = entity.freeY,
                updatedAt = entity.updatedAt
            )
        }
    }
}

@Serializable
data class BookBookmarkSyncDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("book_id") val bookId: String,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("chapter_title") val chapterTitle: String = "",
    val excerpt: String = "",
    @SerialName("is_user") val isUser: Boolean = true,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): BookBookmarkEntity {
        return BookBookmarkEntity(
            bookId = bookId,
            pageNumber = pageNumber,
            chapterTitle = chapterTitle,
            excerpt = excerpt,
            isUser = isUser,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(entity: BookBookmarkEntity, userId: String): BookBookmarkSyncDto {
            return BookBookmarkSyncDto(
                userId = userId,
                bookId = entity.bookId,
                pageNumber = entity.pageNumber,
                chapterTitle = entity.chapterTitle,
                excerpt = entity.excerpt,
                isUser = entity.isUser,
                createdAt = entity.createdAt
            )
        }
    }
}

@Serializable
data class BookHighlightSyncDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("book_id") val bookId: String,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("start_offset") val startOffset: Int,
    @SerialName("end_offset") val endOffset: Int,
    @SerialName("selected_text") val selectedText: String,
    @SerialName("color_hex") val colorHex: String,
    @SerialName("is_quote") val isQuote: Boolean = false,
    @SerialName("note_text") val noteText: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): BookHighlightEntity {
        return BookHighlightEntity(
            id = id,
            bookId = bookId,
            pageNumber = pageNumber,
            startOffset = startOffset,
            endOffset = endOffset,
            selectedText = selectedText,
            colorHex = colorHex,
            isQuote = isQuote,
            noteText = noteText,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(entity: BookHighlightEntity, userId: String): BookHighlightSyncDto {
            return BookHighlightSyncDto(
                id = entity.id,
                userId = userId,
                bookId = entity.bookId,
                pageNumber = entity.pageNumber,
                startOffset = entity.startOffset,
                endOffset = entity.endOffset,
                selectedText = entity.selectedText,
                colorHex = entity.colorHex,
                isQuote = entity.isQuote,
                noteText = entity.noteText,
                createdAt = entity.createdAt
            )
        }
    }
}

@Serializable
data class BookSessionSyncDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("book_id") val bookId: String,
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long,
    @SerialName("pages_read") val pagesRead: Int,
    @SerialName("duration_seconds") val durationSeconds: Long,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): BookReadingSessionEntity {
        return BookReadingSessionEntity(
            id = id,
            bookId = bookId,
            startTime = startTime,
            endTime = endTime,
            pagesRead = pagesRead,
            durationSeconds = durationSeconds,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(entity: BookReadingSessionEntity, userId: String): BookSessionSyncDto {
            return BookSessionSyncDto(
                id = entity.id,
                userId = userId,
                bookId = entity.bookId,
                startTime = entity.startTime,
                endTime = entity.endTime,
                pagesRead = entity.pagesRead,
                durationSeconds = entity.durationSeconds,
                createdAt = entity.createdAt
            )
        }
    }
}

@Serializable
data class BookSettingsSyncDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("show_read_marker") val showReadMarker: Boolean = true,
    @SerialName("button_size_px") val buttonSizePx: Int = 38,
    @SerialName("green_opacity_percent") val greenOpacityPercent: Int = 50,
    @SerialName("read_marker_color_hex") val readMarkerColorHex: String = "#2ECC71",
    val theme: String = "original",
    @SerialName("page_margins_enabled") val pageMarginsEnabled: Boolean = true,
    @SerialName("keep_screen_on") val keepScreenOn: Boolean = true,
    @SerialName("auto_next_page_on_read") val autoNextPageOnRead: Boolean = true,
    @SerialName("volume_keys_turn_page") val volumeKeysTurnPage: Boolean = false,
    @SerialName("sync_across_devices") val syncAcrossDevices: Boolean = true,
    @SerialName("daily_goal_minutes") val dailyGoalMinutes: Int = 30,
    @SerialName("yearly_goal_books") val yearlyGoalBooks: Int = 24,
    @SerialName("pdf_dpi_option") val pdfDpiOption: String = "custom",
    @SerialName("pdf_custom_dpi") val pdfCustomDpi: Int = 400,
    @SerialName("show_timer_badge") val showTimerBadge: Boolean = false,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BookSettings {
        return BookSettings(
            showReadMarker = showReadMarker,
            buttonSizePx = buttonSizePx,
            greenOpacityPercent = greenOpacityPercent,
            readMarkerColorHex = readMarkerColorHex,
            theme = theme,
            pageMarginsEnabled = pageMarginsEnabled,
            keepScreenOn = keepScreenOn,
            autoNextPageOnRead = autoNextPageOnRead,
            volumeKeysTurnPage = volumeKeysTurnPage,
            syncAcrossDevices = syncAcrossDevices,
            dailyGoalMinutes = dailyGoalMinutes,
            yearlyGoalBooks = yearlyGoalBooks,
            pdfDpiOption = pdfDpiOption,
            pdfCustomDpi = pdfCustomDpi,
            showTimerBadge = showTimerBadge
        )
    }

    companion object {
        fun fromDomain(domain: BookSettings, userId: String): BookSettingsSyncDto {
            return BookSettingsSyncDto(
                userId = userId,
                showReadMarker = domain.showReadMarker,
                buttonSizePx = domain.buttonSizePx,
                greenOpacityPercent = domain.greenOpacityPercent,
                readMarkerColorHex = domain.readMarkerColorHex,
                theme = domain.theme,
                pageMarginsEnabled = domain.pageMarginsEnabled,
                keepScreenOn = domain.keepScreenOn,
                autoNextPageOnRead = domain.autoNextPageOnRead,
                volumeKeysTurnPage = domain.volumeKeysTurnPage,
                syncAcrossDevices = domain.syncAcrossDevices,
                dailyGoalMinutes = domain.dailyGoalMinutes,
                yearlyGoalBooks = domain.yearlyGoalBooks,
                pdfDpiOption = domain.pdfDpiOption,
                pdfCustomDpi = domain.pdfCustomDpi,
                showTimerBadge = domain.showTimerBadge
            )
        }
    }
}

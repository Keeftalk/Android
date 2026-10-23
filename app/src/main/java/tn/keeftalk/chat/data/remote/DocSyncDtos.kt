package tn.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.domain.model.*

private val syncJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class DocSyncDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    @SerialName("author_or_source") val authorOrSource: String = "",
    @SerialName("total_pages_or_sections") val totalPagesOrSections: Int = 1,
    val format: String = "PDF",
    @SerialName("file_size") val fileSize: Long = 0L,
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("custom_cover_json") val customCoverJson: String? = null,
    @SerialName("percent_progress") val percentProgress: Int = 0,
    @SerialName("last_read_page") val lastReadPage: Int = 1,
    @SerialName("last_read_position_json") val lastReadPositionJson: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("content_hash") val contentHash: String? = null,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(localFilePath: String = ""): DocEntity {
        return DocEntity(
            id = id,
            title = title,
            authorOrSource = authorOrSource,
            totalPagesOrSections = totalPagesOrSections,
            format = format,
            filePath = localFilePath,
            fileSize = fileSize,
            mimeType = mimeType,
            coverUrl = coverUrl,
            customCoverJson = customCoverJson,
            percentProgress = percentProgress,
            lastReadPage = lastReadPage,
            lastReadPositionJson = lastReadPositionJson,
            isFavorite = isFavorite,
            isArchived = isArchived,
            isDeleted = isDeleted,
            contentHash = contentHash,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: DocEntity, userId: String): DocSyncDto {
            return DocSyncDto(
                id = entity.id,
                userId = userId,
                title = entity.title,
                authorOrSource = entity.authorOrSource,
                totalPagesOrSections = entity.totalPagesOrSections,
                format = entity.format,
                fileSize = entity.fileSize,
                mimeType = entity.mimeType,
                coverUrl = entity.coverUrl,
                customCoverJson = entity.customCoverJson,
                percentProgress = entity.percentProgress,
                lastReadPage = entity.lastReadPage,
                lastReadPositionJson = entity.lastReadPositionJson,
                isFavorite = entity.isFavorite,
                isArchived = entity.isArchived,
                isDeleted = entity.isDeleted,
                contentHash = entity.contentHash,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

@Serializable
data class DocBookmarkSyncDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("doc_id") val docId: String,
    @SerialName("page_or_position") val pageOrPosition: Int,
    @SerialName("chapter_title") val chapterTitle: String = "",
    val excerpt: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): DocBookmarkEntity {
        return DocBookmarkEntity(
            docId = docId,
            pageOrPosition = pageOrPosition,
            chapterTitle = chapterTitle,
            excerpt = excerpt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(entity: DocBookmarkEntity, userId: String): DocBookmarkSyncDto {
            return DocBookmarkSyncDto(
                userId = userId,
                docId = entity.docId,
                pageOrPosition = entity.pageOrPosition,
                chapterTitle = entity.chapterTitle,
                excerpt = entity.excerpt,
                createdAt = entity.createdAt
            )
        }
    }
}

@Serializable
data class DocHighlightSyncDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("doc_id") val docId: String,
    @SerialName("page_or_position") val pageOrPosition: Int,
    @SerialName("start_offset") val startOffset: Int = 0,
    @SerialName("end_offset") val endOffset: Int = 0,
    @SerialName("selected_text") val selectedText: String,
    @SerialName("color_hex") val colorHex: String = "#2ECC71",
    @SerialName("note_text") val noteText: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): DocHighlightEntity {
        return DocHighlightEntity(
            id = id,
            docId = docId,
            pageOrPosition = pageOrPosition,
            startOffset = startOffset,
            endOffset = endOffset,
            selectedText = selectedText,
            colorHex = colorHex,
            noteText = noteText,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(entity: DocHighlightEntity, userId: String): DocHighlightSyncDto {
            return DocHighlightSyncDto(
                id = entity.id,
                userId = userId,
                docId = entity.docId,
                pageOrPosition = entity.pageOrPosition,
                startOffset = entity.startOffset,
                endOffset = entity.endOffset,
                selectedText = entity.selectedText,
                colorHex = entity.colorHex,
                noteText = entity.noteText,
                createdAt = entity.createdAt
            )
        }
    }
}

@Serializable
data class DocSettingsSyncDto(
    @SerialName("user_id") val userId: String? = null,
    val theme: String = "original",
    @SerialName("font_size_sp") val fontSizeSp: Int = 15,
    @SerialName("keep_screen_on") val keepScreenOn: Boolean = true,
    @SerialName("show_line_numbers") val showLineNumbers: Boolean = true,
    @SerialName("word_wrap") val wordWrap: Boolean = true,
    @SerialName("sync_across_devices") val syncAcrossDevices: Boolean = true,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DocSettings {
        return DocSettings(
            theme = theme,
            fontSizeSp = fontSizeSp,
            keepScreenOn = keepScreenOn,
            showLineNumbers = showLineNumbers,
            wordWrap = wordWrap,
            syncAcrossDevices = syncAcrossDevices
        )
    }

    companion object {
        fun fromDomain(domain: DocSettings, userId: String): DocSettingsSyncDto {
            return DocSettingsSyncDto(
                userId = userId,
                theme = domain.theme,
                fontSizeSp = domain.fontSizeSp,
                keepScreenOn = domain.keepScreenOn,
                showLineNumbers = domain.showLineNumbers,
                wordWrap = domain.wordWrap,
                syncAcrossDevices = domain.syncAcrossDevices
            )
        }
    }
}

fun DocEntity.toDomain(): Doc {
    val cover = customCoverJson?.let {
        runCatching { syncJson.decodeFromString<CustomDocCover>(it) }.getOrNull()
    }
    val docFormat = runCatching { DocFormat.valueOf(format) }.getOrDefault(DocFormat.PDF)
    return Doc(
        id = id,
        title = title,
        authorOrSource = authorOrSource,
        totalPagesOrSections = totalPagesOrSections,
        format = docFormat,
        filePath = filePath,
        fileSize = fileSize,
        mimeType = mimeType,
        coverUrl = coverUrl,
        customCover = cover,
        percentProgress = percentProgress,
        lastReadPage = lastReadPage,
        lastReadPositionJson = lastReadPositionJson,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isDeleted = isDeleted,
        contentHash = contentHash,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Doc.toEntity(json: Json = syncJson): DocEntity {
    val coverJson = customCover?.let { json.encodeToString(it) }
    return DocEntity(
        id = id,
        title = title,
        authorOrSource = authorOrSource,
        totalPagesOrSections = totalPagesOrSections,
        format = format.name,
        filePath = filePath,
        fileSize = fileSize,
        mimeType = mimeType,
        coverUrl = coverUrl,
        customCoverJson = coverJson,
        percentProgress = percentProgress,
        lastReadPage = lastReadPage,
        lastReadPositionJson = lastReadPositionJson,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isDeleted = isDeleted,
        contentHash = contentHash,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun DocBookmarkEntity.toDomain(): DocBookmark {
    return DocBookmark(
        docId = docId,
        pageOrPosition = pageOrPosition,
        chapterTitle = chapterTitle,
        excerpt = excerpt,
        createdAt = createdAt
    )
}

fun DocBookmark.toEntity(): DocBookmarkEntity {
    return DocBookmarkEntity(
        docId = docId,
        pageOrPosition = pageOrPosition,
        chapterTitle = chapterTitle,
        excerpt = excerpt,
        createdAt = createdAt
    )
}

fun DocHighlightEntity.toDomain(): DocHighlight {
    return DocHighlight(
        id = id,
        docId = docId,
        pageOrPosition = pageOrPosition,
        startOffset = startOffset,
        endOffset = endOffset,
        selectedText = selectedText,
        colorHex = colorHex,
        noteText = noteText,
        createdAt = createdAt
    )
}

fun DocHighlight.toEntity(): DocHighlightEntity {
    return DocHighlightEntity(
        id = id,
        docId = docId,
        pageOrPosition = pageOrPosition,
        startOffset = startOffset,
        endOffset = endOffset,
        selectedText = selectedText,
        colorHex = colorHex,
        noteText = noteText,
        createdAt = createdAt
    )
}

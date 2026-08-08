package com.keeftalk.chat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "[]",        // JSON-serialized List<NoteBlock>
    val color: String = "#6C63FF",     // hex color for note background
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val container: String = "All",     // "All", "Shared", or custom name
    val ownerId: String = "",          // user ID of creator
    val canOthersAdd: Boolean = false, // whether shared users can add more people
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sharedUsers: List<NoteShare> = emptyList(),
    val attachments: List<File> = emptyList()
)

@Serializable
sealed class NoteBlock {
    abstract val id: String

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        val richText: List<RichTextPart> = emptyList(),
        val style: String = "NORMAL", // NORMAL, H1, H2, QUOTE
        val alignment: String = "LEFT" // LEFT, CENTER, RIGHT
    ) : NoteBlock()

    @Serializable
    @SerialName("image")
    data class Image(
        override val id: String = UUID.randomUUID().toString(),
        val mediaId: String, // This now refers to File.id
        val caption: String? = null
    ) : NoteBlock()

    @Serializable
    @SerialName("video")
    data class Video(
        override val id: String = UUID.randomUUID().toString(),
        val mediaId: String, // This now refers to File.id
        val caption: String? = null
    ) : NoteBlock()

    @Serializable
    @SerialName("table")
    data class Table(
        override val id: String = UUID.randomUUID().toString(),
        val rows: List<TableRow>
    ) : NoteBlock()

    @Serializable
    @SerialName("checklist")
    data class Checklist(
        override val id: String = UUID.randomUUID().toString(),
        val items: List<ChecklistItem>
    ) : NoteBlock()

    @Serializable
    @SerialName("code")
    data class Code(
        override val id: String = UUID.randomUUID().toString(),
        val code: String = "",
        val language: String? = null
    ) : NoteBlock()

    @Serializable
    @SerialName("math")
    data class Math(
        override val id: String = UUID.randomUUID().toString(),
        val latex: String = ""
    ) : NoteBlock()
}

@Serializable
data class RichTextPart(
    val text: String,
    val spans: List<SpanMetadata> = emptyList()
)

@Serializable
data class SpanMetadata(
    val type: String, // BOLD, ITALIC, UNDERLINE, STRIKE, COLOR, SIZE, LINK
    val value: String? = null // For COLOR (hex), SIZE (int), LINK (url)
)

@Serializable
data class TableRow(
    val cells: List<String>
)

@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)

@Serializable
data class NoteShare(
    val user: User,
    val access: String // "read" or "write"
)

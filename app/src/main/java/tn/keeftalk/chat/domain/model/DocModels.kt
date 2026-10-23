package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DocFormat {
    PDF,
    MARKDOWN,
    HTML,
    TXT,
    DOCX,
    EPUB,
    CODE,
    OTHER;

    companion object {
        fun fromExtension(ext: String, mimeType: String = ""): DocFormat {
            val lowerExt = ext.lowercase()
            val lowerMime = mimeType.lowercase()
            return when {
                lowerExt == "pdf" || lowerMime.contains("pdf") -> PDF
                lowerExt in listOf("md", "markdown") || lowerMime.contains("markdown") -> MARKDOWN
                lowerExt in listOf("html", "htm", "xhtml") || lowerMime.contains("html") -> HTML
                lowerExt in listOf("txt", "log", "conf", "ini", "env", "properties", "csv") || lowerMime.startsWith("text/") -> TXT
                lowerExt in listOf("docx", "doc") || lowerMime.contains("word") -> DOCX
                lowerExt in listOf("epub", "mobi", "azw") || lowerMime.contains("epub") -> EPUB
                lowerExt in listOf("kt", "java", "py", "js", "ts", "cpp", "c", "h", "cs", "rs", "go", "sql", "sh", "css", "json", "xml", "yaml", "yml", "dart") -> CODE
                else -> OTHER
            }
        }
    }
}

@Serializable
data class CustomDocCover(
    val colors: List<String>? = null,
    val pattern: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class Doc(
    val id: String,
    val title: String,
    val authorOrSource: String = "",
    val totalPagesOrSections: Int = 1,
    val format: DocFormat = DocFormat.PDF,
    val filePath: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = "",
    val coverUrl: String? = null,
    val customCover: CustomDocCover? = null,
    val percentProgress: Int = 0,
    val lastReadPage: Int = 1,
    val lastReadPositionJson: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val contentHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class DocBookmark(
    val docId: String,
    val pageOrPosition: Int,
    val chapterTitle: String = "",
    val excerpt: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class DocHighlight(
    val id: String,
    val docId: String,
    val pageOrPosition: Int,
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val selectedText: String,
    val colorHex: String = "#2ECC71",
    val noteText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class DocSettings(
    val theme: String = "original", // original, dark, sepia, night
    val fontSizeSp: Int = 15, // 12 to 28
    val keepScreenOn: Boolean = true,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val syncAcrossDevices: Boolean = true
)

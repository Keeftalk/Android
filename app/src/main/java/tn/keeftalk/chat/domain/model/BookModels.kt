package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomCover(
    val colors: List<String>? = null, // e.g. ["#6C63FF", "#2a1a7a"]
    val pattern: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val totalPages: Int,
    val format: String = "PDF", // "PDF" or "EPUB"
    val filePath: String = "",
    val coverUrl: String? = null,
    val customCover: CustomCover? = null,
    val percentProgress: Int = 0,
    val lastReadPage: Int = 1,
    val hoursRead: Float = 0f,
    val pagesPerDay: Int = 10,
    val speedPagesPerHour: Int = 30,
    val insightText: String = "",
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isFavorite: Boolean = false,
    val markedDone: Boolean = false,
    val originalProgress: Int = 0,
    val originalInsight: String = "",
    val finishedAt: Long? = null,
    val contentHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class BookPageProgress(
    val bookId: String,
    val pageNumber: Int,
    val isRead: Boolean = false,
    val phraseDockIndex: Int? = null,
    val freeX: Float? = null,
    val freeY: Float? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class BookHighlight(
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val colorHex: String,
    val isQuote: Boolean = false,
    val noteText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BookBookmark(
    val bookId: String,
    val pageNumber: Int,
    val chapterTitle: String = "",
    val excerpt: String = "",
    val isUser: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BookReadingSession(
    val id: String,
    val bookId: String,
    val startTime: Long,
    val endTime: Long,
    val pagesRead: Int,
    val durationSeconds: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class BookSettings(
    val showReadMarker: Boolean = true,
    val buttonSizePx: Int = 38, // 28 to 64
    val greenOpacityPercent: Int = 50, // 10 to 90
    val readMarkerColorHex: String = "#2ECC71",
    val theme: String = "original", // original, dark, twilight, paper, sepia, sepiap
    val pageMarginsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoNextPageOnRead: Boolean = true,
    val volumeKeysTurnPage: Boolean = false,
    val syncAcrossDevices: Boolean = true,
    val encryptFiles: Boolean = true,
    val autoAddPageReference: Boolean = true,
    val trackReadingTime: Boolean = true,
    val dailyGoalMinutes: Int = 30,
    val yearlyGoalBooks: Int = 24,
    val pdfDpiOption: String = "lowest", // "lowest", "middle", "custom"
    val pdfCustomDpi: Int = 200, // 100 to 750
    val hasSeenLibraryTour: Boolean = false,
    val hasSeenQuotesCard: Boolean = false,
    val hasSeenGoalsCard: Boolean = false,
    val hasSeenAchievementsCard: Boolean = false
) {
    fun effectiveDpi(): Int {
        return when (pdfDpiOption) {
            "lowest" -> 108
            "middle" -> 162
            else -> pdfCustomDpi.coerceIn(100, 750)
        }
    }

    fun dpiScale(): Float {
        return effectiveDpi() / 72f
    }
}

@Serializable
data class Achievement(
    val icon: String,
    val title: String,
    val target: Int,
    val current: Int,
    val rewardName: String
) {
    val isUnlocked: Boolean get() = current >= target
}

data class FinishEstimate(
    val pagesRemaining: Int,
    val daysRemaining: Int,
    val hoursRemaining: Float,
    val estimatedFinishDateStr: String
)

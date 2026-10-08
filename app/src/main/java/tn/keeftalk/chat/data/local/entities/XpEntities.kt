package tn.keeftalk.chat.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "confirmed_pages",
    primaryKeys = ["content_hash", "page_key"]
)
data class ConfirmedPageEntity(
    @SerialName("content_hash") @ColumnInfo(name = "content_hash") val contentHash: String,
    @SerialName("page_key") @ColumnInfo(name = "page_key") val pageKey: String,
    val credit: Double,
    @SerialName("dwell_ms") @ColumnInfo(name = "dwell_ms") val dwellMs: Long,
    @SerialName("confirmed_at") @ColumnInfo(name = "confirmed_at") val confirmedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "confirmed_ranges",
    primaryKeys = ["content_hash", "start_char", "end_char"]
)
data class ConfirmedRangeEntity(
    @SerialName("content_hash") @ColumnInfo(name = "content_hash") val contentHash: String,
    @SerialName("start_char") @ColumnInfo(name = "start_char") val startChar: Long,
    @SerialName("end_char") @ColumnInfo(name = "end_char") val endChar: Long,
    @SerialName("confirmed_at") @ColumnInfo(name = "confirmed_at") val confirmedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "reading_days")
data class ReadingDayEntity(
    @PrimaryKey val date: String,
    @SerialName("credited_pages") @ColumnInfo(name = "credited_pages") val creditedPages: Double,
    @SerialName("active_seconds") @ColumnInfo(name = "active_seconds") val activeSeconds: Int,
    val flagged: Int = 0
)

@Serializable
@Entity(tableName = "unlocked_achievements")
data class UnlockedAchievementEntity(
    @PrimaryKey val id: String,
    @SerialName("unlocked_at") @ColumnInfo(name = "unlocked_at") val unlockedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "xp_events")
data class XpEventEntity(
    @PrimaryKey @SerialName("event_id") @ColumnInfo(name = "event_id") val eventId: String,
    @SerialName("content_hash") @ColumnInfo(name = "content_hash") val contentHash: String,
    @SerialName("page_key_or_range") @ColumnInfo(name = "page_key_or_range") val pageKeyOrRange: String,
    @SerialName("dwell_ms") @ColumnInfo(name = "dwell_ms") val dwellMs: Long,
    @SerialName("anchored_at_ms") @ColumnInfo(name = "anchored_at_ms") val anchoredAtMs: Long,
    val anchored: Int = 0,
    val credit: Double,
    val flags: Int = 0,
    val synced: Int = 0
)

@Serializable
@Entity(tableName = "xp_meta")
data class XpMetaEntity(
    @PrimaryKey val id: Int = 1,
    @SerialName("legacy_reading_xp") @ColumnInfo(name = "legacy_reading_xp") val legacyReadingXp: Long,
    @SerialName("account_tz") @ColumnInfo(name = "account_tz") val accountTz: String,
    @SerialName("last_global_rank") @ColumnInfo(name = "last_global_rank", defaultValue = "0") val lastGlobalRank: Long = 0L,
    @SerialName("last_total_readers") @ColumnInfo(name = "last_total_readers", defaultValue = "0") val lastTotalReaders: Long = 0L,
    @SerialName("last_percentile") @ColumnInfo(name = "last_percentile", defaultValue = "0") val lastPercentile: Int = 0
)

@Serializable
@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    @SerialName("content_hash") @ColumnInfo(name = "content_hash") val contentHash: String,
    @SerialName("book_id") @ColumnInfo(name = "book_id") val bookId: String,
    @SerialName("book_title") @ColumnInfo(name = "book_title") val bookTitle: String,
    @SerialName("book_author") @ColumnInfo(name = "book_author") val bookAuthor: String,
    @SerialName("cover_url") @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    @SerialName("custom_cover_json") @ColumnInfo(name = "custom_cover_json") val customCoverJson: String? = null,
    @SerialName("start_at") @ColumnInfo(name = "start_at") val startAt: Long,
    @SerialName("end_at") @ColumnInfo(name = "end_at") val endAt: Long,
    @SerialName("active_sec") @ColumnInfo(name = "active_sec") val activeSec: Int,
    @SerialName("pages_confirmed") @ColumnInfo(name = "pages_confirmed") val pagesConfirmed: Int,
    @SerialName("credited_pages") @ColumnInfo(name = "credited_pages") val creditedPages: Double,
    @SerialName("xp_base") @ColumnInfo(name = "xp_base") val xpBase: Long,
    @SerialName("xp_bonus") @ColumnInfo(name = "xp_bonus") val xpBonus: Long,
    @SerialName("xp_achievements") @ColumnInfo(name = "xp_achievements") val xpAchievements: Long,
    @SerialName("total_xp_gained") @ColumnInfo(name = "total_xp_gained") val totalXpGained: Long,
    val flagged: Int = 0,
    @SerialName("is_record") @ColumnInfo(name = "is_record") val isRecord: Int = 0,
    val shown: Int = 0
)

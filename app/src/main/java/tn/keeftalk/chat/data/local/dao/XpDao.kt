package tn.keeftalk.chat.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.data.local.entities.*

@Dao
interface XpDao {
    @Query("SELECT * FROM confirmed_pages WHERE content_hash = :contentHash AND page_key = :pageKey LIMIT 1")
    suspend fun getConfirmedPage(contentHash: String, pageKey: String): ConfirmedPageEntity?

    @Query("SELECT * FROM confirmed_pages WHERE content_hash = :contentHash")
    suspend fun getConfirmedPagesForBook(contentHash: String): List<ConfirmedPageEntity>

    @Query("SELECT * FROM confirmed_pages")
    suspend fun getAllConfirmedPages(): List<ConfirmedPageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfirmedPage(page: ConfirmedPageEntity)

    @Query("SELECT * FROM confirmed_ranges WHERE content_hash = :contentHash")
    suspend fun getConfirmedRangesForBook(contentHash: String): List<ConfirmedRangeEntity>

    @Query("SELECT * FROM confirmed_ranges")
    suspend fun getAllConfirmedRanges(): List<ConfirmedRangeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfirmedRange(range: ConfirmedRangeEntity)

    @Query("SELECT * FROM reading_days WHERE date = :date LIMIT 1")
    suspend fun getReadingDay(date: String): ReadingDayEntity?

    @Query("SELECT * FROM reading_days ORDER BY date ASC")
    suspend fun getAllReadingDays(): List<ReadingDayEntity>

    @Query("SELECT * FROM reading_days ORDER BY date ASC")
    fun getAllReadingDaysFlow(): Flow<List<ReadingDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReadingDay(day: ReadingDayEntity)

    @Query("SELECT * FROM unlocked_achievements")
    suspend fun getUnlockedAchievements(): List<UnlockedAchievementEntity>

    @Query("SELECT * FROM unlocked_achievements")
    fun getUnlockedAchievementsFlow(): Flow<List<UnlockedAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedAchievement(achievement: UnlockedAchievementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedAchievements(achievements: List<UnlockedAchievementEntity>)

    @Query("SELECT * FROM xp_events WHERE synced = 0 ORDER BY anchored_at_ms ASC")
    suspend fun getUnsyncedXpEvents(): List<XpEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpEvent(event: XpEventEntity)

    @Query("UPDATE xp_events SET synced = 1 WHERE event_id IN (:eventIds)")
    suspend fun markXpEventsSynced(eventIds: List<String>)

    @Query("SELECT * FROM xp_meta WHERE id = 1 LIMIT 1")
    suspend fun getXpMeta(): XpMetaEntity?

    @Query("SELECT * FROM xp_meta WHERE id = 1 LIMIT 1")
    fun getXpMetaFlow(): Flow<XpMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateXpMeta(meta: XpMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions ORDER BY end_at DESC")
    suspend fun getAllReadingSessions(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions ORDER BY end_at DESC")
    fun getAllReadingSessionsFlow(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE content_hash = :contentHash ORDER BY end_at DESC")
    suspend fun getSessionsForBook(contentHash: String): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE shown = 0 AND total_xp_gained > 0 ORDER BY end_at DESC LIMIT 1")
    suspend fun getPendingUnshownSession(): ReadingSessionEntity?

    @Query("UPDATE reading_sessions SET shown = 1 WHERE id = :id")
    suspend fun markSessionShown(id: String)
}

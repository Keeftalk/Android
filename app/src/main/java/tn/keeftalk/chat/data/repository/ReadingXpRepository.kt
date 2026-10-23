package tn.keeftalk.chat.data.repository

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import tn.keeftalk.chat.data.local.dao.XpDao
import tn.keeftalk.chat.data.local.entities.*
import tn.keeftalk.chat.data.remote.GlobalRankDto
import tn.keeftalk.chat.data.remote.SupabaseXpSyncApi
import tn.keeftalk.chat.data.remote.XpSyncApi
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.xp.ReadingXp
import tn.keeftalk.chat.domain.xp.TapGuard
import tn.keeftalk.chat.domain.xp.TrustedClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.LinkedList
import java.util.UUID

data class ConfirmResult(
    val credit: Double,
    val isFirstTime: Boolean,
    val isFlagged: Boolean,
    val message: String? = null
)

class ReadingXpRepository(
    private val xpDao: XpDao
) {
    private val dwellRingBuffer = LinkedList<Double>()
    private val xRingBuffer = LinkedList<Float>()
    private val yRingBuffer = LinkedList<Float>()

    suspend fun confirmPdfPage(
        context: Context,
        contentHash: String,
        pageNumber: Int,
        dwellSec: Double,
        tapX: Float,
        tapY: Float,
        isObscured: Boolean
    ): ConfirmResult {
        if (isObscured) {
            return ConfirmResult(0.0, isFirstTime = false, isFlagged = false)
        }

        val pageKey = "p:$pageNumber"
        val existingPage = xpDao.getConfirmedPage(contentHash, pageKey)
        if (existingPage != null) {
            // Unit already consumed
            return ConfirmResult(0.0, isFirstTime = false, isFlagged = false)
        }

        // Push to ring buffer
        synchronized(dwellRingBuffer) {
            dwellRingBuffer.add(dwellSec)
            xRingBuffer.add(tapX)
            yRingBuffer.add(tapY)
            if (dwellRingBuffer.size > 20) {
                dwellRingBuffer.removeFirst()
                xRingBuffer.removeFirst()
                yRingBuffer.removeFirst()
            }
        }

        val isAutomated = synchronized(dwellRingBuffer) {
            TapGuard.looksAutomated(dwellRingBuffer, xRingBuffer, yRingBuffer)
        }

        val credit = if (isAutomated) 0.0 else ReadingXp.pageCredit(dwellSec, 1.0)
        val isFlagged = isAutomated

        val (nowMs, anchored) = TrustedClock.getAnchoredNowMs(context)
        val meta = xpDao.getXpMeta()
        val accountTz = meta?.accountTz ?: ZoneId.systemDefault().id
        val localDate = Instant.ofEpochMilli(nowMs).atZone(ZoneId.of(accountTz)).toLocalDate().toString()

        val dwellMs = (dwellSec * 1000).toLong()

        // Single atomic transaction logic
        val confirmedPage = ConfirmedPageEntity(
            contentHash = contentHash,
            pageKey = pageKey,
            credit = credit,
            dwellMs = dwellMs,
            confirmedAt = nowMs
        )

        val existingDay = xpDao.getReadingDay(localDate)
        val currentCredited = existingDay?.creditedPages ?: 0.0
        val currentActiveSec = existingDay?.activeSeconds ?: 0
        val currentFlagged = existingDay?.flagged ?: 0

        val newCredited = currentCredited + credit
        val dwellSecInt = minOf(dwellSec.toInt(), ReadingXp.MAX_ACTIVE_SEC_PER_PAGE)
        val newActiveSec = minOf(currentActiveSec + dwellSecInt, ReadingXp.MAX_ACTIVE_SEC_PER_DAY)
        val newFlagged = if (isFlagged) 1 else currentFlagged

        val updatedDay = ReadingDayEntity(
            date = localDate,
            creditedPages = newCredited,
            activeSeconds = newActiveSec,
            flagged = newFlagged
        )

        val xpEvent = XpEventEntity(
            eventId = UUID.randomUUID().toString(),
            contentHash = contentHash,
            pageKeyOrRange = pageKey,
            dwellMs = dwellMs,
            anchoredAtMs = nowMs,
            anchored = if (anchored) 1 else 0,
            credit = credit,
            flags = if (isFlagged) 1 else 0,
            synced = 0
        )

        xpDao.insertConfirmedPage(confirmedPage)
        xpDao.insertOrUpdateReadingDay(updatedDay)
        xpDao.insertXpEvent(xpEvent)

        val msg = if (isFlagged) "Some pages in this session weren't counted." else null
        return ConfirmResult(credit, isFirstTime = true, isFlagged = isFlagged, message = msg)
    }

    suspend fun confirmEpubRange(
        context: Context,
        contentHash: String,
        startChar: Long,
        endChar: Long,
        dwellSec: Double,
        tapX: Float,
        tapY: Float,
        isObscured: Boolean
    ): ConfirmResult {
        if (isObscured || endChar <= startChar) {
            return ConfirmResult(0.0, isFirstTime = false, isFlagged = false)
        }

        val existingRanges = xpDao.getConfirmedRangesForBook(contentHash)
        var newUncoveredChars = (endChar - startChar)
        for (r in existingRanges) {
            val overlapStart = maxOf(startChar, r.startChar)
            val overlapEnd = minOf(endChar, r.endChar)
            if (overlapEnd > overlapStart) {
                newUncoveredChars -= (overlapEnd - overlapStart)
            }
        }

        if (newUncoveredChars <= 0) {
            return ConfirmResult(0.0, isFirstTime = false, isFlagged = false)
        }

        val contentFactor = minOf(1.0, newUncoveredChars.toDouble() / ReadingXp.EPUB_CHARS_PER_PAGE)

        synchronized(dwellRingBuffer) {
            dwellRingBuffer.add(dwellSec)
            xRingBuffer.add(tapX)
            yRingBuffer.add(tapY)
            if (dwellRingBuffer.size > 20) {
                dwellRingBuffer.removeFirst()
                xRingBuffer.removeFirst()
                yRingBuffer.removeFirst()
            }
        }

        val isAutomated = synchronized(dwellRingBuffer) {
            TapGuard.looksAutomated(dwellRingBuffer, xRingBuffer, yRingBuffer)
        }

        val credit = if (isAutomated) 0.0 else ReadingXp.pageCredit(dwellSec, contentFactor)
        val isFlagged = isAutomated

        val (nowMs, anchored) = TrustedClock.getAnchoredNowMs(context)
        val meta = xpDao.getXpMeta()
        val accountTz = meta?.accountTz ?: ZoneId.systemDefault().id
        val localDate = Instant.ofEpochMilli(nowMs).atZone(ZoneId.of(accountTz)).toLocalDate().toString()

        val dwellMs = (dwellSec * 1000).toLong()

        val confirmedRange = ConfirmedRangeEntity(
            contentHash = contentHash,
            startChar = startChar,
            endChar = endChar,
            confirmedAt = nowMs
        )

        val existingDay = xpDao.getReadingDay(localDate)
        val currentCredited = existingDay?.creditedPages ?: 0.0
        val currentActiveSec = existingDay?.activeSeconds ?: 0
        val currentFlagged = existingDay?.flagged ?: 0

        val newCredited = currentCredited + credit
        val dwellSecInt = minOf(dwellSec.toInt(), ReadingXp.MAX_ACTIVE_SEC_PER_PAGE)
        val newActiveSec = minOf(currentActiveSec + dwellSecInt, ReadingXp.MAX_ACTIVE_SEC_PER_DAY)
        val newFlagged = if (isFlagged) 1 else currentFlagged

        val updatedDay = ReadingDayEntity(
            date = localDate,
            creditedPages = newCredited,
            activeSeconds = newActiveSec,
            flagged = newFlagged
        )

        val rangeKey = "r:${startChar}_$endChar"
        val xpEvent = XpEventEntity(
            eventId = UUID.randomUUID().toString(),
            contentHash = contentHash,
            pageKeyOrRange = rangeKey,
            dwellMs = dwellMs,
            anchoredAtMs = nowMs,
            anchored = if (anchored) 1 else 0,
            credit = credit,
            flags = if (isFlagged) 1 else 0,
            synced = 0
        )

        xpDao.insertConfirmedRange(confirmedRange)
        xpDao.insertOrUpdateReadingDay(updatedDay)
        xpDao.insertXpEvent(xpEvent)

        val msg = if (isFlagged) "Some pages in this session weren't counted." else null
        return ConfirmResult(credit, isFirstTime = true, isFlagged = isFlagged, message = msg)
    }

    fun getReadingDaysFlow(): Flow<List<ReadingDayEntity>> = xpDao.getAllReadingDaysFlow()

    fun getUnlockedAchievementsFlow(): Flow<List<UnlockedAchievementEntity>> = xpDao.getUnlockedAchievementsFlow()

    fun getXpMetaFlow(): Flow<XpMetaEntity?> = xpDao.getXpMetaFlow()

    suspend fun getXpMeta(): XpMetaEntity? = xpDao.getXpMeta()

    suspend fun getAllReadingDays(): List<ReadingDayEntity> = xpDao.getAllReadingDays()

    suspend fun getUnlockedAchievements(): List<UnlockedAchievementEntity> = xpDao.getUnlockedAchievements()

    suspend fun insertUnlockedAchievement(id: String) {
        xpDao.insertUnlockedAchievement(UnlockedAchievementEntity(id = id))
    }

    suspend fun getConfirmedPagesForBook(contentHash: String): List<ConfirmedPageEntity> {
        return xpDao.getConfirmedPagesForBook(contentHash)
    }

    suspend fun insertReadingSession(session: ReadingSessionEntity) {
        xpDao.insertReadingSession(session)
    }

    suspend fun getAllReadingSessions(): List<ReadingSessionEntity> {
        return xpDao.getAllReadingSessions()
    }

    fun getAllReadingSessionsFlow(): Flow<List<ReadingSessionEntity>> {
        return xpDao.getAllReadingSessionsFlow()
    }

    suspend fun getSessionsForBook(contentHash: String): List<ReadingSessionEntity> {
        return xpDao.getSessionsForBook(contentHash)
    }

    suspend fun getPendingUnshownSession(): ReadingSessionEntity? {
        return xpDao.getPendingUnshownSession()
    }

    suspend fun markSessionShown(id: String) {
        xpDao.markSessionShown(id)
    }

    suspend fun syncAndFetchGlobalRank(context: Context, totalXp: Long): GlobalRankDto? {
        return try {
            val supabase = AppModule.provideSupabaseClientAsync(context)
            val user = supabase.auth.currentUserOrNull() ?: return null
            val userId = user.id
            val username = user.userMetadata?.get("username")?.let {
                if (it is JsonPrimitive) it.content else null
            } ?: user.email?.substringBefore("@") ?: "Reader"

            // 1. Fetch server profile total_xp to prevent overwriting higher server XP with 0 on fresh install
            val serverProfile = try {
                supabase.postgrest["profiles"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<tn.keeftalk.chat.domain.model.Profile>()
            } catch (_: Exception) { null }

            val serverXp = serverProfile?.totalXp ?: 0L
            val effectiveXp: Long

            if (serverXp > totalXp) {
                // Restore server XP into local legacyReadingXp
                val meta = xpDao.getXpMeta()
                val currentLegacy = meta?.legacyReadingXp ?: 0L
                val diff = serverXp - totalXp
                val newLegacy = currentLegacy + diff

                val updatedMeta = meta?.copy(legacyReadingXp = newLegacy)
                    ?: XpMetaEntity(id = 1, legacyReadingXp = newLegacy, accountTz = java.time.ZoneId.systemDefault().id)
                xpDao.insertOrUpdateXpMeta(updatedMeta)
                effectiveXp = serverXp
            } else {
                effectiveXp = totalXp
                // Push local total_xp to Supabase profiles table (offline-first sync)
                if (totalXp > 0) {
                    try {
                        supabase.postgrest["profiles"].update(mapOf("total_xp" to totalXp)) {
                            filter { eq("id", userId) }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ReadingXpRepository", "Failed to update profile total_xp: ${e.message}")
                    }
                }
            }

            val api: XpSyncApi = SupabaseXpSyncApi()
            val res = api.syncUserXpAndGetRank(supabase, effectiveXp, username)
            if (res.isSuccess) {
                val rankDto = res.getOrThrow()
                val currentMeta = xpDao.getXpMeta()
                if (currentMeta != null) {
                    xpDao.insertOrUpdateXpMeta(
                        currentMeta.copy(
                            lastGlobalRank = rankDto.globalRank,
                            lastTotalReaders = rankDto.totalReaders,
                            lastPercentile = rankDto.percentile
                        )
                    )
                }
                rankDto
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ReadingXpRepository", "Failed to sync global rank: ${e.message}")
            null
        }
    }
}

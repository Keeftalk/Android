package tn.keeftalk.chat.domain.books

import tn.keeftalk.chat.data.local.entities.ReadingSessionEntity
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

data class SessionRecords(
    val isBestSessionXp: Boolean,
    val isLongestActiveTime: Boolean,
    val isMostPagesConfirmed: Boolean,
    val isBestDayXp: Boolean,
    val gapToBestXp: Long,
    val bestXp: Long,
    val last7AvgXp: Long
)

sealed class BookEtaResult {
    object NotEnoughData : BookEtaResult()
    data class Calculated(
        val hoursRemaining: Float,
        val projectedDate: LocalDate
    ) : BookEtaResult()
}

data class PaceMetrics(
    val pagesPerHour: Double,
    val secPerPage: Double,
    val xpPerHour: Double,
    val pagesPerHourDelta: Double?, // positive for up, negative for down
    val xpPerHourDelta: Double?
)

object SessionSummaryCalculator {

    fun evaluateRecords(
        currentSession: ReadingSessionEntity,
        pastSessions: List<ReadingSessionEntity>,
        todayTotalXp: Long,
        pastDaysXpMap: Map<String, Long>
    ): SessionRecords {
        val nonFlaggedPast = pastSessions.filter { it.flagged == 0 && it.id != currentSession.id }

        val bestPastXp = nonFlaggedPast.maxOfOrNull { it.totalXpGained } ?: 0L
        val isBestXp = currentSession.flagged == 0 && (currentSession.totalXpGained > bestPastXp) && currentSession.totalXpGained > 0

        val maxPastActiveSec = nonFlaggedPast.maxOfOrNull { it.activeSec } ?: 0
        val isLongestActive = currentSession.flagged == 0 && (currentSession.activeSec > maxPastActiveSec) && currentSession.activeSec > 0

        val maxPastPages = nonFlaggedPast.maxOfOrNull { it.pagesConfirmed } ?: 0
        val isMostPages = currentSession.flagged == 0 && (currentSession.pagesConfirmed > maxPastPages) && currentSession.pagesConfirmed > 0

        val todayDateStr = LocalDate.now().toString()
        val pastMaxDayXp = pastDaysXpMap.filterKeys { it != todayDateStr }.values.maxOrNull() ?: 0L
        val isBestDay = todayTotalXp > pastMaxDayXp && todayTotalXp > 0

        val gap = maxOf(0L, bestPastXp - currentSession.totalXpGained)

        val last7 = nonFlaggedPast.take(7)
        val last7Avg = if (last7.isNotEmpty()) last7.map { it.totalXpGained }.average().toLong() else 0L

        return SessionRecords(
            isBestSessionXp = isBestXp,
            isLongestActiveTime = isLongestActive,
            isMostPagesConfirmed = isMostPages,
            isBestDayXp = isBestDay,
            gapToBestXp = gap,
            bestXp = bestPastXp,
            last7AvgXp = last7Avg
        )
    }

    fun getEncouragingLine(
        gapXp: Long,
        bestXp: Long,
        currentXp: Long,
        last7AvgXp: Long,
        seed: Long = System.currentTimeMillis()
    ): String {
        val lines = mutableListOf<String>()

        if (gapXp > 0 && bestXp > 0) {
            lines.add("$gapXp XP away from your best ($bestXp XP). You've got this!")
            lines.add("Great session! Just $gapXp XP shy of your personal high of $bestXp XP.")
            lines.add("Steady progress! Only $gapXp XP until you beat $bestXp XP.")
        } else {
            lines.add("Outstanding focus in this session! Keep building momentum.")
            lines.add("Every confirmed page counts toward your next level.")
        }

        if (last7AvgXp > 0) {
            if (currentXp > last7AvgXp) {
                val diff = currentXp - last7AvgXp
                lines.add("This session was +$diff XP above your 7-session average ($last7AvgXp XP)!")
            } else if (currentXp == last7AvgXp) {
                lines.add("Right on track with your 7-session average ($last7AvgXp XP)!")
            } else {
                lines.add("Consistent effort! Your 7-session average is $last7AvgXp XP.")
            }
        } else {
            lines.add("Building consistency step by step!")
        }

        val rng = Random(seed)
        return lines[rng.nextInt(lines.size)]
    }

    fun calculateBookEta(
        remainingPages: Int,
        pastBookSessions: List<ReadingSessionEntity>,
        dailyRatePagesPerDay: Float,
        currentDate: LocalDate = LocalDate.now()
    ): BookEtaResult {
        if (remainingPages <= 0) {
            return BookEtaResult.Calculated(0f, currentDate)
        }

        val validSessions = pastBookSessions.filter { it.flagged == 0 && it.pagesConfirmed > 0 && it.activeSec > 0 }
        if (validSessions.size < 3) {
            return BookEtaResult.NotEnoughData
        }

        val totalPagesInSessions = validSessions.sumOf { it.pagesConfirmed }
        val totalSecInSessions = validSessions.sumOf { it.activeSec }
        if (totalSecInSessions <= 0 || totalPagesInSessions <= 0) {
            return BookEtaResult.NotEnoughData
        }

        val pagesPerSec = totalPagesInSessions.toDouble() / totalSecInSessions.toDouble()
        val secRemaining = remainingPages.toDouble() / pagesPerSec
        val hoursRemaining = (secRemaining / 3600.0).toFloat()

        val pagesPerDay = if (dailyRatePagesPerDay > 0f) dailyRatePagesPerDay else (pagesPerSec * 1800.0).toFloat() // default 30 min/day
        val daysRemaining = (remainingPages.toDouble() / maxOf(1.0, pagesPerDay.toDouble())).roundToInt()

        val projected = currentDate.plusDays(maxOf(1L, daysRemaining.toLong()))

        return BookEtaResult.Calculated(hoursRemaining, projected)
    }

    fun calculatePaceMetrics(
        currentSession: ReadingSessionEntity,
        previousSession: ReadingSessionEntity?
    ): PaceMetrics {
        val activeSec = maxOf(1, currentSession.activeSec)
        val pages = currentSession.pagesConfirmed
        val xp = currentSession.totalXpGained

        val pph = (pages.toDouble() * 3600.0) / activeSec.toDouble()
        val secPerPage = if (pages > 0) activeSec.toDouble() / pages.toDouble() else 0.0
        val xph = (xp.toDouble() * 3600.0) / activeSec.toDouble()

        var pphDelta: Double? = null
        var xphDelta: Double? = null

        if (previousSession != null && previousSession.activeSec > 0) {
            val prevPph = (previousSession.pagesConfirmed.toDouble() * 3600.0) / previousSession.activeSec.toDouble()
            val prevXph = (previousSession.totalXpGained.toDouble() * 3600.0) / previousSession.activeSec.toDouble()
            pphDelta = pph - prevPph
            xphDelta = xph - prevXph
        }

        return PaceMetrics(
            pagesPerHour = pph,
            secPerPage = secPerPage,
            xpPerHour = xph,
            pagesPerHourDelta = pphDelta,
            xpPerHourDelta = xphDelta
        )
    }
}

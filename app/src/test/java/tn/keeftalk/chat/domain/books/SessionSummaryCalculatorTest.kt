package tn.keeftalk.chat.domain.books

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.ReadingSessionEntity
import java.time.LocalDate

class SessionSummaryCalculatorTest {

    private fun createSession(
        id: String,
        xpGained: Long,
        activeSec: Int,
        pagesConfirmed: Int,
        flagged: Int = 0
    ): ReadingSessionEntity {
        return ReadingSessionEntity(
            id = id,
            contentHash = "hash1",
            bookId = "b1",
            bookTitle = "Book 1",
            bookAuthor = "Author",
            startAt = System.currentTimeMillis() - activeSec * 1000L,
            endAt = System.currentTimeMillis(),
            activeSec = activeSec,
            pagesConfirmed = pagesConfirmed,
            creditedPages = pagesConfirmed.toDouble(),
            xpBase = xpGained / 2,
            xpBonus = xpGained / 2,
            xpAchievements = 0L,
            totalXpGained = xpGained,
            flagged = flagged
        )
    }

    @Test
    fun testRecordDetectionNewBestSessionXp() {
        val s1 = createSession("s1", xpGained = 500L, activeSec = 600, pagesConfirmed = 10)
        val s2 = createSession("s2", xpGained = 700L, activeSec = 800, pagesConfirmed = 15)
        val current = createSession("s3", xpGained = 1000L, activeSec = 900, pagesConfirmed = 20)

        val records = SessionSummaryCalculator.evaluateRecords(
            currentSession = current,
            pastSessions = listOf(s1, s2),
            todayTotalXp = 1500L,
            pastDaysXpMap = mapOf("2025-01-01" to 1200L)
        )

        assertTrue(records.isBestSessionXp)
        assertTrue(records.isLongestActiveTime)
        assertTrue(records.isMostPagesConfirmed)
        assertTrue(records.isBestDayXp)
        assertEquals(0L, records.gapToBestXp)
    }

    @Test
    fun testRecordDetectionNotNewBest() {
        val s1 = createSession("s1", xpGained = 1500L, activeSec = 1200, pagesConfirmed = 30)
        val current = createSession("s2", xpGained = 1000L, activeSec = 900, pagesConfirmed = 20)

        val records = SessionSummaryCalculator.evaluateRecords(
            currentSession = current,
            pastSessions = listOf(s1),
            todayTotalXp = 1000L,
            pastDaysXpMap = mapOf("2025-01-01" to 2000L)
        )

        assertFalse(records.isBestSessionXp)
        assertFalse(records.isLongestActiveTime)
        assertFalse(records.isMostPagesConfirmed)
        assertFalse(records.isBestDayXp)
        assertEquals(500L, records.gapToBestXp)
        assertEquals(1500L, records.bestXp)
    }

    @Test
    fun testRecordDetectionFlaggedSessionIgnored() {
        val flaggedBest = createSession("s1", xpGained = 5000L, activeSec = 3600, pagesConfirmed = 100, flagged = 1)
        val honestPast = createSession("s2", xpGained = 500L, activeSec = 600, pagesConfirmed = 10)
        val current = createSession("s3", xpGained = 800L, activeSec = 900, pagesConfirmed = 15)

        val records = SessionSummaryCalculator.evaluateRecords(
            currentSession = current,
            pastSessions = listOf(flaggedBest, honestPast),
            todayTotalXp = 800L,
            pastDaysXpMap = mapOf("2025-01-01" to 600L)
        )

        // Flagged session s1 is ignored for best XP check, so current (800) beats honestPast (500)
        assertTrue(records.isBestSessionXp)
        assertEquals(500L, records.bestXp)
    }

    @Test
    fun testEtaCalculationFewerThan3Sessions() {
        val s1 = createSession("s1", xpGained = 500L, activeSec = 600, pagesConfirmed = 10)
        val s2 = createSession("s2", xpGained = 500L, activeSec = 600, pagesConfirmed = 10)

        val eta = SessionSummaryCalculator.calculateBookEta(
            remainingPages = 50,
            pastBookSessions = listOf(s1, s2),
            dailyRatePagesPerDay = 20f
        )

        assertTrue(eta is BookEtaResult.NotEnoughData)
    }

    @Test
    fun testEtaCalculationCalculatedResult() {
        val s1 = createSession("s1", xpGained = 500L, activeSec = 600, pagesConfirmed = 10) // 1 page / 60s
        val s2 = createSession("s2", xpGained = 500L, activeSec = 600, pagesConfirmed = 10)
        val s3 = createSession("s3", xpGained = 500L, activeSec = 600, pagesConfirmed = 10)

        val today = LocalDate.of(2025, 5, 1)
        val eta = SessionSummaryCalculator.calculateBookEta(
            remainingPages = 60,
            pastBookSessions = listOf(s1, s2, s3),
            dailyRatePagesPerDay = 20f,
            currentDate = today
        )

        assertTrue(eta is BookEtaResult.Calculated)
        val calc = eta as BookEtaResult.Calculated
        // Total 30 pages in 1800s -> 1 page per 60s -> 60 pages = 3600s = 1.0 hour
        assertEquals(1.0f, calc.hoursRemaining, 0.01f)
        // 60 pages at 20 pages/day = 3 days -> 2025-05-04
        assertEquals(LocalDate.of(2025, 5, 4), calc.projectedDate)
    }

    @Test
    fun testPaceMetricsCalculation() {
        val current = createSession("curr", xpGained = 300L, activeSec = 1800, pagesConfirmed = 10) // 20 p/h, 180 s/p, 600 xp/h
        val prev = createSession("prev", xpGained = 200L, activeSec = 1800, pagesConfirmed = 5) // 10 p/h, 360 s/p, 400 xp/h

        val pace = SessionSummaryCalculator.calculatePaceMetrics(current, prev)

        assertEquals(20.0, pace.pagesPerHour, 0.01)
        assertEquals(180.0, pace.secPerPage, 0.01)
        assertEquals(600.0, pace.xpPerHour, 0.01)
        assertEquals(10.0, pace.pagesPerHourDelta!!, 0.01)
        assertEquals(200.0, pace.xpPerHourDelta!!, 0.01)
    }
}

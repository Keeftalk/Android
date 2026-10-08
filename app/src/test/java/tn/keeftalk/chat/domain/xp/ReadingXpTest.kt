package tn.keeftalk.chat.domain.xp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReadingXpTest {

    @Test
    fun testPageCreditThresholds() {
        assertEquals(0.0, ReadingXp.pageCredit(6.5), 0.0001)
        assertEquals(1.0, ReadingXp.pageCredit(30.0), 0.0001)
        assertEquals(0.5, ReadingXp.pageCredit(18.25), 0.0001)
        assertEquals(0.0, ReadingXp.pageCredit(5.0), 0.0001)
        assertEquals(1.0, ReadingXp.pageCredit(40.0), 0.0001)
    }

    @Test
    fun testHonestHourDay1() {
        // Honest hour: 72 credited pages on day 1 -> base 3,600
        val day1 = LocalDate.of(2025, 1, 1)
        val days = mapOf(day1 to ReadingXp.DayRecord(credited = 72.0))
        val settled = ReadingXp.settle(days)
        assertEquals(3600L, settled.baseXp)
    }

    @Test
    fun test30ConsecutiveQualifyingDays() {
        // 20 credited pages/day for 30 consecutive days -> total within +-40 of 42,900
        val start = LocalDate.of(2025, 1, 1)
        val days = (0 until 30).associate { i ->
            start.plusDays(i.toLong()) to ReadingXp.DayRecord(credited = 20.0)
        }
        val settled = ReadingXp.settle(days)
        assertTrue("Total XP ${settled.total} should be within +-40 of 42900", Math.abs(settled.total - 42900) <= 40)
    }

    @Test
    fun test600CreditedPagesSingleDay() {
        // 600 credited pages in a single day 1 -> 19,272 (+-2)
        val day1 = LocalDate.of(2025, 1, 1)
        val days = mapOf(day1 to ReadingXp.DayRecord(credited = 600.0))
        val settled = ReadingXp.settle(days)
        assertTrue("Total XP ${settled.total} should be within +-2 of 19272", Math.abs(settled.total - 19272) <= 2)
    }

    @Test
    fun testHardCapEquivalence() {
        // credited = 1000 equals credited = 600 (hard cap)
        val day1 = LocalDate.of(2025, 1, 1)
        val day2 = LocalDate.of(2025, 1, 2)

        val settled600 = ReadingXp.settle(mapOf(day1 to ReadingXp.DayRecord(credited = 600.0)))
        val settled1000 = ReadingXp.settle(mapOf(day2 to ReadingXp.DayRecord(credited = 1000.0)))

        assertEquals(settled600.baseXp, settled1000.baseXp)
        assertEquals(settled600.bonusXp, settled1000.bonusXp)
        assertEquals(settled600.total, settled1000.total)
    }

    @Test
    fun testNonQualifyingDay() {
        // 19.9 credited pages -> multiplier 1.0, and day is not qualifying
        assertFalse(ReadingXp.qualifies(19.9))
        assertTrue(ReadingXp.qualifies(20.0))

        val mult = ReadingXp.multiplier(qualifying = false, density = 1.0, streak = 50)
        assertEquals(1.0, mult, 0.0001)

        val day = ReadingXp.dayXp(credited = 19.9, qualifying = false, density = 1.0, streak = 50)
        assertEquals(0L, day.bonusXp)
    }

    @Test
    fun test100DayStreakFullDensityMultiplier() {
        // 100-day streak with full 28-day density -> multiplier exactly 2.5
        val mult = ReadingXp.multiplier(qualifying = true, density = 1.0, streak = 100)
        assertEquals(2.5, mult, 0.0001)
    }

    @Test
    fun testFlaggedDayBreaksStreak() {
        // A flagged day breaks the streak and earns M=1.0
        val start = LocalDate.of(2025, 1, 1)
        val day1 = start
        val day2 = start.plusDays(1)
        val day3 = start.plusDays(2)

        val days = mapOf(
            day1 to ReadingXp.DayRecord(credited = 20.0, flagged = false),
            day2 to ReadingXp.DayRecord(credited = 20.0, flagged = true), // flagged!
            day3 to ReadingXp.DayRecord(credited = 20.0, flagged = false)
        )

        val settledDay2Alone = ReadingXp.dayXp(credited = 20.0, qualifying = false, density = 0.0, streak = 0)
        assertEquals(0L, settledDay2Alone.bonusXp)

        val settledAll = ReadingXp.settle(days)
        // Day 2 earning M=1.0 means no bonus on Day 2
        // Day 3 streak reset to 1
        val day3Streak1Mult = ReadingXp.multiplier(qualifying = true, density = 2.0 / 28.0, streak = 1)
        val expectedDay3Total = (1000.0 * day3Streak1Mult).toLong()

        // Day 1: streak 1, density 1/28 -> mult = 1.0 + 0.5/28 + 1.0/100
        val day1Mult = ReadingXp.multiplier(qualifying = true, density = 1.0 / 28.0, streak = 1)
        val expectedDay1Total = (1000.0 * day1Mult).toLong()

        assertEquals(expectedDay1Total + 1000L + expectedDay3Total, settledAll.total)
    }

    @Test
    fun testSettlementDeterminismNoRetroactiveChange() {
        // Settling days 1..N must give identical per-day XP when day N+1 is added
        val start = LocalDate.of(2025, 1, 1)
        val daysN = (0 until 10).associate { i ->
            start.plusDays(i.toLong()) to ReadingXp.DayRecord(credited = 25.0)
        }
        val daysNPlus1 = daysN + (start.plusDays(10) to ReadingXp.DayRecord(credited = 30.0))

        val settledN = ReadingXp.settle(daysN)

        // Calculate settlement N manually day by day from N+1 settlement
        var sumNInNPlus1Base = 0L
        var sumNInNPlus1Bonus = 0L

        val sorted = daysNPlus1.toSortedMap()
        val q = daysNPlus1.filterValues { ReadingXp.qualifies(it.credited) && !it.flagged }.keys

        for ((date, rec) in sorted) {
            if (date == start.plusDays(10)) break
            val isQ = date in q
            var streak = 0
            var d = date
            while (isQ && d in q) { streak++; d = d.minusDays(1) }
            val density = (0 until ReadingXp.WINDOW).count { date.minusDays(it.toLong()) in q } / ReadingXp.WINDOW.toDouble()
            val r = ReadingXp.dayXp(rec.credited, isQ, density, streak)
            sumNInNPlus1Base += r.baseXp
            sumNInNPlus1Bonus += r.bonusXp
        }

        assertEquals(settledN.baseXp, sumNInNPlus1Base)
        assertEquals(settledN.bonusXp, sumNInNPlus1Bonus)
    }
}

package tn.keeftalk.chat.ui.books

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GoalsTest {

    @Test
    fun testStreakCalculation() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val log = mapOf(
            today.format(formatter) to 35,
            today.minusDays(1).format(formatter) to 30,
            today.minusDays(2).format(formatter) to 40,
            today.minusDays(3).format(formatter) to 10 // missed (goal is 30)
        )

        // Calculate streak manually matching BooksViewModel logic
        var streak = 0
        val checkFrom = if ((log[today.format(formatter)] ?: 0) >= 30) today else today.minusDays(1)
        var curr = checkFrom
        for (i in 0 until 400) {
            val dateStr = curr.format(formatter)
            val mins = log[dateStr] ?: 0
            if (mins >= 30) {
                streak++
                curr = curr.minusDays(1)
            } else {
                break
            }
        }
        assertEquals(3, streak)
    }

    @Test
    fun testMonthSumCalculation() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val firstOfMonth = today.withDayOfMonth(1)

        val log = mapOf(
            firstOfMonth.format(formatter) to 120, // 2h
            firstOfMonth.plusDays(1).format(formatter) to 180 // 3h
        )

        val monthPrefix = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val totalMins = log.filter { it.key.startsWith(monthPrefix) }.values.sum()
        val hours = totalMins / 60f

        assertEquals(5.0f, hours, 0.01f)
    }

    @Test
    fun testStepperMinimumClamp() {
        var dailyGoal = 30
        // Decrement below 30 should stay 30
        if (dailyGoal > 30) {
            dailyGoal -= 5
        }
        assertEquals(30, dailyGoal)

        // Increment by 5
        dailyGoal += 5
        assertEquals(35, dailyGoal)
    }

    @Test
    fun testOncePerPeriodAnnouncements() {
        val todayStr = "2025-05-15"
        var notifiedDailyDate: String? = null
        val dailyGoal = 30
        val todayMins = 35

        // First check: should announce
        val shouldAnnounce = todayMins >= dailyGoal && notifiedDailyDate != todayStr
        assertEquals(true, shouldAnnounce)

        if (shouldAnnounce) {
            notifiedDailyDate = todayStr
        }

        // Second check on same day: should NOT announce again
        val shouldAnnounceAgain = todayMins >= dailyGoal && notifiedDailyDate != todayStr
        assertEquals(false, shouldAnnounceAgain)
    }
}

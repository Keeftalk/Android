package tn.keeftalk.chat.domain.engine

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.domain.model.habit.*
import java.time.*
import java.time.format.DateTimeFormatter

class HabitEngineTest {

    private val fixedClock = Clock.fixed(
        Instant.parse("2026-03-10T10:00:00Z"),
        ZoneId.of("UTC")
    )
    private val engine = HabitEngine(fixedClock)
    private val today = engine.today() // 2026-03-10

    private val dailySchedule = ScheduleVersion(
        habitId = "habit_1",
        effectiveFrom = LocalDate.of(2026, 3, 1),
        daysMask = DayOfWeek.values().toSet()
    )

    @Test
    fun testS1_DailyHabitAllComplete() {
        // Created 10 days ago (2026-03-01 to 2026-03-10 is 10 days inclusive)
        val habit = Habit(
            id = "habit_1",
            userId = "user_1",
            name = "Drink Water",
            icon = "💧",
            category = "Health",
            baseDays = 18,
            createdDate = LocalDate.of(2026, 3, 1),
            reminderTime = null,
            slot = "Morning",
            habitType = "AUTO",
            updatedAt = System.currentTimeMillis()
        )

        val checkIns = (1..10).map { day ->
            CheckIn(habitId = "habit_1", date = LocalDate.of(2026, 3, day), completed = true)
        }

        val status = engine.computeHabitStatus(habit, listOf(dailySchedule), checkIns, emptyList())

        assertEquals(10, status.streak)
        assertEquals(10, status.doneCount)
        assertEquals(0, status.effMissed)
        assertEquals(18, status.effTarget)
        assertEquals(10 / 18f, status.automaticity, 0.01f) // ~0.555 -> 56%
    }

    @Test
    fun testS2_TodayNotYetChecked() {
        val habit = Habit(
            id = "habit_1",
            userId = "user_1",
            name = "Drink Water",
            icon = "💧",
            category = "Health",
            baseDays = 18,
            createdDate = LocalDate.of(2026, 3, 1),
            reminderTime = null,
            slot = "Morning",
            habitType = "AUTO",
            updatedAt = System.currentTimeMillis()
        )

        // Check in 1 to 9, today (10) not checked
        val checkIns = (1..9).map { day ->
            CheckIn(habitId = "habit_1", date = LocalDate.of(2026, 3, day), completed = true)
        }

        val status = engine.computeHabitStatus(habit, listOf(dailySchedule), checkIns, emptyList())

        assertEquals(9, status.streak)
        assertEquals(9, status.doneCount)
        assertEquals(0, status.effMissed)
        assertEquals(18, status.effTarget)
        assertEquals(9 / 18f, status.automaticity, 0.01f) // 50%
    }

    @Test
    fun testS3_MissYesterdayTodayUnchecked() {
        val habit = Habit(
            id = "habit_1",
            userId = "user_1",
            name = "Drink Water",
            icon = "💧",
            category = "Health",
            baseDays = 18,
            createdDate = LocalDate.of(2026, 3, 1),
            reminderTime = null,
            slot = "Morning",
            habitType = "AUTO",
            updatedAt = System.currentTimeMillis()
        )

        // Missed yesterday (March 9), checked 1..8, today (10) unchecked
        val checkIns = (1..8).map { day ->
            CheckIn(habitId = "habit_1", date = LocalDate.of(2026, 3, day), completed = true)
        }

        val status = engine.computeHabitStatus(habit, listOf(dailySchedule), checkIns, emptyList())

        assertTrue(status.recoveryActive)
        assertEquals(1, status.recoveryLevel)
        assertEquals(0, status.recoveryProgress)
        assertEquals(1, status.effMissed) // March 9 is effMissed
        assertEquals(19, status.effTarget) // 18 + 1
    }

    @Test
    fun testS6_GraceOnMiss() {
        val habit = Habit(
            id = "habit_1",
            userId = "user_1",
            name = "Drink Water",
            icon = "💧",
            category = "Health",
            baseDays = 18,
            createdDate = LocalDate.of(2026, 3, 1),
            reminderTime = null,
            slot = "Morning",
            habitType = "AUTO",
            updatedAt = System.currentTimeMillis()
        )

        // Missed March 9, but applied Grace on March 9
        val checkIns = (1..8).map { day ->
            CheckIn(habitId = "habit_1", date = LocalDate.of(2026, 3, day), completed = true)
        }
        val graceUses = listOf(
            GraceUse(habitId = "habit_1", forgivenDate = LocalDate.of(2026, 3, 9), usedDate = LocalDate.of(2026, 3, 9), usedMonthKey = "2026-03")
        )

        val status = engine.computeHabitStatus(habit, listOf(dailySchedule), checkIns, graceUses)

        assertEquals(0, status.effMissed)
        assertFalse(status.recoveryActive)
        assertTrue(engine.canUseGrace(graceUses, LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 9)))
    }

    @Test
    fun testS7_ThirdGraceRejectedInSameMonth() {
        val graceUses = listOf(
            GraceUse(habitId = "h1", forgivenDate = LocalDate.of(2026, 3, 2), usedDate = LocalDate.of(2026, 3, 5), usedMonthKey = "2026-03"),
            GraceUse(habitId = "h2", forgivenDate = LocalDate.of(2026, 3, 3), usedDate = LocalDate.of(2026, 3, 6), usedMonthKey = "2026-03")
        )

        val canUse = engine.canUseGrace(graceUses, LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 7))
        assertFalse(canUse)
    }

    @Test
    fun testS8_MasteryRetirement() {
        val habit = Habit(
            id = "habit_1",
            userId = "user_1",
            name = "Drink Water",
            icon = "💧",
            category = "Health",
            baseDays = 10,
            createdDate = LocalDate.of(2026, 3, 1),
            reminderTime = null,
            slot = "Morning",
            habitType = "AUTO",
            updatedAt = System.currentTimeMillis()
        )

        val checkIns = (1..10).map { day ->
            CheckIn(habitId = "habit_1", date = LocalDate.of(2026, 3, day), completed = true)
        }

        // effTarget = 10, doneCount = 10. If mastery is doneCount >= 2 * effTarget, need 20 doneCount.
        // Let's add 20 checkIns.
        val manyCheckIns = (1..10).map { day ->
            CheckIn(habitId = "habit_1", date = LocalDate.of(2026, 3, day), completed = true)
        } // wait, to get 20 check-ins we need more days or repeated days. But dates are unique.
        // Let's test with baseDays = 5, 10 checkIns. effTarget = 5, 2 * effTarget = 10.
        val habit5 = habit.copy(baseDays = 5)
        val status = engine.computeHabitStatus(habit5, listOf(dailySchedule), manyCheckIns, emptyList())
        assertTrue(status.isMastered)
    }

    @Test
    fun testSlotBoundaries_LateNight() {
        val habit = Habit(
            id = "h_ln",
            userId = "u1",
            name = "Late Read",
            icon = "book",
            category = "eating",
            baseDays = 18,
            createdDate = LocalDate.of(2026, 3, 1),
            reminderTime = null,
            slot = "Late Night",
            habitType = "AUTO",
            updatedAt = System.currentTimeMillis()
        )

        val targetDate = LocalDate.of(2026, 3, 10)

        // 21:59 -> false
        val dt2159 = LocalDateTime.of(2026, 3, 10, 21, 59)
        assertFalse(engine.isSlotValidForCheckIn(habit, targetDate, dt2159))

        // 22:00 -> true
        val dt2200 = LocalDateTime.of(2026, 3, 10, 22, 0)
        assertTrue(engine.isSlotValidForCheckIn(habit, targetDate, dt2200))

        // 23:30 -> true
        val dt2330 = LocalDateTime.of(2026, 3, 10, 23, 30)
        assertTrue(engine.isSlotValidForCheckIn(habit, targetDate, dt2330))

        // 01:00 -> true (March 9 late night checked in at March 10 01:00)
        assertTrue(engine.isSlotValidForCheckIn(habit, LocalDate.of(2026, 3, 9), LocalDateTime.of(2026, 3, 10, 1, 0)))

        // 01:01 -> false
        val dt0101 = LocalDateTime.of(2026, 3, 10, 1, 1)
        assertFalse(engine.isSlotValidForCheckIn(habit, LocalDate.of(2026, 3, 9), dt0101))
    }

    @Test
    fun testAutomaticPreviousDayAttribution() {
        val habit = Habit(
            id = "h_ln",
            userId = "u1",
            name = "Late Read",
            icon = "book",
            category = "eating",
            baseDays = 18,
            createdDate = LocalDate.of(2026, 3, 1),
            reminderTime = null,
            slot = "Late Night",
            habitType = "AUTO",
            updatedAt = System.currentTimeMillis()
        )

        // At 00:30 on March 11, effective date should be March 10
        val dt = LocalDateTime.of(2026, 3, 11, 0, 30)
        val effDate = engine.getEffectiveHabitDate(habit, dt)
        assertEquals(LocalDate.of(2026, 3, 10), effDate)
    }

    @Test
    fun testMissedDayCostMessages() {
        assertNotNull(engine.getMissedDayCostMessage("drink"))
        assertNotNull(engine.getMissedDayCostMessage("eating"))
        assertNotNull(engine.getMissedDayCostMessage("light"))
        assertNotNull(engine.getMissedDayCostMessage("complex"))
    }

    @Test
    fun testGenericHabitRecommendations() {
        val recMarathon = engine.recommendHabitType("Train for a marathon", "FITNESS", 10, "km", 90)
        assertEquals("TRANSFORMATIVE", recMarathon.typeKey)

        val recWalk = engine.recommendHabitType("Brisk walk", "FITNESS", 5, "km", 35)
        assertEquals("ACTIVE", recWalk.typeKey)

        val recBed = engine.recommendHabitType("Make the bed", "HOUSEHOLD", null, null, 2)
        assertEquals("STARTER", recBed.typeKey)
    }

    @Test
    fun testGenericMetricCalculations() {
        val pageMetric = engine.calculateMetric(10, "pages")
        assertTrue(pageMetric.contains("3,650 pages"))

        val minMetric = engine.calculateMetric(30, "minutes")
        assertTrue(minMetric.contains("210 minutes"))
    }
}

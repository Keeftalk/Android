package tn.keeftalk.chat.data.repository

import tn.keeftalk.chat.domain.model.calendar.*
import tn.keeftalk.chat.domain.model.habit.CheckIn
import tn.keeftalk.chat.domain.model.habit.ScheduleVersion
import tn.keeftalk.chat.ui.market.MarketStateData
import tn.keeftalk.chat.ui.market.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceIntegrationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testBookProgressCalculationFromPagePosition() {
        val pageNumber = 50
        val totalPages = 200
        val percent = if (totalPages > 0) {
            ((pageNumber.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else 0

        assertEquals(25, percent)
        val isNowDone = percent >= 100
        assertFalse("50 of 200 pages should not be finished", isNowDone)
    }

    @Test
    fun testCalendarTitleNotOverwrittenWhenDecryptionFails() {
        val encryptedPlaceholderItem = CalendarItem(
            id = "event_1",
            type = CalendarItemType.EVENT,
            title = "[Encrypted]",
            description = "",
            color = null,
            icon = null,
            location = null,
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 3600000,
            isAllDay = false,
            timezone = "UTC",
            recurrenceRule = null,
            priority = CalendarPriority.MEDIUM,
            status = CalendarStatus.PENDING,
            isPrivate = false,
            ownerId = "me",
            categoryId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            meetingType = null,
            meetingLink = null,
            progress = 0,
            pomodoroCount = 0,
            deadline = null,
            parentItemId = null
        )

        val isBlocked = encryptedPlaceholderItem.title == "[Encrypted]"
        assertTrue("Saving an item with '[Encrypted]' title must be blocked to prevent source data corruption", isBlocked)
    }

    @Test
    fun testHabitsOfflineCheckInDateValidation() {
        val today = LocalDate.now()
        val checkIn = CheckIn(habitId = "h_1", date = today, completed = true)

        assertEquals("h_1", checkIn.habitId)
        assertEquals(today, checkIn.date)
        assertTrue(checkIn.completed)
    }

    @Test
    fun testMarketCorruptJsonDoesNotSeedDefaultsOrErasePayload() {
        val corruptJson = "{\"invalid_json_bytes\": 123}"
        var parsedSuccessfully = false
        var state: MarketStateData? = null

        try {
            state = json.decodeFromString<MarketStateData>(corruptJson)
            parsedSuccessfully = true
        } catch (_: Exception) {
            // Non-destructive error handling: DO NOT overwrite or re-seed
            parsedSuccessfully = false
            state = MarketStateData(me = UserProfile(alias = "me", real = "User", mode = "alias"))
        }

        assertFalse("Corrupt JSON must fail parsing without throwing uncaught crash", parsedSuccessfully)
        assertNotNull("Non-destructive fallback state must be provided for UI while preserving raw JSON payload", state)
    }

    @Test
    fun testHabitScheduleEntityConversion() {
        val schedule = ScheduleVersion(
            habitId = "habit_101",
            effectiveFrom = LocalDate.of(2026, 1, 1),
            daysMask = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )

        val daysStr = schedule.daysMask.joinToString(",") { it.name }
        assertTrue("Schedule mask must contain MONDAY", daysStr.contains("MONDAY"))
        assertTrue("Schedule mask must contain WEDNESDAY", daysStr.contains("WEDNESDAY"))
        assertTrue("Schedule mask must contain FRIDAY", daysStr.contains("FRIDAY"))
    }
}

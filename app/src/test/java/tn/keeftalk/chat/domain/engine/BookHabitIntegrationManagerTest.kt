package tn.keeftalk.chat.domain.engine

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.domain.model.BookReadingSession
import tn.keeftalk.chat.domain.model.habit.CheckIn
import tn.keeftalk.chat.domain.model.habit.GraceUse
import tn.keeftalk.chat.domain.model.habit.Habit
import tn.keeftalk.chat.domain.model.habit.MilestoneUnlock
import tn.keeftalk.chat.domain.model.habit.ScheduleVersion
import tn.keeftalk.chat.domain.repository.BookRepository
import tn.keeftalk.chat.domain.repository.HabitsRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class FakeHabitsRepository : HabitsRepository {
    val habits = mutableListOf<Habit>()
    val schedules = mutableListOf<ScheduleVersion>()
    val checkIns = mutableListOf<CheckIn>()

    override fun observeHabits() = flowOf(habits)
    override fun observeSchedules() = flowOf(schedules)
    override fun observeCheckIns() = flowOf(checkIns)
    override fun observeGraceUses() = flowOf(emptyList<GraceUse>())
    override fun observeMilestones() = flowOf(emptyList<MilestoneUnlock>())

    override suspend fun saveHabit(habit: Habit, schedule: ScheduleVersion) {
        habits.add(habit)
        schedules.add(schedule)
    }

    override suspend fun updateHabit(habit: Habit, newSchedule: ScheduleVersion?) {
        val idx = habits.indexOfFirst { it.id == habit.id }
        if (idx != -1) habits[idx] = habit else habits.add(habit)
    }

    override suspend fun deleteHabit(habitId: String) {
        habits.removeIf { it.id == habitId }
    }

    override suspend fun setCheckIn(habitId: String, date: LocalDate, completed: Boolean): Result<Unit> {
        checkIns.removeIf { it.habitId == habitId && it.date == date }
        if (completed) {
            checkIns.add(CheckIn(habitId, date, true))
        }
        return Result.success(Unit)
    }

    override suspend fun useGrace(habitId: String, forgivenDate: LocalDate, usageDate: LocalDate): Result<Unit> = Result.success(Unit)
    override suspend fun unlockMilestone(key: String, date: LocalDate) {}
    override suspend fun seedDefaultDataIfEmpty() {}
}

class FakeBookRepository : BookRepository {
    val sessions = mutableListOf<BookReadingSession>()

    override fun getBookSettingsFlow() = flowOf(tn.keeftalk.chat.domain.model.BookSettings())
    override suspend fun getBookSettings() = tn.keeftalk.chat.domain.model.BookSettings()
    override suspend fun updateBookSettings(settings: tn.keeftalk.chat.domain.model.BookSettings) {}
    override suspend fun syncBooksWithCloud() {}
    override fun getAllBooksFlow() = flowOf(emptyList<tn.keeftalk.chat.domain.model.Book>())
    override suspend fun getAllBooks() = emptyList<tn.keeftalk.chat.domain.model.Book>()
    override suspend fun getBookById(id: String) = null
    override fun getBookByIdFlow(id: String) = flowOf(null)
    override suspend fun saveBook(book: tn.keeftalk.chat.domain.model.Book) {}
    override suspend fun deleteBook(id: String) {}
    override suspend fun restoreBook(id: String) {}
    override suspend fun updateCustomCover(bookId: String, customCover: tn.keeftalk.chat.domain.model.CustomCover?) {}
    override suspend fun getPageProgressForBook(bookId: String) = emptyList<tn.keeftalk.chat.domain.model.BookPageProgress>()
    override fun getPageProgressForBookFlow(bookId: String) = flowOf(emptyList<tn.keeftalk.chat.domain.model.BookPageProgress>())
    override suspend fun savePageProgress(progress: tn.keeftalk.chat.domain.model.BookPageProgress) {}
    override suspend fun getHighlightsForBook(bookId: String) = emptyList<tn.keeftalk.chat.domain.model.BookHighlight>()
    override fun getAllHighlightsFlow() = flowOf(emptyList<tn.keeftalk.chat.domain.model.BookHighlight>())
    override suspend fun saveHighlight(highlight: tn.keeftalk.chat.domain.model.BookHighlight) {}
    override suspend fun deleteHighlight(id: String) {}
    override suspend fun getBookmarksForBook(bookId: String) = emptyList<tn.keeftalk.chat.domain.model.BookBookmark>()
    override fun getAllBookmarksFlow() = flowOf(emptyList<tn.keeftalk.chat.domain.model.BookBookmark>())
    override suspend fun saveBookmark(bookmark: tn.keeftalk.chat.domain.model.BookBookmark) {}
    override suspend fun deleteBookmark(bookId: String, pageNumber: Int) {}

    override suspend fun getAllSessions(): List<BookReadingSession> = sessions
    override suspend fun recordReadingSession(session: BookReadingSession) {
        sessions.removeIf { it.id == session.id }
        sessions.add(session)
    }
}

class BookHabitIntegrationManagerTest {

    private val habitsRepo = FakeHabitsRepository()
    private val bookRepo = FakeBookRepository()
    private val manager = BookHabitIntegrationManager(habitsRepo, bookRepo)

    @Test
    fun testGeneralReadingHabitAutoCompletesWhenTargetReached() = runBlocking {
        val today = LocalDate.now()
        val habit = Habit(
            id = "habit_read_gen",
            userId = "user_1",
            name = "Read 20 minutes daily",
            icon = "book",
            category = "eating",
            baseDays = 18,
            createdDate = today.minusDays(5),
            reminderTime = LocalTime.of(8, 0),
            slot = "Morning",
            habitType = "AUTO",
            amount = 20,
            unit = "min",
            isReadingHabit = true,
            autoCountReading = true,
            linkedBookIds = emptyList(), // General reading
            updatedAt = System.currentTimeMillis()
        )
        val schedule = ScheduleVersion("habit_read_gen", today.minusDays(5), DayOfWeek.entries.toSet())
        habitsRepo.saveHabit(habit, schedule)

        val startTime = today.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val session = BookReadingSession(
            id = "s1",
            bookId = "book_atomic",
            startTime = startTime,
            endTime = startTime + (25 * 60 * 1000L), // 25 minutes
            pagesRead = 15,
            durationSeconds = 25 * 60L
        )
        bookRepo.recordReadingSession(session)

        val summaries = manager.evaluateReadingHabitsForSession(session)

        assertEquals(1, summaries.size)
        val summary = summaries.first()
        assertTrue(summary.isCompleted)
        assertEquals(25, summary.currentAmount)
        assertEquals(20, summary.targetAmount)

        // Verify check-in was automatically recorded
        val checkIn = habitsRepo.checkIns.find { it.habitId == "habit_read_gen" && it.date == today }
        assertNotNull(checkIn)
        assertTrue(checkIn!!.completed)
    }

    @Test
    fun testSpecificBookReadingHabitIgnoresOtherBooks() = runBlocking {
        val today = LocalDate.now()
        val habit = Habit(
            id = "habit_read_atomic",
            userId = "user_1",
            name = "Read Atomic Habits 15 mins",
            icon = "book",
            category = "eating",
            baseDays = 18,
            createdDate = today.minusDays(5),
            reminderTime = null,
            slot = "Morning",
            habitType = "AUTO",
            amount = 15,
            unit = "min",
            isReadingHabit = true,
            autoCountReading = true,
            linkedBookIds = listOf("book_atomic"), // Specific book only
            updatedAt = System.currentTimeMillis()
        )
        val schedule = ScheduleVersion("habit_read_atomic", today.minusDays(5), DayOfWeek.entries.toSet())
        habitsRepo.saveHabit(habit, schedule)

        val startTime = today.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Session for a DIFFERENT book ("book_deep_work")
        val otherSession = BookReadingSession(
            id = "s_other",
            bookId = "book_deep_work",
            startTime = startTime,
            endTime = startTime + (30 * 60 * 1000L),
            pagesRead = 20,
            durationSeconds = 30 * 60L
        )
        bookRepo.recordReadingSession(otherSession)

        val summariesOther = manager.evaluateReadingHabitsForSession(otherSession)
        assertTrue(summariesOther.isEmpty())

        // Verify check-in is NOT recorded for other book
        val checkInOther = habitsRepo.checkIns.find { it.habitId == "habit_read_atomic" && it.date == today }
        assertNull(checkInOther)

        // Session for the MATCHING book ("book_atomic")
        val matchingSession = BookReadingSession(
            id = "s_match",
            bookId = "book_atomic",
            startTime = startTime,
            endTime = startTime + (20 * 60 * 1000L),
            pagesRead = 12,
            durationSeconds = 20 * 60L
        )
        bookRepo.recordReadingSession(matchingSession)

        val summariesMatch = manager.evaluateReadingHabitsForSession(matchingSession)
        assertEquals(1, summariesMatch.size)
        assertTrue(summariesMatch.first().isCompleted)

        // Verify check-in IS recorded for matching book
        val checkInMatch = habitsRepo.checkIns.find { it.habitId == "habit_read_atomic" && it.date == today }
        assertNotNull(checkInMatch)
        assertTrue(checkInMatch!!.completed)
    }

    @Test
    fun testIdempotentSessionEvaluationPreventsDuplicates() = runBlocking {
        val today = LocalDate.now()
        val habit = Habit(
            id = "habit_idempotent",
            userId = "user_1",
            name = "Read daily",
            icon = "book",
            category = "eating",
            baseDays = 18,
            createdDate = today.minusDays(2),
            reminderTime = null,
            slot = "Morning",
            habitType = "AUTO",
            amount = 10,
            unit = "min",
            isReadingHabit = true,
            autoCountReading = true,
            linkedBookIds = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        val schedule = ScheduleVersion("habit_idempotent", today.minusDays(2), DayOfWeek.entries.toSet())
        habitsRepo.saveHabit(habit, schedule)

        val startTime = today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val session = BookReadingSession(
            id = "s_fixed",
            bookId = "b1",
            startTime = startTime,
            endTime = startTime + (15 * 60 * 1000L),
            pagesRead = 10,
            durationSeconds = 15 * 60L
        )
        bookRepo.recordReadingSession(session)

        // Evaluate twice
        manager.evaluateReadingHabitsForSession(session)
        manager.evaluateReadingHabitsForSession(session)

        // Must still have exactly 1 check-in for today
        val matchingCheckIns = habitsRepo.checkIns.filter { it.habitId == "habit_idempotent" && it.date == today }
        assertEquals(1, matchingCheckIns.size)
    }
}

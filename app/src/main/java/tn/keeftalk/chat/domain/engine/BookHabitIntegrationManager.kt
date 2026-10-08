package tn.keeftalk.chat.domain.engine

import kotlinx.coroutines.flow.first
import tn.keeftalk.chat.domain.model.BookReadingSession
import tn.keeftalk.chat.domain.model.habit.Habit
import tn.keeftalk.chat.domain.repository.BookRepository
import tn.keeftalk.chat.domain.repository.HabitsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HabitReadingProgressSummary(
    val habit: Habit,
    val effectiveDate: LocalDate,
    val targetAmount: Int,
    val currentAmount: Int,
    val unit: String,
    val isCompleted: Boolean,
    val remainingAmount: Int
)

class BookHabitIntegrationManager(
    private val habitsRepository: HabitsRepository,
    private val bookRepository: BookRepository,
    private val engine: HabitEngine = HabitEngine()
) {

    /**
     * Evaluates all active reading habits against the recorded book reading sessions
     * for a given reading session or date. Automatically completes habits whose target is reached.
     */
    suspend fun evaluateReadingHabitsForSession(session: BookReadingSession): List<HabitReadingProgressSummary> {
        val allHabits = habitsRepository.observeHabits().first().filter { !it.deleted }
        val schedules = habitsRepository.observeSchedules().first()
        val allSessions = bookRepository.getAllSessions()

        val sessionDateTime = Instant.ofEpochMilli(session.startTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        val summaries = mutableListOf<HabitReadingProgressSummary>()

        val readingHabits = allHabits.filter { habit ->
            isHabitReadingRelated(habit) && habit.autoCountReading
        }

        for (habit in readingHabits) {
            val effectiveDate = engine.getEffectiveHabitDate(habit, sessionDateTime)

            // Check if habit is due on this date
            val habitSchedules = schedules.filter { it.habitId == habit.id }
            if (!engine.isDue(habit, habitSchedules, effectiveDate)) {
                continue
            }

            // Check if book matches
            val isMatchingBook = habit.linkedBookIds.isEmpty() || habit.linkedBookIds.contains(session.bookId)
            if (!isMatchingBook) {
                continue
            }

            // Sum matching reading sessions for effectiveDate
            val matchingSessionsForDate = allSessions.filter { s ->
                val sdt = Instant.ofEpochMilli(s.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val sEffDate = engine.getEffectiveHabitDate(habit, sdt)
                val bookMatch = habit.linkedBookIds.isEmpty() || habit.linkedBookIds.contains(s.bookId)
                sEffDate == effectiveDate && bookMatch
            }

            val targetUnit = habit.unit?.lowercase()?.trim() ?: "min"
            val targetAmount = maxOf(1, habit.amount ?: 15)

            val currentProgress = if (targetUnit in listOf("page", "pages", "p")) {
                matchingSessionsForDate.sumOf { it.pagesRead }
            } else {
                // Minutes
                matchingSessionsForDate.sumOf { (it.durationSeconds / 60).toInt() }
            }

            val isCompleted = currentProgress >= targetAmount
            val remaining = maxOf(0, targetAmount - currentProgress)

            val summary = HabitReadingProgressSummary(
                habit = habit,
                effectiveDate = effectiveDate,
                targetAmount = targetAmount,
                currentAmount = currentProgress,
                unit = targetUnit,
                isCompleted = isCompleted,
                remainingAmount = remaining
            )
            summaries.add(summary)

            // Automatically check in if target reached
            if (isCompleted) {
                habitsRepository.setCheckIn(habit.id, effectiveDate, completed = true)
            }
        }

        return summaries
    }

    /**
     * Returns the reading progress for a given habit on a target date.
     */
    suspend fun getProgressForHabitOnDate(habit: Habit, date: LocalDate): HabitReadingProgressSummary {
        val allSessions = bookRepository.getAllSessions()

        val matchingSessionsForDate = allSessions.filter { s ->
            val sdt = Instant.ofEpochMilli(s.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val sEffDate = engine.getEffectiveHabitDate(habit, sdt)
            val bookMatch = habit.linkedBookIds.isEmpty() || habit.linkedBookIds.contains(s.bookId)
            sEffDate == date && bookMatch
        }

        val targetUnit = habit.unit?.lowercase()?.trim() ?: "min"
        val targetAmount = maxOf(1, habit.amount ?: 15)

        val currentProgress = if (targetUnit in listOf("page", "pages", "p")) {
            matchingSessionsForDate.sumOf { it.pagesRead }
        } else {
            matchingSessionsForDate.sumOf { (it.durationSeconds / 60).toInt() }
        }

        val isCompleted = currentProgress >= targetAmount
        val remaining = maxOf(0, targetAmount - currentProgress)

        return HabitReadingProgressSummary(
            habit = habit,
            effectiveDate = date,
            targetAmount = targetAmount,
            currentAmount = currentProgress,
            unit = targetUnit,
            isCompleted = isCompleted,
            remainingAmount = remaining
        )
    }

    fun isHabitReadingRelated(habit: Habit): Boolean {
        if (habit.isReadingHabit) return true
        if (habit.icon == "book" || habit.icon == "open-book" || habit.icon == "menu-book") return true
        val lowerName = habit.name.lowercase()
        val lowerCat = habit.category.lowercase()
        val lowerUnit = (habit.unit ?: "").lowercase()
        return lowerName.contains("read") || lowerName.contains("book") || lowerName.contains("page") ||
                lowerCat.contains("read") || lowerUnit.contains("page")
    }
}

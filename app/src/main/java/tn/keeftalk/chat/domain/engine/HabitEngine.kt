package tn.keeftalk.chat.domain.engine

import tn.keeftalk.chat.domain.model.habit.*
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitEngine(
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun today(): LocalDate = LocalDate.now(clock)

    fun isDue(habit: Habit, scheduleVersions: List<ScheduleVersion>, date: LocalDate): Boolean {
        if (date.isBefore(habit.createdDate)) return false
        val schedule = getScheduleForDate(scheduleVersions, date) ?: return false
        return schedule.daysMask.contains(date.dayOfWeek)
    }

    private fun getScheduleForDate(scheduleVersions: List<ScheduleVersion>, date: LocalDate): ScheduleVersion? {
        return scheduleVersions
            .filter { !date.isBefore(it.effectiveFrom) }
            .maxByOrNull { it.effectiveFrom }
    }

    fun isComplete(habitId: String, date: LocalDate, checkIns: List<CheckIn>, graceUses: List<GraceUse>): Boolean {
        val checked = checkIns.any { it.habitId == habitId && it.date == date && it.completed }
        val forgiven = graceUses.any { it.habitId == habitId && it.forgivenDate == date }
        return checked || forgiven
    }

    /**
     * Replays history chronologically to compute recovery state and recovered misses.
     * Returns a pair of (activeRecoveryState, recoveredDatesSet)
     */
    data class RecoveryEngineResult(
        val active: Boolean,
        val level: Int, // 0 to 4
        val progress: Int,
        val required: Int,
        val recoveredDates: Set<LocalDate>
    )

    fun computeRecovery(
        habit: Habit,
        scheduleVersions: List<ScheduleVersion>,
        checkIns: List<CheckIn>,
        graceUses: List<GraceUse>,
        upToDate: LocalDate
    ): RecoveryEngineResult {
        // Collect all due dates from createdDate up to (upToDate - 1 day) or upToDate depending on rules.
        // Rule D7: Today is never a miss.
        val dueDates = mutableListOf<LocalDate>()
        var d = habit.createdDate
        while (d.isBefore(upToDate)) {
            if (isDue(habit, scheduleVersions, d)) {
                dueDates.add(d)
            }
            d = d.plusDays(1)
        }

        var active = false
        var level = 0 // 0 means no active challenge
        var progress = 0
        var requiredDays = 7
        val recovered = mutableSetOf<LocalDate>()
        // Keep track of current episode's missed days
        var episodeMisses = mutableListOf<LocalDate>()

        for (date in dueDates) {
            val complete = isComplete(habit.id, date, checkIns, graceUses)
            if (complete) {
                if (active) {
                    progress++
                    if (progress >= requiredDays) {
                        // Challenge completed! Misses in this episode become recovered.
                        recovered.addAll(episodeMisses)
                        active = false
                        level = 0
                        progress = 0
                        episodeMisses.clear()
                    }
                }
            } else {
                // Missed due day
                if (!active) {
                    active = true
                    level = 1
                    progress = 0
                    requiredDays = 7
                    episodeMisses = mutableListOf(date)
                } else {
                    // Another miss while active raises level (cap 4) and resets progress to 0
                    level = minOf(4, level + 1)
                    progress = 0
                    requiredDays = level * 7
                    episodeMisses.add(date)
                }
            }
        }

        return RecoveryEngineResult(
            active = active,
            level = level,
            progress = progress,
            required = if (active) requiredDays else 7,
            recoveredDates = recovered
        )
    }

    fun computeHabitStatus(
        habit: Habit,
        scheduleVersions: List<ScheduleVersion>,
        checkIns: List<CheckIn>,
        graceUses: List<GraceUse>
    ): HabitCalculatedStatus {
        val todayDate = today()
        val dueDatesAll = mutableListOf<LocalDate>()
        var d = habit.createdDate
        val checkDateLimit = todayDate.plusDays(1)
        while (d.isBefore(checkDateLimit)) {
            if (isDue(habit, scheduleVersions, d)) {
                dueDatesAll.add(d)
            }
            d = d.plusDays(1)
        }

        val pastDueDates = dueDatesAll.filter { it.isBefore(todayDate) }
        val recoveryRes = computeRecovery(habit, scheduleVersions, checkIns, graceUses, todayDate)
        val recoveredSet = recoveryRes.recoveredDates

        // effMissed: past due days strictly before today that are not completed, not forgiven and not recovered
        val effMissed = pastDueDates.count { date ->
            val complete = isComplete(habit.id, date, checkIns, graceUses)
            val isRecovered = recoveredSet.contains(date)
            !complete && !isRecovered
        }

        val effTarget = habit.baseDays + effMissed
        val doneCount = checkIns.count { it.habitId == habit.id && it.completed && !it.date.isAfter(todayDate) }
        val automaticity = if (effTarget > 0) minOf(1.0f, doneCount.toFloat() / effTarget.toFloat()) else 0.0f

        // Mastery: doneCount >= 2 * effTarget (and effTarget > 0)
        val isMastered = effTarget > 0 && doneCount >= 2 * effTarget

        val isDueToday = isDue(habit, scheduleVersions, todayDate)
        val isCheckedInToday = checkIns.any { it.habitId == habit.id && it.date == todayDate && it.completed }
        val isForgivenToday = graceUses.any { it.habitId == habit.id && it.forgivenDate == todayDate }

        val hasUngracedMiss = pastDueDates.any { date ->
            val complete = isComplete(habit.id, date, checkIns, graceUses)
            !complete
        }

        // Streak calculation (Rule D4)
        // Consecutive complete due days ending today. If today is due and not yet done, start counting from previous due day.
        val streak = calculateStreak(habit, scheduleVersions, checkIns, graceUses, todayDate)
        val longestStreak = calculateLongestStreak(habit, scheduleVersions, checkIns, graceUses, todayDate)

        return HabitCalculatedStatus(
            habitId = habit.id,
            streak = streak,
            longestStreak = longestStreak,
            doneCount = doneCount,
            effMissed = effMissed,
            effTarget = effTarget,
            automaticity = automaticity,
            isMastered = isMastered,
            isDueToday = isDueToday,
            isCheckedInToday = isCheckedInToday,
            isForgivenToday = isForgivenToday,
            hasUngracedMiss = hasUngracedMiss,
            recoveryActive = recoveryRes.active,
            recoveryLevel = recoveryRes.level,
            recoveryProgress = recoveryRes.progress,
            recoveryRequired = recoveryRes.required
        )
    }

    private fun calculateStreak(
        habit: Habit,
        scheduleVersions: List<ScheduleVersion>,
        checkIns: List<CheckIn>,
        graceUses: List<GraceUse>,
        todayDate: LocalDate
    ): Int {
        val dueDatesDesc = mutableListOf<LocalDate>()
        var d = todayDate
        while (!d.isBefore(habit.createdDate)) {
            if (isDue(habit, scheduleVersions, d)) {
                dueDatesDesc.add(d)
            }
            d = d.minusDays(1)
        }

        if (dueDatesDesc.isEmpty()) return 0

        var startIndex = 0
        val todayDue = isDue(habit, scheduleVersions, todayDate)
        val todayDone = isComplete(habit.id, todayDate, checkIns, graceUses)

        if (todayDue && !todayDone) {
            // If today is due and not yet done, start counting from previous due day
            startIndex = 1
        }

        var streak = 0
        for (i in startIndex until dueDatesDesc.size) {
            val date = dueDatesDesc[i]
            if (isComplete(habit.id, date, checkIns, graceUses)) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateLongestStreak(
        habit: Habit,
        scheduleVersions: List<ScheduleVersion>,
        checkIns: List<CheckIn>,
        graceUses: List<GraceUse>,
        todayDate: LocalDate
    ): Int {
        val dueDatesAsc = mutableListOf<LocalDate>()
        var d = habit.createdDate
        while (!d.isAfter(todayDate)) {
            if (isDue(habit, scheduleVersions, d)) {
                dueDatesAsc.add(d)
            }
            d = d.plusDays(1)
        }

        var maxStreak = 0
        var currentStreak = 0
        for (date in dueDatesAsc) {
            if (isComplete(habit.id, date, checkIns, graceUses)) {
                currentStreak++
                if (currentStreak > maxStreak) maxStreak = currentStreak
            } else {
                currentStreak = 0
            }
        }
        return maxStreak
    }

    fun canUseGrace(
        allGraceUses: List<GraceUse>,
        targetDate: LocalDate,
        usageDate: LocalDate
    ): Boolean {
        val monthKey = usageDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val usesThisMonth = allGraceUses.count { it.usedMonthKey == monthKey }
        return usesThisMonth < 2
    }

    fun getSlotWindowLabel(slot: String): String {
        return when (slot) {
            "Early Morning" -> "04:00 AM – 08:00 AM"
            "Morning" -> "08:00 AM – 11:30 AM"
            "Midday" -> "11:30 AM – 02:00 PM"
            "Afternoon" -> "02:00 PM – 05:30 PM"
            "Evening" -> "05:30 PM – 10:00 PM"
            "Late Night" -> "10:00 PM – 01:00 AM"
            else -> "All day"
        }
    }

    fun getEffectiveHabitDate(
        habit: Habit,
        currentDateTime: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ): LocalDate {
        val currentDate = currentDateTime.toLocalDate()
        val currentTime = currentDateTime.toLocalTime()
        val totalMins = currentTime.hour * 60 + currentTime.minute

        if (habit.slot.equals("Late Night", ignoreCase = true)) {
            // If between 00:00 and 01:00, attribute to yesterday
            if (totalMins <= 60) {
                return currentDate.minusDays(1)
            }
        }
        return currentDate
    }

    fun isSlotValidForCheckIn(
        habit: Habit,
        targetDate: LocalDate,
        currentDateTime: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ): Boolean {
        val currentDate = currentDateTime.toLocalDate()
        val currentTime = currentDateTime.toLocalTime()

        if (habit.slot.isBlank()) return true

        val effectiveDate = getEffectiveHabitDate(habit, currentDateTime)

        if (targetDate == effectiveDate || targetDate == currentDate) {
            return isTimeInSlotWindow(habit.slot, currentTime)
        }

        if (targetDate == currentDate.minusDays(1)) {
            if (habit.slot.equals("Late Night", ignoreCase = true)) {
                return currentTime.hour < 1 || (currentTime.hour == 1 && currentTime.minute == 0)
            }
        }

        return false
    }

    private fun isTimeInSlotWindow(slot: String, time: java.time.LocalTime): Boolean {
        val totalMins = time.hour * 60 + time.minute
        return when (slot) {
            "Early Morning" -> totalMins in (240 - 60)..(480 + 60)
            "Morning" -> totalMins in (480 - 60)..(690 + 60)
            "Midday" -> totalMins in (690 - 60)..(840 + 60)
            "Afternoon" -> totalMins in (840 - 60)..(1050 + 60)
            "Evening" -> totalMins in (1050 - 60)..(1320 + 60)
            "Late Night" -> totalMins >= 1320 || totalMins <= 60
            else -> true
        }
    }

    enum class EffortLevel {
        VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH
    }

    data class HabitProfile(
        val normalizedName: String,
        val category: String,
        val type: String,
        val amount: Int?,
        val unit: String?,
        val durationMinutes: Int,
        val frequency: String,
        val physicalDemand: EffortLevel,
        val cognitiveDemand: EffortLevel,
        val complexity: EffortLevel,
        val commitmentLevel: EffortLevel,
        val measurable: Boolean,
        val longTermGoal: Boolean
    )

    data class HabitTypeRecommendation(
        val typeKey: String,
        val categoryKey: String,
        val title: String,
        val badge: String,
        val reason: String,
        val confidence: Float
    )

    fun buildHabitProfile(name: String, category: String?, amount: Int?, unit: String?, durationMinutes: Int?): HabitProfile {
        val lowerName = (name ?: "").lowercase()
        val un = (unit ?: "").lowercase()
        val amt = amount ?: 0
        val dur = durationMinutes ?: when {
            un.contains("min") -> amt
            un.contains("hour") || un.contains("hr") -> amt * 60
            else -> 15
        }

        var cat = (category ?: "OTHER").uppercase()
        if (cat == "OTHER" || cat.isBlank()) {
            cat = when {
                lowerName.contains("run") || lowerName.contains("gym") || lowerName.contains("workout") || lowerName.contains("swim") || lowerName.contains("cycle") || lowerName.contains("stretch") || lowerName.contains("yoga") || lowerName.contains("exercise") -> "FITNESS"
                lowerName.contains("read") || lowerName.contains("study") || lowerName.contains("learn") || lowerName.contains("vocabulary") || lowerName.contains("spanish") || lowerName.contains("code") || lowerName.contains("programming") -> "LEARNING"
                lowerName.contains("meditat") || lowerName.contains("breathe") || lowerName.contains("pray") || lowerName.contains("mindful") -> "MINDFULNESS"
                lowerName.contains("water") || lowerName.contains("vitamin") || lowerName.contains("sleep") || lowerName.contains("pill") -> "HEALTH"
                lowerName.contains("floss") || lowerName.contains("brush") || lowerName.contains("shower") || lowerName.contains("skincare") -> "PERSONAL_CARE"
                lowerName.contains("clean") || lowerName.contains("tidy") || lowerName.contains("bed") || lowerName.contains("wash") -> "HOUSEHOLD"
                lowerName.contains("save") || lowerName.contains("budget") || lowerName.contains("money") || lowerName.contains("invest") -> "FINANCE"
                lowerName.contains("write") || lowerName.contains("draw") || lowerName.contains("guitar") || lowerName.contains("piano") || lowerName.contains("music") -> "CREATIVE"
                lowerName.contains("call") || lowerName.contains("family") || lowerName.contains("friend") -> "SOCIAL"
                lowerName.contains("meal") || lowerName.contains("cook") || lowerName.contains("fruit") || lowerName.contains("veg") -> "NUTRITION"
                else -> "PRODUCTIVITY"
            }
        }

        val physical = when {
            lowerName.contains("marathon") || lowerName.contains("ironman") || lowerName.contains("heavy") -> EffortLevel.VERY_HIGH
            lowerName.contains("run") || lowerName.contains("gym") || lowerName.contains("workout") || dur >= 45 -> EffortLevel.HIGH
            lowerName.contains("walk") || lowerName.contains("stretch") || dur >= 20 -> EffortLevel.MEDIUM
            else -> EffortLevel.LOW
        }

        val cognitive = when {
            lowerName.contains("code") || lowerName.contains("study") || lowerName.contains("programming") || lowerName.contains("language") -> EffortLevel.HIGH
            lowerName.contains("read") || lowerName.contains("write") || lowerName.contains("journal") -> EffortLevel.MEDIUM
            else -> EffortLevel.LOW
        }

        val complexity = if (cognitive == EffortLevel.HIGH || physical == EffortLevel.HIGH) EffortLevel.HIGH else EffortLevel.LOW
        val commitment = if (lowerName.contains("marathon") || lowerName.contains("transform") || lowerName.contains("master")) EffortLevel.VERY_HIGH else EffortLevel.MEDIUM

        return HabitProfile(
            normalizedName = lowerName,
            category = cat,
            type = "STARTER",
            amount = if (amt > 0) amt else null,
            unit = if (un.isNotBlank()) un else null,
            durationMinutes = dur,
            frequency = "DAILY",
            physicalDemand = physical,
            cognitiveDemand = cognitive,
            complexity = complexity,
            commitmentLevel = commitment,
            measurable = amt > 0 || un.isNotBlank(),
            longTermGoal = commitment == EffortLevel.VERY_HIGH
        )
    }

    fun recommendHabitType(name: String, category: String? = null, amount: Int? = null, unit: String? = null, durationMinutes: Int? = null): HabitTypeRecommendation {
        val profile = buildHabitProfile(name, category, amount, unit, durationMinutes)

        var score = 0
        if (profile.physicalDemand == EffortLevel.VERY_HIGH || profile.cognitiveDemand == EffortLevel.HIGH || profile.longTermGoal) score += 35
        if (profile.physicalDemand == EffortLevel.HIGH || profile.cognitiveDemand == EffortLevel.MEDIUM) score += 22
        if (profile.complexity == EffortLevel.HIGH || profile.durationMinutes >= 30) score += 15
        if (profile.durationMinutes >= 15) score += 8

        val (typeKey, title, badge, reason) = when {
            score >= 30 || profile.longTermGoal -> arrayOf(
                "TRANSFORMATIVE",
                "Transformative Habit",
                "MAJOR TRANSFORMATION",
                "Big lifestyle and long-term capability changes built through sustained patience and multi-month commitment."
            )
            score >= 20 -> arrayOf(
                "ACTIVE",
                "Active Lifestyle",
                "LONG-TERM GROWTH",
                "Habits requiring physical or cognitive exertion, structured planning, and consistent training sessions."
            )
            score >= 10 -> arrayOf(
                "CORE",
                "Core Routine",
                "BUILD MOMENTUM",
                "Everyday knowledge and mental routines that compound through consistent daily repetition."
            )
            else -> arrayOf(
                "STARTER",
                "Starter Habit",
                "QUICK WIN",
                "Small, accessible daily actions that are easy to start and repeat with very low friction."
            )
        }

        return HabitTypeRecommendation(
            typeKey = typeKey,
            categoryKey = profile.category,
            title = title,
            badge = badge,
            reason = reason,
            confidence = 0.92f
        )
    }

    fun calculateMetric(amount: Int, unit: String): String {
        val un = unit.lowercase().trim()
        val annual = amount * 365
        val annualStr = String.format(java.util.Locale.US, "%,d", annual)
        return when {
            un in listOf("page", "pages", "p") -> {
                val books = maxOf(1, annual / 350)
                "📚 $amount $un today. At this daily rate, you'll reach about $annualStr pages over a year — roughly $books average-length books!"
            }
            un in listOf("min", "mins", "minute", "minutes", "m") -> {
                val weekly = amount * 7
                val monthly = amount * 30
                "⏱️ $amount $un today. Stacking this consistently gives you $weekly minutes weekly (~${monthly / 60} hours monthly)!"
            }
            un in listOf("km", "kilometer", "kilometers", "mi", "mile", "miles") -> {
                val weekly = amount * 7
                "🏃 $amount $un today. Your weekly distance is pacing at $weekly $un/week."
            }
            un in listOf("word", "words", "w") -> {
                val annualS = String.format(java.util.Locale.US, "%,d", annual)
                "✍️ $amount $un today. That accumulates to about $annualS words over a year!"
            }
            un in listOf("step", "steps") -> {
                val weekly = amount * 7
                val weeklyS = String.format(java.util.Locale.US, "%,d", weekly)
                "👟 $amount $un today. Keep moving — that's ~$weeklyS steps weekly!"
            }
            un in listOf("rep", "reps", "set", "sets") -> {
                val weekly = amount * 7
                "💪 $amount $un today. Consistent repetition builds real strength."
            }
            un in listOf("dollar", "dollars", "usd", "$", "eur", "€", "gbp", "£") -> {
                val annualS = String.format(java.util.Locale.US, "%,d", annual)
                "💰 Saved/invested $amount $un today. That builds to about $annualS $un over a year!"
            }
            else -> {
                "📈 $amount $un completed today. At this daily rate, you'll accumulate about $annualStr $un over a full year!"
            }
        }
    }

    fun generateHabitSpecificInsight(
        completedHabits: List<Habit>,
        bestStreak: Int
    ): String {
        if (completedHabits.isEmpty()) return "Build your life, one habit at a time."

        val names = completedHabits.map { it.name.lowercase() }
        val firstHabit = completedHabits.first()
        val habitName = firstHabit.name
        val amt = firstHabit.amount
        val unit = firstHabit.unit?.lowercase() ?: ""

        if (amt != null && amt > 0 && unit.isNotBlank()) {
            return calculateMetric(amt, unit)
        }
        if (names.any { it.contains("read") || it.contains("page") }) {
            return "📚 Reading daily expands your perspective and builds lasting knowledge. Great job keeping your reading habit active!"
        }
        if (names.any { it.contains("run") || it.contains("walk") || it.contains("gym") || it.contains("workout") || it.contains("exercise") }) {
            return "💪 Physical consistency is the key to vitality. You showed up for your active routine today!"
        }
        if (names.any { it.contains("water") || it.contains("hydrat") || it.contains("drink") }) {
            return "💧 Hydration routine completed. Staying hydrated supports daily energy and normal body function."
        }
        if (names.any { it.contains("meditat") || it.contains("breathe") || it.contains("journal") }) {
            return "🧘 Mental wellness routine checked off. Taking a moment to pause builds powerful clarity."
        }
        if (completedHabits.size > 1) {
            val habitListStr = completedHabits.joinToString(", ") { it.name }
            return "🎉 $habitListStr — that's a fantastic combination of daily actions completed today!"
        }
        if (bestStreak >= 7) {
            return "🔥 $bestStreak-day streak on \"$habitName\". Consistency compounds into automatic habits."
        }

        return "⭐ Completed \"$habitName\" today. Every small step builds your routine."
    }

    fun getMissedDayCostMessage(category: String): String {
        return when (category.lowercase()) {
            "drink", "hygiene" -> "Your progress isn't gone. One missed day simply means one less repetition toward making this routine automatic."
            "eating" -> "One missed day won't undo your progress, but getting back into the routine today helps protect your momentum."
            "light" -> "Long-term habits depend heavily on consistency. One missed session matters less than getting back on track."
            "complex" -> "Big changes are built over many repetitions. One missed day is a small part of the journey — the important thing is continuing."
            else -> "Your progress remains intact. One missed day is a small pause on your journey."
        }
    }

    fun generateSmartInsight(bestStreak: Int, masteredCount: Int): String {
        return when {
            bestStreak >= 14 -> "🔥 You're on an incredible $bestStreak-day streak. Your consistency is compounding into lasting habits."
            bestStreak >= 7 -> "⭐ A full week of consistency! Your daily routines are taking root."
            masteredCount > 0 -> "🏆 You've mastered $masteredCount habit${if (masteredCount == 1) "" else "s"} so far. Keep building!"
            else -> "🌱 Every small step builds your routine. Consistency matters more than perfection."
        }
    }
}

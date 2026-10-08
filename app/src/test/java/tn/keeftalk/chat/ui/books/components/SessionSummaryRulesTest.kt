package tn.keeftalk.chat.ui.books.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.ReadingSessionEntity

/**
 * Covers the rules layer of the session summary:
 *  - record logic  : history is NON-flagged only and never contains the session itself
 *  - ETA           : needs >= 3 sessions of the book, otherwise "Not enough data yet"
 *  - streak        : days to the next milestone
 *  - encouragement : stable per session, varied across sessions, always in range
 */
class SessionSummaryRulesTest {

    private fun session(id: String, xp: Long, flagged: Int = 0) = ReadingSessionEntity(
        id = id,
        contentHash = "hash",
        bookId = "book",
        bookTitle = "Title",
        bookAuthor = "",
        coverUrl = "",
        customCoverJson = null,
        startAt = 0L,
        endAt = 0L,
        activeSec = 600,
        pagesConfirmed = 10,
        creditedPages = 10.0,
        xpBase = xp,
        xpBonus = 0L,
        xpAchievements = 0L,
        totalXpGained = xp,
        flagged = flagged,
        isRecord = 0,
        shown = 1
    )

    /** Same comparison the banner relies on: strictly above every comparable past session. */
    private fun isNewBest(current: ReadingSessionEntity, past: List<ReadingSessionEntity>): Boolean {
        val baseline = sessionRecordBaseline(past, current.id)
        return current.totalXpGained > 0 && current.totalXpGained > (baseline.maxOfOrNull { it.totalXpGained } ?: 0L)
    }

    // ---------------- record logic ----------------

    @Test fun baseline_excludes_flagged_sessions() {
        val past = listOf(session("a", 100), session("b", 500, flagged = 1), session("c", 80))
        assertEquals(listOf("a", "c"), sessionRecordBaseline(past, "current").map { it.id })
    }

    @Test fun baseline_excludes_the_session_itself() {
        val past = listOf(session("current", 999), session("a", 100))
        assertEquals(listOf("a"), sessionRecordBaseline(past, "current").map { it.id })
    }

    @Test fun flagged_high_score_does_not_block_a_real_record() {
        val past = listOf(session("a", 100), session("flaggedHuge", 900, flagged = 1))
        assertTrue(isNewBest(session("current", 150), past))
    }

    @Test fun not_a_record_when_a_clean_session_was_better() {
        val past = listOf(session("a", 300), session("b", 100))
        assertFalse(isNewBest(session("current", 250), past))
    }

    @Test fun tie_is_not_a_record() {
        assertFalse(isNewBest(session("current", 300), listOf(session("a", 300))))
    }

    @Test fun first_ever_session_with_xp_is_a_record_but_zero_xp_never_is() {
        assertTrue(isNewBest(session("current", 40), emptyList()))
        assertFalse(isNewBest(session("current", 0), emptyList()))
    }

    // ---------------- ETA gate ----------------

    @Test fun eta_needs_three_sessions() {
        assertFalse(sessionEtaHasEnoughData(0))
        assertFalse(sessionEtaHasEnoughData(1))
        assertFalse(sessionEtaHasEnoughData(2))
        assertTrue(sessionEtaHasEnoughData(3))
        assertTrue(sessionEtaHasEnoughData(12))
    }

    @Test fun eta_unknown_count_defers_to_the_calculator() {
        assertTrue(sessionEtaHasEnoughData(-1))
    }

    // ---------------- streak milestones ----------------

    @Test fun next_streak_milestone() {
        assertEquals(3, daysToNextStreakMilestone(0))
        assertEquals(4, daysToNextStreakMilestone(3))   // next is 7
        assertEquals(1, daysToNextStreakMilestone(6))
        assertEquals(1, daysToNextStreakMilestone(364))
        assertNull(daysToNextStreakMilestone(365))
        assertNull(daysToNextStreakMilestone(1000))
    }

    // ---------------- encouragement ----------------

    @Test fun encouragement_is_stable_for_a_session() {
        val first = ssEncouragementIndex("session_book_1700000000000")
        repeat(20) { assertEquals(first, ssEncouragementIndex("session_book_1700000000000")) }
    }

    @Test fun encouragement_is_in_range_and_varied() {
        val seen = (0 until 300).map { ssEncouragementIndex("session_$it") }
        assertTrue(seen.all { it in 0 until SS_ENCOURAGE_VARIANTS })
        assertTrue("expected at least 4 distinct lines, got ${seen.toSet().size}", seen.toSet().size >= 4)
    }
}

package tn.keeftalk.chat.domain.xp

import android.view.MotionEvent
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.sqrt

object ReadingXp {
    const val BASE_XP = 50.0
    const val COOLDOWN_S = 6.5
    const val FULL_CREDIT_S = 30.0
    const val QUALIFY_PAGES = 20.0
    const val SOFT_CAP = 300.0
    const val HARD_CAP = 600.0
    const val OVER_RATE = 0.25
    const val STREAK_CAP = 100
    const val WINDOW = 28
    const val W_DENSITY = 0.5
    const val W_STREAK = 1.0
    const val EPUB_CHARS_PER_PAGE = 1800.0
    const val MAX_ACTIVE_SEC_PER_PAGE = 120
    const val MAX_ACTIVE_SEC_PER_DAY = 8 * 3600

    data class DayRecord(val credited: Double, val flagged: Boolean = false)
    data class Settled(val baseXp: Long, val bonusXp: Long) { val total get() = baseXp + bonusXp }

    fun pageCredit(dwellSec: Double, contentFactor: Double = 1.0): Double =
        ((dwellSec - COOLDOWN_S) / (FULL_CREDIT_S - COOLDOWN_S)).coerceIn(0.0, 1.0) *
            contentFactor.coerceIn(0.0, 1.0)

    fun qualifies(credited: Double) = credited >= QUALIFY_PAGES

    fun multiplier(qualifying: Boolean, density: Double, streak: Int): Double =
        if (!qualifying) 1.0
        else 1.0 + W_DENSITY * density.coerceIn(0.0, 1.0) +
            W_STREAK * minOf(streak, STREAK_CAP) / STREAK_CAP.toDouble()

    private fun effectivePages(credited: Double): Double {
        val c = minOf(credited, HARD_CAP)
        return minOf(c, SOFT_CAP) + OVER_RATE * maxOf(c - SOFT_CAP, 0.0)
    }

    fun dayXp(credited: Double, qualifying: Boolean, density: Double, streak: Int): Settled {
        val base = effectivePages(credited) * BASE_XP
        val total = base * multiplier(qualifying, density, streak)
        val b = base.toLong()
        return Settled(b, total.toLong() - b)
    }

    /** Deterministic; every day's multiplier depends only on days <= that day. */
    fun settle(days: Map<LocalDate, DayRecord>): Settled {
        val q = days.filterValues { qualifies(it.credited) && !it.flagged }.keys
        var base = 0L; var bonus = 0L
        for ((date, rec) in days.toSortedMap()) {
            val isQ = date in q
            var streak = 0
            var d = date
            while (isQ && d in q) { streak++; d = d.minusDays(1) }
            val density = (0 until WINDOW).count { date.minusDays(it.toLong()) in q } / WINDOW.toDouble()
            val r = dayXp(rec.credited, isQ, density, streak)
            base += r.baseXp; bonus += r.bonusXp
        }
        return Settled(base, bonus)
    }
}

object TapGuard {
    private fun std(v: List<Float>): Double {
        if (v.isEmpty()) return 0.0
        val m = v.average()
        return sqrt(v.map { (it - m).toDouble().pow(2) }.average())
    }

    /** Last 20 accepted confirmations. Starting thresholds, keep them as named constants. */
    fun looksAutomated(dwellSec: List<Double>, xs: List<Float>, ys: List<Float>): Boolean {
        if (dwellSec.size < 20) return false
        val mean = dwellSec.average()
        if (mean <= 0.0) return true
        val cv = sqrt(dwellSec.map { (it - mean).pow(2) }.average()) / mean
        return cv < 0.08 || maxOf(std(xs), std(ys)) < 1.5
    }

    fun isObscured(e: MotionEvent): Boolean {
        val flagPartiallyObscured = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED
        } else {
            0x00000002
        }
        return (e.flags and (MotionEvent.FLAG_WINDOW_IS_OBSCURED or flagPartiallyObscured)) != 0
    }
}

package tn.keeftalk.chat.domain.prayer

import java.util.Calendar
import java.util.Date
import java.util.Locale

object HijriCalendar {

    private val HIJRI_MONTH_NAMES = arrayOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    data class HijriDate(
        val day: Int,
        val month: Int,
        val monthName: String,
        val year: Int
    ) {
        fun format(): String = "$day $monthName $year AH"
    }

    /**
     * Converts a Gregorian Date to Hijri Date using Tabular Islamic Calendar algorithm.
     */
    fun getHijriDate(date: Date = Date()): HijriDate {
        val cal = Calendar.getInstance().apply { time = date }
        val dayG = cal.get(Calendar.DAY_OF_MONTH)
        val monthG = cal.get(Calendar.MONTH) + 1
        val yearG = cal.get(Calendar.YEAR)

        var m = monthG
        var y = yearG
        if (m < 3) {
            y -= 1
            m += 12
        }

        val a = Math.floor(y / 100.0)
        val b = 2 - a + Math.floor(a / 4.0)
        val julianDay = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + dayG + b - 1524.5

        val z = julianDay - 1948439.5
        val cyc = Math.floor(z / 10631.0)
        val r = z - cyc * 10631.0
        val yearH = Math.floor((r - 0.1388) / 354.366) + cyc * 30 + 1
        val dayOfYearH = r - Math.floor(yearH * 354.366 - cyc * 10631.0 - 354.366 + 0.1388)
        val monthH = Math.min(12, Math.max(1, Math.ceil((dayOfYearH - 0.5) / 29.5).toInt()))
        val dayH = Math.max(1, (dayOfYearH - Math.floor((monthH - 1) * 29.5) + 1).toInt())

        val monthName = HIJRI_MONTH_NAMES.getOrElse(monthH - 1) { "Shawwal" }

        return HijriDate(
            day = dayH,
            month = monthH,
            monthName = monthName,
            year = yearH.toInt()
        )
    }
}

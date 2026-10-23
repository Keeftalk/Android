package tn.keeftalk.chat.extension.calculator

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

enum class CalculationMethod {
    MWL,      // Muslim World League (Fajr 18°, Isha 17°)
    ISNA,     // Islamic Society of North America (Fajr 15°, Isha 15°)
    EGYPT,    // Egyptian General Authority of Survey (Fajr 19.5°, Isha 17.5°)
    MAKKAH,   // Umm al-Qura University, Makkah (Fajr 18.5°, Isha 90 mins after Maghrib)
    KARACHI,  // University of Islamic Sciences, Karachi (Fajr 18°, Isha 18°)
    TUNIS     // Tunisian Ministry of Religious Affairs (Fajr 18°, Isha 18°)
}

enum class AsrJuristicMethod {
    STANDARD, // Shafi, Maliki, Hanbali (Shadow length = 1 + object height)
    HANAFI    // Hanafi (Shadow length = 2 + object height)
}

data class PrayerTimesResult(
    val fajr: Date,
    val sunrise: Date,
    val dhuhr: Date,
    val asr: Date,
    val maghrib: Date,
    val isha: Date,
    val timezoneOffsetHours: Double
)

class SalatCalculator(
    private val latitude: Double,
    private val longitude: Double,
    private val elevation: Double = 0.0,
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val method: CalculationMethod = CalculationMethod.TUNIS,
    private val juristic: AsrJuristicMethod = AsrJuristicMethod.STANDARD
) {

    private data class Angles(val fajrAngle: Double, val ishaAngle: Double, val ishaMinutes: Int? = null)

    private fun getMethodAngles(): Angles {
        return when (method) {
            CalculationMethod.MWL -> Angles(18.0, 17.0)
            CalculationMethod.ISNA -> Angles(15.0, 15.0)
            CalculationMethod.EGYPT -> Angles(19.5, 17.5)
            CalculationMethod.MAKKAH -> Angles(18.5, 0.0, 90)
            CalculationMethod.KARACHI -> Angles(18.0, 18.0)
            CalculationMethod.TUNIS -> Angles(18.0, 18.0)
        }
    }

    fun calculatePrayerTimes(date: Date): PrayerTimesResult {
        val cal = Calendar.getInstance(timeZone).apply { time = date }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val tzOffset = timeZone.getOffset(date.time) / 3600000.0

        // Julian Date
        val jd = calcJulianDate(year, month, day)

        // Sun coordinates
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l)))) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))

        val noon = fixHour(12.0 - longitude / 15.0 - eqt + tzOffset)

        val angles = getMethodAngles()

        val dhuhrTime = noon
        val fajrZ = 90.0 + angles.fajrAngle
        val ishaZ = if (angles.ishaAngle > 0) 90.0 + angles.ishaAngle else 0.0

        val fajrTime = noon - sunAngleTime(fajrZ, decl, latitude)
        val sunriseTime = noon - sunAngleTime(90.833 + 0.0347 * sqrt(elevation), decl, latitude)
        val sunsetTime = noon + sunAngleTime(90.833 + 0.0347 * sqrt(elevation), decl, latitude)

        val asrShadowFactor = if (juristic == AsrJuristicMethod.HANAFI) 2.0 else 1.0
        val asrTime = noon + asrAngleTime(asrShadowFactor, decl, latitude)

        val maghribTime = sunsetTime
        val ishaTime = if (angles.ishaMinutes != null) {
            sunsetTime + angles.ishaMinutes / 60.0
        } else {
            noon + sunAngleTime(ishaZ, decl, latitude)
        }

        return PrayerTimesResult(
            fajr = timeToDate(date, fajrTime, timeZone),
            sunrise = timeToDate(date, sunriseTime, timeZone),
            dhuhr = timeToDate(date, dhuhrTime, timeZone),
            asr = timeToDate(date, asrTime, timeZone),
            maghrib = timeToDate(date, maghribTime, timeZone),
            isha = timeToDate(date, ishaTime, timeZone),
            timezoneOffsetHours = tzOffset
        )
    }

    private fun calcJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunAngleTime(zenithAngle: Double, decl: Double, lat: Double): Double {
        val radLat = Math.toRadians(lat)
        val radDecl = Math.toRadians(decl)
        val radZenith = Math.toRadians(zenithAngle)
        val num = cos(radZenith) - sin(radLat) * sin(radDecl)
        val den = cos(radLat) * cos(radDecl)
        if (abs(den) < 1e-9) return 0.0
        val ratio = (num / den).coerceIn(-1.0, 1.0)
        val hours = Math.toDegrees(acos(ratio)) / 15.0
        return hours
    }

    private fun asrAngleTime(factor: Double, decl: Double, lat: Double): Double {
        val radLat = Math.toRadians(lat)
        val radDecl = Math.toRadians(decl)
        val angle = Math.toDegrees(atan(1.0 / (factor + tan(abs(radLat - radDecl)))))
        return sunAngleTime(90.0 - angle, decl, lat)
    }

    private fun fixAngle(a: Double): Double {
        var angle = a % 360.0
        if (angle < 0) angle += 360.0
        return angle
    }

    private fun fixHour(h: Double): Double {
        var hour = h % 24.0
        if (hour < 0) hour += 24.0
        return hour
    }

    private fun timeToDate(baseDate: Date, hours: Double, tz: TimeZone): Date {
        val cal = Calendar.getInstance(tz).apply {
            time = baseDate
            val h = hours.toInt()
            val m = ((hours - h) * 60).toInt()
            val s = (((hours - h) * 60 - m) * 60).toInt()
            set(Calendar.HOUR_OF_DAY, h.coerceIn(0, 23))
            set(Calendar.MINUTE, m.coerceIn(0, 59))
            set(Calendar.SECOND, s.coerceIn(0, 59))
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }
}

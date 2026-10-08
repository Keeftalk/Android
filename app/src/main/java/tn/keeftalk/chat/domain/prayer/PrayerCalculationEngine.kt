package tn.keeftalk.chat.domain.prayer

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object PrayerCalculationEngine {

    data class CalculatedTimings(
        val fajr: Date,
        val sunrise: Date,
        val dhuhr: Date,
        val asr: Date,
        val maghrib: Date,
        val isha: Date
    )

    fun calculateTimes(
        latitude: Double,
        longitude: Double,
        date: Date = Date(),
        timeZone: TimeZone = TimeZone.getDefault(),
        method: CalculationMethod = CalculationMethod.MWL,
        asrMadhhab: AsrMadhhab = AsrMadhhab.SHAFI,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDNIGHT
    ): CalculatedTimings {
        val cal = Calendar.getInstance(timeZone).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val julianDate = getJulianDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )

        val tzOffsetHours = timeZone.getOffset(cal.timeInMillis) / 3600000.0

        // Sun parameters
        val (dhuhrHour, declination, eqTime) = calculateSolarTransits(julianDate, longitude, tzOffsetHours)

        // Sunrise and Sunset (zenith = 90.833 degrees)
        val sunriseHourAngle = calculateHourAngle(latitude, declination, 90.833)
        val sunriseHour = dhuhrHour - sunriseHourAngle / 15.0
        val sunsetHour = dhuhrHour + sunriseHourAngle / 15.0

        // Fajr (angle from method)
        var fajrHourAngle = calculateHourAngle(latitude, declination, 90.0 + method.fajrAngle)
        if (fajrHourAngle.isNaN()) {
            fajrHourAngle = adjustHighLatitudeHourAngle(sunriseHourAngle, method.fajrAngle, highLatitudeRule)
        }
        val fajrHour = dhuhrHour - fajrHourAngle / 15.0

        // Asr (shadow factor = 1 for Shafi, 2 for Hanafi)
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val asrAltitude = Math.toDegrees(atan(1.0 / (asrMadhhab.shadowFactor + tan(abs(latRad - decRad)))))
        val asrHourAngle = calculateHourAngle(latitude, declination, 90.0 - asrAltitude)
        val asrHour = dhuhrHour + asrHourAngle / 15.0

        // Maghrib = Sunset
        val maghribHour = sunsetHour

        // Isha
        val ishaHour = if (method.ishaIntervalMinutes > 0) {
            maghribHour + (method.ishaIntervalMinutes / 60.0)
        } else {
            var ishaHA = calculateHourAngle(latitude, declination, 90.0 + method.ishaAngle)
            if (ishaHA.isNaN()) {
                ishaHA = adjustHighLatitudeHourAngle(sunriseHourAngle, method.ishaAngle, highLatitudeRule)
            }
            dhuhrHour + ishaHA / 15.0
        }

        return CalculatedTimings(
            fajr = hourToDate(cal, fajrHour),
            sunrise = hourToDate(cal, sunriseHour),
            dhuhr = hourToDate(cal, dhuhrHour),
            asr = hourToDate(cal, asrHour),
            maghrib = hourToDate(cal, maghribHour),
            isha = hourToDate(cal, ishaHour)
        )
    }

    private fun getJulianDate(year: Int, month: Int, day: Int): Double {
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

    private data class SolarParams(val dhuhr: Double, val declination: Double, val eqTime: Double)

    private fun calculateSolarTransits(jd: Double, longitude: Double, tzOffsetHours: Double): SolarParams {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))
        val raFixed = fixAngle(ra) / 15.0

        val declination = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        val eqTime = (q / 15.0) - raFixed

        val dhuhr = fixHour(12.0 + tzOffsetHours - (longitude / 15.0) - eqTime)
        return SolarParams(dhuhr, declination, eqTime)
    }

    private fun calculateHourAngle(latitude: Double, declination: Double, zenith: Double): Double {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val zenRad = Math.toRadians(zenith)

        val cosH = (cos(zenRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        return if (cosH in -1.0..1.0) Math.toDegrees(acos(cosH)) else Double.NaN
    }

    private fun adjustHighLatitudeHourAngle(
        sunriseHA: Double,
        angle: Double,
        rule: HighLatitudeRule
    ): Double {
        val portion = when (rule) {
            HighLatitudeRule.MIDNIGHT -> 0.5
            HighLatitudeRule.ONE_THIRD -> 1.0 / 3.0
            HighLatitudeRule.ANGLE_BASED -> angle / 60.0
            else -> 0.5
        }
        val nightPortion = portion * (24.0 - 2 * (sunriseHA / 15.0)) * 15.0
        return nightPortion
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour % 24.0
        if (h < 0) h += 24.0
        return h
    }

    private fun hourToDate(baseCal: Calendar, hour: Double): Date {
        val cal = baseCal.clone() as Calendar
        val hours = floor(hour).toInt()
        val minutesDouble = (hour - hours) * 60.0
        val minutes = floor(minutesDouble).toInt()
        val seconds = floor((minutesDouble - minutes) * 60.0).toInt()

        cal.set(Calendar.HOUR_OF_DAY, hours % 24)
        cal.set(Calendar.MINUTE, minutes % 60)
        cal.set(Calendar.SECOND, seconds % 60)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun formatTime(date: Date): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
}

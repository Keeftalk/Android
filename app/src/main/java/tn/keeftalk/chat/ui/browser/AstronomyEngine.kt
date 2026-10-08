package tn.keeftalk.chat.ui.browser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.Date
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

data class HorizPos(
    val alt: Double,
    val az: Double,
    val maxAlt: Double = 90.0
)

data class SunTimesResult(
    val rise: Date?,
    val set: Date?,
    val noon: Date?
)

object AstronomyEngine {
    private const val RAD = PI / 180.0

    // Default location: Tunisia latitude 35.0, longitude calculated from timezone
    var userLat = 35.0
    var userLon = -(Calendar.getInstance().timeZone.rawOffset / (1000.0 * 60.0 * 60.0) * 15.0)

    fun jd(d: Date): Double {
        return d.time / 864e5 + 2440587.5
    }

    private fun eq(lon: Double, lat: Double, e: Double): Pair<Double, Double> {
        val sl = sin(lon * RAD)
        val cl = cos(lon * RAD)
        val se = sin(e * RAD)
        val ce = cos(e * RAD)
        val ra = atan2(sl * ce - tan(lat * RAD) * se, cl) / RAD
        val dec = asin(sin(lat * RAD) * ce + cos(lat * RAD) * se * sl) / RAD
        return Pair(ra, dec)
    }

    fun horiz(ra: Double, dec: Double, D: Double, lat: Double = userLat, lon: Double = userLon): HorizPos {
        val H = (280.46061837 + 360.98564736629 * D + lon - ra) * RAD
        val la = lat * RAD
        val dc = dec * RAD
        val alt = asin(sin(la) * sin(dc) + cos(la) * cos(dc) * cos(H))
        val az = atan2(-sin(H) * cos(dc), cos(la) * sin(dc) - sin(la) * cos(dc) * cos(H))
        val azDeg = (az / RAD + 360.0) % 360.0
        val maxA = max(8.0, 90.0 - abs(lat - dec))
        return HorizPos(alt / RAD, azDeg, maxA)
    }

    private fun sunLon(D: Double): Double {
        val g = (357.528 + 0.9856003 * D) * RAD
        return 280.46 + 0.9856474 * D + 1.915 * sin(g) + 0.02 * sin(2.0 * g)
    }

    private fun moonEcl(D: Double): Pair<Double, Double> {
        val M = (134.963 + 13.064993 * D) * RAD
        val F = (93.272 + 13.22935 * D) * RAD
        val lon = 218.316 + 13.176396 * D + 6.289 * sin(M)
        val lat = 5.128 * sin(F)
        return Pair(lon, lat)
    }

    fun sunPos(d: Date, lat: Double = userLat, lon: Double = userLon): HorizPos {
        val D = jd(d) - 2451545.0
        val e = 23.439 - 4e-7 * D
        val r = eq(sunLon(D), 0.0, e)
        return horiz(r.first, r.second, D, lat, lon)
    }

    fun moonPos(d: Date, lat: Double = userLat, lon: Double = userLon): HorizPos {
        val D = jd(d) - 2451545.0
        val e = 23.439 - 4e-7 * D
        val m = moonEcl(D)
        val r = eq(m.first, m.second, e)
        return horiz(r.first, r.second, D, lat, lon)
    }

    fun elong(d: Date): Double {
        val D = jd(d) - 2451545.0
        return (((moonEcl(D).first - sunLon(D)) % 360.0) + 360.0) % 360.0
    }

    fun phaseName(e: Double): String {
        return when {
            e < 22.5 || e >= 337.5 -> "New moon"
            e < 67.5 -> "Waxing crescent"
            e < 112.5 -> "First quarter"
            e < 157.5 -> "Waxing gibbous"
            e < 202.5 -> "Full moon"
            e < 247.5 -> "Waning gibbous"
            e < 292.5 -> "Last quarter"
            else -> "Waning crescent"
        }
    }

    fun sunTimes(d: Date, lat: Double = userLat, lon: Double = userLon): SunTimesResult {
        val cal = Calendar.getInstance().apply {
            time = d
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t0 = cal.timeInMillis
        var rise: Date? = null
        var set: Date? = null
        var noon: Date? = null
        var best = -99.0
        var prev: Double? = null

        var m = 0
        while (m <= 1440) {
            val x = Date(t0 + m * 60000L)
            val a = sunPos(x, lat, lon).alt
            if (prev != null) {
                if (prev < -0.83 && a >= -0.83 && rise == null) rise = x
                if (prev >= -0.83 && a < -0.83 && set == null) set = x
            }
            if (a > best) {
                best = a
                noon = x
            }
            prev = a
            m += 4
        }
        return SunTimesResult(rise, set, noon)
    }

    fun simFor(period: String, baseDate: Date = Date(), lat: Double = userLat, lon: Double = userLon): Date {
        val s = sunTimes(baseDate, lat, lon)
        val cal = Calendar.getInstance().apply {
            time = baseDate
        }
        val at = { hour: Int ->
            cal.apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.time
        }
        val offset = { dt: Date, minOffset: Long -> Date(dt.time + minOffset * 60000L) }

        return when (period) {
            "dawn" -> s.rise?.let { offset(it, 14) } ?: at(6)
            "dusk" -> s.set?.let { offset(it, -14) } ?: at(18)
            "night" -> s.set?.let { offset(it, 150) } ?: at(22)
            else -> s.noon ?: at(13)
        }
    }

    fun periodFor(d: Date = Date(), lat: Double = userLat, lon: Double = userLon): String {
        val a = sunPos(d, lat, lon)
        return when {
            a.alt > 8.0 -> "day"
            a.alt < -6.0 -> "night"
            a.az < 180.0 -> "dawn"
            else -> "dusk"
        }
    }

    // Sky colors interpolation logic matching JS SM/SE color stops
    private val SM = listOf(
        Pair(-18.0, Pair(Color(0xFF050818), Color(0xFF0e1640))),
        Pair(-8.0, Pair(Color(0xFF0b1230), Color(0xFF26366e))),
        Pair(-2.0, Pair(Color(0xFF3a3f86), Color(0xFFffa882))),
        Pair(4.0, Pair(Color(0xFF8aa7e0), Color(0xFFffd6a8))),
        Pair(16.0, Pair(Color(0xFF7cc4f2), Color(0xFFe3f4ff)))
    )

    private val SE = listOf(
        Pair(-18.0, Pair(Color(0xFF050818), Color(0xFF0e1640))),
        Pair(-8.0, Pair(Color(0xFF0b1230), Color(0xFF26366e))),
        Pair(-2.0, Pair(Color(0xFF4d3f95), Pair(Color(0xFFf58a6a), Color(0xFFf58a6a)).first)),
        Pair(-2.0, Pair(Color(0xFF4d3f95), Color(0xFFf58a6a))),
        Pair(4.0, Pair(Color(0xFF6a5aa8), Color(0xFFffb08a))),
        Pair(16.0, Pair(Color(0xFF7cc4f2), Color(0xFFe3f4ff)))
    )

    private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
        val clampedT = t.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * clampedT,
            green = c1.green + (c2.green - c1.green) * clampedT,
            blue = c1.blue + (c2.blue - c1.blue) * clampedT,
            alpha = c1.alpha + (c2.alpha - c1.alpha) * clampedT
        )
    }

    fun skyColors(alt: Double, isAM: Boolean): Pair<Color, Color> {
        val stops = if (isAM) SM else SE
        if (alt <= stops.first().first) return stops.first().second
        for (i in 1 until stops.size) {
            if (alt <= stops[i].first) {
                val a = stops[i - 1]
                val b = stops[i]
                val t = ((alt - a.first) / (b.first - a.first)).toFloat().coerceIn(0f, 1f)
                val top = lerpColor(a.second.first, b.second.first, t)
                val bot = lerpColor(a.second.second, b.second.second, t)
                return Pair(top, bot)
            }
        }
        return stops.last().second
    }

    fun celestialPosition(
        widthPx: Float,
        heightPx: Float,
        alt: Double,
        az: Double,
        maxAlt: Double,
        orbSizePx: Float = 84f
    ): Offset {
        val k = (alt / maxAlt).coerceIn(0.0, 1.0)
        val bowSign = if (az < 180.0) -1.0 else 1.0
        val bow = bowSign * sin(PI * min(1.0, abs(k))) * min(46.0, widthPx * 0.1)
        val x0 = 14f
        val y0 = 372f
        val x1 = widthPx - 14f - orbSizePx
        val y1 = 64f
        val x = (x0 + (x1 - x0) * k + bow * 0.7).toFloat()
        val y = (y0 + (y1 - y0) * k + bow * 0.7 - orbSizePx / 2f).toFloat()
        return Offset(x, y)
    }
}

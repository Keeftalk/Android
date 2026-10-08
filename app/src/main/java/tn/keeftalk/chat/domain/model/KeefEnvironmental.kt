package tn.keeftalk.chat.domain.model

import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos

enum class PeriodOfDay {
    DAWN, // Sunrise / warm dawn atmosphere
    DAY,  // Daylight atmosphere
    DUSK, // Sunset / warm dusk atmosphere
    NIGHT // Dark night atmosphere with stars & moon
}

enum class GroundType {
    FOREST, // Pine trees, forest landscape
    CITY,   // City buildings, towers, skyline with lit windows
    SAHARA, // Desert sand dunes, warm ridges
    GREEN   // Rolling green hills & meadow landscape
}

enum class WeatherConditionType {
    CLEAR,
    CLOUDY,
    RAIN,
    STORM,
    SNOW,
    FOG
}

data class MoonState(
    val phaseName: String,
    val illumination: Float, // 0.0f (New Moon) to 1.0f (Full Moon)
    val phaseProgress: Float, // 0.0f to 1.0f across lunar cycle
    val isVisible: Boolean
)

data class EnvironmentalState(
    val period: PeriodOfDay = PeriodOfDay.DAY,
    val weatherCondition: WeatherConditionType = WeatherConditionType.CLEAR,
    val temperatureCelsius: Float = 22f,
    val weatherDescription: String = "Clear sky",
    val locationName: String = "Locating…",
    val isDay: Boolean = true,
    val cloudCover: Int = 10,
    val precipitation: Float = 0f,
    val windSpeed: Float = 5f,
    val moonState: MoonState = MoonState("Full Moon", 1.0f, 0.5f, false),
    val sunProgress: Float = 0.5f, // 0.0 (sunrise/dawn) to 1.0 (sunset/dusk)
    val moonProgress: Float = 0.5f, // 0.0 (moonrise/sunset) to 1.0 (moonset/sunrise)
    val groundType: GroundType = GroundType.GREEN,
    val lightPollution: Int = 30, // 0 (pristine dark sky) to 100% (city light pollution)
    val tempMax: Float = 25f,
    val tempMin: Float = 15f,
    val hourlyTemps: List<Double> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isStale: Boolean = false,
    val cloudSmoothness: Float = 0.8f, // 0.0f to 1.0f
    val starsDensity: Int = 60, // 0 to 100%
    val rainDensity: Int = 25, // 0 to 100%
    val thunderFrequency: Int = 0, // 0 to 100%
    val dayOfMonth: Int = 15, // 1 to 31
    val isSimulated: Boolean = false
) {
    companion object {
        fun default(): EnvironmentalState = EnvironmentalState()
    }
}

object EnvironmentalCalculators {

    // Synodic month in milliseconds: 29.53058867 days
    private const val SYNODIC_MONTH_MS = 29.53058867 * 24.0 * 60.0 * 60.0 * 1000.0
    // Known reference New Moon date: Jan 11, 2024 11:57 UTC
    private const val REF_NEW_MOON_MS = 1704974220000L

    fun calculateMoonState(timeMillis: Long = System.currentTimeMillis(), isNightTime: Boolean = false): MoonState {
        val diff = timeMillis - REF_NEW_MOON_MS
        val cycles = diff / SYNODIC_MONTH_MS
        val progress = (cycles - Math.floor(cycles)).toFloat() // 0.0 to 1.0

        val illumination = (0.5f * (1f - cos(2f * PI.toFloat() * progress))).coerceIn(0f, 1f)

        val name = when {
            progress < 0.03f || progress > 0.97f -> "New Moon"
            progress in 0.03f..0.22f -> "Waxing Crescent"
            progress in 0.22f..0.28f -> "First Quarter"
            progress in 0.28f..0.47f -> "Waxing Gibbous"
            progress in 0.47f..0.53f -> "Full Moon"
            progress in 0.53f..0.72f -> "Waning Gibbous"
            progress in 0.72f..0.78f -> "Last Quarter"
            else -> "Waning Crescent"
        }

        return MoonState(
            phaseName = name,
            illumination = illumination,
            phaseProgress = progress,
            isVisible = isNightTime || progress in 0.2f..0.8f
        )
    }

    fun calculatePeriodOfDay(
        timeMillis: Long = System.currentTimeMillis(),
        sunriseStr: String? = null,
        sunsetStr: String? = null
    ): PeriodOfDay {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val sunriseMin = parseTimeToMinutes(sunriseStr) ?: (6 * 60 + 30) // Default 06:30
        val sunsetMin = parseTimeToMinutes(sunsetStr) ?: (18 * 60 + 30) // Default 18:30

        val dawnStart = (sunriseMin - 45).coerceAtLeast(0)
        val dawnEnd = sunriseMin + 45
        val duskStart = sunsetMin - 45
        val duskEnd = (sunsetMin + 45).coerceAtMost(24 * 60)

        return when {
            currentMinutes in dawnStart..dawnEnd -> PeriodOfDay.DAWN
            currentMinutes in (dawnEnd + 1)..<duskStart -> PeriodOfDay.DAY
            currentMinutes in duskStart..duskEnd -> PeriodOfDay.DUSK
            else -> PeriodOfDay.NIGHT
        }
    }

    fun calculateSunProgress(
        timeMillis: Long = System.currentTimeMillis(),
        sunriseStr: String? = null,
        sunsetStr: String? = null
    ): Float {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val sunriseMin = parseTimeToMinutes(sunriseStr) ?: (6 * 60 + 30)
        val sunsetMin = parseTimeToMinutes(sunsetStr) ?: (18 * 60 + 30)

        if (currentMinutes < sunriseMin || currentMinutes > sunsetMin) {
            return 0f
        }
        val dayDuration = (sunsetMin - sunriseMin).toFloat().coerceAtLeast(1f)
        return ((currentMinutes - sunriseMin) / dayDuration).coerceIn(0f, 1f)
    }

    fun calculateMoonProgress(
        timeMillis: Long = System.currentTimeMillis(),
        sunriseStr: String? = null,
        sunsetStr: String? = null
    ): Float {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val sunriseMin = parseTimeToMinutes(sunriseStr) ?: (6 * 60 + 30)
        val sunsetMin = parseTimeToMinutes(sunsetStr) ?: (18 * 60 + 30)

        val nightSpan = (24 * 60 - sunsetMin + sunriseMin).toFloat().coerceAtLeast(1f)
        return if (currentMinutes >= sunsetMin) {
            val elapsed = currentMinutes - sunsetMin
            (elapsed / nightSpan).coerceIn(0f, 1f)
        } else if (currentMinutes <= sunriseMin) {
            val elapsed = 24 * 60 - sunsetMin + currentMinutes
            (elapsed / nightSpan).coerceIn(0f, 1f)
        } else {
            val daySpan = (sunsetMin - sunriseMin).toFloat().coerceAtLeast(1f)
            ((currentMinutes - sunriseMin) / daySpan).coerceIn(0f, 1f)
        }
    }

    fun inferGroundType(locationName: String?, description: String? = null): GroundType {
        val combined = "${locationName ?: ""} ${description ?: ""}".lowercase(Locale.ROOT)
        return when {
            combined.contains("sahara") || combined.contains("desert") || combined.contains("dune") || combined.contains("dubai") || combined.contains("cairo") -> GroundType.SAHARA
            combined.contains("forest") || combined.contains("pine") || combined.contains("wood") || combined.contains("jungle") || combined.contains("park") || combined.contains("mountain") -> GroundType.FOREST
            combined.contains("city") || combined.contains("york") || combined.contains("tokyo") || combined.contains("london") || combined.contains("paris") || combined.contains("downtown") || combined.contains("urban") || combined.contains("metropolis") -> GroundType.CITY
            else -> GroundType.GREEN
        }
    }

    fun inferLightPollution(groundType: GroundType): Int {
        return when (groundType) {
            GroundType.CITY -> 80
            GroundType.GREEN -> 30
            GroundType.FOREST -> 15
            GroundType.SAHARA -> 5
        }
    }

    fun mapWmoCodeToConditionType(wmoCode: Int): WeatherConditionType {
        return when (wmoCode) {
            0, 1 -> WeatherConditionType.CLEAR
            2, 3 -> WeatherConditionType.CLOUDY
            45, 48 -> WeatherConditionType.FOG
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherConditionType.RAIN
            71, 73, 75, 77, 85, 86 -> WeatherConditionType.SNOW
            95, 96, 99 -> WeatherConditionType.STORM
            else -> WeatherConditionType.CLEAR
        }
    }

    fun createEnvironmentalState(
        weather: WeatherInfo?,
        timeMillis: Long = System.currentTimeMillis(),
        groundTypeOverride: GroundType? = null,
        lightPollutionOverride: Int? = null
    ): EnvironmentalState {
        if (weather == null) {
            val period = calculatePeriodOfDay(timeMillis)
            val isNight = period == PeriodOfDay.NIGHT
            val ground = groundTypeOverride ?: GroundType.GREEN
            val pollution = lightPollutionOverride ?: inferLightPollution(ground)
            return EnvironmentalState(
                period = period,
                weatherCondition = WeatherConditionType.CLEAR,
                temperatureCelsius = 22f,
                weatherDescription = if (isNight) "Clear night" else "Clear sky",
                locationName = "Current Location",
                isDay = !isNight,
                cloudCover = 5,
                precipitation = 0f,
                windSpeed = 5f,
                moonState = calculateMoonState(timeMillis, isNight),
                sunProgress = calculateSunProgress(timeMillis),
                moonProgress = calculateMoonProgress(timeMillis),
                groundType = ground,
                lightPollution = pollution,
                timestamp = timeMillis,
                isStale = true
            )
        }

        val wmoCode = weather.iconCode.toIntOrNull() ?: 0
        val conditionType = mapWmoCodeToConditionType(wmoCode)
        val period = calculatePeriodOfDay(timeMillis, weather.sunrise, weather.sunset)
        val sunProgress = calculateSunProgress(timeMillis, weather.sunrise, weather.sunset)
        val moonProgress = calculateMoonProgress(timeMillis, weather.sunrise, weather.sunset)
        val isNight = period == PeriodOfDay.NIGHT || !weather.isDay
        val moonState = calculateMoonState(timeMillis, isNight)
        val ground = groundTypeOverride ?: inferGroundType(weather.locationName, weather.condition)
        val pollution = lightPollutionOverride ?: inferLightPollution(ground)

        return EnvironmentalState(
            period = period,
            weatherCondition = conditionType,
            temperatureCelsius = weather.temperature,
            weatherDescription = weather.condition.ifBlank { "Clear sky" },
            locationName = weather.locationName.ifBlank { "Current Location" },
            isDay = weather.isDay && period != PeriodOfDay.NIGHT,
            cloudCover = weather.cloudCover,
            precipitation = weather.precipitation,
            windSpeed = weather.windSpeed,
            moonState = moonState,
            sunProgress = sunProgress,
            moonProgress = moonProgress,
            groundType = ground,
            lightPollution = pollution,
            tempMax = weather.tempMax,
            tempMin = weather.tempMin,
            hourlyTemps = weather.hourlyTemps,
            timestamp = weather.timestamp,
            isStale = (System.currentTimeMillis() - weather.timestamp) > 3 * 3600 * 1000L
        )
    }

    fun createSimulatedState(
        timeOfDayHours: Float,
        cloudDensityPercent: Int,
        cloudSmoothness: Float,
        windSpeedKmH: Float,
        rainDensityPercent: Int,
        starsDensityPercent: Int,
        thunderFrequencyPercent: Int = 0,
        dayOfMonth: Int = 15,
        groundTypeOverride: GroundType? = null,
        lightPollutionOverride: Int? = null
    ): EnvironmentalState {
        val totalMinutes = (timeOfDayHours * 60f).toInt().coerceIn(0, 24 * 60 - 1)
        val cal = Calendar.getInstance().apply {
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, totalMinutes / 60)
            set(Calendar.MINUTE, totalMinutes % 60)
            set(Calendar.SECOND, 0)
        }
        val simTimeMs = cal.timeInMillis

        val sunriseMin = 6 * 60 + 30
        val sunsetMin = 18 * 60 + 30
        val dawnStart = sunriseMin - 45
        val dawnEnd = sunriseMin + 45
        val duskStart = sunsetMin - 45
        val duskEnd = sunsetMin + 45

        val period = when {
            totalMinutes in dawnStart..dawnEnd -> PeriodOfDay.DAWN
            totalMinutes in (dawnEnd + 1)..<duskStart -> PeriodOfDay.DAY
            totalMinutes in duskStart..duskEnd -> PeriodOfDay.DUSK
            else -> PeriodOfDay.NIGHT
        }

        val isNight = period == PeriodOfDay.NIGHT
        val isDay = period == PeriodOfDay.DAY || period == PeriodOfDay.DAWN

        val sunProgress = if (totalMinutes in sunriseMin..sunsetMin) {
            ((totalMinutes - sunriseMin) / (sunsetMin - sunriseMin).toFloat()).coerceIn(0f, 1f)
        } else 0f

        val moonProgress = calculateMoonProgress(simTimeMs, "06:30", "18:30")

        val condition = when {
            thunderFrequencyPercent > 20 || rainDensityPercent > 60 -> WeatherConditionType.STORM
            rainDensityPercent > 10 -> WeatherConditionType.RAIN
            cloudDensityPercent > 40 -> WeatherConditionType.CLOUDY
            else -> WeatherConditionType.CLEAR
        }

        val description = when (condition) {
            WeatherConditionType.STORM -> "Simulated Storm (${rainDensityPercent}% Rain, ${thunderFrequencyPercent}% Lightning)"
            WeatherConditionType.RAIN -> "Simulated Rain (${rainDensityPercent}%)"
            WeatherConditionType.CLOUDY -> "Simulated Cloudy (${cloudDensityPercent}%)"
            WeatherConditionType.CLEAR -> if (isNight) "Simulated Clear Night" else "Simulated Clear Sky"
            else -> "Simulated Weather"
        }

        val moonState = calculateMoonState(simTimeMs, isNight)
        val ground = groundTypeOverride ?: GroundType.GREEN
        val pollution = lightPollutionOverride ?: inferLightPollution(ground)

        return EnvironmentalState(
            period = period,
            weatherCondition = condition,
            temperatureCelsius = 20f + (if (isDay) 4f else -4f),
            weatherDescription = description,
            locationName = "Simulated Sky",
            isDay = isDay,
            cloudCover = cloudDensityPercent,
            precipitation = rainDensityPercent.toFloat(),
            windSpeed = windSpeedKmH,
            moonState = moonState,
            sunProgress = sunProgress,
            moonProgress = moonProgress,
            groundType = ground,
            lightPollution = pollution,
            cloudSmoothness = cloudSmoothness,
            starsDensity = starsDensityPercent,
            rainDensity = rainDensityPercent,
            thunderFrequency = thunderFrequencyPercent,
            dayOfMonth = dayOfMonth,
            isSimulated = true,
            timestamp = simTimeMs
        )
    }

    private fun parseTimeToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val hours = parts[0].toInt()
                val minutes = parts[1].take(2).toInt()
                hours * 60 + minutes
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

package tn.keeftalk.chat.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.ui.theme.keef.KeefThemeTokens
import java.util.Calendar

class KeefEnvironmentalTest {

    @Test
    fun testPeriodOfDayCalculation() {
        val calendar = Calendar.getInstance()

        // Midday 12:00 -> DAY
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        val dayPeriod = EnvironmentalCalculators.calculatePeriodOfDay(
            timeMillis = calendar.timeInMillis,
            sunriseStr = "06:30",
            sunsetStr = "18:30"
        )
        assertEquals(PeriodOfDay.DAY, dayPeriod)

        // Sunrise time 06:30 -> DAWN
        calendar.set(Calendar.HOUR_OF_DAY, 6)
        calendar.set(Calendar.MINUTE, 30)
        val dawnPeriod = EnvironmentalCalculators.calculatePeriodOfDay(
            timeMillis = calendar.timeInMillis,
            sunriseStr = "06:30",
            sunsetStr = "18:30"
        )
        assertEquals(PeriodOfDay.DAWN, dawnPeriod)

        // Sunset time 18:30 -> DUSK
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 30)
        val duskPeriod = EnvironmentalCalculators.calculatePeriodOfDay(
            timeMillis = calendar.timeInMillis,
            sunriseStr = "06:30",
            sunsetStr = "18:30"
        )
        assertEquals(PeriodOfDay.DUSK, duskPeriod)

        // Midnight 23:00 -> NIGHT
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 0)
        val nightPeriod = EnvironmentalCalculators.calculatePeriodOfDay(
            timeMillis = calendar.timeInMillis,
            sunriseStr = "06:30",
            sunsetStr = "18:30"
        )
        assertEquals(PeriodOfDay.NIGHT, nightPeriod)
    }

    @Test
    fun testMoonStateCalculation() {
        val moonState = EnvironmentalCalculators.calculateMoonState(
            timeMillis = System.currentTimeMillis(),
            isNightTime = true
        )

        assertNotNull(moonState.phaseName)
        assertTrue(moonState.illumination in 0.0f..1.0f)
        assertTrue(moonState.phaseProgress in 0.0f..1.0f)
    }

    @Test
    fun testMoonProgressCalculation() {
        val calendar = Calendar.getInstance()

        // Sunset 18:30 -> moon progress near 0.0
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 30)
        val sunsetProgress = EnvironmentalCalculators.calculateMoonProgress(
            timeMillis = calendar.timeInMillis,
            sunriseStr = "06:30",
            sunsetStr = "18:30"
        )
        assertEquals(0.0f, sunsetProgress, 0.05f)

        // Midnight 00:30 -> moon progress near 0.5 (apex)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 30)
        val midnightProgress = EnvironmentalCalculators.calculateMoonProgress(
            timeMillis = calendar.timeInMillis,
            sunriseStr = "06:30",
            sunsetStr = "18:30"
        )
        assertEquals(0.5f, midnightProgress, 0.1f)
    }

    @Test
    fun testGroundTypeInference() {
        assertEquals(GroundType.SAHARA, EnvironmentalCalculators.inferGroundType("Sahara Desert", "Sunny"))
        assertEquals(GroundType.FOREST, EnvironmentalCalculators.inferGroundType("Sherwood Forest", "Pine woods"))
        assertEquals(GroundType.CITY, EnvironmentalCalculators.inferGroundType("New York City", "Metropolis"))
        assertEquals(GroundType.GREEN, EnvironmentalCalculators.inferGroundType("Green Valley", "Meadow"))

        assertEquals(80, EnvironmentalCalculators.inferLightPollution(GroundType.CITY))
        assertEquals(5, EnvironmentalCalculators.inferLightPollution(GroundType.SAHARA))
    }

    @Test
    fun testWmoCodeMapping() {
        assertEquals(WeatherConditionType.CLEAR, EnvironmentalCalculators.mapWmoCodeToConditionType(0))
        assertEquals(WeatherConditionType.CLOUDY, EnvironmentalCalculators.mapWmoCodeToConditionType(3))
        assertEquals(WeatherConditionType.RAIN, EnvironmentalCalculators.mapWmoCodeToConditionType(61))
        assertEquals(WeatherConditionType.SNOW, EnvironmentalCalculators.mapWmoCodeToConditionType(71))
        assertEquals(WeatherConditionType.STORM, EnvironmentalCalculators.mapWmoCodeToConditionType(95))
        assertEquals(WeatherConditionType.FOG, EnvironmentalCalculators.mapWmoCodeToConditionType(45))
    }

    @Test
    fun testDesignTokensForEnvironment() {
        val dayState = EnvironmentalState(
            period = PeriodOfDay.DAY,
            weatherCondition = WeatherConditionType.CLEAR
        )
        val dayTokens = KeefThemeTokens.forEnvironment(dayState)
        assertNotNull(dayTokens.sky1)
        assertNotNull(dayTokens.accent)
        assertEquals(20, dayTokens.cardBlurPercent)

        val nightState = EnvironmentalState(
            period = PeriodOfDay.NIGHT,
            weatherCondition = WeatherConditionType.STORM
        )
        val nightTokens = KeefThemeTokens.forEnvironment(nightState)
        assertTrue(nightTokens.veilAlpha > 0f)
    }

    @Test
    fun testSimulatedEnvironmentalState() {
        val simState = EnvironmentalCalculators.createSimulatedState(
            timeOfDayHours = 14.5f,
            cloudDensityPercent = 350,
            cloudSmoothness = 0.9f,
            windSpeedKmH = 250f,
            rainDensityPercent = 70,
            starsDensityPercent = 80,
            thunderFrequencyPercent = 60,
            dayOfMonth = 15,
            groundTypeOverride = GroundType.CITY
        )

        assertTrue(simState.isSimulated)
        assertEquals(350, simState.cloudCover)
        assertEquals(0.9f, simState.cloudSmoothness, 0.01f)
        assertEquals(250f, simState.windSpeed, 0.01f)
        assertEquals(70, simState.rainDensity)
        assertEquals(60, simState.thunderFrequency)
        assertEquals(15, simState.dayOfMonth)
        assertEquals(80, simState.starsDensity)
        assertEquals(GroundType.CITY, simState.groundType)
        assertEquals(80, simState.lightPollution)
        assertEquals(WeatherConditionType.STORM, simState.weatherCondition)
    }
}

package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.extension.calculator.CalculationMethod
import tn.keeftalk.chat.extension.calculator.SalatCalculator
import java.util.Date
import java.util.TimeZone

class SalatCalculatorTest {

    @Test
    fun testSalatPrayerTimesCalculationForTunis() {
        val calculator = SalatCalculator(
            latitude = 36.8065,
            longitude = 10.1815,
            timeZone = TimeZone.getTimeZone("Africa/Tunis"),
            method = CalculationMethod.TUNIS
        )

        val result = calculator.calculatePrayerTimes(Date())
        println("Fajr: ${result.fajr}, Sunrise: ${result.sunrise}, Dhuhr: ${result.dhuhr}, Asr: ${result.asr}, Maghrib: ${result.maghrib}, Isha: ${result.isha}")

        assertNotNull(result.fajr)
        assertNotNull(result.sunrise)
        assertNotNull(result.dhuhr)
        assertNotNull(result.asr)
        assertNotNull(result.maghrib)
        assertNotNull(result.isha)

        assertTrue(result.fajr.before(result.sunrise))
        assertTrue(result.sunrise.before(result.dhuhr))
        assertTrue(result.dhuhr.before(result.asr))
        assertTrue(result.asr.before(result.maghrib))
        assertTrue(result.maghrib.before(result.isha))
    }
}

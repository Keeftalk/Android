package tn.keeftalk.chat.domain.prayer

enum class CalculationMethod(
    val title: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int = 0
) {
    MWL("Muslim World League", 18.0, 17.0),
    EGYPTIAN("Egyptian General Authority", 19.5, 17.5),
    KARACHI("Univ. of Islamic Sciences, Karachi", 18.0, 18.0),
    UMMALQURA("Umm al-Qura University", 18.5, 0.0, 90),
    ISNA("ISNA (North America)", 15.0, 15.0),
    DUBAI("Dubai", 18.2, 18.2),
    QATAR("Qatar", 18.0, 0.0, 90),
    KUWAIT("Kuwait", 18.0, 17.5),
    MOONSIGHTING("Moonsighting Committee", 18.0, 18.0);

    companion object {
        fun fromName(name: String): CalculationMethod {
            return values().find { it.name.equals(name, ignoreCase = true) || it.title.equals(name, ignoreCase = true) } ?: MWL
        }
    }
}

enum class AsrMadhhab(val title: String, val shadowFactor: Int) {
    SHAFI("Standard (Shafi'i, Maliki, Hanbali)", 1),
    HANAFI("Hanafi", 2);

    companion object {
        fun fromName(name: String): AsrMadhhab {
            return values().find { it.name.equals(name, ignoreCase = true) || it.title.equals(name, ignoreCase = true) } ?: SHAFI
        }
    }
}

enum class HighLatitudeRule {
    NONE,
    MIDNIGHT,
    ONE_THIRD,
    ANGLE_BASED
}

data class PrayerTime(
    val name: String,
    val timeFormatted: String, // HH:mm
    val timestampMs: Long,
    val isPassed: Boolean = false,
    val isNext: Boolean = false
)

data class DailyPrayerSchedule(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val hijriDateFormatted: String,
    val gregorianDateFormatted: String,
    val prayerList: List<PrayerTime>
)

data class PrayerReminderSetting(
    val fajrEnabled: Boolean = true,
    val dhuhrEnabled: Boolean = true,
    val asrEnabled: Boolean = true,
    val maghribEnabled: Boolean = true,
    val ishaEnabled: Boolean = true
)

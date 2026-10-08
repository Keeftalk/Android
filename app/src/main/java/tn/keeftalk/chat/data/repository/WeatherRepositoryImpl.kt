package tn.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.prefs.UserPreferencesRepository
import tn.keeftalk.chat.domain.model.EnvironmentalCalculators
import tn.keeftalk.chat.domain.model.EnvironmentalState
import tn.keeftalk.chat.domain.model.WeatherInfo
import tn.keeftalk.chat.domain.repository.FeedRepository
import tn.keeftalk.chat.domain.repository.WeatherRepository

class WeatherRepositoryImpl(
    private val feedRepository: FeedRepository,
    private val context: Context
) : WeatherRepository {

    private val userPreferencesRepository = UserPreferencesRepository(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Clock flow ticks every 60 seconds to update daylight, period, and moon state smoothly
    private val clockFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }

    init {
        // Continuous real weather update loop according to keefUpdateIntervalMinutes (5 min - 24 hours)
        scope.launch {
            while (isActive) {
                val prefs = try {
                    userPreferencesRepository.userPreferencesFlow.first()
                } catch (e: Exception) {
                    Log.w("WeatherRepositoryImpl", "Error reading preferences for weather loop", e)
                    null
                }
                val intervalMinutes = prefs?.keefUpdateIntervalMinutes?.coerceIn(5, 1440) ?: 15
                val intervalMs = intervalMinutes * 60_000L

                if (prefs?.keefWeatherMode == "REAL") {
                    try {
                        feedRepository.refreshWeather()
                    } catch (e: Exception) {
                        Log.e("WeatherRepositoryImpl", "Continuous weather refresh failed", e)
                    }
                }
                delay(intervalMs)
            }
        }
    }

    override val weatherInfo: StateFlow<WeatherInfo?> = feedRepository.getWeather()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val environmentalState: StateFlow<EnvironmentalState> = combine(
        weatherInfo,
        clockFlow,
        userPreferencesRepository.userPreferencesFlow
    ) { weather, currentTime, prefs ->
        val groundOverride = if (prefs.keefGroundType != "AUTO") {
            try { tn.keeftalk.chat.domain.model.GroundType.valueOf(prefs.keefGroundType) } catch (_: Exception) { null }
        } else null
        val pollutionOverride = if (prefs.keefLightPollutionPercent in 0..100) prefs.keefLightPollutionPercent else null

        if (prefs.keefWeatherMode == "SIMULATED") {
            EnvironmentalCalculators.createSimulatedState(
                timeOfDayHours = prefs.keefSimTimeOfDay,
                cloudDensityPercent = prefs.keefSimCloudDensity,
                cloudSmoothness = prefs.keefSimCloudSmoothness,
                windSpeedKmH = prefs.keefSimWindSpeed,
                rainDensityPercent = prefs.keefSimRainDensity,
                starsDensityPercent = prefs.keefSimStarsDensity,
                thunderFrequencyPercent = prefs.keefSimThunderFrequency,
                dayOfMonth = prefs.keefSimDayOfMonth,
                groundTypeOverride = groundOverride,
                lightPollutionOverride = pollutionOverride
            )
        } else {
            EnvironmentalCalculators.createEnvironmentalState(
                weather = weather,
                timeMillis = currentTime,
                groundTypeOverride = groundOverride,
                lightPollutionOverride = pollutionOverride
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, EnvironmentalState.default())

    override suspend fun refreshWeather(force: Boolean) {
        try {
            feedRepository.refreshWeather()
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error refreshing weather", e)
        }
    }
}

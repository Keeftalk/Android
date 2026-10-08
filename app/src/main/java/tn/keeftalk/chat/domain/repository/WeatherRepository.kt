package tn.keeftalk.chat.domain.repository

import kotlinx.coroutines.flow.StateFlow
import tn.keeftalk.chat.domain.model.EnvironmentalState
import tn.keeftalk.chat.domain.model.WeatherInfo

interface WeatherRepository {
    val weatherInfo: StateFlow<WeatherInfo?>
    val environmentalState: StateFlow<EnvironmentalState>

    suspend fun refreshWeather(force: Boolean = false)
}

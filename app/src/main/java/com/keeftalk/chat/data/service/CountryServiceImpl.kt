package com.keeftalk.chat.data.service

import android.content.Context
import android.telephony.TelephonyManager
import com.keeftalk.chat.data.prefs.UserPreferencesRepository
import com.keeftalk.chat.domain.model.Country
import com.keeftalk.chat.domain.service.CountryService
import com.keeftalk.chat.util.CountryData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

class CountryServiceImpl(
    private val context: Context,
    private val prefs: UserPreferencesRepository
) : CountryService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val _currentRegion = MutableStateFlow(getDefaultCountry())
    override val currentRegion: StateFlow<Country> = _currentRegion.asStateFlow()

    private var isManualOverride = false

    private val _accountCountry = MutableStateFlow<Country?>(null)
    override val accountCountry: StateFlow<Country?> = _accountCountry.asStateFlow()

    init {
        scope.launch {
            prefs.userPreferencesFlow.collect { userPrefs ->
                userPrefs.accountCountryIso?.let { iso ->
                    val country = CountryData.getByIso(iso)
                    _accountCountry.value = country
                    if (country != null && !isManualOverride) {
                        _currentRegion.value = country
                    }
                }
                
                val detectedIso = userPrefs.detectedCountryIso
                if (detectedIso != null) {
                    if (!isManualOverride && _accountCountry.value == null) {
                        _currentRegion.value = CountryData.getByIso(detectedIso) ?: getDefaultCountry()
                    }
                } else {
                    detectAndCacheCountry()
                }
            }
        }
    }

    private suspend fun detectAndCacheCountry() {
        val detected = detectCountryFromIp() 
            ?: getSimCountry() 
            ?: getLocaleCountry() 
            ?: getDefaultCountry()
            
        prefs.updateDetectedCountryIso(detected.isoCode)
        _currentRegion.value = detected
    }

    override suspend fun detectCountryFromIp(): Country? {
        return try {
            val response: IpApiResponse = client.get("https://ipapi.co/json/").body()
            CountryData.getByIso(response.country_code)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun setAccountCountry(country: Country) {
        prefs.updateAccountCountryIso(country.isoCode)
        _accountCountry.value = country
        _currentRegion.value = country
        isManualOverride = true
    }

    override fun setCurrentRegion(country: Country) {
        _currentRegion.value = country
        isManualOverride = true
    }

    override fun getCountryByIso(isoCode: String): Country? {
        return CountryData.getByIso(isoCode)
    }

    override fun getAllCountries(): List<Country> {
        return CountryData.countries
    }

    private fun getSimCountry(): Country? {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val simCountry = tm.simCountryIso?.uppercase()
            if (!simCountry.isNullOrBlank()) {
                CountryData.getByIso(simCountry)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getLocaleCountry(): Country? {
        val localeCountry = Locale.getDefault().country
        return if (!localeCountry.isNullOrBlank()) {
            CountryData.getByIso(localeCountry)
        } else null
    }

    private fun getDefaultCountry(): Country {
        // Fallback to Tunisia if nothing else works, as per user's example
        return CountryData.getByIso("TN") ?: CountryData.countries.first()
    }

    @Serializable
    private data class IpApiResponse(
        val country_code: String
    )
}

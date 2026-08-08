package com.keeftalk.chat.domain.service

import com.keeftalk.chat.domain.model.Country
import kotlinx.coroutines.flow.StateFlow

interface CountryService {
    /**
     * The current detected region for formatting purposes (may change while traveling).
     */
    val currentRegion: StateFlow<Country>

    /**
     * The permanent account country (detected at signup or set by user).
     */
    val accountCountry: StateFlow<Country?>

    /**
     * Detects the country from IP geolocation.
     */
    suspend fun detectCountryFromIp(): Country?

    /**
     * Sets the account country permanently.
     */
    suspend fun setAccountCountry(country: Country)

    /**
     * Overrides the current region temporarily.
     */
    fun setCurrentRegion(country: Country)

    /**
     * Gets country metadata by ISO code.
     */
    fun getCountryByIso(isoCode: String): Country?

    /**
     * Gets all supported countries.
     */
    fun getAllCountries(): List<Country>
}

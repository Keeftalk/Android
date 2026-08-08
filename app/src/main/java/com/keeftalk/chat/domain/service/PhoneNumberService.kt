package com.keeftalk.chat.domain.service

interface PhoneNumberService {
    /**
     * Formats a phone number for display (e.g., "22 123 456" or "+216 22 123 456").
     */
    fun formatForDisplay(phone: String, countryIso: String): String

    /**
     * Normalizes a phone number to E.164 format (e.g., "+21622123456").
     */
    fun normalizeToE164(phone: String, countryIso: String): String?

    /**
     * Validates if a phone number is valid for a given country.
     */
    fun isValid(phone: String, countryIso: String): Boolean

    /**
     * Extracts the country ISO code from an E.164 number.
     */
    fun getRegionCodeForNumber(phone: String): String?

    /**
     * Returns an AsYouTypeFormatter equivalent for real-time formatting.
     */
    fun formatAsYouType(phone: String, countryIso: String): String
}

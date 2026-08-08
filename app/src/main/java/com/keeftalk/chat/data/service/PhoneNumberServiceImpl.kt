package com.keeftalk.chat.data.service

import android.content.Context
import com.keeftalk.chat.domain.service.PhoneNumberService
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

class PhoneNumberServiceImpl(context: Context) : PhoneNumberService {
    private val phoneUtil = PhoneNumberUtil.createInstance(context)

    override fun formatForDisplay(phone: String, countryIso: String): String {
        return try {
            val numberProto = phoneUtil.parse(phone, countryIso)
            if (phoneUtil.isValidNumber(numberProto)) {
                phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            } else {
                phone
            }
        } catch (_: Exception) {
            phone
        }
    }

    override fun normalizeToE164(phone: String, countryIso: String): String? {
        return try {
            val numberProto = phoneUtil.parse(phone, countryIso)
            if (phoneUtil.isValidNumber(numberProto)) {
                phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun isValid(phone: String, countryIso: String): Boolean {
        return try {
            val numberProto = phoneUtil.parse(phone, countryIso)
            phoneUtil.isValidNumber(numberProto)
        } catch (e: Exception) {
            false
        }
    }

    override fun getRegionCodeForNumber(phone: String): String? {
        return try {
            val numberProto = phoneUtil.parse(phone, null)
            phoneUtil.getRegionCodeForNumber(numberProto)
        } catch (e: Exception) {
            null
        }
    }

    override fun formatAsYouType(phone: String, countryIso: String): String {
        if (phone.startsWith("*") || phone.contains("#")) return phone
        val formatter = phoneUtil.getAsYouTypeFormatter(countryIso)
        var result = ""
        val filtered = phone.filter { it.isDigit() || it == '+' }
        filtered.forEach { char ->
            result = formatter.inputDigit(char)
        }
        return result
    }
}

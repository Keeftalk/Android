package tn.keeftalk.chat.feature.finances.util

import android.content.Context

enum class CardNetwork(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMEX("American Express"),
    DISCOVER("Discover"),
    JCB("JCB"),
    DINERS_CLUB("Diners Club"),
    UNIONPAY("UnionPay"),
    MAESTRO("Maestro"),
    MIR("Mir"),
    TROY("Troy"),
    RUPAY("RuPay"),
    UNKNOWN("Card")
}

data class DetectedCard(
    val network: CardNetwork,
    val networkDisplayName: String,
    val rawDigits: String,
    val formattedNumber: String,
    val isValidLuhn: Boolean,
    val isCompleteLength: Boolean,
    val issuingBank: String? = null,
    val issuingCountryCode: String? = null,
    val issuingCountryName: String? = null,
    val countryFlagEmoji: String? = null,
    val cardCategory: CardCategory? = null,
    val cardLevel: String? = null
)

object OfflineCardDetector {

    /**
     * Determines the card network offline from PAN/IIN prefix rules.
     */
    fun detectNetwork(digits: String): CardNetwork {
        if (digits.isBlank()) return CardNetwork.UNKNOWN

        return when {
            // Visa: starts with 4
            digits.startsWith("4") -> CardNetwork.VISA

            // Mastercard: 51-55 or 2221-2720
            digits.matches("^5[1-5].*".toRegex()) || matchesRange(digits, 2221, 2720, 4) -> CardNetwork.MASTERCARD

            // American Express: 34 or 37
            digits.startsWith("34") || digits.startsWith("37") -> CardNetwork.AMEX

            // Discover: 6011, 622126-622925, 644-649, 65
            digits.startsWith("6011") || digits.startsWith("65") ||
                    matchesRange(digits, 644, 649, 3) ||
                    matchesRange(digits, 622126, 622925, 6) -> CardNetwork.DISCOVER

            // JCB: 3528-3589
            matchesRange(digits, 3528, 3589, 4) -> CardNetwork.JCB

            // Diners Club: 300-305, 36, 38, 3095
            digits.startsWith("36") || digits.startsWith("38") || digits.startsWith("3095") ||
                    matchesRange(digits, 300, 305, 3) -> CardNetwork.DINERS_CLUB

            // UnionPay: 62 or 81
            digits.startsWith("62") || digits.startsWith("81") -> CardNetwork.UNIONPAY

            // Mir: 2200-2204
            matchesRange(digits, 2200, 2204, 4) -> CardNetwork.MIR

            // Troy: 22050-22052 or 9792
            digits.startsWith("9792") || matchesRange(digits, 22050, 22052, 5) -> CardNetwork.TROY

            // RuPay: 60, 65, 81, 82, 508
            digits.startsWith("60") || digits.startsWith("65") || digits.startsWith("81") ||
                    digits.startsWith("82") || digits.startsWith("508") -> CardNetwork.RUPAY

            // Maestro: 5018, 5020, 5038, 5893, 6304, 6759, 6761, 6762, 6763
            digits.startsWith("5018") || digits.startsWith("5020") || digits.startsWith("5038") ||
                    digits.startsWith("5893") || digits.startsWith("6304") || digits.startsWith("6759") ||
                    digits.startsWith("6761") || digits.startsWith("6762") || digits.startsWith("6763") -> CardNetwork.MAESTRO

            else -> CardNetwork.UNKNOWN
        }
    }

    /**
     * Automatically formats the card number while typing according to network rules.
     */
    fun formatCardNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) return ""

        val network = detectNetwork(digits)
        return when (network) {
            CardNetwork.AMEX -> {
                // Format: 4 - 6 - 5 (max 15)
                val truncated = digits.take(15)
                val sb = StringBuilder()
                for (i in truncated.indices) {
                    if (i == 4 || i == 10) sb.append(' ')
                    sb.append(truncated[i])
                }
                sb.toString()
            }
            CardNetwork.DINERS_CLUB -> {
                if (digits.length <= 14) {
                    // Format: 4 - 6 - 4 (max 14)
                    val truncated = digits.take(14)
                    val sb = StringBuilder()
                    for (i in truncated.indices) {
                        if (i == 4 || i == 10) sb.append(' ')
                        sb.append(truncated[i])
                    }
                    sb.toString()
                } else {
                    formatInGroupsOfFour(digits.take(19))
                }
            }
            else -> formatInGroupsOfFour(digits.take(19))
        }
    }

    private fun formatInGroupsOfFour(digits: String): String {
        val sb = StringBuilder()
        for (i in digits.indices) {
            if (i > 0 && i % 4 == 0) sb.append(' ')
            sb.append(digits[i])
        }
        return sb.toString()
    }

    /**
     * Validates card number using the offline Luhn algorithm.
     */
    fun validateLuhn(cardNumberDigits: String): Boolean {
        val digits = cardNumberDigits.filter { it.isDigit() }
        if (digits.length < 12) return false

        var sum = 0
        var isSecond = false

        for (i in digits.length - 1 downTo 0) {
            var d = digits[i] - '0'
            if (isSecond) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            isSecond = !isSecond
        }
        return sum % 10 == 0
    }

    /**
     * Synchronous detection function that identifies network, Luhn validity, BIN database issuer info, country, tier, etc.
     */
    fun detectCard(input: String): DetectedCard {
        val digits = input.filter { it.isDigit() }
        val formatted = formatCardNumber(input)
        val network = detectNetwork(digits)
        val isValidLuhn = validateLuhn(digits)

        val minCompleteLength = when (network) {
            CardNetwork.AMEX -> 15
            CardNetwork.DINERS_CLUB -> 14
            else -> 16
        }
        val isCompleteLength = digits.length >= minCompleteLength

        val binRecord = if (digits.length >= 6) CardBinDatabase.lookup(digits) else null

        val flagEmoji = binRecord?.countryCode?.let { countryCodeToFlagEmoji(it) }

        return DetectedCard(
            network = network,
            networkDisplayName = network.displayName,
            rawDigits = digits,
            formattedNumber = formatted,
            isValidLuhn = isValidLuhn,
            isCompleteLength = isCompleteLength,
            issuingBank = binRecord?.bankName,
            issuingCountryCode = binRecord?.countryCode,
            issuingCountryName = binRecord?.countryName,
            countryFlagEmoji = flagEmoji,
            cardCategory = binRecord?.cardCategory,
            cardLevel = binRecord?.cardLevel
        )
    }

    /**
     * Async detection that checks local database first, and queries Supabase "BIN_DB" table when online.
     */
    suspend fun detectCardOnline(context: Context, input: String): DetectedCard {
        val offlineCard = detectCard(input)
        val digits = input.filter { it.isDigit() }
        if (digits.length < 6) return offlineCard

        val onlineBinRecord = CardBinDatabase.lookupOnline(context, digits) ?: return offlineCard

        val flagEmoji = countryCodeToFlagEmoji(onlineBinRecord.countryCode)

        return offlineCard.copy(
            network = if (offlineCard.network == CardNetwork.UNKNOWN) onlineBinRecord.network else offlineCard.network,
            networkDisplayName = if (offlineCard.network == CardNetwork.UNKNOWN) onlineBinRecord.network.displayName else offlineCard.networkDisplayName,
            issuingBank = onlineBinRecord.bankName,
            issuingCountryCode = onlineBinRecord.countryCode,
            issuingCountryName = onlineBinRecord.countryName,
            countryFlagEmoji = flagEmoji,
            cardCategory = onlineBinRecord.cardCategory,
            cardLevel = onlineBinRecord.cardLevel
        )
    }

    private fun matchesRange(digits: String, start: Int, end: Int, length: Int): Boolean {
        if (digits.length < length) return false
        val prefix = digits.take(length).toIntOrNull() ?: return false
        return prefix in start..end
    }

    fun countryCodeToFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}

package tn.keeftalk.chat.feature.finances.util

import java.math.BigInteger

enum class AccountIdentifierType(val displayName: String) {
    IBAN("IBAN"),
    TUNISIAN_RIB("Tunisian RIB"),
    GENERIC_ACCOUNT("Bank Account"),
    UNKNOWN("Account")
}

data class DetectedBankAccount(
    val identifierType: AccountIdentifierType,
    val rawInput: String,
    val formattedInput: String,
    val isValidChecksum: Boolean,
    val countryCode: String? = null,
    val countryName: String? = null,
    val countryFlagEmoji: String? = null,
    val bankCode: String? = null,
    val bankName: String? = null,
    val branchCode: String? = null,
    val accountNumber: String? = null,
    val checkKey: String? = null,
    val swiftBic: String? = null
)

object OfflineBankDetector {

    /**
     * Formats IBAN in standard 4-character blocks.
     */
    fun formatIban(input: String): String {
        val clean = input.replace("\\s".toRegex(), "").replace("-", "").uppercase()
        val sb = StringBuilder()
        for (i in clean.indices) {
            if (i > 0 && i % 4 == 0) sb.append(' ')
            sb.append(clean[i])
        }
        return sb.toString()
    }

    /**
     * Formats Tunisian 20-digit RIB: BB CCC AAAAAAAAAAAAA KK
     */
    fun formatTunisianRib(input: String): String {
        val digits = input.filter { it.isDigit() }.take(20)
        if (digits.isEmpty()) return ""

        val sb = StringBuilder()
        for (i in digits.indices) {
            when (i) {
                2, 5, 18 -> sb.append(' ')
            }
            sb.append(digits[i])
        }
        return sb.toString()
    }

    /**
     * Validates IBAN check digits using offline MOD-97 algorithm.
     */
    fun validateIbanMod97(iban: String): Boolean {
        val clean = iban.replace("\\s".toRegex(), "").replace("-", "").uppercase()
        if (clean.length < 15 || !clean.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]+$".toRegex())) {
            return false
        }

        val countryCode = clean.take(2)
        val spec = BankMetadataDatabase.getCountrySpec(countryCode)
        if (spec != null && spec.expectedIbanLength > 0 && clean.length != spec.expectedIbanLength) {
            return false
        }

        // Rearrange: move first 4 characters (country + check digits) to the end
        val rearranged = clean.substring(4) + clean.substring(0, 4)

        // Convert letters to digits A=10 .. Z=35
        val numericStr = StringBuilder()
        for (ch in rearranged) {
            if (ch in 'A'..'Z') {
                numericStr.append(ch - 'A' + 10)
            } else {
                numericStr.append(ch)
            }
        }

        // Calculate modulo 97
        return try {
            val bigNum = BigInteger(numericStr.toString())
            bigNum.mod(BigInteger.valueOf(97)) == BigInteger.ONE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates Tunisian 20-digit RIB check key using MOD-97 formula.
     */
    fun validateTunisianRibKey(ribDigits: String): Boolean {
        val digits = ribDigits.filter { it.isDigit() }
        if (digits.length != 20) return false

        val bankCodeStr = digits.substring(0, 2)
        val branchCodeStr = digits.substring(2, 5)
        val accountNumberStr = digits.substring(5, 18)
        val keyStr = digits.substring(18, 20)

        return try {
            val keyProvided = keyStr.toInt()
            // Formula: MOD97 of (BankCode * 10^18 + BranchCode * 10^15 + AccountNumber * 10^2)
            val fullNumStr = "$bankCodeStr$branchCodeStr${accountNumberStr}00"
            val bigNum = BigInteger(fullNumStr)
            val remainder = bigNum.mod(BigInteger.valueOf(97)).toInt()
            val expectedKey = if (remainder == 0) 97 else (97 - remainder)

            expectedKey == keyProvided
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detects bank account format, country, bank code, branch, and validation state offline.
     */
    fun detectBankAccount(input: String): DetectedBankAccount {
        val cleanUpper = input.replace("\\s".toRegex(), "").replace("-", "").uppercase()
        val digitsOnly = input.filter { it.isDigit() }

        // 1. Check if IBAN (starts with 2 letters followed by 2 digits)
        if (cleanUpper.length >= 4 && cleanUpper.take(2).matches("^[A-Z]{2}$".toRegex()) && cleanUpper.substring(2, 4).matches("^[0-9]{2}$".toRegex())) {
            val countryCode = cleanUpper.take(2)
            val spec = BankMetadataDatabase.getCountrySpec(countryCode)
            val countryName = spec?.countryName ?: getCountryNameFromCode(countryCode)
            val flagEmoji = OfflineCardDetector.countryCodeToFlagEmoji(countryCode)
            val isValidMod97 = validateIbanMod97(cleanUpper)
            val formatted = formatIban(cleanUpper)

            var bankCode: String? = null
            var branchCode: String? = null
            var accountNumber: String? = null
            var checkKey: String? = null
            var bankRecord: BankRecord? = null

            if (countryCode == "TN" && cleanUpper.length >= 24) {
                // TN59 + 2 bank + 3 branch + 13 account + 2 key = 24 chars
                bankCode = cleanUpper.substring(4, 6)
                branchCode = cleanUpper.substring(6, 9)
                accountNumber = cleanUpper.substring(9, 22)
                checkKey = cleanUpper.substring(22, 24)
                bankRecord = BankMetadataDatabase.lookupBank("TN", bankCode)
            } else if (countryCode == "FR" && cleanUpper.length >= 27) {
                // FR76 + 5 bank + 5 branch + 11 account + 2 key
                bankCode = cleanUpper.substring(4, 9)
                branchCode = cleanUpper.substring(9, 14)
                accountNumber = cleanUpper.substring(14, 25)
                checkKey = cleanUpper.substring(25, 27)
                bankRecord = BankMetadataDatabase.lookupBank("FR", bankCode)
            } else if (countryCode == "DE" && cleanUpper.length >= 22) {
                // DE89 + 8 BLZ + 10 account
                bankCode = cleanUpper.substring(4, 12)
                accountNumber = cleanUpper.substring(12, 22)
                bankRecord = BankMetadataDatabase.lookupBank("DE", bankCode)
            } else if (countryCode == "GB" && cleanUpper.length >= 22) {
                // GB29 + 4 bank code + 6 sort code + 8 account
                bankCode = cleanUpper.substring(8, 14) // Sort code
                accountNumber = cleanUpper.substring(14, 22)
                bankRecord = BankMetadataDatabase.lookupBank("GB", bankCode)
            }

            return DetectedBankAccount(
                identifierType = AccountIdentifierType.IBAN,
                rawInput = cleanUpper,
                formattedInput = formatted,
                isValidChecksum = isValidMod97,
                countryCode = countryCode,
                countryName = countryName,
                countryFlagEmoji = flagEmoji,
                bankCode = bankCode,
                bankName = bankRecord?.bankName,
                branchCode = branchCode,
                accountNumber = accountNumber,
                checkKey = checkKey,
                swiftBic = bankRecord?.swiftBic
            )
        }

        // 2. Check if Tunisian RIB (20 digits or digits starting with 0..9)
        if (digitsOnly.length == 20 || (digitsOnly.length in 15..20 && (input.startsWith("0") || input.startsWith("1") || input.startsWith("2") || input.startsWith("3") || input.startsWith("4") || input.startsWith("8")))) {
            val formattedRib = formatTunisianRib(digitsOnly)
            val isValidRib = validateTunisianRibKey(digitsOnly)

            val bankCode = if (digitsOnly.length >= 2) digitsOnly.substring(0, 2) else null
            val branchCode = if (digitsOnly.length >= 5) digitsOnly.substring(2, 5) else null
            val accountNumber = if (digitsOnly.length >= 18) digitsOnly.substring(5, 18) else null
            val checkKey = if (digitsOnly.length == 20) digitsOnly.substring(18, 20) else null

            val bankRecord = bankCode?.let { BankMetadataDatabase.lookupBank("TN", it) }

            return DetectedBankAccount(
                identifierType = AccountIdentifierType.TUNISIAN_RIB,
                rawInput = digitsOnly,
                formattedInput = formattedRib,
                isValidChecksum = isValidRib,
                countryCode = "TN",
                countryName = "Tunisia",
                countryFlagEmoji = OfflineCardDetector.countryCodeToFlagEmoji("TN"),
                bankCode = bankCode,
                bankName = bankRecord?.bankName,
                branchCode = branchCode,
                accountNumber = accountNumber,
                checkKey = checkKey,
                swiftBic = bankRecord?.swiftBic
            )
        }

        // 3. Generic account identifier
        return DetectedBankAccount(
            identifierType = if (digitsOnly.isNotBlank()) AccountIdentifierType.GENERIC_ACCOUNT else AccountIdentifierType.UNKNOWN,
            rawInput = cleanUpper,
            formattedInput = cleanUpper,
            isValidChecksum = false
        )
    }

    private fun getCountryNameFromCode(code: String): String {
        return when (code) {
            "TN" -> "Tunisia"
            "FR" -> "France"
            "DE" -> "Germany"
            "GB" -> "United Kingdom"
            "US" -> "United States"
            "AE" -> "UAE"
            "SA" -> "Saudi Arabia"
            "QA" -> "Qatar"
            "IT" -> "Italy"
            "ES" -> "Spain"
            "BE" -> "Belgium"
            "NL" -> "Netherlands"
            "CH" -> "Switzerland"
            "CA" -> "Canada"
            "MA" -> "Morocco"
            "DZ" -> "Algeria"
            "EG" -> "Egypt"
            else -> code
        }
    }
}

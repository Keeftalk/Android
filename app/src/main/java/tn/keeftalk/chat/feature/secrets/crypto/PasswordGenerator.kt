package tn.keeftalk.chat.feature.secrets.crypto

import java.security.SecureRandom
import java.util.Locale

object PasswordGenerator {

    private val secureRandom = SecureRandom()

    private const val LOWERCASE = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val NUMBERS = "23456789"
    private const val SYMBOLS = "!@#$%^&*-_=+?"

    private val LEET_MAP = mapOf(
        'a' to "@", 'e' to "3", 'i' to "!", 'o' to "0", 's' to "$", 't' to "7"
    )

    /**
     * Generates a cryptographically secure random password.
     */
    fun generate(
        length: Int = 16,
        includeSymbols: Boolean = true,
        includeNumbers: Boolean = true,
        personalizedName: String = ""
    ): String {
        val targetLength = length.coerceIn(8, 64)

        if (personalizedName.isNotBlank()) {
            val cleanName = personalizedName.replace("[^a-zA-Z]".toRegex(), "")
            if (cleanName.isNotEmpty()) {
                val prefixLength = minOf(cleanName.length, targetLength - 4)
                val namePart = cleanName.substring(0, prefixLength)

                val transformed = StringBuilder()
                namePart.forEachIndexed { idx, ch ->
                    val lower = ch.lowercaseChar()
                    if (idx == 0) {
                        transformed.append(ch.uppercaseChar())
                    } else if (includeSymbols && LEET_MAP.containsKey(lower) && secureRandom.nextBoolean()) {
                        transformed.append(LEET_MAP[lower])
                    } else if (secureRandom.nextBoolean()) {
                        transformed.append(ch.lowercaseChar())
                    } else {
                        transformed.append(ch.uppercaseChar())
                    }
                }

                val remainingLen = targetLength - transformed.length
                val randomTail = generatePureRandom(remainingLen, includeSymbols, includeNumbers)
                return transformed.toString() + randomTail
            }
        }

        return generatePureRandom(targetLength, includeSymbols, includeNumbers)
    }

    private fun generatePureRandom(length: Int, includeSymbols: Boolean, includeNumbers: Boolean): String {
        val poolBuilder = StringBuilder(LOWERCASE + UPPERCASE)
        if (includeNumbers) poolBuilder.append(NUMBERS)
        if (includeSymbols) poolBuilder.append(SYMBOLS)

        val pool = poolBuilder.toString()
        val result = CharArray(length)

        // Ensure at least one char from each requested category
        var idx = 0
        result[idx++] = LOWERCASE[secureRandom.nextInt(LOWERCASE.length)]
        result[idx++] = UPPERCASE[secureRandom.nextInt(UPPERCASE.length)]
        if (includeNumbers && idx < length) {
            result[idx++] = NUMBERS[secureRandom.nextInt(NUMBERS.length)]
        }
        if (includeSymbols && idx < length) {
            result[idx++] = SYMBOLS[secureRandom.nextInt(SYMBOLS.length)]
        }

        while (idx < length) {
            result[idx++] = pool[secureRandom.nextInt(pool.length)]
        }

        // Shuffle the result array securely
        for (i in result.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        return String(result)
    }
}

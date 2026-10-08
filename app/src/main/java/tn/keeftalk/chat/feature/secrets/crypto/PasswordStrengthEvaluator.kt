package tn.keeftalk.chat.feature.secrets.crypto

import java.util.Locale
import kotlin.math.log2
import kotlin.math.min

data class PasswordStrengthResult(
    val score: Int, // 0 to 100
    val entropyBits: Double,
    val label: String, // "Weak", "Fair", "Strong", "Very Strong"
    val bruteForceTime: String,
    val targetedGuessTime: String,
    val feedback: String
)

object PasswordStrengthEvaluator {

    private val COMMON_WEAKNESSES = setOf(
        "password", "123456", "12345678", "qwerty", "admin", "welcome",
        "letmein", "monkey", "dragon", "111111", "iloveyou", "sunshine",
        "master", "keeftalk", "secret", "000000"
    )

    /**
     * Evaluates the strength of a password deterministically on-device.
     */
    fun evaluate(password: String, personalizedName: String = ""): PasswordStrengthResult {
        if (password.isEmpty()) {
            return PasswordStrengthResult(
                score = 0,
                entropyBits = 0.0,
                label = "Weak",
                bruteForceTime = "instantly",
                targetedGuessTime = "instantly",
                feedback = "Password cannot be empty"
            )
        }

        var charsetSize = 0
        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        if (hasLower) charsetSize += 26
        if (hasUpper) charsetSize += 26
        if (hasDigit) charsetSize += 10
        if (hasSymbol) charsetSize += 32

        val rawBits = password.length * log2(charsetSize.toDouble().coerceAtLeast(2.0))

        var score = 0

        // Length bonus (up to 40)
        score += min(40, password.length * 3)

        // Character diversity (up to 30)
        if (hasLower) score += 5
        if (hasUpper) score += 7
        if (hasDigit) score += 8
        if (hasSymbol) score += 10

        // Length threshold bonus
        if (password.length >= 12) score += 10
        if (password.length >= 16) score += 10
        if (password.length >= 20) score += 10

        // Penalties
        val lowerPass = password.lowercase(Locale.US)

        // Common weak passwords
        if (COMMON_WEAKNESSES.any { lowerPass.contains(it) }) {
            score -= 35
        }

        // Sequential characters penalty (e.g. 1234, abcd)
        if (hasSequentialChars(lowerPass)) {
            score -= 15
        }

        // Repeating character penalty
        val repeatRatio = password.length.toDouble() / password.toSet().size.toDouble()
        if (repeatRatio > 2.0) {
            score -= 15
        }

        // Personalization factor
        var effectiveBits = rawBits
        if (personalizedName.isNotBlank()) {
            val cleanName = personalizedName.lowercase(Locale.US).replace("[^a-z]".toRegex(), "")
            if (cleanName.isNotEmpty() && lowerPass.contains(cleanName)) {
                effectiveBits = (password.length - cleanName.length) * log2(charsetSize.toDouble().coerceAtLeast(2.0)) + 10.0
                score -= 10
            }
        }

        val finalScore = score.coerceIn(0, 100)
        val label = when {
            finalScore >= 80 -> "Very Strong"
            finalScore >= 60 -> "Strong"
            finalScore >= 40 -> "Fair"
            else -> "Weak"
        }

        val bruteForceSecs = 2.0.pow(rawBits.coerceAtLeast(1.0) - 1.0) / 1e10 // 10 billion guesses/sec
        val targetedSecs = 2.0.pow(effectiveBits.coerceAtLeast(1.0) - 1.0) / 1e10

        val feedback = when {
            finalScore >= 80 -> "Excellent password. High entropy and diversity."
            finalScore >= 60 -> "Strong password. Adding symbols or extra length improves security."
            finalScore >= 40 -> "Fair password. Consider using numbers, symbols, and mixed cases."
            else -> "Weak password. Easy to guess or crack. Use a generated password."
        }

        return PasswordStrengthResult(
            score = finalScore,
            entropyBits = Math.round(effectiveBits * 10.0) / 10.0,
            label = label,
            bruteForceTime = formatTime(bruteForceSecs),
            targetedGuessTime = formatTime(targetedSecs),
            feedback = feedback
        )
    }

    private fun Double.pow(n: Double) = Math.pow(this, n)

    private fun hasSequentialChars(s: String): Boolean {
        for (i in 0 until s.length - 2) {
            val c1 = s[i].code
            val c2 = s[i + 1].code
            val c3 = s[i + 2].code
            if (c2 == c1 + 1 && c3 == c2 + 1) return true
            if (c2 == c1 - 1 && c3 == c2 - 1) return true
        }
        return false
    }

    private fun formatTime(seconds: Double): String {
        if (seconds < 1.0) return "instantly"
        val units = listOf(
            "seconds" to 60.0,
            "minutes" to 60.0,
            "hours" to 24.0,
            "days" to 365.0,
            "years" to 1000.0,
            "thousand years" to 1000.0,
            "million years" to 1000.0,
            "billion years" to 1e9
        )

        var s = seconds
        var unitName = "seconds"

        for ((name, div) in units) {
            unitName = name
            if (s < div) break
            s /= div
        }

        if (unitName == "billion years" && s >= 1000.0) return "over a trillion years"
        return if (s < 10.0) "%.1f %s".format(Locale.US, s, unitName) else "%d %s".format(Locale.US, Math.round(s), unitName)
    }
}

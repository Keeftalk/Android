package com.keeftalk.chat.util

import android.util.Patterns

object AuthUtils {

    enum class IdentifierType {
        EMAIL,
        PHONE,
        USERNAME
    }

    fun detectIdentifierType(identifier: String): IdentifierType {
        return when {
            Patterns.EMAIL_ADDRESS.matcher(identifier).matches() -> IdentifierType.EMAIL
            isPhoneNumber(identifier) -> IdentifierType.PHONE
            else -> IdentifierType.USERNAME
        }
    }

    private fun isPhoneNumber(identifier: String): Boolean {
        // Simple check: if it starts with + or consists mostly of digits and common separators
        val cleaned = identifier.filter { it.isDigit() || it == '+' }
        if (cleaned.startsWith("+") && cleaned.length >= 8) return true
        
        // If it's all digits and long enough
        if (identifier.all { it.isDigit() || it == ' ' || it == '-' } && identifier.filter { it.isDigit() }.length >= 7) {
            return true
        }
        
        return false
    }

    fun normalizePhone(phone: String): String {
        // Remove all non-digit characters except the leading +
        val hasPlus = phone.startsWith("+")
        val digits = phone.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    fun isValidUsername(username: String): Boolean {
        // 4–30 characters, letters (a-z), numbers, underscores, periods, no spaces, not an email
        val regex = "^[a-z0-9._]{4,30}$".toRegex()
        return regex.matches(username) && !Patterns.EMAIL_ADDRESS.matcher(username).matches()
    }

    fun maskEmail(email: String): String {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return email
        val parts = email.split("@")
        if (parts.size != 2) return email
        val local = parts[0]
        val domain = parts[1]
        
        return when {
            local.length <= 2 -> "${local.take(1)}***@$domain"
            local.length <= 5 -> "${local.take(2)}***@$domain"
            else -> "${local.take(3)}***${local.takeLast(2)}@$domain"
        }
    }
}

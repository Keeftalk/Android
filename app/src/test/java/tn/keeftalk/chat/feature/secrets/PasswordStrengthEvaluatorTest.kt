package tn.keeftalk.chat.feature.secrets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.feature.secrets.crypto.PasswordStrengthEvaluator

class PasswordStrengthEvaluatorTest {

    @Test
    fun evaluate_emptyPassword_returnsZeroScore() {
        val result = PasswordStrengthEvaluator.evaluate("")
        assertEquals(0, result.score)
        assertEquals("Weak", result.label)
    }

    @Test
    fun evaluate_weakCommonPassword_penalizesScore() {
        val result = PasswordStrengthEvaluator.evaluate("password123")
        assertTrue(result.score < 55)
        assertEquals("Weak", result.label)
    }

    @Test
    fun evaluate_strongComplexPassword_returnsHighScore() {
        val result = PasswordStrengthEvaluator.evaluate("K3e#fT!aLk_Vault_2026_Secure")
        assertTrue(result.score >= 80)
        assertEquals("Very Strong", result.label)
    }

    @Test
    fun evaluate_personalizedNameInPassword_adjustsEntropyBits() {
        val resultWithPerson = PasswordStrengthEvaluator.evaluate("Steve12345!$", personalizedName = "Steve")
        val resultWithoutPerson = PasswordStrengthEvaluator.evaluate("Steve12345!$", personalizedName = "")

        assertTrue(resultWithPerson.entropyBits <= resultWithoutPerson.entropyBits)
    }
}

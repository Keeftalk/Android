package tn.keeftalk.chat.feature.secrets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.feature.secrets.crypto.PasswordGenerator

class PasswordGeneratorTest {

    @Test
    fun generate_requestedLength_matchesLength() {
        val pw = PasswordGenerator.generate(length = 20)
        assertEquals(20, pw.length)
    }

    @Test
    fun generate_withSymbolsAndNumbers_containsDiverseCharacters() {
        val pw = PasswordGenerator.generate(length = 24, includeSymbols = true, includeNumbers = true)
        assertTrue(pw.any { it.isUpperCase() })
        assertTrue(pw.any { it.isLowerCase() })
        assertTrue(pw.any { it.isDigit() })
        assertTrue(pw.any { !it.isLetterOrDigit() })
    }

    @Test
    fun generate_personalizedName_startsWithNamePrefix() {
        val pw = PasswordGenerator.generate(length = 18, personalizedName = "Steve")
        assertEquals(18, pw.length)
        assertTrue(pw.lowercase().startsWith("s") || pw.lowercase().startsWith("st3"))
    }
}

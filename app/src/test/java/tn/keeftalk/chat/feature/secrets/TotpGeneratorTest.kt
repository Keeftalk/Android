package tn.keeftalk.chat.feature.secrets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.feature.secrets.crypto.TotpGenerator

class TotpGeneratorTest {

    @Test
    fun decodeBase32_validSecret_returnsDecodedBytes() {
        val secret = "JBSWY3DP"
        val bytes = TotpGenerator.decodeBase32(secret)
        assertTrue(bytes.isNotEmpty())
        assertEquals("Hello", String(bytes, Charsets.UTF_8))
    }

    @Test
    fun generateCode_rfc6238KnownVector_matchesExpectedCode() {
        val secretBytes = "12345678901234567890".toByteArray(Charsets.UTF_8)
        // At T = 59s
        val code1 = TotpGenerator.generateCodeFromBytes(keyBytes = secretBytes, timeSeconds = 59L, period = 30, digits = 8)
        assertEquals("94287082", code1)

        // At T = 1111111109s
        val code2 = TotpGenerator.generateCodeFromBytes(keyBytes = secretBytes, timeSeconds = 1111111109L, period = 30, digits = 8)
        assertEquals("07081804", code2)
    }

    @Test
    fun getRemainingSeconds_calculatesCorrectCountdown() {
        val remaining = TotpGenerator.getRemainingSeconds(timeSeconds = 25L, period = 30)
        assertEquals(5, remaining)

        val remainingExactBoundary = TotpGenerator.getRemainingSeconds(timeSeconds = 30L, period = 30)
        assertEquals(30, remainingExactBoundary)
    }

    @Test
    fun parseOtpUri_validUri_parsesFieldsCorrectly() {
        val uri = "otpauth://totp/GitHub:dev-handle?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA1&digits=6&period=30"
        val parsed = TotpGenerator.parseOtpUri(uri)
        assertNotNull(parsed)
        assertEquals("GitHub", parsed?.serviceName)
        assertEquals("dev-handle", parsed?.username)
        assertEquals("JBSWY3DPEHPK3PXP", parsed?.secretKey)
        assertEquals("GitHub", parsed?.issuer)
        assertEquals("SHA1", parsed?.algorithm)
        assertEquals(6, parsed?.digits)
        assertEquals(30, parsed?.period)
    }
}

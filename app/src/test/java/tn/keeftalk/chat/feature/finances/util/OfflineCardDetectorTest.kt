package tn.keeftalk.chat.feature.finances.util

import org.junit.Assert.*
import org.junit.Test

class OfflineCardDetectorTest {

    @Test
    fun testNetworkDetection() {
        assertEquals(CardNetwork.VISA, OfflineCardDetector.detectNetwork("4000123456789010"))
        assertEquals(CardNetwork.MASTERCARD, OfflineCardDetector.detectNetwork("5123456789012345"))
        assertEquals(CardNetwork.MASTERCARD, OfflineCardDetector.detectNetwork("2221000000000000"))
        assertEquals(CardNetwork.AMEX, OfflineCardDetector.detectNetwork("340012345678901"))
        assertEquals(CardNetwork.AMEX, OfflineCardDetector.detectNetwork("378282246310005"))
        assertEquals(CardNetwork.DISCOVER, OfflineCardDetector.detectNetwork("6011000000000000"))
        assertEquals(CardNetwork.JCB, OfflineCardDetector.detectNetwork("3528000000000000"))
        assertEquals(CardNetwork.DINERS_CLUB, OfflineCardDetector.detectNetwork("36000000000000"))
        assertEquals(CardNetwork.UNIONPAY, OfflineCardDetector.detectNetwork("6212345678901234"))
        assertEquals(CardNetwork.MAESTRO, OfflineCardDetector.detectNetwork("5018000000000000"))
        assertEquals(CardNetwork.MIR, OfflineCardDetector.detectNetwork("2200000000000000"))
        assertEquals(CardNetwork.TROY, OfflineCardDetector.detectNetwork("9792000000000000"))
    }

    @Test
    fun testCardFormatting() {
        // Standard 16-digit card
        assertEquals("4532 0151 1283 0366", OfflineCardDetector.formatCardNumber("4532015112830366"))

        // Amex 15-digit 4-6-5 format
        assertEquals("3782 822463 10005", OfflineCardDetector.formatCardNumber("378282246310005"))

        // Diners Club 14-digit 4-6-4 format
        assertEquals("3600 123456 7890", OfflineCardDetector.formatCardNumber("36001234567890"))
    }

    @Test
    fun testLuhnValidation() {
        // Valid Luhn numbers
        assertTrue(OfflineCardDetector.validateLuhn("4532015112830366"))
        assertTrue(OfflineCardDetector.validateLuhn("378282246310005"))

        // Invalid Luhn numbers
        assertFalse(OfflineCardDetector.validateLuhn("4532015112830367"))
        assertFalse(OfflineCardDetector.validateLuhn("12345678"))
    }

    @Test
    fun testKnownAndUnknownBinCases() {
        // Known BIAT Tunisian card
        val biatCard = OfflineCardDetector.detectCard("5358960012345678")
        assertEquals(CardNetwork.MASTERCARD, biatCard.network)
        assertEquals("Banque Internationale Arabe de Tunisie (BIAT)", biatCard.issuingBank)
        assertEquals("TN", biatCard.issuingCountryCode)
        assertEquals("Tunisia", biatCard.issuingCountryName)
        assertEquals(CardCategory.CREDIT, biatCard.cardCategory)
        assertEquals("Platinum", biatCard.cardLevel)

        // Known Chase US card
        val chaseCard = OfflineCardDetector.detectCard("4246310012345678")
        assertEquals(CardNetwork.VISA, chaseCard.network)
        assertEquals("JPMorgan Chase Bank", chaseCard.issuingBank)
        assertEquals("US", chaseCard.issuingCountryCode)

        // Unknown BIN graceful handling (valid network, null issuer)
        val unknownCard = OfflineCardDetector.detectCard("4999999999999999")
        assertEquals(CardNetwork.VISA, unknownCard.network)
        assertNull(unknownCard.issuingBank)
        assertNull(unknownCard.issuingCountryCode)
        assertNull(unknownCard.cardCategory)
    }
}

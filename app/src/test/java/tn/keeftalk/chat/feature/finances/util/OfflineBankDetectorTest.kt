package tn.keeftalk.chat.feature.finances.util

import org.junit.Assert.*
import org.junit.Test

class OfflineBankDetectorTest {

    @Test
    fun testIbanFormattingAndMod97() {
        // Tunisian IBAN test (TN59 + 2 bank + 3 branch + 13 account + 2 key = 24 chars)
        val rawTnIban = "TN5908001000123456789012"
        val formattedTn = OfflineBankDetector.formatIban(rawTnIban)
        assertEquals("TN59 0800 1000 1234 5678 9012", formattedTn)

        val detectedTn = OfflineBankDetector.detectBankAccount(rawTnIban)
        assertEquals(AccountIdentifierType.IBAN, detectedTn.identifierType)
        assertEquals("TN", detectedTn.countryCode)
        assertEquals("Tunisia", detectedTn.countryName)
        assertEquals("08", detectedTn.bankCode)
        assertEquals("Banque Internationale Arabe de Tunisie (BIAT)", detectedTn.bankName)

        // French IBAN test
        val rawFrIban = "FR7630002005501234567890185"
        val formattedFr = OfflineBankDetector.formatIban(rawFrIban)
        assertEquals("FR76 3000 2005 5012 3456 7890 185", formattedFr)

        val detectedFr = OfflineBankDetector.detectBankAccount(rawFrIban)
        assertEquals(AccountIdentifierType.IBAN, detectedFr.identifierType)
        assertEquals("FR", detectedFr.countryCode)
        assertEquals("France", detectedFr.countryName)
        assertEquals("30002", detectedFr.bankCode)
        assertEquals("BNP Paribas", detectedFr.bankName)
    }

    @Test
    fun testTunisianRibValidationAndFormatting() {
        // Tunisian RIB: 20 digits (08 BIAT + 001 guichet + 0001234567890 account + key)
        val rawRib = "08001000123456789028"
        val formattedRib = OfflineBankDetector.formatTunisianRib(rawRib)
        assertEquals("08 001 0001234567890 28", formattedRib)

        val detectedRib = OfflineBankDetector.detectBankAccount(rawRib)
        assertEquals(AccountIdentifierType.TUNISIAN_RIB, detectedRib.identifierType)
        assertEquals("TN", detectedRib.countryCode)
        assertEquals("Tunisia", detectedRib.countryName)
        assertEquals("08", detectedRib.bankCode)
        assertEquals("Banque Internationale Arabe de Tunisie (BIAT)", detectedRib.bankName)
    }

    @Test
    fun testBankCodeLookup() {
        // Tunisian Bank Codes
        val stb = BankMetadataDatabase.lookupBank("TN", "03")
        assertEquals("Société Tunisienne de Banque (STB)", stb?.bankName)

        val attijari = BankMetadataDatabase.lookupBank("TN", "17")
        assertEquals("Attijari Bank Tunisia", attijari?.bankName)

        val ccp = BankMetadataDatabase.lookupBank("TN", "81")
        assertEquals("La Poste Tunisienne (CCP)", ccp?.bankName)

        // German BLZ
        val db = BankMetadataDatabase.lookupBank("DE", "10070000")
        assertEquals("Deutsche Bank", db?.bankName)
    }
}

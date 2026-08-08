package com.keeftalk.chat.security.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.SecretKey

@RunWith(AndroidJUnit4::class)
class SecurityArchitectureTest {

    @Test
    fun aekDerivation_domainSeparation_producesUniqueKeys() {
        // Setup AEK
        val aek = KeyManager.generateAEK()
        KeyManager.setAEK(aek)

        // Derive purpose-specific keys
        val notesKey = KeyManager.getNotesKey()
        val agendaKey = KeyManager.getAgendaKey()
        val vaultKey = KeyManager.getVaultKey()
        val convKey = KeyManager.getConversationProtectionKey()
        val fileKey = KeyManager.getFileProtectionKey()

        // Verify uniqueness
        val keys = listOf(notesKey, agendaKey, vaultKey, convKey, fileKey)
        val encodedKeys = keys.map { android.util.Base64.encodeToString(it.encoded, android.util.Base64.NO_WRAP) }
        
        assertEquals("All derived keys should be unique", keys.size, encodedKeys.distinct().size)
        
        // Verify root key is different from derived keys
        val aekEncoded = android.util.Base64.encodeToString(aek.encoded, android.util.Base64.NO_WRAP)
        assertFalse("Derived key should not equal AEK", encodedKeys.contains(aekEncoded))
    }

    @Test
    fun aekWrapping_recovery_success() {
        val password = "TestPassword123!"
        val aek = KeyManager.generateAEK()
        val salt = Argon2idManager.generateSalt()
        
        // Derive KEK and Wrap AEK
        val kek = Argon2idManager.deriveKey(password, salt)
        val encryptedAEK = KeyManager.encryptAEK(aek, kek)

        // Simulate Recovery
        val recoveredKek = Argon2idManager.deriveKey(password, salt)
        val recoveredAEK = KeyManager.decryptAEK(encryptedAEK, recoveredKek)

        assertArrayEquals("Recovered AEK should match original", aek.encoded, recoveredAEK.encoded)
    }

    @Test
    fun aekVerification_tagValidation() {
        val aek = KeyManager.generateAEK()
        val tag = KeyManager.generateVerificationTag(aek)

        assertTrue("Validation should pass with correct AEK", KeyManager.validateAEK(aek, tag))

        val wrongAEK = KeyManager.generateAEK()
        assertFalse("Validation should fail with incorrect AEK", KeyManager.validateAEK(wrongAEK, tag))
    }

    @Test
    fun hmac_consistency_check() {
        val aek = KeyManager.generateAEK()
        KeyManager.setAEK(aek)
        
        val key1 = KeyManager.getNotesKey()
        val key2 = KeyManager.getNotesKey()
        
        assertArrayEquals("Derived key should be consistent for same domain", key1.encoded, key2.encoded)
    }
}

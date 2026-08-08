package com.keeftalk.chat.security.crypto

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import javax.crypto.SecretKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class FullRecoverySimulationTest {

    @Test
    fun fullAccountRecovery_simulation() {
        // --- STEP 1: INITIAL SETUP (Device A) ---
        val password = "UserStrongPassword"
        val masterAek = KeyManager.generateAEK()
        
        // Wrap AEK for cloud
        val salt = Argon2idManager.generateSalt()
        val kek = Argon2idManager.deriveKey(password, salt)
        val cloudWrappedAek = KeyManager.encryptAEK(masterAek, kek)
        val verificationTag = KeyManager.generateVerificationTag(masterAek)

        // Create a Note Root Key
        KeyManager.setAEK(masterAek)
        val notesKey = KeyManager.getNotesKey()
        
        // Encrypt a Note
        val noteContent = "My Secret Note Content"
        val encryptedNote = StorageCryptoService.encrypt(noteContent.toByteArray(), notesKey)

        // Create a Chat Key
        val cpk = KeyManager.getConversationProtectionKey()
        val pck = StorageCryptoService.generateRandomKey()
        val cloudWrappedPck = StorageCryptoService.wrapKey(pck, cpk)

        // Encrypt a Message
        val messageText = "Hello E2EE"
        val encryptedMessage = StorageCryptoService.encrypt(messageText.toByteArray(), pck)

        // --- STEP 2: RECOVERY (Device B) ---
        // Simulating clean state
        val recoveredKek = Argon2idManager.deriveKey(password, salt)
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, recoveredKek)

        // Verify AEK
        assertTrue("AEK should be valid", KeyManager.validateAEK(recoveredAek, verificationTag))
        KeyManager.setAEK(recoveredAek)

        // Recover Notes Key
        val recoveredNotesKey = KeyManager.getNotesKey()
        val decryptedNote = StorageCryptoService.decrypt(encryptedNote, recoveredNotesKey)
        assertEquals("Note should decrypt correctly", noteContent, String(decryptedNote))

        // Recover PCK
        val recoveredCpk = KeyManager.getConversationProtectionKey()
        val recoveredPck = StorageCryptoService.unwrapKey(cloudWrappedPck, recoveredCpk)
        val decryptedMessage = StorageCryptoService.decrypt(encryptedMessage, recoveredPck)
        assertEquals("Message should decrypt correctly", messageText, String(decryptedMessage))
        
        println("Full Recovery Simulation SUCCESSFUL.")
    }
}

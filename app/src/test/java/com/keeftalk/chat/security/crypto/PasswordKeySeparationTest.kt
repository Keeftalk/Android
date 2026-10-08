package tn.keeftalk.chat.security.crypto

import org.junit.Assert.*
import org.junit.Test

class PasswordKeySeparationTest {

    @Test
    fun passwordChange_existingEncryptedFiles_stillDecrypt() {
        val oldPassword = "OriginalPassword123!"
        val newPassword = "NewPassword456!"

        // 1. Initial Setup: Create AEK and FEK, encrypt file content
        val aek = KeyManager.generateAEK()
        val salt1 = Argon2idManager.generateSalt()
        val kek1 = Argon2idManager.deriveKey(oldPassword, salt1)
        var cloudWrappedAek = KeyManager.encryptAEK(aek, kek1)

        KeyManager.setAEK(aek)
        val fileProtectionKey = KeyManager.getFileProtectionKey()
        val fek = StorageCryptoService.generateRandomKey()
        val wrappedFek = StorageCryptoService.wrapKey(fek, fileProtectionKey)

        val fileData = "Confidential User Data - Old File".toByteArray(Charsets.UTF_8)
        val encryptedFile = StorageCryptoService.encrypt(fileData, fek)

        // 2. Normal Password Change: Re-wrap AEK with new password's KEK
        val salt2 = Argon2idManager.generateSalt()
        val kek2 = Argon2idManager.deriveKey(newPassword, salt2)
        cloudWrappedAek = KeyManager.encryptAEK(aek, kek2)

        // 3. Verification: Recover AEK with new password and decrypt existing file
        val recoveredKek = Argon2idManager.deriveKey(newPassword, salt2)
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, recoveredKek)
        KeyManager.setAEK(recoveredAek)

        val recoveredFpk = KeyManager.getFileProtectionKey()
        val recoveredFek = StorageCryptoService.unwrapKey(wrappedFek, recoveredFpk)
        val decryptedData = StorageCryptoService.decrypt(encryptedFile, recoveredFek)

        assertArrayEquals("Original AEK must be preserved", aek.encoded, recoveredAek.encoded)
        assertEquals("Existing file must remain decryptable after password change",
            String(fileData, Charsets.UTF_8), String(decryptedData, Charsets.UTF_8))
    }

    @Test
    fun forgotPassword_existingEncryptedFiles_stillDecrypt() {
        val initialPassword = "UserPass1!"
        val resetPassword = "ResetPass2!"

        // 1. Existing data encrypted under AEK
        val aek = KeyManager.generateAEK()
        val salt1 = Argon2idManager.generateSalt()
        val kek1 = Argon2idManager.deriveKey(initialPassword, salt1)
        var cloudWrappedAek = KeyManager.encryptAEK(aek, kek1)

        KeyManager.setAEK(aek)
        val vaultKey = KeyManager.getVaultKey()
        val vaultData = "Secret Vault Item".toByteArray(Charsets.UTF_8)
        val encryptedVault = StorageCryptoService.encrypt(vaultData, vaultKey)

        // 2. Forgot Password / Reset Flow: Existing AEK is re-wrapped under new password
        val salt2 = Argon2idManager.generateSalt()
        val kek2 = Argon2idManager.deriveKey(resetPassword, salt2)
        cloudWrappedAek = KeyManager.encryptAEK(aek, kek2)

        // 3. Login after reset with new password
        val recoveredKek = Argon2idManager.deriveKey(resetPassword, salt2)
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, recoveredKek)
        KeyManager.setAEK(recoveredAek)

        val recoveredVaultKey = KeyManager.getVaultKey()
        val decryptedVault = StorageCryptoService.decrypt(encryptedVault, recoveredVaultKey)

        assertEquals("Vault data must remain decryptable after Forgot Password reset",
            String(vaultData, Charsets.UTF_8), String(decryptedVault, Charsets.UTF_8))
    }

    @Test
    fun otpRecovery_existingEncryptedFiles_stillDecrypt() {
        val initialPassword = "PasswordAlpha!"
        val otpRecoveryPassword = "PasswordBeta!"

        val aek = KeyManager.generateAEK()
        val salt1 = Argon2idManager.generateSalt()
        val kek1 = Argon2idManager.deriveKey(initialPassword, salt1)
        var cloudWrappedAek = KeyManager.encryptAEK(aek, kek1)

        KeyManager.setAEK(aek)
        val notesKey = KeyManager.getNotesKey()
        val noteContent = "Encrypted Note before OTP recovery"
        val encryptedNote = StorageCryptoService.encrypt(noteContent.toByteArray(), notesKey)

        // OTP Recovery establishes new password and re-wraps existing AEK
        val salt2 = Argon2idManager.generateSalt()
        val kek2 = Argon2idManager.deriveKey(otpRecoveryPassword, salt2)
        cloudWrappedAek = KeyManager.encryptAEK(aek, kek2)

        // Verify recovery
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, kek2)
        KeyManager.setAEK(recoveredAek)

        val recoveredNotesKey = KeyManager.getNotesKey()
        val decryptedNote = StorageCryptoService.decrypt(encryptedNote, recoveredNotesKey)

        assertEquals("Note must remain decryptable after OTP recovery", noteContent, String(decryptedNote))
    }

    @Test
    fun logoutLogin_existingEncryptedFiles_stillDecrypt() {
        val password = "SecureLoginPass!"

        val aek = KeyManager.generateAEK()
        val salt = Argon2idManager.generateSalt()
        val kek = Argon2idManager.deriveKey(password, salt)
        val cloudWrappedAek = KeyManager.encryptAEK(aek, kek)

        KeyManager.setAEK(aek)
        val agendaKey = KeyManager.getAgendaKey()
        val agendaText = "Calendar event details"
        val encryptedAgenda = StorageCryptoService.encrypt(agendaText.toByteArray(), agendaKey)

        // Simulate Logout (clears in-memory AEK)
        KeyManager.setAEK(StorageCryptoService.generateRandomKey()) // Mutate memory key to simulate wipe
        assertFalse("In-memory key should not match original AEK after wipe",
            KeyManager.getAEK()!!.encoded.contentEquals(aek.encoded))

        // Simulate Login (fetches cloudWrappedAek and decrypts with password)
        val recoveredKek = Argon2idManager.deriveKey(password, salt)
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, recoveredKek)
        KeyManager.setAEK(recoveredAek)

        val recoveredAgendaKey = KeyManager.getAgendaKey()
        val decryptedAgenda = StorageCryptoService.decrypt(encryptedAgenda, recoveredAgendaKey)

        assertEquals("Agenda item must remain decryptable after logout/login", agendaText, String(decryptedAgenda))
    }

    @Test
    fun appRestart_existingEncryptedFiles_stillDecrypt() {
        val password = "RestartPassword!"

        val aek = KeyManager.generateAEK()
        val salt = Argon2idManager.generateSalt()
        val kek = Argon2idManager.deriveKey(password, salt)
        val cloudWrappedAek = KeyManager.encryptAEK(aek, kek)

        KeyManager.setAEK(aek)
        val convKey = KeyManager.getConversationProtectionKey()
        val pck = StorageCryptoService.generateRandomKey()
        val wrappedPck = StorageCryptoService.wrapKey(pck, convKey)
        val chatMsg = "Message before app restart"
        val encryptedMsg = StorageCryptoService.encrypt(chatMsg.toByteArray(), pck)

        // Simulate App Restart (process kill, memory cleared)
        // Memory AEK is restored from cloud/local storage
        val restoredAek = KeyManager.decryptAEK(cloudWrappedAek, kek)
        KeyManager.setAEK(restoredAek)

        val restoredConvKey = KeyManager.getConversationProtectionKey()
        val restoredPck = StorageCryptoService.unwrapKey(wrappedPck, restoredConvKey)
        val decryptedMsg = StorageCryptoService.decrypt(encryptedMsg, restoredPck)

        assertEquals("Chat message must remain decryptable after app restart", chatMsg, String(decryptedMsg))
    }

    @Test
    fun multiplePasswordChanges_filesRemainDecryptable() {
        val passwords = listOf("PassOne11!", "PassTwo22!", "PassThree33!", "PassFour44!")
        val aek = KeyManager.generateAEK()
        KeyManager.setAEK(aek)

        val fileKey = KeyManager.getFileProtectionKey()
        val fileContent = "Multi-password test document"
        val encryptedDoc = StorageCryptoService.encrypt(fileContent.toByteArray(), fileKey)

        var currentSalt = Argon2idManager.generateSalt()
        var currentKek = Argon2idManager.deriveKey(passwords.first(), currentSalt)
        var cloudWrappedAek = KeyManager.encryptAEK(aek, currentKek)

        // Perform multiple password changes sequentially
        for (i in 1 until passwords.size) {
            val newPass = passwords[i]
            val newSalt = Argon2idManager.generateSalt()
            val newKek = Argon2idManager.deriveKey(newPass, newSalt)

            // Re-wrap SAME AEK with new password
            cloudWrappedAek = KeyManager.encryptAEK(aek, newKek)
            currentSalt = newSalt
        }

        // Recover with final password
        val finalPass = passwords.last()
        val recoveredKek = Argon2idManager.deriveKey(finalPass, currentSalt)
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, recoveredKek)
        KeyManager.setAEK(recoveredAek)

        val recoveredFileKey = KeyManager.getFileProtectionKey()
        val decryptedDoc = StorageCryptoService.decrypt(encryptedDoc, recoveredFileKey)

        assertEquals("Document must remain decryptable after multiple password changes", 
            fileContent, String(decryptedDoc))
    }

    @Test
    fun newlyUploadedFilesAfterPasswordReset_decryptCorrectly() {
        val newPass = "NewPass456!"

        val aek = KeyManager.generateAEK()
        KeyManager.setAEK(aek)

        // Reset password
        val salt = Argon2idManager.generateSalt()
        val newKek = Argon2idManager.deriveKey(newPass, salt)
        val cloudWrappedAek = KeyManager.encryptAEK(aek, newKek)

        // Log in with new password
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, newKek)
        KeyManager.setAEK(recoveredAek)

        // Upload NEW file after reset
        val fpk = KeyManager.getFileProtectionKey()
        val newFek = StorageCryptoService.generateRandomKey()
        val wrappedNewFek = StorageCryptoService.wrapKey(newFek, fpk)
        val newFileContent = "File uploaded AFTER password reset"
        val encryptedNewFile = StorageCryptoService.encrypt(newFileContent.toByteArray(), newFek)

        // Decrypt new file
        val unwrappedNewFek = StorageCryptoService.unwrapKey(wrappedNewFek, fpk)
        val decryptedNewFile = StorageCryptoService.decrypt(encryptedNewFile, unwrappedNewFek)

        assertEquals("Newly uploaded file after password reset must decrypt correctly",
            newFileContent, String(decryptedNewFile))
    }

    @Test
    fun oldFilesAndNewFiles_bothRemainCompatible() {
        val oldPass = "Password2024!"
        val newPass = "Password2025!"

        val aek = KeyManager.generateAEK()
        KeyManager.setAEK(aek)

        // 1. Encrypt OLD file before password change
        val fpkBefore = KeyManager.getFileProtectionKey()
        val oldFek = StorageCryptoService.generateRandomKey()
        val wrappedOldFek = StorageCryptoService.wrapKey(oldFek, fpkBefore)
        val oldContent = "OLD File Content Created Before Reset"
        val encryptedOldFile = StorageCryptoService.encrypt(oldContent.toByteArray(), oldFek)

        // 2. Change password (re-wrapping AEK)
        val salt = Argon2idManager.generateSalt()
        val kek = Argon2idManager.deriveKey(newPass, salt)
        val cloudWrappedAek = KeyManager.encryptAEK(aek, kek)

        // 3. Recover AEK on new session
        val recoveredAek = KeyManager.decryptAEK(cloudWrappedAek, kek)
        KeyManager.setAEK(recoveredAek)

        // 4. Encrypt NEW file after password change
        val fpkAfter = KeyManager.getFileProtectionKey()
        val newFek = StorageCryptoService.generateRandomKey()
        val wrappedNewFek = StorageCryptoService.wrapKey(newFek, fpkAfter)
        val newContent = "NEW File Content Created After Reset"
        val encryptedNewFile = StorageCryptoService.encrypt(newContent.toByteArray(), newFek)

        // 5. Decrypt BOTH files and verify compatibility
        val unwrappedOldFek = StorageCryptoService.unwrapKey(wrappedOldFek, fpkAfter)
        val decryptedOldFile = StorageCryptoService.decrypt(encryptedOldFile, unwrappedOldFek)

        val unwrappedNewFek = StorageCryptoService.unwrapKey(wrappedNewFek, fpkAfter)
        val decryptedNewFile = StorageCryptoService.decrypt(encryptedNewFile, unwrappedNewFek)

        assertEquals("Old file must decrypt with post-reset AEK", oldContent, String(decryptedOldFile))
        assertEquals("New file must decrypt with post-reset AEK", newContent, String(decryptedNewFile))
    }
}

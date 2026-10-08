package tn.keeftalk.chat.security.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordKeySeparationRobustnessTest {

    private lateinit var context: Context
    private val testUserId = "test_user_separation_123"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        KeyManager.init(context)
        runBlocking {
            KeyManager.clearAEK(context, testUserId, clearDisk = true)
        }
    }

    @Test
    fun localStorage_retainsAEKAcrossLogout_enablingOTPRecovery() = runBlocking {
        val originalPass = "UserPass2024!"
        val aek = KeyManager.generateAEK()

        // 1. Persist AEK to local EncryptedSharedPreferences on device
        KeyManager.persistAEK(context, testUserId, aek)
        val salt = Argon2idManager.generateSalt()
        val kek = Argon2idManager.deriveKey(originalPass, salt)
        val cloudWrappedAek = KeyManager.encryptAEK(aek, kek)

        // 2. Perform Logout (Memory cleared, but Disk retained for recovery)
        KeyManager.clearAEK(context, testUserId, clearDisk = false)
        assertNull("Memory AEK should be null after logout", KeyManager.getAEK())

        // 3. Perform OTP Password Reset: Local AEK is restored from EncryptedSharedPreferences
        val restoredLocally = KeyManager.restoreAEK(context, testUserId)
        assertTrue("AEK should be restored from EncryptedSharedPreferences on device", restoredLocally)
        assertNotNull("Restored AEK should be available in memory", KeyManager.getAEK())

        // 4. Re-wrap restored AEK with new password
        val resetPass = "UserPass2025!"
        val salt2 = Argon2idManager.generateSalt()
        val kek2 = Argon2idManager.deriveKey(resetPass, salt2)
        val newCloudWrappedAek = KeyManager.encryptAEK(KeyManager.getAEK()!!, kek2)

        // 5. Decrypt new wrapped AEK with new password
        val recoveredAek = KeyManager.decryptAEK(newCloudWrappedAek, kek2)
        assertArrayEquals("AEK must remain identical across password reset", aek.encoded, recoveredAek.encoded)
    }

    @Test
    fun passwordChange_preservesDomainSpecificKeys() = runBlocking {
        val newPass = "BetaPassword2!"

        val aek = KeyManager.generateAEK()
        KeyManager.setAEK(aek)

        val notesKeyBefore = KeyManager.getNotesKey()
        val agendaKeyBefore = KeyManager.getAgendaKey()
        val vaultKeyBefore = KeyManager.getVaultKey()
        val fileKeyBefore = KeyManager.getFileProtectionKey()
        val convKeyBefore = KeyManager.getConversationProtectionKey()

        // Re-wrap AEK for password change
        val salt = Argon2idManager.generateSalt()
        val newKek = Argon2idManager.deriveKey(newPass, salt)
        val wrappedAek = KeyManager.encryptAEK(aek, newKek)

        // Recover AEK under new password
        val recoveredAek = KeyManager.decryptAEK(wrappedAek, newKek)
        KeyManager.setAEK(recoveredAek)

        val notesKeyAfter = KeyManager.getNotesKey()
        val agendaKeyAfter = KeyManager.getAgendaKey()
        val vaultKeyAfter = KeyManager.getVaultKey()
        val fileKeyAfter = KeyManager.getFileProtectionKey()
        val convKeyAfter = KeyManager.getConversationProtectionKey()

        assertArrayEquals("Notes Key must match after password change", notesKeyBefore.encoded, notesKeyAfter.encoded)
        assertArrayEquals("Agenda Key must match after password change", agendaKeyBefore.encoded, agendaKeyAfter.encoded)
        assertArrayEquals("Vault Key must match after password change", vaultKeyBefore.encoded, vaultKeyAfter.encoded)
        assertArrayEquals("File Key must match after password change", fileKeyBefore.encoded, fileKeyAfter.encoded)
        assertArrayEquals("Conversation Key must match after password change", convKeyBefore.encoded, convKeyAfter.encoded)
    }
}

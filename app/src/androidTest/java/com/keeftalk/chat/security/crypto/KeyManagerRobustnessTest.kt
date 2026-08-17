package com.keeftalk.chat.security.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyManagerRobustnessTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        KeyManager.init(context)
        // Clear AEK for a clean start
        runBlocking {
            KeyManager.clearAEK(context, "user1")
            KeyManager.clearAEK(context, "user2")
        }
    }

    @Test
    fun userScopedPersistence_preventsKeyLeakageBetweenUsers() = runBlocking {
        val aek1 = KeyManager.generateAEK()
        val aek2 = KeyManager.generateAEK()

        KeyManager.persistAEK(context, "user1", aek1)
        KeyManager.persistAEK(context, "user2", aek2)

        KeyManager.restoreAEK(context, "user1")
        assertArrayEquals("Should restore AEK for user1", aek1.encoded, KeyManager.getAEK()!!.encoded)

        KeyManager.restoreAEK(context, "user2")
        assertArrayEquals("Should restore AEK for user2", aek2.encoded, KeyManager.getAEK()!!.encoded)
    }

    @Test
    fun clearAEK_onlyRemovesSpecifiedUserKey() = runBlocking {
        val aek1 = KeyManager.generateAEK()
        val aek2 = KeyManager.generateAEK()

        KeyManager.persistAEK(context, "user1", aek1)
        KeyManager.persistAEK(context, "user2", aek2)

        KeyManager.clearAEK(context, "user1")
        
        assertFalse("AEK for user1 should be gone", KeyManager.restoreAEK(context, "user1"))
        assertTrue("AEK for user2 should remain", KeyManager.restoreAEK(context, "user2"))
    }

    @Test
    fun migrationFromLegacyStorage_success() = runBlocking {
        val aekLegacy = KeyManager.generateAEK()
        
        // Manually put it into legacy storage (simulating old version)
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()

        val legacyPrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "secure_key_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        legacyPrefs.edit()
            .putString("wrapped_aek", android.util.Base64.encodeToString(aekLegacy.encoded, android.util.Base64.NO_WRAP))
            .commit()

        // Verify restoration/migration
        assertTrue("Restoration should succeed via migration", KeyManager.restoreAEK(context, "new_user"))
        assertArrayEquals("Migrated key should match legacy", aekLegacy.encoded, KeyManager.getAEK()!!.encoded)
        
        // Verify legacy is cleared
        assertFalse("Legacy storage should be empty after migration", legacyPrefs.contains("wrapped_aek"))
        
        // Verify new user-scoped storage has the key
        KeyManager.setAEK(KeyManager.generateAEK()) // Change in memory
        assertTrue("Should still be able to restore from new storage", KeyManager.restoreAEK(context, "new_user"))
        assertArrayEquals("Restored key should match legacy", aekLegacy.encoded, KeyManager.getAEK()!!.encoded)
    }
}

package com.keeftalk.chat.security.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryRobustnessTest {

    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        KeyManager.init(context)
        securityManager = SecurityManager(context, KeyManager)
        runBlocking {
            KeyManager.clearAEK(context, "robust_user")
        }
    }

    @Test
    fun getEncryptionContext_suspendsUntilReady() = runBlocking {
        securityManager.initializeForUser("robust_user")
        assertEquals(SecurityState.RECOVERY_REQUIRED, securityManager.state.value)

        val aek = KeyManager.generateAEK()
        
        val deferredResult = async {
            securityManager.getEncryptionContext()
        }

        // Give it some time to be "suspended"
        delay(500)
        assertFalse("Should still be suspended", deferredResult.isCompleted)

        // Trigger recovery
        securityManager.onRecoverySuccess(aek)

        val result = withTimeout(2000) { deferredResult.await() }
        assertArrayEquals(aek.encoded, result.encoded)
        assertEquals(SecurityState.READY, securityManager.state.value)
    }

    @Test
    fun recoveryToken_persistence_works() = runBlocking {
        val kek = StorageCryptoService.generateRandomKey()
        
        KeyManager.persistRecoveryToken(context, "robust_user", kek)
        val restoredKek = KeyManager.getRecoveryToken(context, "robust_user")
        
        assertNotNull(restoredKek)
        assertArrayEquals(kek.encoded, restoredKek?.encoded)
        
        KeyManager.clearAEK(context, "robust_user")
        assertNull(KeyManager.getRecoveryToken(context, "robust_user"))
    }
}

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
class SecurityManagerTest {

    private lateinit var context: Context
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        KeyManager.init(context)
        securityManager = SecurityManager(context, KeyManager)
        runBlocking {
            KeyManager.clearAEK(context, "test_user")
        }
    }

    @Test
    fun initialization_missingAEK_transitionsToRecoveryRequired() = runBlocking {
        securityManager.initializeForUser("test_user")
        assertEquals(SecurityState.RECOVERY_REQUIRED, securityManager.state.value)
        
        try {
            securityManager.getEncryptionContext()
            fail("Should have thrown SecurityRecoveryRequiredException")
        } catch (_: SecurityRecoveryRequiredException) {
            // Success
        }
    }

    @Test
    fun initialization_existingAEK_transitionsToReady() = runBlocking {
        val aek = KeyManager.generateAEK()
        KeyManager.persistAEK(context, "test_user", aek)
        
        securityManager.initializeForUser("test_user")
        assertEquals(SecurityState.READY, securityManager.state.value)
        assertArrayEquals(aek.encoded, securityManager.getEncryptionContext().encoded)
    }

    @Test
    fun onRecoverySuccess_transitionsToReady() = runBlocking {
        securityManager.initializeForUser("test_user")
        assertEquals(SecurityState.RECOVERY_REQUIRED, securityManager.state.value)
        
        val aek = KeyManager.generateAEK()
        securityManager.onRecoverySuccess(aek)
        
        assertEquals(SecurityState.READY, securityManager.state.value)
        assertArrayEquals(aek.encoded, securityManager.getEncryptionContext().encoded)
    }
}

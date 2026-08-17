package com.keeftalk.chat.security.crypto

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.crypto.SecretKey

enum class SecurityState {
    INITIALIZING,
    READY,
    RECOVERY_REQUIRED,
    ERROR
}

/**
 * Manages the application-level security lifecycle and encryption context readiness.
 * Decouples encryption requirements from authentication status.
 */
class SecurityManager(
    private val context: Context,
    private val keyManager: KeyManager
) {
    private val TAG = "SecurityManager"

    private val _state = MutableStateFlow(SecurityState.INITIALIZING)
    val state: StateFlow<SecurityState> = _state.asStateFlow()

    private val initMutex = Mutex()

    @Volatile
    private var currentUserId: String? = null

    /**
     * Attempts to initialize the encryption context for the given user.
     */
    suspend fun initializeForUser(userId: String) = initMutex.withLock {
        if (currentUserId == userId && _state.value == SecurityState.READY) {
            Log.d(TAG, "Security context already READY for user: $userId. Skipping re-init.")
            return@withLock
        }

        Log.i(TAG, "Initializing security context for user: $userId (Previous state: ${_state.value})")
        _state.value = SecurityState.INITIALIZING
        currentUserId = userId
        
        try {
            // KeyManager handles the actual restoration/persistence synchronization
            val restored = keyManager.restoreAEK(context, userId)
            if (restored) {
                Log.i(TAG, "Security context READY for user: $userId")
                _state.value = SecurityState.READY
            } else {
                Log.w(TAG, "AEK missing for user: $userId. Recovery required.")
                _state.value = SecurityState.RECOVERY_REQUIRED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize security context for user: $userId", e)
            _state.value = SecurityState.ERROR
        }
    }

    /**
     * Provides the current Account Encryption Key (AEK) if ready.
     * Waits if the state is INITIALIZING or RECOVERY_REQUIRED.
     * @throws SecurityRecoveryRequiredException if the AEK is not available and recovery is not possible.
     */
    suspend fun getEncryptionContext(): SecretKey {
        val currentState = _state.value
        if (currentState == SecurityState.INITIALIZING || currentState == SecurityState.RECOVERY_REQUIRED) {
            Log.i(TAG, "Encryption context requested while $currentState. Waiting for READY...")
            // Wait until state is READY or ERROR
            val finalState = state.first { it == SecurityState.READY || it == SecurityState.ERROR }
            if (finalState == SecurityState.ERROR) {
                Log.e(TAG, "Encryption context wait failed: Security state is ERROR")
                throw SecurityRecoveryRequiredException()
            }
        }
        
        return keyManager.getAEK() ?: throw SecurityRecoveryRequiredException()
    }

    /**
     * Reports that a successful recovery has occurred, transitioning the state to READY.
     */
    fun onRecoverySuccess(aek: SecretKey) {
        Log.i(TAG, "Security recovery successful. Context is now READY.")
        keyManager.setAEK(aek)
        _state.value = SecurityState.READY
    }

    /**
     * Resets the security state (e.g., on logout).
     */
    fun reset() {
        Log.i(TAG, "Resetting security manager")
        _state.value = SecurityState.INITIALIZING
    }
}

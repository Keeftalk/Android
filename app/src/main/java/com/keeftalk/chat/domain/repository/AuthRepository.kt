package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserProfile: Flow<Profile?>
    val isLogged: Flow<Boolean>
    val isEncryptionContextAvailable: Flow<Boolean>

    suspend fun login(identifier: String, password: String): Result<Unit>
    suspend fun signup(
        fullName: String,
        username: String,
        email: String,
        phone: String,
        password: String,
        country: String? = null,
        countryCode: String? = null,
        phoneCountryCode: String? = null
    ): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentSession(): Profile?
    suspend fun getAuthenticatedUser(): io.github.jan.supabase.auth.user.UserInfo?
    
    suspend fun updateProfile(profile: Profile): Result<Unit>
    suspend fun uploadAvatar(byteArray: ByteArray): Result<String>
    suspend fun uploadCover(byteArray: ByteArray): Result<String>
    suspend fun getProfile(userId: String): Result<Profile>
    suspend fun checkUsernameAvailability(username: String): Result<Boolean>
    suspend fun updateFcmToken(token: String): Result<Unit>
    suspend fun updatePresence(isActive: Boolean): Result<Unit>
    suspend fun refreshProfile(userId: String): Result<Unit>
    suspend fun optimisticUpdateStorageUsed(userId: String, delta: Long)

    suspend fun sendPasswordResetOtp(email: String): Result<Unit>
    suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun resetSecuritySettings(): Result<Unit>
    suspend fun recoverSecurityContext(password: String): Result<Unit>
    
    // Email Verification Features
    val isEmailVerified: Flow<Boolean>
    suspend fun resendVerificationEmail(email: String? = null): Result<Unit>
    suspend fun updateEmail(newEmail: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun refreshSession(): Result<Unit>

    fun handleDeepLink(intent: android.content.Intent)
    fun startSessionObservation()

    suspend fun findProfilesByIdentifier(identifier: String): Result<List<com.keeftalk.chat.domain.model.Profile>>

    suspend fun verifySubscriptionPurchase(purchaseToken: String, productId: String): Result<Unit>

    fun shutdown()

    /**
     * Suspends until the authentication session is restored and the security context is READY.
     * Useful for gating long-running operations that require E2EE.
     */
    suspend fun awaitReady()
}

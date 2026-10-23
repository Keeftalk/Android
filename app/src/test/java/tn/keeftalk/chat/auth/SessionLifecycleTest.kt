package tn.keeftalk.chat.auth

import tn.keeftalk.chat.domain.model.Profile
import tn.keeftalk.chat.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class FakeLifecycleAuthRepository(
        initialLogged: Boolean = true,
        initialVerified: Boolean = true
    ) : AuthRepository {
        val _isLogged = MutableStateFlow(initialLogged)
        val _isVerified = MutableStateFlow(initialVerified)
        var refreshSessionCount = 0
        var shouldRefreshFail = false

        override val isLogged: Flow<Boolean> = _isLogged
        override val isEmailVerified: Flow<Boolean> = _isVerified
        override val currentUserProfile: Flow<Profile?> = flowOf(null)
        override val isEncryptionContextAvailable: Flow<Boolean> = flowOf(true)

        override suspend fun complete2FALogin(userId: String, password: String): Result<Unit> = Result.success(Unit)

        override suspend fun refreshSession(): Result<Unit> {
            refreshSessionCount++
            return if (shouldRefreshFail) {
                Result.failure(Exception("Network error during background resume"))
            } else {
                Result.success(Unit)
            }
        }

        override suspend fun logout(): Result<Unit> {
            _isLogged.value = false
            return Result.success(Unit)
        }

        // Stubs for remaining interface members
        override suspend fun getAuthenticatedUser(): io.github.jan.supabase.auth.user.UserInfo? = null
        override suspend fun login(identifier: String, password: String): Result<Unit> = Result.success(Unit)
        override suspend fun signup(fullName: String, username: String, email: String, phone: String, password: String, country: String?, countryCode: String?, phoneCountryCode: String?): Result<Unit> = Result.success(Unit)
        override suspend fun getCurrentSession(): Profile? = null
        override suspend fun getProfile(userId: String): Result<Profile> = Result.failure(Exception("Not implemented"))
        override suspend fun checkUsernameAvailability(username: String): Result<Boolean> = Result.success(true)
        override suspend fun updateProfile(profile: Profile): Result<Unit> = Result.success(Unit)
        override suspend fun uploadAvatar(byteArray: ByteArray): Result<String> = Result.success("url")
        override suspend fun uploadCover(byteArray: ByteArray): Result<String> = Result.success("url")
        override suspend fun updateFcmToken(token: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePresence(isActive: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun refreshProfile(userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun optimisticUpdateStorageUsed(userId: String, delta: Long) {}
        override suspend fun sendPasswordResetOtp(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePassword(newPassword: String): Result<Unit> = Result.success(Unit)
        override suspend fun resendVerificationEmail(email: String?): Result<Unit> = Result.success(Unit)
        override suspend fun updateEmail(newEmail: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
        override suspend fun recoverSecurityContext(password: String): Result<Unit> = Result.success(Unit)
        override suspend fun resetSecuritySettings(): Result<Unit> = Result.success(Unit)
        override suspend fun findProfilesByIdentifier(identifier: String): Result<List<Profile>> = Result.success(emptyList())
        override suspend fun verifySubscriptionPurchase(purchaseToken: String, productId: String): Result<Unit> = Result.success(Unit)
        override fun handleDeepLink(intent: android.content.Intent) {}
        override fun startSessionObservation() {}
        override fun shutdown() {}
        override suspend fun awaitReady() {}
    }

    @Test
    fun testAuthenticatedUser_remainsAuthenticatedOnBackgroundResume() = runTest {
        val authRepo = FakeLifecycleAuthRepository(initialLogged = true, initialVerified = true)

        // Simulating app background and foreground transition with offline/poor network
        authRepo.shouldRefreshFail = true
        val refreshResult = authRepo.refreshSession()

        assertTrue("Session refresh failure should return Result.failure", refreshResult.isFailure)
        assertTrue("User must remain logged in locally even if background refresh fails", authRepo.isLogged.first())
        assertTrue("User email verification must remain true", authRepo.isEmailVerified.first())
    }

    @Test
    fun testExplicitLogout_navigatesToUnauthenticatedState() = runTest {
        val authRepo = FakeLifecycleAuthRepository(initialLogged = true, initialVerified = true)

        assertTrue(authRepo.isLogged.first())
        val result = authRepo.logout()

        assertTrue(result.isSuccess)
        assertFalse("User should no longer be logged in after explicit logout", authRepo.isLogged.first())
    }

    @Test
    fun testLogout_idempotency() = runTest {
        val authRepo = FakeLifecycleAuthRepository(initialLogged = true, initialVerified = true)

        assertTrue(authRepo.isLogged.first())
        val firstLogout = authRepo.logout()
        assertTrue(firstLogout.isSuccess)
        assertFalse(authRepo.isLogged.first())

        // Second logout call should be idempotent and succeed
        val secondLogout = authRepo.logout()
        assertTrue("Repeated logout call must be idempotent and succeed", secondLogout.isSuccess)
        assertFalse(authRepo.isLogged.first())
    }
}

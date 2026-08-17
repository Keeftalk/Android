package com.keeftalk.chat.feature.auth.forgotpassword.viewmodel

import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.domain.repository.AuthRepository
import com.keeftalk.chat.feature.auth.forgotpassword.state.ForgotPasswordStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private lateinit var viewModel: ForgotPasswordViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ForgotPasswordViewModel(FakeAuthRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is ENTER_IDENTITY`() {
        assertEquals(ForgotPasswordStep.ENTER_IDENTITY, viewModel.forgotPasswordState.value.step)
    }

    @Test
    fun `findAccount success with one profile transitions to CONFIRM_PROFILE`() = runTest {
        viewModel.onIdentifierChange("test@example.com")
        viewModel.findAccount()
        
        advanceUntilIdle()
        
        assertEquals(ForgotPasswordStep.CONFIRM_PROFILE, viewModel.forgotPasswordState.value.step)
        assertEquals("test@example.com", viewModel.forgotPasswordState.value.selectedProfile?.email)
    }

    @Test
    fun `sendOtp success transitions to VERIFY_OTP`() = runTest {
        viewModel.onIdentifierChange("test@example.com")
        viewModel.findAccount()
        advanceUntilIdle()
        
        viewModel.sendOtp()
        advanceUntilIdle()
        
        assertEquals(ForgotPasswordStep.VERIFY_OTP, viewModel.forgotPasswordState.value.step)
    }

    @Test
    fun `verifyOtp success transitions to CREATE_NEW_PASSWORD`() = runTest {
        viewModel.onIdentifierChange("test@example.com")
        viewModel.findAccount()
        advanceUntilIdle()
        viewModel.sendOtp()
        advanceUntilIdle()
        
        viewModel.onOtpChange("123456")
        viewModel.verifyOtp()
        advanceUntilIdle()
        
        assertEquals(ForgotPasswordStep.CREATE_NEW_PASSWORD, viewModel.forgotPasswordState.value.step)
    }

    @Test
    fun `updatePassword success transitions to CONFIRMATION`() = runTest {
        viewModel.onIdentifierChange("test@example.com")
        viewModel.findAccount()
        advanceUntilIdle()
        viewModel.sendOtp()
        advanceUntilIdle()
        viewModel.onOtpChange("123456")
        viewModel.verifyOtp()
        advanceUntilIdle()
        
        viewModel.onNewPasswordChange("NewPass123!")
        viewModel.onConfirmPasswordChange("NewPass123!")
        viewModel.updatePassword()
        advanceUntilIdle()
        
        assertEquals(ForgotPasswordStep.CONFIRMATION, viewModel.forgotPasswordState.value.step)
    }

    class FakeAuthRepository : AuthRepository {
        override val currentUserProfile: Flow<Profile?> = flowOf(null)
        override val isLogged: Flow<Boolean> = flowOf(false)
        override val isEmailVerified: Flow<Boolean> = flowOf(false)
        override val isEncryptionContextAvailable: Flow<Boolean> = flowOf(true)

        override suspend fun getAuthenticatedUser(): io.github.jan.supabase.auth.user.UserInfo? = null

        override suspend fun findProfilesByIdentifier(identifier: String): Result<List<Profile>> {
            return if (identifier == "test@example.com") {
                Result.success(listOf(Profile(id = "1", username = "test", email = "test@example.com")))
            } else {
                Result.success(emptyList())
            }
        }

        override suspend fun sendPasswordResetOtp(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyPasswordResetOtp(email: String, otp: String): Result<Unit> = Result.success(Unit)
        override suspend fun updatePassword(newPassword: String): Result<Unit> = Result.success(Unit)
        override suspend fun recoverSecurityContext(password: String): Result<Unit> = Result.success(Unit)

        // Stubs for other methods
        override suspend fun login(identifier: String, password: String): Result<Unit> = TODO()
        override suspend fun signup(fullName: String, username: String, email: String, phone: String, password: String, country: String?, countryCode: String?, phoneCountryCode: String?): Result<Unit> = TODO()
        override suspend fun logout(): Result<Unit> = TODO()
        override suspend fun getCurrentSession(): Profile? = TODO()
        override suspend fun updateProfile(profile: Profile): Result<Unit> = TODO()
        override suspend fun uploadAvatar(byteArray: ByteArray): Result<String> = TODO()
        override suspend fun uploadCover(byteArray: ByteArray): Result<String> = TODO()
        override suspend fun getProfile(userId: String): Result<Profile> = TODO()
        override suspend fun checkUsernameAvailability(username: String): Result<Boolean> = TODO()
        override suspend fun updateFcmToken(token: String): Result<Unit> = TODO()
        override suspend fun updatePresence(isActive: Boolean): Result<Unit> = TODO()
        override suspend fun resendVerificationEmail(email: String?): Result<Unit> = TODO()
        override suspend fun updateEmail(newEmail: String): Result<Unit> = TODO()
        override suspend fun deleteAccount(): Result<Unit> = TODO()
        override suspend fun refreshSession(): Result<Unit> = TODO()
        override fun handleDeepLink(intent: android.content.Intent) = TODO()
        override fun startSessionObservation() = TODO()
        override suspend fun verifySubscriptionPurchase(purchaseToken: String, productId: String): Result<Unit> = TODO()
        override suspend fun refreshProfile(userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun optimisticUpdateStorageUsed(userId: String, delta: Long) {}
        override suspend fun resetSecuritySettings(): Result<Unit> = Result.success(Unit)
        override fun shutdown() = TODO()

        override suspend fun awaitReady() {}
    }
}

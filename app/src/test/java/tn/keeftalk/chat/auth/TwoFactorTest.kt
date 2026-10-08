package tn.keeftalk.chat.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.TwoFactorRepository
import tn.keeftalk.chat.ui.security.TwoFactorUiState
import tn.keeftalk.chat.ui.security.TwoFactorViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class TwoFactorTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class FakeTwoFactorRepository : TwoFactorRepository {
        private val _status = MutableStateFlow<TwoFactorStatus?>(TwoFactorStatus())
        override val status: Flow<TwoFactorStatus?> = _status

        override suspend fun get2FAStatus(): Result<TwoFactorStatus> {
            return Result.success(_status.value ?: TwoFactorStatus())
        }

        override suspend fun beginTotpEnrollment(): Result<TotpEnrollmentData> {
            return Result.success(
                TotpEnrollmentData(
                    factorId = "f123",
                    secret = "JBSWY3DPEHPK3PXP",
                    qrCodeUri = "otpauth://totp/Keeftalk:user@test.com?secret=JBSWY3DPEHPK3PXP&issuer=Keeftalk"
                )
            )
        }

        override suspend fun verifyTotpEnrollment(totpCode: String): Result<Unit> {
            return if (totpCode == "123456") {
                _status.value = _status.value?.copy(twoFactorEnabled = true)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid TOTP code"))
            }
        }

        override suspend fun requestEmailOtp(purpose: String): Result<OtpChallengeResponse> {
            return Result.success(OtpChallengeResponse(success = true, challengeId = "c123", maskedDestination = "u***@test.com"))
        }

        override suspend fun requestSmsOtp(purpose: String): Result<OtpChallengeResponse> {
            return Result.success(OtpChallengeResponse(success = true, challengeId = "c456", maskedDestination = "+1 *** *** 7890"))
        }

        override suspend fun verifyOtpChallenge(challengeId: String, otpCode: String): Result<Boolean> {
            return if (otpCode == "654321") Result.success(true) else Result.failure(Exception("Invalid OTP"))
        }

        override suspend fun generateBackupCodes(): Result<List<String>> {
            val codes = listOf("A1B2-C3D4", "E5F6-G7H8", "I9J0-K1L2")
            _status.value = _status.value?.copy(hasBackupCodes = true)
            return Result.success(codes)
        }

        override suspend fun createLoginChallenge(userId: String): Result<TwoFactorLoginChallenge> {
            return Result.success(
                TwoFactorLoginChallenge(
                    requires2FA = true,
                    loginChallengeId = "ch_login_1",
                    userId = userId,
                    allowedFactors = listOf(TwoFactorMethod.TOTP, TwoFactorMethod.EMAIL)
                )
            )
        }

        override suspend fun verifyLoginChallenge(
            loginChallengeId: String,
            factorType: TwoFactorMethod,
            code: String
        ): Result<TwoFactorVerificationResult> {
            return if (code == "123456" || code == "A1B2-C3D4") {
                Result.success(TwoFactorVerificationResult(success = true, verificationToken = "tok_abc", userId = "u1"))
            } else {
                Result.failure(Exception("Invalid verification code"))
            }
        }

        override suspend fun disable2FAMethod(factorType: TwoFactorMethod): Result<Unit> {
            _status.value = _status.value?.copy(twoFactorEnabled = false)
            return Result.success(Unit)
        }

        override suspend fun setPreferredMethod(factorType: TwoFactorMethod): Result<Unit> {
            _status.value = _status.value?.copy(preferredMethod = factorType)
            return Result.success(Unit)
        }
    }

    @Test
    fun `test TwoFactorMethod serialization mapping`() {
        assertEquals("totp", TwoFactorMethod.TOTP.toServerString())
        assertEquals("email", TwoFactorMethod.EMAIL.toServerString())
        assertEquals("sms", TwoFactorMethod.SMS.toServerString())
        assertEquals("backup_code", TwoFactorMethod.BACKUP_CODE.toServerString())

        assertEquals(TwoFactorMethod.TOTP, TwoFactorMethod.fromServerString("totp"))
        assertEquals(TwoFactorMethod.EMAIL, TwoFactorMethod.fromServerString("email"))
        assertEquals(TwoFactorMethod.SMS, TwoFactorMethod.fromServerString("sms"))
        assertEquals(TwoFactorMethod.BACKUP_CODE, TwoFactorMethod.fromServerString("backup_code"))
    }

    @Test
    fun `test begin TOTP enrollment updates ViewModel state`() = runTest {
        val repo = FakeTwoFactorRepository()
        val vm = TwoFactorViewModel(repo)

        vm.beginTotpEnrollment()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is TwoFactorUiState.TotpEnrollmentStarted)
        val data = (state as TwoFactorUiState.TotpEnrollmentStarted).data
        assertEquals("JBSWY3DPEHPK3PXP", data.secret)
    }

    @Test
    fun `test verify TOTP success`() = runTest {
        val repo = FakeTwoFactorRepository()
        val vm = TwoFactorViewModel(repo)

        vm.verifyTotpEnrollment("123456")
        advanceUntilIdle()

        assertTrue(vm.uiState.value is TwoFactorUiState.Success)
    }

    @Test
    fun `test generate backup codes`() = runTest {
        val repo = FakeTwoFactorRepository()
        val vm = TwoFactorViewModel(repo)

        vm.generateBackupCodes()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is TwoFactorUiState.BackupCodesGenerated)
        val codes = (state as TwoFactorUiState.BackupCodesGenerated).codes
        assertEquals(3, codes.size)
    }
}

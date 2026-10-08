package tn.keeftalk.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.domain.model.*

interface TwoFactorRepository {
    val status: Flow<TwoFactorStatus?>

    suspend fun get2FAStatus(): Result<TwoFactorStatus>
    suspend fun beginTotpEnrollment(): Result<TotpEnrollmentData>
    suspend fun verifyTotpEnrollment(totpCode: String): Result<Unit>
    suspend fun requestEmailOtp(purpose: String = "ENROLLMENT"): Result<OtpChallengeResponse>
    suspend fun requestSmsOtp(purpose: String = "ENROLLMENT"): Result<OtpChallengeResponse>
    suspend fun verifyOtpChallenge(challengeId: String, otpCode: String): Result<Boolean>
    suspend fun generateBackupCodes(): Result<List<String>>
    suspend fun createLoginChallenge(userId: String): Result<TwoFactorLoginChallenge>
    suspend fun verifyLoginChallenge(loginChallengeId: String, factorType: TwoFactorMethod, code: String): Result<TwoFactorVerificationResult>
    suspend fun disable2FAMethod(factorType: TwoFactorMethod): Result<Unit>
    suspend fun setPreferredMethod(factorType: TwoFactorMethod): Result<Unit>
}

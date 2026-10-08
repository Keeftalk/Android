package tn.keeftalk.chat.data.repository

import android.content.Context
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tn.keeftalk.chat.data.remote.*
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.domain.repository.TwoFactorRepository

class TwoFactorRepositoryImpl(
    private val context: Context
) : TwoFactorRepository {

    private val TAG = "TwoFactorRepo"

    private val _status = MutableStateFlow<TwoFactorStatus?>(null)
    override val status: StateFlow<TwoFactorStatus?> = _status.asStateFlow()

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    override suspend fun get2FAStatus(): Result<TwoFactorStatus> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val dto = supabase.postgrest.rpc("rpc_get_2fa_status").decodeAs<TwoFactorStatusDto>()
            val domain = dto.toDomain()
            _status.value = domain
            Result.success(domain)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get 2FA status", e)
            Result.failure(e)
        }
    }

    override suspend fun beginTotpEnrollment(): Result<TotpEnrollmentData> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val dto = supabase.postgrest.rpc("rpc_begin_totp_enrollment").decodeAs<TotpEnrollmentDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to begin TOTP enrollment", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyTotpEnrollment(totpCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject { put("p_totp_code", totpCode) }
            val response = supabase.postgrest.rpc("rpc_verify_totp_enrollment", params).decodeAs<TwoFactorVerificationResultDto>()
            if (response.success) {
                get2FAStatus()
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Invalid TOTP code"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify TOTP enrollment", e)
            Result.failure(e)
        }
    }

    override suspend fun requestEmailOtp(purpose: String): Result<OtpChallengeResponse> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject { put("p_purpose", purpose) }
            val dto = supabase.postgrest.rpc("rpc_request_email_otp", params).decodeAs<OtpRequestResponseDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request email OTP", e)
            Result.failure(e)
        }
    }

    override suspend fun requestSmsOtp(purpose: String): Result<OtpChallengeResponse> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject { put("p_purpose", purpose) }
            val dto = supabase.postgrest.rpc("rpc_request_sms_otp", params).decodeAs<OtpRequestResponseDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request SMS OTP", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyOtpChallenge(challengeId: String, otpCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject {
                put("p_challenge_id", challengeId)
                put("p_otp_code", otpCode)
            }
            val dto = supabase.postgrest.rpc("rpc_verify_otp_challenge", params).decodeAs<TwoFactorVerificationResultDto>()
            if (dto.success) {
                get2FAStatus()
                Result.success(true)
            } else {
                Result.failure(Exception(dto.message ?: "Verification failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify OTP challenge", e)
            Result.failure(e)
        }
    }

    override suspend fun generateBackupCodes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val dto = supabase.postgrest.rpc("rpc_generate_backup_codes").decodeAs<BackupCodesResponseDto>()
            if (dto.success) {
                get2FAStatus()
                Result.success(dto.backupCodes)
            } else {
                Result.failure(Exception("Failed to generate backup codes"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate backup codes", e)
            Result.failure(e)
        }
    }

    override suspend fun createLoginChallenge(userId: String): Result<TwoFactorLoginChallenge> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject { put("p_user_id", userId) }
            val dto = supabase.postgrest.rpc("rpc_create_2fa_login_challenge", params).decodeAs<TwoFactorLoginChallengeDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create 2FA login challenge", e)
            Result.failure(e)
        }
    }

    override suspend fun verifyLoginChallenge(
        loginChallengeId: String,
        factorType: TwoFactorMethod,
        code: String
    ): Result<TwoFactorVerificationResult> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject {
                put("p_login_challenge_id", loginChallengeId)
                put("p_factor_type", factorType.toServerString())
                put("p_code", code)
            }
            val dto = supabase.postgrest.rpc("rpc_verify_2fa_login_challenge", params).decodeAs<TwoFactorVerificationResultDto>()
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify 2FA login challenge", e)
            Result.failure(e)
        }
    }

    override suspend fun disable2FAMethod(factorType: TwoFactorMethod): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject { put("p_factor_type", factorType.toServerString()) }
            supabase.postgrest.rpc("rpc_disable_2fa_method", params)
            get2FAStatus()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable 2FA method", e)
            Result.failure(e)
        }
    }

    override suspend fun setPreferredMethod(factorType: TwoFactorMethod): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val params = buildJsonObject { put("p_factor_type", factorType.toServerString()) }
            supabase.postgrest.rpc("rpc_set_preferred_2fa_method", params)
            get2FAStatus()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set preferred 2FA method", e)
            Result.failure(e)
        }
    }
}

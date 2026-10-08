package tn.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tn.keeftalk.chat.domain.model.*

@Serializable
data class TwoFactorFactorDto(
    @SerialName("id") val id: String,
    @SerialName("factor_type") val factorType: String,
    @SerialName("status") val status: String,
    @SerialName("verified_destination") val verifiedDestination: String? = null,
    @SerialName("is_preferred") val isPreferred: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("last_used_at") val lastUsedAt: Long? = null
) {
    fun toDomain() = TwoFactorFactor(
        id = id,
        type = TwoFactorMethod.fromServerString(factorType),
        status = FactorStatus.fromServerString(status),
        verifiedDestination = verifiedDestination,
        isPreferred = isPreferred,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}

@Serializable
data class TwoFactorStatusDto(
    @SerialName("two_factor_enabled") val twoFactorEnabled: Boolean = false,
    @SerialName("preferred_method") val preferredMethod: String? = null,
    @SerialName("has_backup_codes") val hasBackupCodes: Boolean = false,
    @SerialName("active_factors") val activeFactors: List<TwoFactorFactorDto> = emptyList()
) {
    fun toDomain() = TwoFactorStatus(
        twoFactorEnabled = twoFactorEnabled,
        preferredMethod = preferredMethod?.let { TwoFactorMethod.fromServerString(it) },
        hasBackupCodes = hasBackupCodes,
        activeFactors = activeFactors.map { it.toDomain() }
    )
}

@Serializable
data class TotpEnrollmentDto(
    @SerialName("factor_id") val factorId: String,
    @SerialName("secret") val secret: String,
    @SerialName("qr_code_uri") val qrCodeUri: String
) {
    fun toDomain() = TotpEnrollmentData(
        factorId = factorId,
        secret = secret,
        qrCodeUri = qrCodeUri
    )
}

@Serializable
data class OtpRequestResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("challenge_id") val challengeId: String? = null,
    @SerialName("masked_destination") val maskedDestination: String? = null,
    @SerialName("cooldown_seconds") val cooldownSeconds: Int = 60,
    @SerialName("message") val message: String? = null
) {
    fun toDomain() = OtpChallengeResponse(
        success = success,
        challengeId = challengeId,
        maskedDestination = maskedDestination,
        cooldownSeconds = cooldownSeconds,
        message = message
    )
}

@Serializable
data class TwoFactorLoginChallengeDto(
    @SerialName("requires_2fa") val requires2FA: Boolean = false,
    @SerialName("login_challenge_id") val loginChallengeId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("allowed_factors") val allowedFactors: List<String> = emptyList(),
    @SerialName("preferred_factor") val preferredFactor: String? = null
) {
    fun toDomain() = TwoFactorLoginChallenge(
        requires2FA = requires2FA,
        loginChallengeId = loginChallengeId,
        userId = userId,
        allowedFactors = allowedFactors.map { TwoFactorMethod.fromServerString(it) },
        preferredFactor = preferredFactor?.let { TwoFactorMethod.fromServerString(it) }
    )
}

@Serializable
data class TwoFactorVerificationResultDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("verification_token") val verificationToken: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("message") val message: String? = null
) {
    fun toDomain() = TwoFactorVerificationResult(
        success = success,
        verificationToken = verificationToken,
        userId = userId,
        message = message
    )
}

@Serializable
data class BackupCodesResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("backup_codes") val backupCodes: List<String> = emptyList()
)

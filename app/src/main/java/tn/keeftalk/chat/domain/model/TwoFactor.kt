package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

enum class TwoFactorMethod {
    TOTP,
    EMAIL,
    SMS,
    BACKUP_CODE;

    fun toServerString(): String = when (this) {
        TOTP -> "totp"
        EMAIL -> "email"
        SMS -> "sms"
        BACKUP_CODE -> "backup_code"
    }

    companion object {
        fun fromServerString(value: String?): TwoFactorMethod = when (value?.lowercase()) {
            "totp" -> TOTP
            "email" -> EMAIL
            "sms" -> SMS
            "backup_code" -> BACKUP_CODE
            else -> TOTP
        }
    }
}

enum class FactorStatus {
    PENDING,
    ACTIVE,
    DISABLED;

    companion object {
        fun fromServerString(value: String?): FactorStatus = when (value?.uppercase()) {
            "ACTIVE" -> ACTIVE
            "DISABLED" -> DISABLED
            else -> PENDING
        }
    }
}

@Serializable
data class TwoFactorFactor(
    val id: String,
    val type: TwoFactorMethod,
    val status: FactorStatus,
    val verifiedDestination: String? = null,
    val isPreferred: Boolean = false,
    val createdAt: Long = 0,
    val lastUsedAt: Long? = null
)

@Serializable
data class TwoFactorStatus(
    val twoFactorEnabled: Boolean = false,
    val preferredMethod: TwoFactorMethod? = null,
    val hasBackupCodes: Boolean = false,
    val activeFactors: List<TwoFactorFactor> = emptyList()
)

@Serializable
data class TotpEnrollmentData(
    val factorId: String,
    val secret: String,
    val qrCodeUri: String
)

@Serializable
data class OtpChallengeResponse(
    val success: Boolean,
    val challengeId: String? = null,
    val maskedDestination: String? = null,
    val cooldownSeconds: Int = 60,
    val message: String? = null
)

@Serializable
data class TwoFactorLoginChallenge(
    val requires2FA: Boolean,
    val loginChallengeId: String? = null,
    val userId: String? = null,
    val allowedFactors: List<TwoFactorMethod> = emptyList(),
    val preferredFactor: TwoFactorMethod? = null
)

@Serializable
data class TwoFactorVerificationResult(
    val success: Boolean,
    val verificationToken: String? = null,
    val userId: String? = null,
    val message: String? = null
)

class TwoFactorRequiredException(
    val challenge: TwoFactorLoginChallenge
) : Exception("2FA verification required")

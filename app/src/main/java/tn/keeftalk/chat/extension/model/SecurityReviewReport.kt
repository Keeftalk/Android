package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
data class SecurityWarning(
    val title: String,
    val detail: String,
    val isCritical: Boolean = false
)

@Serializable
data class SecurityReviewReport(
    val formatStatus: VerificationResult = VerificationResult.NOT_CHECKED,
    val formatDetails: String = "",
    val signatureStatus: VerificationResult = VerificationResult.NOT_CHECKED,
    val signatureDetails: String = "",
    val publisherName: String = "",
    val isPublisherTrusted: Boolean = false,
    val assetIntegrityStatus: VerificationResult = VerificationResult.NOT_CHECKED,
    val assetIntegrityDetails: String = "",
    val compatibilityStatus: VerificationResult = VerificationResult.NOT_CHECKED,
    val compatibilityDetails: String = "",
    val onlineRevocationStatus: VerificationResult = VerificationResult.NOT_CHECKED,
    val onlineRevocationDetails: String = "Online revocation check was unavailable (offline execution).",
    val warnings: List<SecurityWarning> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val isExecutableContentDetected: Boolean = false,
    val isEligibleForInstallation: Boolean = false
) {
    fun hasCriticalFailures(): Boolean {
        return formatStatus == VerificationResult.FAILED ||
                signatureStatus == VerificationResult.FAILED ||
                assetIntegrityStatus == VerificationResult.FAILED ||
                compatibilityStatus == VerificationResult.FAILED ||
                isExecutableContentDetected
    }
}

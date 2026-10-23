package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
enum class VerificationResult {
    VERIFIED,
    PASSED,
    WARNING,
    FAILED,
    NOT_CHECKED,
    UNSUPPORTED
}

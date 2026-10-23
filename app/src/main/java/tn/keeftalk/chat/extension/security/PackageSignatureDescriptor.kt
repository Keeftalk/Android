package tn.keeftalk.chat.extension.security

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageSignatureDescriptor(
    val version: Int = 1,
    val algorithm: String = "SHA256withRSA",
    @SerialName("keyId") val keyId: String = "",
    @SerialName("publicKeyPem") val publicKeyPem: String = "",
    @SerialName("signatureBase64") val signatureBase64: String = ""
)

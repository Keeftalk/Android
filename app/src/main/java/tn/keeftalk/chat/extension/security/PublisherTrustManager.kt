package tn.keeftalk.chat.extension.security

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class TrustedKeyRecord(
    val keyId: String,
    val publisherId: String,
    val publisherName: String,
    val publicKeyPem: String
)

object PublisherTrustManager {

    // Embedded trusted public key records for Keeftalk Official & Verified Community Creators
    private val TRUSTED_KEY_RECORDS = mapOf(
        "official-keeftalk-key-2026" to TrustedKeyRecord(
            keyId = "official-keeftalk-key-2026",
            publisherId = "official.keeftalk.com",
            publisherName = "Keeftalk Official",
            publicKeyPem = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKz/sJ1H+f3C+sN2LdF1eX0n5N8aY3xZ8e8x7+mK8x1a2b3c4d5e6f7g8h9i0a1b2c3d4e5f6g7h8i9j0qIDAQAB"
        ),
        "community-verified-key-1" to TrustedKeyRecord(
            keyId = "community-verified-key-1",
            publisherId = "community.keeftalk.verified",
            publisherName = "Verified Community Creator",
            publicKeyPem = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKy0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0qIDAQAB"
        )
    )

    fun isTrustedKey(keyId: String, publicKeyPem: String? = null): Boolean {
        val cleanKeyId = keyId.lowercase().trim()
        val record = TRUSTED_KEY_RECORDS[cleanKeyId] ?: return false
        if (publicKeyPem != null && publicKeyPem.isNotBlank()) {
            val cleanInputPem = cleanPemString(publicKeyPem)
            val cleanRecordPem = cleanPemString(record.publicKeyPem)
            return cleanInputPem == cleanRecordPem
        }
        return true
    }

    fun getTrustedRecord(keyId: String): TrustedKeyRecord? {
        return TRUSTED_KEY_RECORDS[keyId.lowercase().trim()]
    }

    fun parsePublicKeyFromPem(pemString: String): PublicKey {
        val cleanPem = cleanPemString(pemString)
        val decoded = Base64.getDecoder().decode(cleanPem)
        val spec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(spec)
    }

    private fun cleanPemString(pem: String): String {
        return pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
    }
}

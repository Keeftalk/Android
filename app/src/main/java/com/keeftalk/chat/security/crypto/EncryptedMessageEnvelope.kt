package com.keeftalk.chat.security.crypto

/**
 * Represents an encrypted message payload ready for network transmission or local storage.
 * @param type The encryption protocol version (e.g., 100 for Cloud-E2EE PCK).
 * @param ciphertext The actual encrypted message bytes.
 * @param nonce The initialization vector (IV) used for AES-GCM decryption.
 */
data class EncryptedMessageEnvelope(
    val type: Int,
    val ciphertext: ByteArray,
    val nonce: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedMessageEnvelope
        if (type != other.type) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (nonce != other.nonce) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + (nonce?.hashCode() ?: 0)
        return result
    }
}

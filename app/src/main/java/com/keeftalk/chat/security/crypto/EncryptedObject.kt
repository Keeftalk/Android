package com.keeftalk.chat.security.crypto

import kotlinx.serialization.Serializable

/**
 * Standard container for all encrypted data in Keeftalk.
 */
@Serializable
data class EncryptedObject(
    val version: Int,
    val keyId: String,
    val iv: String, // Base64
    val ciphertext: String, // Base64
    val tag: String? = null // Base64, if not included in ciphertext
)

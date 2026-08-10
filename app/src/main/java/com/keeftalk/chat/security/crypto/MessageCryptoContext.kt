package com.keeftalk.chat.security.crypto

import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Provides canonical and deterministic construction of Additional Authenticated Data (AAD)
 * for message encryption. Binding metadata to the ciphertext prevents tampering
 * with message context (e.g. replaying a message in a different chat).
 */
object MessageCryptoContext {
    
    private const val TAG = "MessageCryptoContext"

    /**
     * Builds AAD for Version 4+ messages.
     * Format: v{version}|{chatId}|{messageId}
     * All IDs are normalized to lowercase to ensure cross-platform consistency.
     */
    fun buildMessageAad(
        version: Int,
        chatId: String,
        messageId: String
    ): Pair<ByteArray, String> {
        // Normalize IDs to ensure bit-identical AAD even if casing varies in DB/network
        val normalizedChatId = chatId.lowercase(Locale.US).trim()
        val normalizedMsgId = messageId.lowercase(Locale.US).trim()
        
        val aadString = "v$version|$normalizedChatId|$normalizedMsgId"
        
        Log.d(TAG, "[CRYPTO_DIAG] buildMessageAad | [AAD_STR] $aadString")
        
        return aadString.toByteArray(StandardCharsets.UTF_8) to aadString
    }
}

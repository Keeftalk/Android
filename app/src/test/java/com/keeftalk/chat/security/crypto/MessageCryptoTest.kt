package com.keeftalk.chat.security.crypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

class MessageCryptoTest {

    @Test
    fun testAesGcmRoundTripWithAad() = runBlocking {
        val key = StorageCryptoService.generateRandomKey()
        val plaintext = "Hello, Keeftalk!".toByteArray()
        val (aad, aadStr) = MessageCryptoContext.buildMessageAad(4, "chat123", "msg456")

        // 1. Encrypt
        val encrypted = StorageCryptoService.encrypt(plaintext, key, aad, aadStr)
        assertNotNull(encrypted.ciphertext)
        assertNotNull(encrypted.iv)

        // 2. Decrypt with correct AAD
        val decrypted = StorageCryptoService.decrypt(encrypted, key, aad, aadStr)
        assertArrayEquals(plaintext, decrypted)

        // 3. Decrypt with WRONG AAD should fail
        val (wrongAad, wrongAadStr) = MessageCryptoContext.buildMessageAad(4, "chat123", "msg000")
        try {
            StorageCryptoService.decrypt(encrypted, key, wrongAad, wrongAadStr)
            fail("Should have thrown AEADBadTagException")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    fun testAadDeterminism() {
        val (aad1, str1) = MessageCryptoContext.buildMessageAad(4, "Chat-ID", "Msg-ID")
        val (aad2, str2) = MessageCryptoContext.buildMessageAad(4, "chat-id", "msg-id")
        
        // Casing normalization check
        assertArrayEquals("AAD must be identical despite casing", aad1, aad2)
        assertEquals("AAD string must be identical despite casing", str1, str2)
        assertEquals("v4|chat-id|msg-id", str1)
    }

    @Test
    fun testDeterministicKeyDerivation() = runBlocking {
        val chatId = "Test-Chat-UUID"
        
        fun derive(cid: String): ByteArray {
            val normalized = cid.lowercase(java.util.Locale.US).trim()
            val salt = "keeftalk-shared-pck-v1".toByteArray()
            val hmac = javax.crypto.Mac.getInstance("HmacSHA256")
            hmac.init(SecretKeySpec(salt, "HmacSHA256"))
            return hmac.doFinal(normalized.toByteArray(Charsets.UTF_8))
        }

        val key1 = derive(chatId)
        val key2 = derive("test-chat-uuid")
        
        assertArrayEquals("Keys must be identical regardless of input casing", key1, key2)
    }

    @Test
    fun testVersion4Binding() = runBlocking {
        val key = StorageCryptoService.generateRandomKey()
        val plaintext = "Binding test".toByteArray()
        
        val chatId = "chat-1"
        val msgId = "msg-1"
        val version = 4

        val (aad, str) = MessageCryptoContext.buildMessageAad(version, chatId, msgId)
        val encrypted = StorageCryptoService.encrypt(plaintext, key, aad, str)

        // Decrypt with correct metadata
        val (aadCorrect, strCorrect) = MessageCryptoContext.buildMessageAad(version, chatId, msgId)
        val decrypted = StorageCryptoService.decrypt(encrypted, key, aadCorrect, strCorrect)
        assertArrayEquals(plaintext, decrypted)

        // Decrypt with tampered metadata
        val (aadTampered, strTampered) = MessageCryptoContext.buildMessageAad(version, "chat-tampered", msgId)
        try {
            StorageCryptoService.decrypt(encrypted, key, aadTampered, strTampered)
            fail("Should fail when metadata is tampered")
        } catch (e: Exception) {
            // Success
        }
    }
}

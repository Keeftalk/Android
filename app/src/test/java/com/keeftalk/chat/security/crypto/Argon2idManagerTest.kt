package com.keeftalk.chat.security.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import javax.crypto.SecretKey

class Argon2idManagerTest {

    @Test
    fun deriveKey_sameInput_producesSameOutput() {
        val password = "StrongPassword123!"
        val salt = Argon2idManager.generateSalt()

        val key1 = Argon2idManager.deriveKey(password, salt)
        val key2 = Argon2idManager.deriveKey(password, salt)

        assertArrayEquals(key1.encoded, key2.encoded)
    }

    @Test
    fun deriveKey_differentSalt_producesDifferentOutput() {
        val password = "StrongPassword123!"
        val salt1 = Argon2idManager.generateSalt()
        val salt2 = Argon2idManager.generateSalt()

        val key1 = Argon2idManager.deriveKey(password, salt1)
        val key2 = Argon2idManager.deriveKey(password, salt2)

        var same = true
        for (i in key1.encoded.indices) {
            if (key1.encoded[i] != key2.encoded[i]) {
                same = false
                break
            }
        }
        assert(!same)
    }

    @Test
    fun generateSalt_returnsValidSalt() {
        val salt = Argon2idManager.generateSalt()
        assertNotNull(salt)
        assert(salt.size == 16)
    }
}

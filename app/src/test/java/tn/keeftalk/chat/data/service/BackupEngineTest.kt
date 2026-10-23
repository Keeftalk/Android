package tn.keeftalk.chat.data.service

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tn.keeftalk.chat.security.crypto.BackupCryptoEngine
import java.io.File

class BackupEngineTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val password = "secureBackupPassword123!"
        val plaintext = "Keeftalk Backup Payload Test Data"

        val sourceFile = temporaryFolder.newFile("source.txt")
        sourceFile.writeText(plaintext)

        val encryptedFile = temporaryFolder.newFile("backup.ktb")
        BackupCryptoEngine.encryptFile(sourceFile, encryptedFile, password)

        assertTrue(encryptedFile.exists())
        assertTrue(encryptedFile.length() > sourceFile.length())

        val decryptedFile = temporaryFolder.newFile("decrypted.txt")
        BackupCryptoEngine.decryptFile(encryptedFile, decryptedFile, password)

        assertTrue(decryptedFile.exists())
        assertEquals(plaintext, decryptedFile.readText())
    }

    @Test(expected = SecurityException::class)
    fun testIncorrectPasswordThrowsSecurityException() {
        val password = "correctPassword"
        val wrongPassword = "wrongPassword"
        val plaintext = "Secret Backup Data"

        val sourceFile = temporaryFolder.newFile("source.txt")
        sourceFile.writeText(plaintext)

        val encryptedFile = temporaryFolder.newFile("backup.ktb")
        BackupCryptoEngine.encryptFile(sourceFile, encryptedFile, password)

        val decryptedFile = temporaryFolder.newFile("decrypted.txt")
        BackupCryptoEngine.decryptFile(encryptedFile, decryptedFile, wrongPassword)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCorruptedFileThrowsException() {
        val password = "password123"
        val corruptedFile = temporaryFolder.newFile("corrupted.ktb")
        corruptedFile.writeText("too short")

        val decryptedFile = temporaryFolder.newFile("decrypted.txt")
        BackupCryptoEngine.decryptFile(corruptedFile, decryptedFile, password)
    }
}

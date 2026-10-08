package tn.keeftalk.chat.feature.secrets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.feature.secrets.data.local.entities.SecretCredentialEntity
import tn.keeftalk.chat.feature.secrets.util.CsvImporterExporter

class CsvImporterExporterTest {

    @Test
    fun parseRawCsv_handlesQuotedFieldsAndEscapedQuotes() {
        val sample = "name,url,username,password,note\r\nTwitch,https://www.twitch.tv,streamer01,Ts9!kLm2,\"gaming, main\"\r\nNotion,https://www.notion.so,you@mail.com,Qw8#nB4z,simple"
        val rows = CsvImporterExporter.parseRawCsv(sample)
        assertEquals(3, rows.size)
        assertEquals("gaming, main", rows[1][4])
    }

    @Test
    fun importFromCsv_googleFormat_parsesAndFiltersDuplicates() {
        val sample = "name,url,username,password,note\r\nTwitch,https://www.twitch.tv,streamer01,Ts9!kLm2,\"gaming\"\r\nExisting,https://example.com,user1,pass123,note1"

        val existing = listOf(
            SecretCredentialEntity(
                id = "1",
                userId = "u1",
                serviceName = "Existing",
                website = "https://example.com",
                username = "user1",
                encryptedPassword = "enc",
                category = "Personal"
            )
        )

        val result = CsvImporterExporter.importFromCsv(sample, existing)
        assertEquals("Google Password Manager", result.detectedSource)
        assertEquals(1, result.importedItems.size)
        assertEquals("Twitch", result.importedItems[0].serviceName)
        assertEquals(1, result.duplicateCount)
    }

    @Test
    fun exportToCsv_validCredentials_generatesCorrectFormat() {
        val credentials = listOf(
            Pair(
                SecretCredentialEntity(
                    id = "1",
                    userId = "u1",
                    serviceName = "GitHub",
                    website = "https://github.com",
                    username = "dev-handle",
                    encryptedPassword = "enc",
                    notes = "work account"
                ),
                "SecretPass123!"
            )
        )

        val csv = CsvImporterExporter.exportToCsv(credentials)
        assertTrue(csv.contains("name,url,username,password,note"))
        assertTrue(csv.contains("GitHub,https://github.com,dev-handle,SecretPass123!,work account"))
    }
}

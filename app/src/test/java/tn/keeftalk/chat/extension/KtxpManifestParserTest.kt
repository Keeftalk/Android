package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.extension.model.ExtensionType
import tn.keeftalk.chat.extension.parser.KtxpManifestParseException
import tn.keeftalk.chat.extension.parser.KtxpManifestParser

class KtxpManifestParserTest {

    @Test
    fun testParseValidThemeManifest() {
        val json = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.theme.midnight",
                "type": "THEME",
                "name": "Midnight Dark Theme",
                "version": "1.0.0",
                "description": "Dark aesthetic theme for Keeftalk",
                "publisher": {
                    "name": "Keeftalk Official",
                    "id": "official.keeftalk.com",
                    "isVerifiedPublisher": true
                },
                "minAppVersion": 1,
                "entry": "theme/theme.json"
            }
        """.trimIndent()

        val manifest = KtxpManifestParser.parseManifest(json)
        assertEquals("com.keeftalk.theme.midnight", manifest.id)
        assertEquals(ExtensionType.THEME, manifest.type)
        assertEquals("Midnight Dark Theme", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals("theme/theme.json", manifest.entry)
        assertTrue(manifest.isValidPackageId())
    }

    @Test(expected = KtxpManifestParseException::class)
    fun testParseInvalidReverseDnsId() {
        val json = """
            {
                "schemaVersion": 1,
                "id": "invalid-id-without-dots",
                "type": "THEME",
                "name": "Test Theme",
                "version": "1.0.0",
                "entry": "theme.json"
            }
        """.trimIndent()

        KtxpManifestParser.parseManifest(json)
    }

    @Test(expected = KtxpManifestParseException::class)
    fun testParseEmptyManifest() {
        KtxpManifestParser.parseManifest("")
    }

    @Test(expected = KtxpManifestParseException::class)
    fun testParseManifestMissingEntry() {
        val json = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.theme.test",
                "type": "THEME",
                "name": "Test Theme",
                "version": "1.0.0",
                "entry": ""
            }
        """.trimIndent()

        KtxpManifestParser.parseManifest(json)
    }
}

package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tn.keeftalk.chat.extension.model.ExtensionType
import tn.keeftalk.chat.extension.model.VerificationResult
import tn.keeftalk.chat.extension.parser.KtxpPackageReader
import tn.keeftalk.chat.extension.security.PackageVerifier
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SampleKtxpGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun generateRsaKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

    private fun createSignedKtxpBytes(
        manifestJson: String,
        entryPath: String,
        entryContent: String,
        keyPair: KeyPair,
        keyId: String = "test-key-id"
    ): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")

        val entries = mapOf(
            "manifest.json" to manifestJson.toByteArray(Charsets.UTF_8),
            entryPath to entryContent.toByteArray(Charsets.UTF_8)
        ).toSortedMap()

        for ((path, bytes) in entries) {
            digest.update(path.toByteArray(Charsets.UTF_8))
            digest.update(bytes)
        }
        val payload = digest.digest()

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(payload)
        val signatureBytes = signer.sign()
        val sigBase64 = Base64.getEncoder().encodeToString(signatureBytes)

        val publicKeyPem = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val sigDescriptorJson = """
            {
                "version": 1,
                "algorithm": "SHA256withRSA",
                "keyId": "$keyId",
                "publicKeyPem": "$publicKeyPem",
                "signatureBase64": "$sigBase64"
            }
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(entryPath))
            zos.write(entryContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("signatures/package-signature.json"))
            zos.write(sigDescriptorJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    @Test
    fun generateSalatV3KtxpPackageFile() {
        val keyPair = generateRsaKeyPair()

        val manifestJson = """
            {
              "schemaVersion": 3,
              "id": "com.keeftalk.card.salat",
              "type": "FEED_CARD",
              "name": "Islamic Salat & Prayer Platform v3.0",
              "version": "3.0.0",
              "description": "Enterprise-grade v3.0 Islamic prayer platform with multi-screen navigation, sandboxed workflow execution, capability bridging, and local notification scheduling.",
              "publisher": {
                "name": "Keeftalk Official",
                "id": "official.keeftalk.com",
                "website": "https://keeftalk.com",
                "isVerifiedPublisher": true
              },
              "minAppVersion": 1,
              "capabilities": ["FEED_TOP_WIDGET", "FULL_SCREEN_SCREEN", "SETTINGS_PAGE", "WORKFLOW_ENGINE", "CAPABILITY_BRIDGE"],
              "permissions": ["LOCATION_COARSE", "NOTIFICATIONS", "STORAGE"],
              "entry": "cards/salat_card_v3.json",
              "assets": [
                {
                  "path": "cards/salat_card_v3.json",
                  "mimeType": "application/json",
                  "sizeBytes": 1024
                },
                {
                  "path": "screens/salat_main_screen_v3.json",
                  "mimeType": "application/json",
                  "sizeBytes": 4096
                },
                {
                  "path": "screens/salat_settings_screen_v3.json",
                  "mimeType": "application/json",
                  "sizeBytes": 3072
                },
                {
                  "path": "assets/hadith_library.json",
                  "mimeType": "application/json",
                  "sizeBytes": 8192
                },
                {
                  "path": "workflows/recalculation_workflow_v3.json",
                  "mimeType": "application/json",
                  "sizeBytes": 2048
                }
              ]
            }
        """.trimIndent()

        val cardJson = """
            {
              "id": "com.keeftalk.card.salat",
              "title": "🕌 Next Prayer: Asr in 35m v3",
              "subtitle": "Tunis • 14 Ramadan 1447 AH • Qibla 112° SE",
              "category": "ISLAMIC",
              "providerName": "Keeftalk Salat v3.0",
              "refreshIntervalMinutes": 1,
              "components": [
                {
                  "type": "BADGE",
                  "text": "CURRENT: DHUHR (12:30 PM)",
                  "badgeText": "🟢 ACTIVE: DHUHR v3"
                },
                {
                  "type": "NUMBER_STAT",
                  "text": "03:45 PM",
                  "subtext": "Asr Prayer Time • Shafi / Maliki"
                },
                {
                  "type": "PROGRESS_BAR",
                  "text": "Daily Prayer Progress (3/5 Completed)",
                  "value": 60,
                  "maxValue": 100
                },
                {
                  "type": "DIVIDER"
                },
                {
                  "type": "BUTTON",
                  "text": "Open v3 Enterprise Prayer Experience",
                  "action": {
                    "type": "NAVIGATE",
                    "screenId": "salat_main_screen_v3"
                  }
                }
              ],
              "actionUrl": "screen://salat_main_screen_v3",
              "primaryAction": {
                "type": "NAVIGATE",
                "screenId": "salat_main_screen_v3"
              }
            }
        """.trimIndent()

        val screenMainJson = """
            {
              "id": "salat_main_screen_v3",
              "title": "Islamic Salat & Prayer Platform v3.0",
              "subtitle": "Tunis, Tunisia • Lat 36.8° N, Lon 10.1° E • Timezone UTC+1",
              "extensionId": "com.keeftalk.card.salat",
              "components": [
                {
                  "type": "HEADER",
                  "label": "Next Prayer: Asr in 35m 45s",
                  "subtext": "Scheduled at 03:45 PM • 14 Ramadan 1447 AH"
                },
                {
                  "type": "SECTION_TITLE",
                  "label": "Daily Prayer Timetable v3"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Fajr (Dawn)",
                  "value": "05:15 AM"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Sunrise",
                  "value": "06:40 AM"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Dhuhr (Midday)",
                  "value": "12:30 PM"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Asr (Afternoon)",
                  "value": "03:45 PM"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Maghrib (Sunset)",
                  "value": "06:22 PM"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Isha (Night)",
                  "value": "07:48 PM"
                },
                {
                  "type": "DIVIDER"
                },
                {
                  "type": "SECTION_TITLE",
                  "label": "Qibla Direction & Sun Position"
                },
                {
                  "type": "STAT",
                  "label": "Qibla Bearing",
                  "subtext": "Degrees from True North",
                  "value": "112° SE"
                },
                {
                  "type": "DIVIDER"
                },
                {
                  "type": "SECTION_TITLE",
                  "label": "Navigation & Settings"
                },
                {
                  "type": "BUTTON",
                  "label": "Open v3 Calculation Settings",
                  "action": {
                    "type": "NAVIGATE",
                    "screenId": "salat_settings_screen_v3"
                  }
                },
                {
                  "type": "BUTTON",
                  "label": "Schedule v3 Reminder Notifications",
                  "action": {
                    "type": "SCHEDULE_NOTIFICATION",
                    "key": "Keeftalk Salat v3.0",
                    "message": "Enterprise prayer reminders successfully scheduled."
                  }
                }
              ]
            }
        """.trimIndent()

        val screenSettingsJson = """
            {
              "id": "salat_settings_screen_v3",
              "title": "Salat Calculation & Preferences v3.0",
              "subtitle": "Configure astronomical methods and juristic parameters",
              "extensionId": "com.keeftalk.card.salat",
              "components": [
                {
                  "type": "SECTION_TITLE",
                  "label": "Location & Coordinates"
                },
                {
                  "type": "TEXT_INPUT",
                  "label": "City Name",
                  "value": "Tunis",
                  "key": "setting_city"
                },
                {
                  "type": "TEXT_INPUT",
                  "label": "Latitude",
                  "value": "36.8065",
                  "key": "setting_lat"
                },
                {
                  "type": "TEXT_INPUT",
                  "label": "Longitude",
                  "value": "10.1815",
                  "key": "setting_lon"
                },
                {
                  "type": "DIVIDER"
                },
                {
                  "type": "SECTION_TITLE",
                  "label": "Calculation Conventions"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Method",
                  "value": "Tunisian Ministry / MWL"
                },
                {
                  "type": "KEY_VALUE",
                  "label": "Asr Madhhab",
                  "value": "Standard (Shafi / Maliki / Hanbali)"
                },
                {
                  "type": "BUTTON",
                  "label": "Save & Recalculate Times",
                  "action": {
                    "type": "SHOW_TOAST",
                    "message": "Preferences saved and prayer schedule recalculated."
                  }
                },
                {
                  "type": "BUTTON",
                  "label": "Reset to Default Preferences",
                  "action": {
                    "type": "CLEAR_PREFERENCES",
                    "message": "All extension preferences reset to defaults."
                  }
                }
              ]
            }
        """.trimIndent()

        val hadithLibraryJson = """
            {
              "collection": "Authentic Hadith & Quranic Verses on Prayer v3",
              "entries": [
                {
                  "reference": "Sahih Al-Bukhari 528",
                  "text": "The first matter that the slave will be brought to account for on the Day of Judgment is the prayer."
                }
              ]
            }
        """.trimIndent()

        val workflowJson = """
            {
              "id": "recalculation_workflow_v3",
              "name": "Prayer Recalculation Workflow v3",
              "maxSteps": 100,
              "steps": [
                {
                  "id": "step1",
                  "type": "VALIDATE_NOT_BLANK",
                  "inputKey": "setting_lat",
                  "successNextStep": "step2"
                },
                {
                  "id": "step2",
                  "type": "SHOW_MESSAGE",
                  "value": "Coordinates validated successfully in v3."
                }
              ]
            }
        """.trimIndent()

        val listingJson = """
            {
              "schemaVersion": 3,
              "extensionId": "com.keeftalk.card.salat",
              "name": "Islamic Salat & Prayer Platform v3.0",
              "type": "FEED_CARD",
              "publisher": {
                "id": "official.keeftalk.com",
                "name": "Keeftalk Official",
                "verified": true
              },
              "description": "Enterprise-grade v3.0 Islamic prayer platform with multi-screen navigation, sandboxed workflow execution, capability bridging, and local notification scheduling.",
              "version": "3.0.0",
              "releaseDate": "2026-03-05",
              "releaseNotes": "v3.0.0: Maximum power upgrade featuring multi-screen navigation, isolated preference storage, custom workflows, and background reminder scheduling.",
              "minimumAppVersion": "3.0.0",
              "ageRating": "4+",
              "languages": ["en", "ar", "fr"],
              "compatibleFeatures": ["MY_FEED", "FULL_SCREEN_SCREEN", "SETTINGS_PAGE", "WORKFLOW_ENGINE"],
              "permissions": ["LOCATION_COARSE", "NOTIFICATIONS", "STORAGE"]
            }
        """.trimIndent()

        val releaseNotesJson = """
            {
              "version": "3.0.0",
              "releaseDate": "2026-03-05",
              "notes": "v3.0.0 release of Keeftalk Salat platform."
            }
        """.trimIndent()

        val entries = mapOf(
            "cards/salat_card_v3.json" to cardJson.toByteArray(Charsets.UTF_8),
            "screens/salat_main_screen_v3.json" to screenMainJson.toByteArray(Charsets.UTF_8),
            "screens/salat_settings_screen_v3.json" to screenSettingsJson.toByteArray(Charsets.UTF_8),
            "assets/hadith_library.json" to hadithLibraryJson.toByteArray(Charsets.UTF_8),
            "workflows/recalculation_workflow_v3.json" to workflowJson.toByteArray(Charsets.UTF_8),
            "manifest.json" to manifestJson.toByteArray(Charsets.UTF_8),
            "metadata/listing.json" to listingJson.toByteArray(Charsets.UTF_8),
            "metadata/release-notes.json" to releaseNotesJson.toByteArray(Charsets.UTF_8)
        ).toSortedMap()

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        for ((path, bytes) in entries) {
            digest.update(path.toByteArray(Charsets.UTF_8))
            digest.update(bytes)
        }
        val payload = digest.digest()

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(payload)
        val signatureBytes = signer.sign()
        val sigBase64 = Base64.getEncoder().encodeToString(signatureBytes)
        val publicKeyPem = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val sigDescriptorJson = """
            {
                "version": 3,
                "algorithm": "SHA256withRSA",
                "keyId": "salat-v3-dev-key-2026",
                "publicKeyPem": "$publicKeyPem",
                "signatureBase64": "$sigBase64"
            }
        """.trimIndent()

        val outputFile = File("/tmp/salat-extension-v3.ktxp")
        outputFile.outputStream().use { fos ->
            ZipOutputStream(fos).use { zos ->
                for ((path, bytes) in entries) {
                    zos.putNextEntry(ZipEntry(path))
                    zos.write(bytes)
                    zos.closeEntry()
                }

                zos.putNextEntry(ZipEntry("signatures/package-signature.json"))
                zos.write(sigDescriptorJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        // Verify package integrity using PackageVerifier
        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, outputFile.inputStream())
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.card.salat", manifest?.id)
        assertEquals("3.0.0", manifest?.version)
        assertEquals(VerificationResult.PASSED, report.signatureStatus)
        assertTrue(report.isEligibleForInstallation)
    }

    @Test
    fun testRealCryptographicSignatureVerification() {
        val keyPair = generateRsaKeyPair()

        val manifestJson = """
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

        val themeContent = """
            {
                "id": "com.keeftalk.theme.midnight",
                "name": "Midnight Dark Theme",
                "isDark": true,
                "darkColors": {
                    "primaryHex": "#0A84FF",
                    "onPrimaryHex": "#FFFFFF"
                }
            }
        """.trimIndent()

        val ktxpBytes = createSignedKtxpBytes(manifestJson, "theme/theme.json", themeContent, keyPair)
        val context = FakeTestContext(tempFolder.root)

        val session = KtxpPackageReader.readAndStagePackageFromStream(context, ByteArrayInputStream(ktxpBytes))
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.theme.midnight", manifest?.id)
        assertEquals(ExtensionType.THEME, manifest?.type)
        assertEquals(VerificationResult.PASSED, report.signatureStatus) // Cryptographically valid signature from test key
        assertTrue(report.isEligibleForInstallation)
    }

    @Test
    fun testTamperedManifestFailsCryptographicSignature() {
        val keyPair = generateRsaKeyPair()

        val originalManifest = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.theme.midnight",
                "type": "THEME",
                "name": "Midnight Dark Theme",
                "version": "1.0.0",
                "entry": "theme/theme.json"
            }
        """.trimIndent()

        val tamperedManifest = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.theme.midnight",
                "type": "THEME",
                "name": "TAMPERED HACKED THEME",
                "version": "1.0.0",
                "entry": "theme/theme.json"
            }
        """.trimIndent()

        // Sign originalManifest but write tamperedManifest into the ZIP package
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(originalManifest.toByteArray(Charsets.UTF_8))
        val sigBase64 = Base64.getEncoder().encodeToString(signer.sign())
        val publicKeyPem = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val sigDescriptorJson = """
            {
                "version": 1,
                "algorithm": "SHA256withRSA",
                "keyId": "test-key",
                "publicKeyPem": "$publicKeyPem",
                "signatureBase64": "$sigBase64"
            }
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(tamperedManifest.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("theme/theme.json"))
            zos.write("{}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("signatures/package-signature.json"))
            zos.write(sigDescriptorJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, ByteArrayInputStream(baos.toByteArray()))
        val (_, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertEquals(VerificationResult.FAILED, report.signatureStatus)
        assertFalse(report.isEligibleForInstallation)
        assertTrue(report.hasCriticalFailures())
    }

    @Test
    fun testSpoofedPublisherStringPatternFailsWithoutTwoWayCheck() {
        val context = FakeTestContext(tempFolder.root)

        val spoofedManifest = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.spoofed",
                "type": "THEME",
                "name": "Spoofed Official Theme",
                "version": "1.0.0",
                "publisher": {
                    "name": "Keeftalk Official",
                    "id": "official.keeftalk.com",
                    "isVerifiedPublisher": true
                },
                "entry": "theme/theme.json"
            }
        """.trimIndent()

        val fakeSigJson = """
            {
                "verified": true,
                "signer": "official.keeftalk.com"
            }
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(spoofedManifest.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("theme/theme.json"))
            zos.write("{}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("signatures/package-signature.json"))
            zos.write(fakeSigJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val session = KtxpPackageReader.readAndStagePackageFromStream(context, ByteArrayInputStream(baos.toByteArray()))
        val (_, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertEquals(VerificationResult.FAILED, report.signatureStatus)
        assertFalse(report.isEligibleForInstallation)
    }

    @Test
    fun generateProductionEnglishDictionaryKtxp() {
        val dictionaryFile = File("/home/m/Downloads/filtered.json")
        val coverFile = File("/home/m/Pictures/Screenshots/dic_cover.png")

        if (!dictionaryFile.exists()) {
            return
        }

        val keyPair = generateRsaKeyPair()

        val manifestJson = """
            {
              "schemaVersion": 1,
              "id": "com.keeftalk.dictionary.english.pro",
              "type": "DICTIONARY",
              "name": "English Explanatory Dictionary Pro",
              "version": "1.0.0",
              "description": "Comprehensive offline English dictionary with definitions, meanings, synonyms, and antonyms for the Books Reader.",
              "publisher": {
                "name": "Keeftalk Official",
                "id": "official.keeftalk.com",
                "website": "https://keeftalk.com",
                "isVerifiedPublisher": true
              },
              "minAppVersion": 1,
              "capabilities": ["OFFLINE_DICTIONARY"],
              "permissions": [],
              "entry": "dictionary/filtered.json",
              "dictionaryLanguages": ["en", "en"],
              "assets": [
                {
                  "path": "dictionary/filtered.json",
                  "mimeType": "application/json"
                },
                {
                  "path": "assets/cover.png",
                  "mimeType": "image/png"
                }
              ]
            }
        """.trimIndent()

        val listingJson = """
            {
              "schemaVersion": 1,
              "extensionId": "com.keeftalk.dictionary.english.pro",
              "name": "English Explanatory Dictionary Pro",
              "type": "DICTIONARY",
              "publisher": {
                "id": "official.keeftalk.com",
                "name": "Keeftalk Official",
                "verified": true
              },
              "description": "Comprehensive offline English dictionary with definitions, meanings, synonyms, and antonyms for the Books Reader.",
              "version": "1.0.0",
              "releaseDate": "2026-03-05",
              "minimumAppVersion": "3.0.0",
              "ageRating": "4+",
              "languages": ["English"],
              "compatibleFeatures": ["BOOKS_READER"]
            }
        """.trimIndent()

        val entries = mutableMapOf<String, ByteArray>()
        entries["manifest.json"] = manifestJson.toByteArray(Charsets.UTF_8)
        entries["dictionary/filtered.json"] = dictionaryFile.readBytes()
        val coverBytes = if (coverFile.exists()) coverFile.readBytes() else "fake png cover bytes".toByteArray()
        entries["assets/cover.png"] = coverBytes
        entries["metadata/listing.json"] = listingJson.toByteArray(Charsets.UTF_8)

        val sortedEntries = entries.toSortedMap()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        for ((path, bytes) in sortedEntries) {
            digest.update(path.toByteArray(Charsets.UTF_8))
            digest.update(bytes)
        }
        val payload = digest.digest()

        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(payload)
        val signatureBytes = signer.sign()
        val sigBase64 = Base64.getEncoder().encodeToString(signatureBytes)
        val publicKeyPem = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        val sigDescriptorJson = """
            {
                "version": 1,
                "algorithm": "SHA256withRSA",
                "keyId": "dictionary-pro-key-2026",
                "publicKeyPem": "$publicKeyPem",
                "signatureBase64": "$sigBase64"
            }
        """.trimIndent()

        val outputFile = File("/tmp/english-dictionary-pro.ktxp")
        outputFile.outputStream().use { fos ->
            ZipOutputStream(fos).use { zos ->
                for ((path, bytes) in sortedEntries) {
                    zos.putNextEntry(ZipEntry(path))
                    zos.write(bytes)
                    zos.closeEntry()
                }

                zos.putNextEntry(ZipEntry("signatures/package-signature.json"))
                zos.write(sigDescriptorJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, outputFile.inputStream())
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.dictionary.english.pro", manifest?.id)
        assertEquals(VerificationResult.PASSED, report.signatureStatus)
        assertTrue(report.isEligibleForInstallation)
    }

    @Test
    fun generateReaderThemesPackKtxp() {
        val keyPair = generateRsaKeyPair()

        val manifestJson = """
            {
              "schemaVersion": 1,
              "id": "com.keeftalk.theme.reader.pack",
              "type": "THEME",
              "name": "Reader Themes Collection Pack",
              "version": "1.0.0",
              "description": "Premium theme collection pack for Books Reader: Dark, Twilight, Paper, Sepia, and Sepia+.",
              "publisher": {
                "name": "Keeftalk Official",
                "id": "official.keeftalk.com",
                "website": "https://keeftalk.com",
                "isVerifiedPublisher": true
              },
              "minAppVersion": 1,
              "capabilities": ["READER_THEMES"],
              "permissions": [],
              "entry": "theme/reader_themes.json",
              "assets": [
                {
                  "path": "theme/reader_themes.json",
                  "mimeType": "application/json"
                }
              ]
            }
        """.trimIndent()

        val readerThemesContent = """
            [
              {
                "id": "dark",
                "name": "Dark",
                "isDark": true,
                "isReaderThemeOnly": true,
                "darkColors": {
                  "backgroundBodyHex": "#121214",
                  "onBackgroundHex": "#D4D4DC",
                  "onSurfaceVariantHex": "#8A8A98",
                  "surfaceVariantHex": "#18FFFFFF"
                }
              },
              {
                "id": "twilight",
                "name": "Twilight",
                "isDark": true,
                "isReaderThemeOnly": true,
                "darkColors": {
                  "backgroundBodyHex": "#1D1A30",
                  "onBackgroundHex": "#D9D3F0",
                  "onSurfaceVariantHex": "#8F88B3",
                  "surfaceVariantHex": "#1AFFFFFF"
                }
              },
              {
                "id": "paper",
                "name": "Paper",
                "isDark": false,
                "isReaderThemeOnly": true,
                "darkColors": {
                  "backgroundBodyHex": "#FBFAF5",
                  "onBackgroundHex": "#23221F",
                  "onSurfaceVariantHex": "#8C897E",
                  "surfaceVariantHex": "#14000000"
                }
              },
              {
                "id": "sepia",
                "name": "Sepia",
                "isDark": false,
                "isReaderThemeOnly": true,
                "darkColors": {
                  "backgroundBodyHex": "#F1E4C8",
                  "onBackgroundHex": "#4A3820",
                  "onSurfaceVariantHex": "#8F7B58",
                  "surfaceVariantHex": "#224A3820"
                }
              },
              {
                "id": "sepiap",
                "name": "Sepia+",
                "isDark": false,
                "isReaderThemeOnly": true,
                "darkColors": {
                  "backgroundBodyHex": "#D8BF94",
                  "onBackgroundHex": "#33210D",
                  "onSurfaceVariantHex": "#7D6642",
                  "surfaceVariantHex": "#2633210D"
                }
              }
            ]
        """.trimIndent()

        val ktxpBytes = createSignedKtxpBytes(manifestJson, "theme/reader_themes.json", readerThemesContent, keyPair)
        val outputFile = File("/tmp/reader-themes-pack.ktxp")
        outputFile.writeBytes(ktxpBytes)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, outputFile.inputStream())
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.theme.reader.pack", manifest?.id)
        assertEquals(ExtensionType.THEME, manifest?.type)
        assertEquals(VerificationResult.PASSED, report.signatureStatus)
        assertTrue(report.isEligibleForInstallation)
    }

    @Test
    fun generateAppThemeCollectionKtxp() {
        val keyPair = generateRsaKeyPair()

        val manifestJson = """
            {
              "schemaVersion": 1,
              "id": "com.keeftalk.theme.app.collection",
              "type": "THEME",
              "name": "App Theme Collection Pack",
              "version": "1.0.0",
              "description": "Complete palette collection for Keeftalk: AMOLED Pitch Black, Solarized Dark/Light, Cyberpunk Neon, Nordic Glacier, and OLED Green Matrix.",
              "publisher": {
                "name": "Keeftalk Official",
                "id": "official.keeftalk.com",
                "website": "https://keeftalk.com",
                "isVerifiedPublisher": true
              },
              "minAppVersion": 1,
              "capabilities": ["GLOBAL_APP_THEME"],
              "permissions": [],
              "entry": "theme/app_themes.json",
              "assets": [
                {
                  "path": "theme/app_themes.json",
                  "mimeType": "application/json"
                }
              ]
            }
        """.trimIndent()

        val appThemesContent = """
            [
              {
                "id": "com.keeftalk.theme.amoled",
                "name": "AMOLED Pitch Black",
                "isDark": true,
                "darkColors": {
                  "primaryHex": "#0A84FF",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#000000",
                  "onBackgroundHex": "#FFFFFF",
                  "surfaceHex": "#0A0A0C",
                  "onSurfaceHex": "#FFFFFF",
                  "surfaceVariantHex": "#121215",
                  "onSurfaceVariantHex": "#8E8E93",
                  "accentHex": "#30D158"
                }
              },
              {
                "id": "com.keeftalk.theme.solarized.dark",
                "name": "Solarized Dark",
                "isDark": true,
                "darkColors": {
                  "primaryHex": "#268BD2",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#002B36",
                  "onBackgroundHex": "#839496",
                  "surfaceHex": "#073642",
                  "onSurfaceHex": "#93A1A1",
                  "surfaceVariantHex": "#094352",
                  "onSurfaceVariantHex": "#657B83",
                  "accentHex": "#2AA198"
                }
              },
              {
                "id": "com.keeftalk.theme.solarized.light",
                "name": "Solarized Light",
                "isDark": false,
                "lightColors": {
                  "primaryHex": "#268BD2",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#FDF6E3",
                  "onBackgroundHex": "#657B83",
                  "surfaceHex": "#EEE8D5",
                  "onSurfaceHex": "#586E75",
                  "surfaceVariantHex": "#E0D8C3",
                  "onSurfaceVariantHex": "#93A1A1",
                  "accentHex": "#2AA198"
                }
              },
              {
                "id": "com.keeftalk.theme.cyberpunk",
                "name": "Cyberpunk Neon",
                "isDark": true,
                "darkColors": {
                  "primaryHex": "#FF007F",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#0D0221",
                  "onBackgroundHex": "#00F5D4",
                  "surfaceHex": "#190538",
                  "onSurfaceHex": "#7B2CBF",
                  "surfaceVariantHex": "#24094F",
                  "onSurfaceVariantHex": "#F72585",
                  "accentHex": "#7B2CBF"
                }
              },
              {
                "id": "com.keeftalk.theme.nordic",
                "name": "Nordic Glacier",
                "isDark": true,
                "darkColors": {
                  "primaryHex": "#88C0D0",
                  "onPrimaryHex": "#2E3440",
                  "backgroundBodyHex": "#2E3440",
                  "onBackgroundHex": "#ECEFF4",
                  "surfaceHex": "#3B4252",
                  "onSurfaceHex": "#E5E9F0",
                  "surfaceVariantHex": "#434C5E",
                  "onSurfaceVariantHex": "#D8DEE9",
                  "accentHex": "#A3BE8C"
                }
              },
              {
                "id": "com.keeftalk.theme.matrix",
                "name": "OLED Green Matrix",
                "isDark": true,
                "darkColors": {
                  "primaryHex": "#00FF41",
                  "onPrimaryHex": "#000000",
                  "backgroundBodyHex": "#000000",
                  "onBackgroundHex": "#00FF66",
                  "surfaceHex": "#051505",
                  "onSurfaceHex": "#00FF41",
                  "surfaceVariantHex": "#0A250A",
                  "onSurfaceVariantHex": "#00CC33",
                  "accentHex": "#00FF41"
                }
              }
            ]
        """.trimIndent()

        val ktxpBytes = createSignedKtxpBytes(manifestJson, "theme/app_themes.json", appThemesContent, keyPair)
        val outputFile = File("/tmp/app-theme-collection.ktxp")
        outputFile.writeBytes(ktxpBytes)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, outputFile.inputStream())
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.theme.app.collection", manifest?.id)
        assertEquals(ExtensionType.THEME, manifest?.type)
        assertEquals(VerificationResult.PASSED, report.signatureStatus)
        assertTrue(report.isEligibleForInstallation)
    }

    @Test
    fun generateAestheticAppThemesPackKtxp() {
        val keyPair = generateRsaKeyPair()

        val manifestJson = """
            {
              "schemaVersion": 1,
              "id": "com.keeftalk.theme.app.aesthetic",
              "type": "THEME",
              "name": "Aesthetic App Themes Pack",
              "version": "1.0.0",
              "description": "Aesthetic global app themes collection for Keeftalk: Twilight Violet, Warm Parchment, Sahara Gold, and Antique Bronze.",
              "publisher": {
                "name": "Keeftalk Official",
                "id": "official.keeftalk.com",
                "website": "https://keeftalk.com",
                "isVerifiedPublisher": true
              },
              "minAppVersion": 1,
              "capabilities": ["GLOBAL_APP_THEME"],
              "permissions": [],
              "entry": "theme/aesthetic_app_themes.json",
              "assets": [
                {
                  "path": "theme/aesthetic_app_themes.json",
                  "mimeType": "application/json"
                }
              ]
            }
        """.trimIndent()

        val appThemesContent = """
            [
              {
                "id": "com.keeftalk.theme.twilight.violet",
                "name": "Twilight Violet",
                "isDark": true,
                "isReaderThemeOnly": false,
                "darkColors": {
                  "primaryHex": "#8F88B3",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#1D1A30",
                  "onBackgroundHex": "#D9D3F0",
                  "surfaceHex": "#25213D",
                  "onSurfaceHex": "#D9D3F0",
                  "surfaceVariantHex": "#2E2A4A",
                  "onSurfaceVariantHex": "#8F88B3",
                  "accentHex": "#A29BFE"
                }
              },
              {
                "id": "com.keeftalk.theme.warm.parchment",
                "name": "Warm Parchment",
                "isDark": false,
                "isReaderThemeOnly": false,
                "lightColors": {
                  "primaryHex": "#736C58",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#FBFAF5",
                  "onBackgroundHex": "#23221F",
                  "surfaceHex": "#F3F0E6",
                  "onSurfaceHex": "#23221F",
                  "surfaceVariantHex": "#E8E4D8",
                  "onSurfaceVariantHex": "#8C897E",
                  "accentHex": "#736C58"
                }
              },
              {
                "id": "com.keeftalk.theme.sahara.gold",
                "name": "Sahara Gold",
                "isDark": false,
                "isReaderThemeOnly": false,
                "lightColors": {
                  "primaryHex": "#8F7B58",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#F1E4C8",
                  "onBackgroundHex": "#4A3820",
                  "surfaceHex": "#E8D8B5",
                  "onSurfaceHex": "#4A3820",
                  "surfaceVariantHex": "#DFCBA3",
                  "onSurfaceVariantHex": "#8F7B58",
                  "accentHex": "#8F7B58"
                }
              },
              {
                "id": "com.keeftalk.theme.antique.bronze",
                "name": "Antique Bronze",
                "isDark": false,
                "isReaderThemeOnly": false,
                "lightColors": {
                  "primaryHex": "#7D6642",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#D8BF94",
                  "onBackgroundHex": "#33210D",
                  "surfaceHex": "#CCAFA1",
                  "onSurfaceHex": "#33210D",
                  "surfaceVariantHex": "#C2A374",
                  "onSurfaceVariantHex": "#7D6642",
                  "accentHex": "#7D6642"
                }
              }
            ]
        """.trimIndent()

        val ktxpBytes = createSignedKtxpBytes(manifestJson, "theme/aesthetic_app_themes.json", appThemesContent, keyPair)
        val outputFile = File("/tmp/aesthetic-app-themes-pack.ktxp")
        outputFile.writeBytes(ktxpBytes)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, outputFile.inputStream())
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.theme.app.aesthetic", manifest?.id)
        assertEquals(ExtensionType.THEME, manifest?.type)
        assertEquals(VerificationResult.PASSED, report.signatureStatus)
        assertTrue(report.isEligibleForInstallation)
    }

    @Test
    fun generateGenderAppThemesPackKtxp() {
        val keyPair = generateRsaKeyPair()

        val manifestJson = """
            {
              "schemaVersion": 1,
              "id": "com.keeftalk.theme.gender.pack",
              "type": "THEME",
              "name": "Gender Palette Collection (Girls & Boys)",
              "version": "1.0.0",
              "description": "Custom global app theme collection: 3 Girls (Sakura Blossom, Rose Gold Elegance, Sweet Cotton Candy) and 3 Boys (Frostbite Titanium, Stealth Tactical, Cyber Commander).",
              "publisher": {
                "name": "Keeftalk Official",
                "id": "official.keeftalk.com",
                "website": "https://keeftalk.com",
                "isVerifiedPublisher": true
              },
              "minAppVersion": 1,
              "capabilities": ["GLOBAL_APP_THEME"],
              "permissions": [],
              "entry": "theme/gender_themes.json",
              "assets": [
                {
                  "path": "theme/gender_themes.json",
                  "mimeType": "application/json"
                }
              ]
            }
        """.trimIndent()

        val genderThemesContent = """
            [
              {
                "id": "com.keeftalk.theme.pink.sakura",
                "name": "🌸 Sakura Blossom (Girls)",
                "isDark": false,
                "isReaderThemeOnly": false,
                "lightColors": {
                  "primaryHex": "#FF69B4",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#FFF0F5",
                  "onBackgroundHex": "#4A1525",
                  "surfaceHex": "#FFECF2",
                  "onSurfaceHex": "#4A1525",
                  "surfaceVariantHex": "#FFD6E3",
                  "onSurfaceVariantHex": "#FF1493",
                  "accentHex": "#FF1493",
                  "msgMeHex": "#FF69B4",
                  "msgThemHex": "#FFD6E3"
                }
              },
              {
                "id": "com.keeftalk.theme.pink.rosegold",
                "name": "🌹 Rose Gold Elegance (Girls)",
                "isDark": true,
                "isReaderThemeOnly": false,
                "darkColors": {
                  "primaryHex": "#F48FB1",
                  "onPrimaryHex": "#2A1B24",
                  "backgroundBodyHex": "#2A1B24",
                  "onBackgroundHex": "#FCE4EC",
                  "surfaceHex": "#382330",
                  "onSurfaceHex": "#FCE4EC",
                  "surfaceVariantHex": "#4A2E40",
                  "onSurfaceVariantHex": "#FF80AB",
                  "accentHex": "#FF80AB",
                  "msgMeHex": "#F48FB1",
                  "msgThemHex": "#4A2E40"
                }
              },
              {
                "id": "com.keeftalk.theme.pink.cottoncandy",
                "name": "🍬 Sweet Cotton Candy (Girls)",
                "isDark": false,
                "isReaderThemeOnly": false,
                "lightColors": {
                  "primaryHex": "#FF4081",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#FFF5F8",
                  "onBackgroundHex": "#3D0C20",
                  "surfaceHex": "#FFE4EC",
                  "onSurfaceHex": "#3D0C20",
                  "surfaceVariantHex": "#FFCDDC",
                  "onSurfaceVariantHex": "#E91E63",
                  "accentHex": "#E91E63",
                  "msgMeHex": "#FF4081",
                  "msgThemHex": "#FFCDDC"
                }
              },
              {
                "id": "com.keeftalk.theme.boys.frostbite",
                "name": "❄️ Frostbite Titanium (Boys)",
                "isDark": true,
                "isReaderThemeOnly": false,
                "darkColors": {
                  "primaryHex": "#00B4D8",
                  "onPrimaryHex": "#0B131E",
                  "backgroundBodyHex": "#0B131E",
                  "onBackgroundHex": "#E1EBF5",
                  "surfaceHex": "#142132",
                  "onSurfaceHex": "#E1EBF5",
                  "surfaceVariantHex": "#1E314B",
                  "onSurfaceVariantHex": "#90E0EF",
                  "accentHex": "#90E0EF",
                  "msgMeHex": "#00B4D8",
                  "msgThemHex": "#1E314B"
                }
              },
              {
                "id": "com.keeftalk.theme.boys.stealth",
                "name": "🛡️ Stealth Tactical (Boys)",
                "isDark": true,
                "isReaderThemeOnly": false,
                "darkColors": {
                  "primaryHex": "#3B82F6",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#0F1115",
                  "onBackgroundHex": "#D1D5DB",
                  "surfaceHex": "#1A1D24",
                  "onSurfaceHex": "#D1D5DB",
                  "surfaceVariantHex": "#262A34",
                  "onSurfaceVariantHex": "#2563EB",
                  "accentHex": "#2563EB",
                  "msgMeHex": "#3B82F6",
                  "msgThemHex": "#262A34"
                }
              },
              {
                "id": "com.keeftalk.theme.boys.cybercommander",
                "name": "⚡ Cyber Commander (Boys)",
                "isDark": true,
                "isReaderThemeOnly": false,
                "darkColors": {
                  "primaryHex": "#0077B6",
                  "onPrimaryHex": "#FFFFFF",
                  "backgroundBodyHex": "#060A12",
                  "onBackgroundHex": "#F0F6FC",
                  "surfaceHex": "#0E1726",
                  "onSurfaceHex": "#F0F6FC",
                  "surfaceVariantHex": "#17253D",
                  "onSurfaceVariantHex": "#00B4D8",
                  "accentHex": "#00B4D8",
                  "msgMeHex": "#0077B6",
                  "msgThemHex": "#17253D"
                }
              }
            ]
        """.trimIndent()

        val ktxpBytes = createSignedKtxpBytes(manifestJson, "theme/gender_themes.json", genderThemesContent, keyPair)
        val outputFile = File("/tmp/gender-app-themes-pack.ktxp")
        outputFile.writeBytes(ktxpBytes)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, outputFile.inputStream())
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.theme.gender.pack", manifest?.id)
        assertEquals(ExtensionType.THEME, manifest?.type)
        assertEquals(VerificationResult.PASSED, report.signatureStatus)
        assertTrue(report.isEligibleForInstallation)
    }

    @Test
    fun generateReaderTimerExtensionKtxp() {
        val keyPair = generateRsaKeyPair()

        val manifestJson = """
            {
              "schemaVersion": 1,
              "id": "com.keeftalk.timer.reader",
              "type": "TOOL_WIDGET",
              "name": "Reader Timer Pro",
              "version": "1.0.0",
              "description": "Adds a movable floating reading timer and countdown circle inside the Books Reader.",
              "publisher": {
                "name": "Keeftalk Official",
                "id": "official.keeftalk.com",
                "website": "https://keeftalk.com",
                "isVerifiedPublisher": true
              },
              "minAppVersion": 1,
              "capabilities": ["BOOKS_READER_OVERLAY", "READER_TOOL"],
              "permissions": ["READ_STORAGE", "WRITE_STORAGE"],
              "entry": "timer/config.json",
              "assets": [
                {
                  "path": "timer/config.json",
                  "mimeType": "application/json"
                }
              ]
            }
        """.trimIndent()

        val timerConfigContent = """
            {
              "defaultMinutes": 15,
              "allowDragging": true,
              "circleSizeDp": 48,
              "soundAlertOnComplete": true
            }
        """.trimIndent()

        val ktxpBytes = createSignedKtxpBytes(manifestJson, "timer/config.json", timerConfigContent, keyPair)
        val outputFile = File("/tmp/reader-timer-extension.ktxp")
        outputFile.writeBytes(ktxpBytes)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val context = FakeTestContext(tempFolder.root)
        val session = KtxpPackageReader.readAndStagePackageFromStream(context, outputFile.inputStream())
        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.timer.reader", manifest?.id)
        assertEquals(ExtensionType.TOOL_WIDGET, manifest?.type)
        assertEquals(VerificationResult.PASSED, report.signatureStatus)
        assertTrue(report.isEligibleForInstallation)
    }
}

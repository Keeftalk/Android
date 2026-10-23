package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tn.keeftalk.chat.extension.model.VerificationResult
import tn.keeftalk.chat.extension.parser.ExtractedPackageSession
import tn.keeftalk.chat.extension.security.PackageVerifier
import java.io.File

class PackageVerifierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testValidPackageVerification() {
        val stagingDir = tempFolder.newFolder("staging_test")
        val entryFile = File(stagingDir, "theme/theme.json")
        entryFile.parentFile?.mkdirs()
        entryFile.writeText("{\"id\": \"com.keeftalk.theme.midnight\"}")

        val manifestJson = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.theme.midnight",
                "type": "THEME",
                "name": "Midnight Dark Theme",
                "version": "1.0.0",
                "minAppVersion": 1,
                "entry": "theme/theme.json"
            }
        """.trimIndent()

        val session = ExtractedPackageSession(
            stagingDir = stagingDir,
            manifestJson = manifestJson,
            totalExpandedBytes = 1024L,
            entryCount = 2,
            hasExecutableContent = false
        )

        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals("com.keeftalk.theme.midnight", manifest?.id)
        assertEquals(VerificationResult.PASSED, report.formatStatus)
        assertEquals(VerificationResult.PASSED, report.compatibilityStatus)
        assertEquals(VerificationResult.PASSED, report.assetIntegrityStatus)
        assertTrue(report.isEligibleForInstallation)
        assertFalse(report.hasCriticalFailures())
    }

    @Test
    fun testIncompatibleAppVersionRejected() {
        val stagingDir = tempFolder.newFolder("staging_incompatible")
        val entryFile = File(stagingDir, "entry.json")
        entryFile.writeText("{}")

        val manifestJson = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.future.extension",
                "type": "THEME",
                "name": "Future Extension",
                "version": "2.0.0",
                "minAppVersion": 999,
                "entry": "entry.json"
            }
        """.trimIndent()

        val session = ExtractedPackageSession(
            stagingDir = stagingDir,
            manifestJson = manifestJson,
            totalExpandedBytes = 512L,
            entryCount = 2,
            hasExecutableContent = false
        )

        val (manifest, report) = PackageVerifier.verifyPackageSession(session, isNetworkAvailable = false)

        assertNotNull(manifest)
        assertEquals(VerificationResult.FAILED, report.compatibilityStatus)
        assertFalse(report.isEligibleForInstallation)
        assertTrue(report.hasCriticalFailures())
    }

    @Test
    fun testFileSha256Calculation() {
        val testFile = tempFolder.newFile("sample.txt")
        testFile.writeText("Hello Keeftalk Extension Platform")

        val sha256 = PackageVerifier.computeFileSha256Hex(testFile)
        assertNotNull(sha256)
        assertEquals(64, sha256.length) // 64 hex characters
    }
}

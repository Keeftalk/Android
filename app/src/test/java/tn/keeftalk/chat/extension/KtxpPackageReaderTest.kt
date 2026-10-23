package tn.keeftalk.chat.extension

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tn.keeftalk.chat.extension.parser.KtxpPackageReader
import tn.keeftalk.chat.extension.parser.KtxpSecurityException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FakeTestContext(private val baseDir: java.io.File) : android.content.ContextWrapper(null) {
    override fun getFilesDir(): java.io.File {
        return baseDir
    }
}

class KtxpPackageReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSampleZipByteArray(entries: Map<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    @Test
    fun testValidZipPackageExtraction() {
        val context = FakeTestContext(tempFolder.root)

        val manifestContent = """
            {
                "schemaVersion": 1,
                "id": "com.keeftalk.emoji.test",
                "type": "EMOJI_PACK",
                "name": "Test Emoji Pack",
                "version": "1.0.0",
                "entry": "emojis/emojis.json"
            }
        """.trimIndent()

        val zipBytes = createSampleZipByteArray(
            mapOf(
                "manifest.json" to manifestContent,
                "emojis/emojis.json" to "{\"id\": \"test\"}"
            )
        )

        val session = KtxpPackageReader.readAndStagePackageFromStream(context, ByteArrayInputStream(zipBytes))
        assertNotNull(session)
        assertEquals(manifestContent, session.manifestJson)
        assertEquals(2, session.entryCount)
        assertFalse(session.hasExecutableContent)
        assertTrue(session.stagingDir.exists())
    }

    @Test(expected = KtxpSecurityException::class)
    fun testPackageWithExecutableCodeRejected() {
        val context = FakeTestContext(tempFolder.root)

        val zipBytes = createSampleZipByteArray(
            mapOf(
                "manifest.json" to "{\"id\":\"com.test\"}",
                "libnative.so" to "ELF binary content"
            )
        )

        KtxpPackageReader.readAndStagePackageFromStream(context, ByteArrayInputStream(zipBytes))
    }

    @Test(expected = KtxpSecurityException::class)
    fun testPackageWithPathTraversalRejected() {
        val context = FakeTestContext(tempFolder.root)

        val zipBytes = createSampleZipByteArray(
            mapOf(
                "manifest.json" to "{\"id\":\"com.test\"}",
                "../outside_file.txt" to "traversal payload"
            )
        )

        KtxpPackageReader.readAndStagePackageFromStream(context, ByteArrayInputStream(zipBytes))
    }

    @Test(expected = KtxpSecurityException::class)
    fun testPackageMissingManifestRejected() {
        val context = FakeTestContext(tempFolder.root)

        val zipBytes = createSampleZipByteArray(
            mapOf(
                "readme.txt" to "Hello World"
            )
        )

        KtxpPackageReader.readAndStagePackageFromStream(context, ByteArrayInputStream(zipBytes))
    }
}

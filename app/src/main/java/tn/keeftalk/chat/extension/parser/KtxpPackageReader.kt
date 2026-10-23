package tn.keeftalk.chat.extension.parser

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class KtxpSecurityException(message: String) : Exception(message)

data class ExtractedPackageSession(
    val stagingDir: File,
    val manifestJson: String,
    val listingJson: String? = null,
    val releaseNotesJson: String? = null,
    val totalExpandedBytes: Long,
    val entryCount: Int,
    val hasExecutableContent: Boolean,
    val coverFile: File? = null,
    val screenshotFiles: List<File> = emptyList()
)

object KtxpPackageReader {
    private const val TAG = "KtxpPackageReader"
    private const val MAX_TOTAL_EXPANDED_BYTES = 150L * 1024L * 1024L // 150 MB limit
    private const val MAX_ENTRY_COUNT = 1000
    private const val MAX_SINGLE_FILE_BYTES = 100L * 1024L * 1024L // 100 MB limit

    private val EXECUTABLE_EXTENSIONS = setOf(
        "dex", "jar", "so", "class", "sh", "exe", "elf", "apk", "aab", "py", "bat", "cmd", "ps1"
    )

    fun readAndStagePackage(context: Context, packageUri: Uri): ExtractedPackageSession {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(packageUri)
            ?: throw KtxpSecurityException("Unable to open input stream for package URI: $packageUri")

        return readAndStagePackageFromStream(context, inputStream)
    }

    fun readAndStagePackageFromStream(context: Context, inputStream: InputStream): ExtractedPackageSession {
        val stagingBaseDir = File(context.filesDir, "extensions/staging")
        if (!stagingBaseDir.exists()) {
            stagingBaseDir.mkdirs()
        }

        val sessionDir = File(stagingBaseDir, "pkg_" + UUID.randomUUID().toString())
        sessionDir.mkdirs()

        var totalExpandedBytes = 0L
        var entryCount = 0
        var manifestJson: String? = null
        var listingJson: String? = null
        var releaseNotesJson: String? = null
        var hasExecutable = false

        val extractedFiles = mutableListOf<File>()

        try {
            ZipInputStream(inputStream.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val rawName = entry.name
                    entryCount++

                    if (entryCount > MAX_ENTRY_COUNT) {
                        throw KtxpSecurityException("Package exceeds maximum entry count limit ($MAX_ENTRY_COUNT entries).")
                    }

                    // Path Traversal Security Check
                    val normalizedPath = sanitizeAndValidatePath(rawName)
                    val ext = normalizedPath.substringAfterLast('.', "").lowercase()

                    if (EXECUTABLE_EXTENSIONS.contains(ext)) {
                        Log.w(TAG, "Forbidden executable extension detected in package: $rawName")
                        hasExecutable = true
                    }

                    val targetFile = File(sessionDir, normalizedPath)
                    // Canonical path check
                    if (!targetFile.canonicalPath.startsWith(sessionDir.canonicalPath + File.separator) &&
                        targetFile.canonicalPath != sessionDir.canonicalPath) {
                        throw KtxpSecurityException("Path traversal attempt detected in entry: $rawName")
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        var entryBytes = 0L
                        FileOutputStream(targetFile).use { out ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (zip.read(buffer).also { bytesRead = it } != -1) {
                                entryBytes += bytesRead
                                totalExpandedBytes += bytesRead

                                if (entryBytes > MAX_SINGLE_FILE_BYTES) {
                                    throw KtxpSecurityException("Single file in package exceeds size limit ($rawName).")
                                }
                                if (totalExpandedBytes > MAX_TOTAL_EXPANDED_BYTES) {
                                    throw KtxpSecurityException("Total decompressed size exceeds maximum limit of 150MB (Zip bomb protection).")
                                }
                                out.write(buffer, 0, bytesRead)
                            }
                        }

                        extractedFiles.add(targetFile)

                        if (normalizedPath == "manifest.json") {
                            manifestJson = targetFile.readText()
                        } else if (normalizedPath == "metadata/listing.json") {
                            listingJson = targetFile.readText()
                        } else if (normalizedPath == "metadata/release-notes.json") {
                            releaseNotesJson = targetFile.readText()
                        }
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (manifestJson.isNullOrBlank()) {
                sessionDir.deleteRecursively()
                throw KtxpSecurityException("Package is missing required 'manifest.json' file at root.")
            }

            if (hasExecutable) {
                sessionDir.deleteRecursively()
                throw KtxpSecurityException("Package contains forbidden executable code or binary libraries. Keeftalk strictly bans executable extension code.")
            }

            val coverFile = extractedFiles.firstOrNull { it.name.startsWith("cover") || it.path.contains("assets/cover") }
            val screenshots = extractedFiles.filter { it.path.contains("assets/screenshots") }.sortedBy { it.name }

            return ExtractedPackageSession(
                stagingDir = sessionDir,
                manifestJson = manifestJson,
                listingJson = listingJson,
                releaseNotesJson = releaseNotesJson,
                totalExpandedBytes = totalExpandedBytes,
                entryCount = entryCount,
                hasExecutableContent = hasExecutable,
                coverFile = coverFile,
                screenshotFiles = screenshots
            )
        } catch (e: Exception) {
            sessionDir.deleteRecursively()
            throw if (e is KtxpSecurityException) e else KtxpSecurityException("Failed to extract package: ${e.message}")
        }
    }

    private fun sanitizeAndValidatePath(entryName: String): String {
        val cleanName = entryName.replace('\\', '/')
        if (cleanName.startsWith("/") || cleanName.contains("../") || cleanName.contains("/..") || cleanName == "..") {
            throw KtxpSecurityException("Path traversal or illegal relative path detected: $entryName")
        }
        return cleanName
    }
}

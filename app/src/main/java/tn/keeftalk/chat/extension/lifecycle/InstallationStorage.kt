package tn.keeftalk.chat.extension.lifecycle

import android.content.Context
import android.util.Log
import java.io.File

class InstallationStorage(private val context: Context) {
    private val TAG = "InstallationStorage"

    val baseInstalledDir: File
        get() = File(context.filesDir, "extensions/installed").also { if (!it.exists()) it.mkdirs() }

    fun getExtensionInstallDir(extensionId: String, version: String): File {
        val sanitizedId = extensionId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val sanitizedVersion = version.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(baseInstalledDir, "$sanitizedId/$sanitizedVersion")
    }

    fun installFromStaging(stagingDir: File, extensionId: String, version: String): File {
        val targetDir = getExtensionInstallDir(extensionId, version)
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        try {
            stagingDir.copyRecursively(targetDir, overwrite = true)
            Log.i(TAG, "Successfully copied staged package to target installation directory: ${targetDir.absolutePath}")
            return targetDir
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            throw IllegalStateException("Failed to copy staged extension files to target installation directory: ${e.message}", e)
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    fun uninstallExtension(extensionId: String): Boolean {
        val sanitizedId = extensionId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val parentDir = File(baseInstalledDir, sanitizedId)
        return if (parentDir.exists()) {
            val deleted = parentDir.deleteRecursively()
            Log.i(TAG, "Uninstalled extension '$extensionId', files removed: $deleted")
            deleted
        } else {
            true
        }
    }
}

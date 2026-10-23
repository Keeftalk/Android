package tn.keeftalk.chat.ui.docs.viewer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

object DocAssetDownloadManager {
    private const val TAG = "DocAssetDownloadManager"
    private const val DIR_NAME = "doc_assets"

    const val ASSET_MERMAID = "mermaid.min.js"
    const val ASSET_KATEX_JS = "katex.min.js"
    const val ASSET_KATEX_CSS = "katex.min.css"
    const val ASSET_CHART_JS = "chart.umd.min.js"

    private val assetUrls = mapOf(
        ASSET_MERMAID to "https://public.cdn.keeftalk.com/mermaid.min.js",
        ASSET_KATEX_JS to "https://public.cdn.keeftalk.com/katex.min.js",
        ASSET_KATEX_CSS to "https://public.cdn.keeftalk.com/katex.min.css",
        ASSET_CHART_JS to "https://public.cdn.keeftalk.com/chart.umd.min.js"
    )

    private val declinedAssets = mutableSetOf<String>()

    fun isDeclined(assetKey: String): Boolean = declinedAssets.contains(assetKey)
    fun setDeclined(assetKey: String, declined: Boolean) {
        if (declined) declinedAssets.add(assetKey) else declinedAssets.remove(assetKey)
    }

    fun getAssetFile(context: Context, assetKey: String): File {
        val dir = File(context.filesDir, DIR_NAME)
        return File(dir, assetKey)
    }

    fun isAssetInstalled(context: Context, assetKey: String): Boolean {
        val file = getAssetFile(context, assetKey)
        val minSize = if (assetKey.endsWith(".css")) 500 else 10000
        return file.exists() && file.length() > minSize
    }

    suspend fun downloadAsset(
        context: Context,
        assetKey: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Fallback for katex.min.css if CDN URL is derived from katex.min.js
            val urlString = assetUrls[assetKey] ?: if (assetKey == ASSET_KATEX_CSS) {
                "https://public.cdn.keeftalk.com/katex.min.css"
            } else {
                return@withContext Result.failure(Exception("Unknown asset: $assetKey"))
            }

            Log.i(TAG, "Starting secure download of $assetKey from $urlString")
            val dir = File(context.filesDir, DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val targetFile = File(dir, assetKey)
            val tempFile = File(dir, "$assetKey.tmp")

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpsURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                requestMethod = "GET"
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP error code: $responseCode"))
            }

            val fileLength = connection.contentLength.toFloat()
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (fileLength > 0) {
                            onProgress(totalBytesRead / fileLength)
                        }
                    }
                    output.flush()
                }
            }

            val computedChecksum = digest.digest().joinToString("") { "%02x".format(it) }
            Log.i(TAG, "Downloaded $assetKey. Size: $totalBytesRead bytes, SHA-256: $computedChecksum")

            val minSize = if (assetKey.endsWith(".css")) 500 else 10000
            if (tempFile.length() < minSize) {
                tempFile.delete()
                return@withContext Result.failure(Exception("Downloaded file size is too small or invalid."))
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Log.i(TAG, "$assetKey installed successfully at ${targetFile.absolutePath}")
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download $assetKey", e)
            Result.failure(e)
        }
    }
}

package com.keeftalk.chat.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object StorageUtils {
    private const val TAG = "StorageUtils"

    fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        var fileName = "temp_file_${UUID.randomUUID()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        val tempFile = File(context.cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Saves a private file to the public Downloads folder and registers it with MediaStore.
     */
    fun saveFileToPublicDownloads(context: Context, sourceFile: File, displayName: String, mimeType: String?): Uri? {
        Log.i(TAG, "SAVING_TO_PUBLIC_DOWNLOADS | name=$displayName | mime=$mimeType")
        val contentResolver = context.contentResolver
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                    Log.i(TAG, "PUBLIC_SAVE_SUCCESS | uri=$uri")
                    uri
                } catch (e: Exception) {
                    Log.e(TAG, "PUBLIC_SAVE_FAILED", e)
                    contentResolver.delete(uri, null, null)
                    null
                }
            } else null
        } else {
            // Legacy implementation for older Android versions
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destFile = File(downloadsDir, displayName)
            try {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                // Trigger media scanner so it shows up in "Files" app
                android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                Log.i(TAG, "PUBLIC_SAVE_SUCCESS_LEGACY | path=${destFile.absolutePath}")
                Uri.fromFile(destFile)
            } catch (e: Exception) {
                Log.e(TAG, "PUBLIC_SAVE_FAILED_LEGACY", e)
                null
            }
        }
    }
}

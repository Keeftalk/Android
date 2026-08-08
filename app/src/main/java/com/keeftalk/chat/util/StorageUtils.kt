package com.keeftalk.chat.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.*

object StorageUtils {
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
}

package com.keeftalk.chat.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    fun openFile(context: Context, file: File) {
        val authority = "com.keeftalk.chat.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        val extension = file.extension
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "No app found to open this file type", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

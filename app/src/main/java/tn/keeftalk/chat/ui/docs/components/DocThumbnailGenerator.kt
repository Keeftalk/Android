package tn.keeftalk.chat.ui.docs.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.domain.model.Doc
import tn.keeftalk.chat.domain.model.DocFormat
import java.io.File
import java.io.FileOutputStream

object DocThumbnailGenerator {

    /**
     * Generates or retrieves a cached bitmap thumbnail for a document.
     */
    suspend fun getThumbnail(context: Context, doc: Doc): Bitmap? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "doc_thumbnails").apply { if (!exists()) mkdirs() }
        val thumbnailFile = File(cacheDir, "${doc.id}_thumb.png")

        if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
            val cachedBmp = android.graphics.BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            if (cachedBmp != null) return@withContext cachedBmp
        }

        val docFile = File(doc.filePath)
        if (!docFile.exists()) return@withContext null

        val bitmap = when (doc.format) {
            DocFormat.PDF -> renderPdfPageZero(docFile)
            else -> null
        }

        if (bitmap != null) {
            runCatching {
                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                }
            }
        }

        bitmap
    }

    private fun renderPdfPageZero(file: File): Bitmap? {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val width = 240
                            val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(180, 360)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    } else null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DocThumbnailGenerator", "Error rendering PDF thumbnail", e)
            null
        }
    }
}

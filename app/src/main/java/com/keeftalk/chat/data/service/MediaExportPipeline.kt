package com.keeftalk.chat.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.*
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import com.keeftalk.chat.ui.screens.editor.graphics.*
import com.keeftalk.chat.domain.model.EditorModel
import com.keeftalk.chat.domain.model.MessageType
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

@UnstableApi
class MediaExportPipeline(
    private val context: Context,
    private val chatRepository: ChatRepository,
) {

    suspend fun exportAndSend(
        chatId: String,
        editorModels: List<EditorModel>
    ) = withContext(Dispatchers.IO) {
        editorModels.forEach { model ->
            val exportedFile = when {
                model.mediaItem?.isImage == true -> exportImage(model)
                model.mediaItem?.isVideo == true -> exportVideo(model)
                model.documentItem?.type == com.keeftalk.chat.domain.model.DocumentType.PDF -> {
                    // CRITICAL: If no annotations are present, we MUST copy the original file 
                    // to preserve multi-page content. exportPdf() currently renders to a 1-page bitmap.
                    if (model.drawingPaths.isEmpty() && model.blurRegions.isEmpty() && model.textElements.isEmpty()) {
                        model.documentItem.uri.let { uri ->
                            val file = File(context.cacheDir, "temp_${UUID.randomUUID()}_${model.documentItem.name}")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(file).use { output -> input.copyTo(output) }
                            }
                            file
                        }
                    } else {
                        exportPdf(model)
                    }
                }
                model.documentItem != null && model.editedText != null -> exportText(model)
                else -> model.documentItem?.uri?.let { uri ->
                    // Just copy the original file if no edits
                    val file = File(context.cacheDir, "temp_${UUID.randomUUID()}_${model.documentItem.name}")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output -> input.copyTo(output) }
                    }
                    file
                }
            }

            exportedFile?.let { file ->
                val type = when {
                    model.mediaItem?.isImage == true -> MessageType.IMAGE
                    model.mediaItem?.isVideo == true -> MessageType.VIDEO
                    model.documentItem?.type == com.keeftalk.chat.domain.model.DocumentType.PDF -> MessageType.PDF
                    else -> MessageType.FILE
                }
                chatRepository.sendMessage(
                    chatId = chatId,
                    content = model.caption,
                    type = type,
                    filePath = file.absolutePath
                )
            }
        }
    }

    private suspend fun exportPdf(model: EditorModel): File? = withContext(Dispatchers.IO) {
        // Simple PDF export: Render annotated bitmap into a single-page PDF for now
        // A more advanced version would use a PDF library to overlay on existing PDF
        try {
            val bitmap = renderAnnotatedBitmap(model) ?: return@withContext null
            val pdfFile = File(context.cacheDir, "exported_${UUID.randomUUID()}.pdf")
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
            FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun exportText(model: EditorModel): File? = withContext(Dispatchers.IO) {
        try {
            val textFile = File(context.cacheDir, "edited_${UUID.randomUUID()}_${model.documentItem?.name ?: "doc.txt"}")
            textFile.writeText(model.editedText ?: "")
            textFile
        } catch (e: Exception) {
            null
        }
    }

    private fun renderAnnotatedBitmap(model: EditorModel): Bitmap? {
        val uri = model.mediaItem?.uri ?: model.documentItem?.uri ?: return null
        
        // If it's a PDF, we need to render the first page to a bitmap first
        val sourceBitmap = if (model.documentItem?.type == com.keeftalk.chat.domain.model.DocumentType.PDF) {
            renderPdfPageToBitmap(uri)
        } else {
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it)
            }
        } ?: return null

        val mutableBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap.asImageBitmap())
        val bitmapWidth = mutableBitmap.width.toFloat()
        val bitmapHeight = mutableBitmap.height.toFloat()

        // 1. Draw paths
        model.drawingPaths.forEach { drawingPath ->
            canvas.save()
            val refSize = drawingPath.referenceSize
            if (refSize != null && refSize.width > 0 && refSize.height > 0) {
                val scaleX = bitmapWidth / refSize.width
                val scaleY = bitmapHeight / refSize.height
                val matrix = android.graphics.Matrix()
                matrix.postScale(scaleX, scaleY)
                canvas.nativeCanvas.concat(matrix)
            }

            val renderer = BezierDrawingRenderer(
                color = drawingPath.color,
                strokeWidth = drawingPath.strokeWidth,
                path = drawingPath.path,
                isEraser = drawingPath.isEraser
            )
            renderer.draw(canvas)
            canvas.restore()
        }

        // 2. Blur regions
        model.blurRegions.forEach { region ->
            val bounds = android.graphics.RectF()
            region.path.asAndroidPath().computeBounds(bounds, true)
            val rect = android.graphics.Rect(
                (bounds.left * bitmapWidth).toInt().coerceIn(0, mutableBitmap.width),
                (bounds.top * bitmapHeight).toInt().coerceIn(0, mutableBitmap.height),
                (bounds.right * bitmapWidth).toInt().coerceIn(0, mutableBitmap.width),
                (bounds.bottom * bitmapHeight).toInt().coerceIn(0, mutableBitmap.height)
            )
            if (rect.width() > 0 && rect.height() > 0) {
                applyMosaicBlur(mutableBitmap, rect)
            }
        }

        // 3. Text elements
        model.textElements.forEach { textElement ->
            canvas.save()
            val renderer = TextRenderer(textElement.text, textElement.color, textElement.fontSize * (bitmapWidth / 1080f))
            val matrix = android.graphics.Matrix()
            matrix.postTranslate(textElement.x * bitmapWidth, textElement.y * bitmapHeight)
            canvas.nativeCanvas.concat(matrix)
            renderer.draw(canvas)
            canvas.restore()
        }

        return mutableBitmap
    }

    private fun renderPdfPageToBitmap(uri: Uri): Bitmap? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun exportImage(model: EditorModel): File? = withContext(Dispatchers.IO) {
        try {
            val annotatedBitmap = renderAnnotatedBitmap(model) ?: return@withContext null
            val tempFile = File(context.cacheDir, "exported_${UUID.randomUUID()}.jpg")
            FileOutputStream(tempFile).use { out ->
                annotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            stripMetadata(tempFile)
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun exportVideo(model: EditorModel): File? = withContext(Dispatchers.Main) {
        val mediaUri = model.mediaItem?.uri ?: return@withContext null
        android.util.Log.d("MEDIA_EDITOR_DEBUG", "Exporting video: $mediaUri")
        val outputFile = File(context.cacheDir, "exported_${UUID.randomUUID()}.mp4")
        val deferred = kotlinx.coroutines.CompletableDeferred<File?>()

        try {
            val transformer = Transformer.Builder(context)
                .build()
            
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(mediaUri)
            
            model.videoTrimRange?.let { range ->
                android.util.Log.d("MEDIA_EDITOR_DEBUG", "Trimming video: ${range.first}ms to ${range.last}ms")
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(range.first)
                        .setEndPositionMs(range.last)
                        .build()
                )
            }

            val mediaItem = mediaItemBuilder.build()
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(false)
                .build()

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                    android.util.Log.d("MEDIA_EDITOR_DEBUG", "Video export COMPLETED")
                    deferred.complete(outputFile)
                }

                override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exportException: ExportException) {
                    android.util.Log.e("MEDIA_EDITOR_DEBUG", "Video export FAILED", exportException)
                    deferred.complete(null)
                }
            }

            transformer.addListener(listener)
            transformer.start(editedMediaItem, outputFile.absolutePath)
            
            val result = deferred.await()
            result
        } catch (e: Exception) {
            android.util.Log.e("MEDIA_EDITOR_DEBUG", "Video export setup failed", e)
            null
        }
    }

    private fun stripMetadata(file: File) {
        try {
            val exifInterface = ExifInterface(file.absolutePath)
            val attributes = arrayOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_DATETIME
            )
            attributes.forEach { exifInterface.setAttribute(it, null) }
            exifInterface.saveAttributes()
        } catch (_: Exception) {}
    }

    private fun applyMosaicBlur(bitmap: Bitmap, rect: android.graphics.Rect) {
        val mosaicSize = 40
        val width = rect.width()
        val height = rect.height()
        
        val small = Bitmap.createScaledBitmap(
            Bitmap.createBitmap(bitmap, rect.left, rect.top, width, height),
            (width / mosaicSize).coerceAtLeast(1),
            (height / mosaicSize).coerceAtLeast(1),
            false
        )
        val blurred = Bitmap.createScaledBitmap(small, width, height, false)
        
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawBitmap(blurred, rect.left.toFloat(), rect.top.toFloat(), null)
    }
}

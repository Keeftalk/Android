package com.keeftalk.chat.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Typeface
import androidx.compose.ui.graphics.*
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.TextureOverlay
import com.keeftalk.chat.domain.model.EditorModel
import com.keeftalk.chat.domain.model.MessageType
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext
import com.keeftalk.chat.ui.screens.editor.signal.model.SignalEditorModel
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

    private val typefaceProvider = object : RendererContext.TypefaceProvider {
        override fun getSelectedTypeface(context: Context, renderer: Renderer, invalidate: RendererContext.Invalidate): Typeface = Typeface.DEFAULT
    }

    suspend fun exportAndSend(
        chatId: String,
        editorModels: List<EditorModel>
    ) = withContext(Dispatchers.IO) {
        editorModels.forEach { model ->
            val exportedFile = when {
                model.mediaItem?.isImage == true -> exportImage(model)
                model.mediaItem?.isVideo == true -> exportVideo(model)
                model.documentItem?.type == com.keeftalk.chat.domain.model.DocumentType.PDF -> {
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
        try {
            val uri = model.mediaItem?.uri ?: model.documentItem?.uri ?: return@withContext null
            val sourceBitmap = renderPdfPageToBitmap(uri) ?: return@withContext null
            val pdfFile = File(context.cacheDir, "exported_${UUID.randomUUID()}.pdf")
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(sourceBitmap.width, sourceBitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
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
            val uri = model.mediaItem?.uri ?: return@withContext null
            val signalModel = model.signalState as? SignalEditorModel
            val tempFile = File(context.cacheDir, "exported_${UUID.randomUUID()}.jpg")
            
            if (signalModel != null) {
                android.util.Log.d("MEDIA_EDITOR_DEBUG", "Exporting image with Signal state. Changed: ${signalModel.isChanged()}")
                val bitmap = signalModel.render(context, typefaceProvider)
                FileOutputStream(tempFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }
                bitmap.recycle()
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
            }
            
            stripMetadata(tempFile)
            tempFile
        } catch (e: Exception) {
            android.util.Log.e("MEDIA_EDITOR_DEBUG", "Image export failed", e)
            null
        }
    }

    private suspend fun exportVideo(model: EditorModel): File? = withContext(Dispatchers.Main) {
        val mediaUri = model.mediaItem?.uri ?: return@withContext null
        val signalModel = model.signalState as? SignalEditorModel
        val outputFile = File(context.cacheDir, "exported_${UUID.randomUUID()}.mp4")
        val deferred = kotlinx.coroutines.CompletableDeferred<File?>()

        try {
            val mediaItemBuilder = MediaItem.Builder().setUri(mediaUri)
            model.videoTrimRange?.let { range ->
                mediaItemBuilder.setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(range.first)
                        .setEndPositionMs(range.last)
                        .build()
                )
            }

            val editedMediaItemBuilder = EditedMediaItem.Builder(mediaItemBuilder.build())
                .setRemoveAudio(false)

            if (signalModel != null) {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, mediaUri)
                val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1024
                val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 1024
                retriever.release()

                val overlayBitmap = withContext(Dispatchers.IO) {
                    signalModel.renderAnnotationsOnly(context, Point(width, height), typefaceProvider)
                }
                
                val overlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
                val overlayEffect = OverlayEffect(com.google.common.collect.ImmutableList.of(overlay as TextureOverlay))
                
                editedMediaItemBuilder.setEffects(Effects(
                    listOf<AudioProcessor>(),
                    listOf<Effect>(overlayEffect)
                ))
            }

            val transformer = Transformer.Builder(context).build()
            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                    deferred.complete(outputFile)
                }
                override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exportException: ExportException) {
                    deferred.complete(null)
                }
            })

            transformer.start(editedMediaItemBuilder.build(), outputFile.absolutePath)
            deferred.await()
        } catch (e: Exception) {
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
}

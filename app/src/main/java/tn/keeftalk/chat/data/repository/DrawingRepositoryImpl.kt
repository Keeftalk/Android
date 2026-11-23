package tn.keeftalk.chat.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.data.local.dao.DrawingDao
import tn.keeftalk.chat.data.local.entities.DrawingEntity
import tn.keeftalk.chat.data.local.entities.DrawingSyncQueueEntity
import tn.keeftalk.chat.data.remote.DrawingSyncDto
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.Drawing
import tn.keeftalk.chat.domain.model.DrawingElement
import tn.keeftalk.chat.domain.model.DrawingExportFormat
import tn.keeftalk.chat.domain.repository.DrawingRepository
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

class DrawingRepositoryImpl(
    private val drawingDao: DrawingDao,
    private val context: Context
) : DrawingRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val drawingsDir by lazy {
        File(context.filesDir, "drawings").apply { if (!exists()) mkdirs() }
    }

    private fun getNativeFile(drawingId: String): File {
        return File(drawingsDir, "$drawingId.ktd")
    }

    private fun getThumbnailFile(drawingId: String): File {
        return File(drawingsDir, "${drawingId}_thumb.png")
    }

    private suspend fun readDrawingFromDisk(entity: DrawingEntity): Drawing {
        val file = getNativeFile(entity.id)
        if (file.exists()) {
            return runCatching {
                Drawing.fromJson(file.readText())
            }.getOrNull() ?: entityToDomain(entity)
        }
        return entityToDomain(entity)
    }

    private fun entityToDomain(entity: DrawingEntity): Drawing {
        return Drawing(
            id = entity.id,
            ownerUserId = entity.ownerUserId,
            title = entity.title,
            nativeFilePath = entity.nativeFilePath,
            thumbnailPath = entity.thumbnailPath,
            canvasWidth = entity.canvasWidth,
            canvasHeight = entity.canvasHeight,
            backgroundColorHex = entity.backgroundColorHex,
            formatVersion = entity.formatVersion,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isDeleted = entity.isDeleted,
            syncStatus = entity.syncStatus,
            revision = entity.revision,
            contentHash = entity.contentHash
        )
    }

    private suspend fun enqueueSync(action: String, entityId: String, payloadJson: String) {
        runCatching {
            val item = DrawingSyncQueueEntity(
                id = "drawing_sync_${System.currentTimeMillis()}_${UUID.randomUUID()}",
                action = action,
                entityType = "DRAWING",
                payloadJson = payloadJson,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            drawingDao.insertSyncItem(item)
        }
    }

    override fun getAllDrawingsFlow(): Flow<List<Drawing>> {
        return drawingDao.getAllDrawingsFlow().map { list ->
            list.map { readDrawingFromDisk(it) }
        }
    }

    override suspend fun getAllDrawings(): List<Drawing> = withContext(Dispatchers.IO) {
        drawingDao.getAllDrawings().map { readDrawingFromDisk(it) }
    }

    override suspend fun getDrawingById(id: String): Drawing? = withContext(Dispatchers.IO) {
        val entity = drawingDao.getDrawingById(id) ?: return@withContext null
        readDrawingFromDisk(entity)
    }

    override fun getDrawingByIdFlow(id: String): Flow<Drawing?> {
        return drawingDao.getDrawingByIdFlow(id).map { entity ->
            entity?.let { readDrawingFromDisk(it) }
        }
    }

    override suspend fun saveDrawing(drawing: Drawing, thumbnailBitmap: Bitmap?): Drawing = withContext(Dispatchers.IO) {
        val updatedDrawing = drawing.copy(updatedAt = System.currentTimeMillis())

        // Save native .ktd file
        val nativeFile = getNativeFile(updatedDrawing.id)
        nativeFile.writeText(updatedDrawing.toJson())

        // Save thumbnail if provided
        var thumbPath = updatedDrawing.thumbnailPath
        if (thumbnailBitmap != null) {
            val thumbFile = getThumbnailFile(updatedDrawing.id)
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            thumbPath = thumbFile.absolutePath
        }

        val finalDrawing = updatedDrawing.copy(
            nativeFilePath = nativeFile.absolutePath,
            thumbnailPath = thumbPath
        )

        val entity = DrawingEntity(
            id = finalDrawing.id,
            ownerUserId = finalDrawing.ownerUserId,
            title = finalDrawing.title,
            nativeFilePath = finalDrawing.nativeFilePath,
            thumbnailPath = finalDrawing.thumbnailPath,
            canvasWidth = finalDrawing.canvasWidth,
            canvasHeight = finalDrawing.canvasHeight,
            backgroundColorHex = finalDrawing.backgroundColorHex,
            formatVersion = finalDrawing.formatVersion,
            contentHash = finalDrawing.contentHash,
            revision = finalDrawing.revision,
            isDeleted = finalDrawing.isDeleted,
            syncStatus = "PENDING",
            createdAt = finalDrawing.createdAt,
            updatedAt = finalDrawing.updatedAt
        )

        drawingDao.insertOrUpdateDrawing(entity)

        val userId = runCatching {
            AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id
        }.getOrNull() ?: "local"

        val syncDto = DrawingSyncDto.fromEntity(entity, userId, finalDrawing.toJson())
        enqueueSync("UPSERT", entity.id, json.encodeToString(syncDto))

        return@withContext finalDrawing
    }

    override suspend fun duplicateDrawing(id: String): Drawing? = withContext(Dispatchers.IO) {
        val original = getDrawingById(id) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        val duplicated = original.copy(
            id = newId,
            title = "${original.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            nativeFilePath = "",
            thumbnailPath = ""
        )

        val origThumb = File(original.thumbnailPath)
        var thumbBitmap: Bitmap? = null
        if (origThumb.exists()) {
            thumbBitmap = android.graphics.BitmapFactory.decodeFile(origThumb.absolutePath)
        }

        saveDrawing(duplicated, thumbBitmap)
    }

    override suspend fun deleteDrawing(id: String) = withContext(Dispatchers.IO) {
        val entity = drawingDao.getDrawingById(id) ?: return@withContext
        drawingDao.markDrawingDeleted(id)
        val userId = runCatching {
            AppModule.provideSupabaseClientAsync(context).auth.currentUserOrNull()?.id
        }.getOrNull() ?: "local"

        val syncDto = DrawingSyncDto.fromEntity(entity.copy(isDeleted = true), userId)
        enqueueSync("DELETE", id, json.encodeToString(syncDto))
    }

    override suspend fun exportDrawing(
        drawing: Drawing,
        format: DrawingExportFormat,
        outputStream: OutputStream,
        bitmapProvider: suspend (Drawing) -> Bitmap?
    ) = withContext(Dispatchers.IO) {
        when (format) {
            DrawingExportFormat.PNG -> {
                val bitmap = bitmapProvider(drawing)
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            DrawingExportFormat.JPEG -> {
                val bitmap = bitmapProvider(drawing)
                if (bitmap != null) {
                    val rgbBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(rgbBitmap)
                    val bgColor = try { Color.parseColor(drawing.backgroundColorHex) } catch (_: Exception) { Color.WHITE }
                    canvas.drawColor(bgColor)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    rgbBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
            }
            DrawingExportFormat.KTD -> {
                outputStream.write(drawing.toJson().toByteArray(Charsets.UTF_8))
            }
            DrawingExportFormat.SVG -> {
                val svgContent = buildSvgString(drawing)
                outputStream.write(svgContent.toByteArray(Charsets.UTF_8))
            }
        }
        outputStream.flush()
    }

    private fun buildSvgString(drawing: Drawing): String {
        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="${drawing.canvasWidth}" height="${drawing.canvasHeight}" viewBox="0 0 ${drawing.canvasWidth} ${drawing.canvasHeight}">""").append("\n")
        sb.append("""  <rect width="100%" height="100%" fill="${drawing.backgroundColorHex}" />""").append("\n")

        for (layer in drawing.layers.filter { it.isVisible }) {
            sb.append("""  <g opacity="${layer.opacity}">""").append("\n")
            for (element in layer.elements) {
                when (element) {
                    is DrawingElement.Stroke -> {
                        if (element.points.size >= 2) {
                            val pathData = StringBuilder()
                            pathData.append("M ").append(element.points.first().x).append(" ").append(element.points.first().y)
                            for (p in element.points.drop(1)) {
                                pathData.append(" L ").append(p.x).append(" ").append(p.y)
                            }
                            sb.append("""    <path d="$pathData" stroke="${element.colorHex}" stroke-width="${element.strokeWidth}" fill="none" stroke-linecap="round" stroke-linejoin="round" opacity="${element.opacity}" />""").append("\n")
                        }
                    }
                    is DrawingElement.Shape -> {
                        val minX = minOf(element.startX, element.endX)
                        val minY = minOf(element.startY, element.endY)
                        val width = kotlin.math.abs(element.endX - element.startX)
                        val height = kotlin.math.abs(element.endY - element.startY)
                        val fill = if (element.isFilled) element.fillColorHex else "none"

                        sb.append("""    <rect x="$minX" y="$minY" width="$width" height="$height" stroke="${element.colorHex}" stroke-width="${element.strokeWidth}" fill="$fill" />""").append("\n")
                    }
                    is DrawingElement.Text -> {
                        sb.append("""    <text x="${element.x}" y="${element.y}" fill="${element.colorHex}" font-size="${element.fontSizeSp}px">${element.text}</text>""").append("\n")
                    }
                    is DrawingElement.ImportedImage -> {
                        sb.append("""    <!-- Imported image: ${element.imagePath} -->""").append("\n")
                    }
                }
            }
            sb.append("  </g>\n")
        }
        sb.append("</svg>")
        return sb.toString()
    }

    override suspend fun syncPendingDrawings() = withContext(Dispatchers.IO) {
        val pendingItems = drawingDao.getPendingSyncItems()
        if (pendingItems.isEmpty()) return@withContext

        val supabase = runCatching { AppModule.provideSupabaseClientAsync(context) }.getOrNull() ?: return@withContext
        val currentUser = supabase.auth.currentUserOrNull() ?: return@withContext

        for (item in pendingItems) {
            runCatching {
                val dto = json.decodeFromString<DrawingSyncDto>(item.payloadJson)
                val syncedDto = dto.copy(userId = currentUser.id)

                if (item.action == "DELETE") {
                    supabase.postgrest["user_drawings"].delete {
                        filter {
                            eq("id", dto.id)
                            eq("user_id", currentUser.id)
                        }
                    }
                } else {
                    supabase.postgrest["user_drawings"].upsert(syncedDto)
                }

                drawingDao.deleteSyncItem(item.id)
            }
        }
    }
}

package tn.keeftalk.chat.domain.repository

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import tn.keeftalk.chat.domain.model.Drawing
import tn.keeftalk.chat.domain.model.DrawingExportFormat
import java.io.OutputStream

interface DrawingRepository {
    fun getAllDrawingsFlow(): Flow<List<Drawing>>
    suspend fun getAllDrawings(): List<Drawing>
    suspend fun getDrawingById(id: String): Drawing?
    fun getDrawingByIdFlow(id: String): Flow<Drawing?>
    suspend fun saveDrawing(drawing: Drawing, thumbnailBitmap: Bitmap? = null): Drawing
    suspend fun duplicateDrawing(id: String): Drawing?
    suspend fun deleteDrawing(id: String)
    suspend fun exportDrawing(drawing: Drawing, format: DrawingExportFormat, outputStream: OutputStream, bitmapProvider: suspend (Drawing) -> Bitmap?)
    suspend fun syncPendingDrawings()
}

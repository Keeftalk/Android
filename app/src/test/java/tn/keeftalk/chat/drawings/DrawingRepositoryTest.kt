package tn.keeftalk.chat.drawings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.data.local.dao.DrawingDao
import tn.keeftalk.chat.data.local.entities.DrawingEntity
import tn.keeftalk.chat.data.local.entities.DrawingSyncQueueEntity
import tn.keeftalk.chat.domain.model.Drawing
import tn.keeftalk.chat.domain.model.DrawingExportFormat
import java.io.ByteArrayOutputStream

class DrawingRepositoryTest {

    private class FakeDrawingDao : DrawingDao {
        val drawings = mutableMapOf<String, DrawingEntity>()
        val syncQueue = mutableListOf<DrawingSyncQueueEntity>()

        override fun getAllDrawingsFlow(): Flow<List<DrawingEntity>> = flowOf(drawings.values.filter { !it.isDeleted })
        override suspend fun getAllDrawings(): List<DrawingEntity> = drawings.values.filter { !it.isDeleted }
        override suspend fun getDrawingById(id: String): DrawingEntity? = drawings[id]?.takeIf { !it.isDeleted }
        override fun getDrawingByIdFlow(id: String): Flow<DrawingEntity?> = flowOf(drawings[id]?.takeIf { !it.isDeleted })
        override suspend fun insertOrUpdateDrawing(drawing: DrawingEntity) {
            drawings[drawing.id] = drawing
        }
        override suspend fun insertOrUpdateDrawings(drawings: List<DrawingEntity>) {
            drawings.forEach { this.drawings[it.id] = it }
        }
        override suspend fun markDrawingDeleted(id: String, timestamp: Long) {
            drawings[id]?.let { drawings[id] = it.copy(isDeleted = true, updatedAt = timestamp) }
        }
        override suspend fun restoreDrawing(id: String, timestamp: Long) {
            drawings[id]?.let { drawings[id] = it.copy(isDeleted = false, updatedAt = timestamp) }
        }
        override suspend fun hardDeleteDrawing(id: String) {
            drawings.remove(id)
        }
        override suspend fun getPendingSyncItems(): List<DrawingSyncQueueEntity> = syncQueue.filter { it.status == "PENDING" }
        override suspend fun insertSyncItem(item: DrawingSyncQueueEntity) {
            syncQueue.add(item)
        }
        override suspend fun deleteSyncItem(id: String) {
            syncQueue.removeIf { it.id == id }
        }
    }

    @Test
    fun testFakeDrawingDaoCrudOperations() = runBlocking {
        val dao = FakeDrawingDao()
        val drawing = DrawingEntity(
            id = "d_1",
            title = "Test Painting",
            canvasWidth = 1080,
            canvasHeight = 1920
        )

        dao.insertOrUpdateDrawing(drawing)
        val retrieved = dao.getDrawingById("d_1")
        assertNotNull(retrieved)
        assertEquals("Test Painting", retrieved?.title)

        dao.markDrawingDeleted("d_1")
        val afterDelete = dao.getDrawingById("d_1")
        assertNull(afterDelete)

        val syncItem = DrawingSyncQueueEntity(id = "s_1", action = "UPSERT", payloadJson = "{}")
        dao.insertSyncItem(syncItem)
        assertEquals(1, dao.getPendingSyncItems().size)

        dao.deleteSyncItem("s_1")
        assertEquals(0, dao.getPendingSyncItems().size)
    }

    @Test
    fun testDrawingExportFormatting() = runBlocking {
        val drawing = Drawing(
            id = "d_export",
            title = "Export Test",
            canvasWidth = 500,
            canvasHeight = 500,
            backgroundColorHex = "#FFFFFF"
        )

        val jsonBytes = drawing.toJson().toByteArray(Charsets.UTF_8)
        assertTrue(jsonBytes.isNotEmpty())

        val jsonStr = String(jsonBytes, Charsets.UTF_8)
        assertTrue(jsonStr.contains("Export Test"))
    }
}

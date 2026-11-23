package tn.keeftalk.chat.drawings

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.DrawingEntity
import tn.keeftalk.chat.data.remote.DrawingSyncDto
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.drawings.engine.UndoRedoManager

class DrawingFormatTest {

    @Test
    fun testDrawingJsonSerializationAndDeserialization() {
        val stroke = DrawingElement.Stroke(
            layerId = "layer_1",
            toolType = DrawingToolType.PENCIL,
            points = listOf(DrawingPoint(10f, 20f), DrawingPoint(30f, 40f)),
            colorHex = "#FF0000",
            strokeWidth = 10f
        )

        val shape = DrawingElement.Shape(
            layerId = "layer_1",
            shapeType = DrawingToolType.RECTANGLE,
            startX = 50f,
            startY = 50f,
            endX = 200f,
            endY = 200f,
            colorHex = "#00FF00",
            strokeWidth = 4f,
            isFilled = true,
            fillColorHex = "#0000FF"
        )

        val textEl = DrawingElement.Text(
            layerId = "layer_1",
            x = 100f,
            y = 100f,
            text = "Keeftalk Paint",
            colorHex = "#000000",
            fontSizeSp = 28f
        )

        val layer = DrawingLayer(
            id = "layer_1",
            name = "Background Layer",
            isVisible = true,
            opacity = 0.9f,
            elements = listOf(stroke, shape, textEl)
        )

        val drawing = Drawing(
            id = "drawing_123",
            title = "Test Painting",
            canvasWidth = 1920,
            canvasHeight = 1080,
            backgroundColorHex = "#FFFFFF",
            layers = listOf(layer)
        )

        val json = drawing.toJson()
        assertNotNull(json)
        assertTrue(json.contains("Test Painting"))

        val deserialized = Drawing.fromJson(json)
        assertEquals("drawing_123", deserialized.id)
        assertEquals("Test Painting", deserialized.title)
        assertEquals(1920, deserialized.canvasWidth)
        assertEquals(1080, deserialized.canvasHeight)
        assertEquals(1, deserialized.layers.size)

        val restoredLayer = deserialized.layers.first()
        assertEquals("Background Layer", restoredLayer.name)
        assertEquals(3, restoredLayer.elements.size)

        val restoredStroke = restoredLayer.elements[0] as DrawingElement.Stroke
        assertEquals("#FF0000", restoredStroke.colorHex)
        assertEquals(2, restoredStroke.points.size)

        val restoredShape = restoredLayer.elements[1] as DrawingElement.Shape
        assertTrue(restoredShape.isFilled)
        assertEquals("#0000FF", restoredShape.fillColorHex)

        val restoredText = restoredLayer.elements[2] as DrawingElement.Text
        assertEquals("Keeftalk Paint", restoredText.text)
    }

    @Test
    fun testDrawingSyncDtoMapping() {
        val now = System.currentTimeMillis()
        val entity = DrawingEntity(
            id = "drawing_sync_1",
            ownerUserId = "local",
            title = "Landscape Sketch",
            nativeFilePath = "/drawings/drawing_sync_1.ktd",
            thumbnailPath = "/drawings/drawing_sync_1_thumb.png",
            canvasWidth = 1080,
            canvasHeight = 1920,
            backgroundColorHex = "#000000",
            createdAt = now,
            updatedAt = now
        )

        val userId = "user_uuid_100"
        val dto = DrawingSyncDto.fromEntity(entity, userId, contentJson = "{}", thumbnailUrl = "https://example.com/thumb.png")

        assertEquals("drawing_sync_1", dto.id)
        assertEquals(userId, dto.userId)
        assertEquals("Landscape Sketch", dto.title)
        assertEquals("https://example.com/thumb.png", dto.thumbnailUrl)

        val mappedEntity = dto.toEntity(localNativeFilePath = entity.nativeFilePath, localThumbnailPath = entity.thumbnailPath)
        assertEquals("drawing_sync_1", mappedEntity.id)
        assertEquals("Landscape Sketch", mappedEntity.title)
        assertEquals("/drawings/drawing_sync_1.ktd", mappedEntity.nativeFilePath)
    }

    @Test
    fun testUndoRedoManagerStackOperations() {
        val undoManager = UndoRedoManager(maxHistorySize = 5)
        val layer1 = listOf(DrawingLayer(name = "L1"))
        val layer2 = listOf(DrawingLayer(name = "L1"), DrawingLayer(name = "L2"))
        val layer3 = listOf(DrawingLayer(name = "L1"), DrawingLayer(name = "L2"), DrawingLayer(name = "L3"))

        undoManager.clear(layer1)
        assertFalse(undoManager.canUndo)
        assertFalse(undoManager.canRedo)

        undoManager.pushState(layer2)
        assertTrue(undoManager.canUndo)
        assertFalse(undoManager.canRedo)

        undoManager.pushState(layer3)
        val undone1 = undoManager.undo()
        assertNotNull(undone1)
        assertEquals(2, undone1?.size)

        val undone2 = undoManager.undo()
        assertNotNull(undone2)
        assertEquals(1, undone2?.size)

        assertFalse(undoManager.canUndo)
        assertTrue(undoManager.canRedo)

        val redone1 = undoManager.redo()
        assertNotNull(redone1)
        assertEquals(2, redone1?.size)
    }
}

package tn.keeftalk.chat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val drawingJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

@Serializable
enum class DrawingToolType {
    PENCIL,
    PEN,
    BRUSH,
    MARKER,
    HIGHLIGHTER,
    AIRBRUSH,
    ERASER,
    LINE,
    RECTANGLE,
    FILLED_RECTANGLE,
    CIRCLE,
    ELLIPSE,
    FILLED_CIRCLE,
    POLYGON,
    TEXT,
    EYEDROPPER,
    FILL_BUCKET,
    IMAGE_INSERT
}

@Serializable
enum class DrawingExportFormat {
    PNG,
    JPEG,
    SVG,
    KTD
}

@Serializable
data class DrawingPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

@Serializable
sealed class DrawingElement {
    abstract val id: String
    abstract val layerId: String

    @Serializable
    @SerialName("stroke")
    data class Stroke(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        val toolType: DrawingToolType = DrawingToolType.PENCIL,
        val points: List<DrawingPoint> = emptyList(),
        val colorHex: String = "#000000",
        val strokeWidth: Float = 5.0f,
        val opacity: Float = 1.0f,
        val hardness: Float = 1.0f
    ) : DrawingElement()

    @Serializable
    @SerialName("shape")
    data class Shape(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        val shapeType: DrawingToolType = DrawingToolType.RECTANGLE,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val colorHex: String = "#000000",
        val strokeWidth: Float = 5.0f,
        val isFilled: Boolean = false,
        val fillColorHex: String = "#00000000",
        val polygonSides: Int = 5
    ) : DrawingElement()

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        val x: Float,
        val y: Float,
        val text: String,
        val colorHex: String = "#000000",
        val fontSizeSp: Float = 24.0f
    ) : DrawingElement()

    @Serializable
    @SerialName("image")
    data class ImportedImage(
        override val id: String = UUID.randomUUID().toString(),
        override val layerId: String,
        val imagePath: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val aspectRatio: Float = 1.0f
    ) : DrawingElement()
}

@Serializable
data class DrawingLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer 1",
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val elements: List<DrawingElement> = emptyList()
)

@Serializable
data class CanvasSettings(
    val width: Int = 1080,
    val height: Int = 1920,
    val backgroundColorHex: String = "#FFFFFF",
    val isTransparent: Boolean = false
)

@Serializable
data class Drawing(
    val id: String = UUID.randomUUID().toString(),
    val ownerUserId: String = "local",
    val title: String = "Untitled Drawing",
    val nativeFilePath: String = "",
    val thumbnailPath: String = "",
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1920,
    val backgroundColorHex: String = "#FFFFFF",
    val formatVersion: Int = 1,
    val layers: List<DrawingLayer> = listOf(DrawingLayer(name = "Layer 1")),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = "PENDING",
    val revision: Long = 1L,
    val contentHash: String? = null
) {
    fun toJson(): String {
        return drawingJson.encodeToString(this)
    }

    companion object {
        fun fromJson(json: String): Drawing {
            return drawingJson.decodeFromString(json)
        }
    }
}

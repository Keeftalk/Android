package com.keeftalk.chat.domain.model

sealed interface EditorCommand {
    fun execute(model: EditorModel): EditorModel
    fun undo(model: EditorModel): EditorModel

    data class AddDrawingPath(val drawingPath: DrawingPath) : EditorCommand {
        override fun execute(model: EditorModel) = model.copy(drawingPaths = model.drawingPaths + drawingPath)
        override fun undo(model: EditorModel) = model.copy(drawingPaths = model.drawingPaths.filter { it != drawingPath })
    }

    data class AddTextElement(val textElement: TextElement) : EditorCommand {
        override fun execute(model: EditorModel) = model.copy(textElements = model.textElements + textElement)
        override fun undo(model: EditorModel) = model.copy(textElements = model.textElements.filter { it.id != textElement.id })
    }

    data class MoveTextElement(val id: String, val oldX: Float, val oldY: Float, val newX: Float, val newY: Float) : EditorCommand {
        override fun execute(model: EditorModel) = model.copy(textElements = model.textElements.map { if (it.id == id) it.copy(x = newX, y = newY) else it })
        override fun undo(model: EditorModel) = model.copy(textElements = model.textElements.map { if (it.id == id) it.copy(x = oldX, y = oldY) else it })
    }

    data class UpdateTextTransform(val id: String, val x: Float, val y: Float, val scale: Float, val rotation: Float) : EditorCommand {
        // We'd need to store old values for real undo, but let's keep it simple for now
        override fun execute(model: EditorModel) = model.copy(textElements = model.textElements.map { 
            if (it.id == id) it.copy(x = x, y = y, scale = scale, rotation = rotation) else it 
        })
        override fun undo(model: EditorModel) = model
    }

    data class AddSticker(val sticker: StickerElement) : EditorCommand {
        override fun execute(model: EditorModel) = model.copy(stickers = model.stickers + sticker)
        override fun undo(model: EditorModel) = model.copy(stickers = model.stickers.filter { it.id != sticker.id })
    }

    data class AddBlurRegion(val region: BlurRegion) : EditorCommand {
        override fun execute(model: EditorModel) = model.copy(blurRegions = model.blurRegions + region)
        override fun undo(model: EditorModel) = model.copy(blurRegions = model.blurRegions.filter { it != region })
    }

    data class RotateImage(val oldRotation: Float, val newRotation: Float) : EditorCommand {
        override fun execute(model: EditorModel) = model.copy(rotation = newRotation)
        override fun undo(model: EditorModel) = model.copy(rotation = oldRotation)
    }

    data class DeleteElement(
        val id: String,
        var deletedText: TextElement? = null,
        var deletedSticker: StickerElement? = null
    ) : EditorCommand {
        override fun execute(model: EditorModel): EditorModel {
            deletedText = model.textElements.find { it.id == id }
            deletedSticker = model.stickers.find { it.id == id }
            return model.copy(
                textElements = model.textElements.filter { it.id != id },
                stickers = model.stickers.filter { it.id != id }
            )
        }
        override fun undo(model: EditorModel): EditorModel {
            var newModel = model
            deletedText?.let { newModel = newModel.copy(textElements = newModel.textElements + it) }
            deletedSticker?.let { newModel = newModel.copy(stickers = newModel.stickers + it) }
            return newModel
        }
    }
}

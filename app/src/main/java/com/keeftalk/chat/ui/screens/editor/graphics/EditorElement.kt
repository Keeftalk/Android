package com.keeftalk.chat.ui.screens.editor.graphics

import android.graphics.Matrix
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import java.util.*

class EditorElement(
    val renderer: Renderer,
    val zIndex: Int = 0,
    val id: UUID = UUID.randomUUID()
) {
    val localMatrix = Matrix()
    val children = mutableListOf<EditorElement>()
    var parent: EditorElement? = null

    fun addChild(element: EditorElement) {
        element.parent = this
        children.add(element)
        children.sortBy { it.zIndex }
    }

    fun removeChild(element: EditorElement) {
        children.remove(element)
    }

    fun draw(canvas: Canvas) {
        canvas.save()
        canvas.nativeCanvas.concat(localMatrix)
        
        renderer.draw(canvas)
        
        children.forEach { it.draw(canvas) }
        
        canvas.restore()
    }

    fun forAllInTree(action: (EditorElement) -> Unit) {
        action(this)
        children.forEach { it.forAllInTree(action) }
    }

    fun findElementWithId(uuid: UUID): EditorElement? {
        if (id == uuid) return this
        children.forEach {
            val found = it.findElementWithId(uuid)
            if (found != null) return found
        }
        return null
    }
}

interface Renderer {
    fun draw(canvas: Canvas)
}

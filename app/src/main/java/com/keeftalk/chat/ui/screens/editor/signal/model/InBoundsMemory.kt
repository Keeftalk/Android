package com.keeftalk.chat.ui.screens.editor.signal.model

import android.graphics.Matrix

/**
 * Ported from Signal Android (AGPL-3.0)
 */
internal class InBoundsMemory {
    private val lastGoodUserCrop = Matrix()
    private val lastGoodMainImage = Matrix()

    fun push(mainImage: EditorElement?, userCrop: EditorElement) {
        if (mainImage == null) {
            lastGoodMainImage.reset()
        } else {
            lastGoodMainImage.set(mainImage.localMatrix)
            lastGoodMainImage.preConcat(mainImage.editorMatrix)
        }
        lastGoodUserCrop.set(userCrop.localMatrix)
        lastGoodUserCrop.preConcat(userCrop.editorMatrix)
    }

    fun restore(mainImage: EditorElement?, cropEditorElement: EditorElement, invalidate: Runnable?) {
        mainImage?.animateLocalTo(lastGoodMainImage, invalidate)
        cropEditorElement.animateLocalTo(lastGoodUserCrop, invalidate)
    }

    fun getLastKnownGoodMainImageMatrix(): Matrix = Matrix(lastGoodMainImage)
}

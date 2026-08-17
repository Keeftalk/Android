package com.keeftalk.chat.ui.screens.editor.signal.model

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.keeftalk.chat.R
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import com.keeftalk.chat.ui.screens.editor.signal.renderers.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class EditorElementHierarchy private constructor(val root: EditorElement) {

    val view: EditorElement = root.getChild(0)
    val flipRotate: EditorElement = view.getChild(0)
    val imageRoot: EditorElement = flipRotate.getChild(0)
    val overlay: EditorElement = flipRotate.getChild(1)
    val imageCrop: EditorElement = overlay.getChild(0)
    val selection: EditorElement = overlay.getChild(1)
    val cropEditorElement: EditorElement = imageCrop.getChild(0)
    val blackout: EditorElement = cropEditorElement.getChild(0)
    val thumbs: EditorElement = cropEditorElement.getChild(1)
    val fade: EditorElement = cropEditorElement.getChild(2)
    val trash: EditorElement = cropEditorElement.getChild(3)

    var selectedElement: EditorElement? = null
        private set

    enum class CropStyle {
        RECTANGLE, CIRCLE, PINCH_AND_PAN
    }

    fun removeAllSelectionArtifacts() {
        selection.deleteAllChildren()
        selectedElement = null
    }

    fun updateSelectionThumbsForElement(element: EditorElement, overlayMappingMatrix: Matrix?) {
        if (element === selectedElement) {
            setOrUpdateSelectionThumbsForElement(element, overlayMappingMatrix)
        }
    }

    fun setOrUpdateSelectionThumbsForElement(element: EditorElement, overlayMappingMatrix: Matrix?) {
        if (selectedElement !== element) {
            removeAllSelectionArtifacts()
            selectedElement = element
            
            if (selectedElement == null) return

            selection.addElement(createSelectionBox())
            selection.addElement(createScaleControlThumb(element))
            selection.addElement(createRotateControlThumb(element))
        }

        if (overlayMappingMatrix != null) {
            val selectionMatrix = selection.localMatrix
            selectionMatrix.setRectToRect(Bounds.FULL_BOUNDS, Bounds.FULL_BOUNDS, Matrix.ScaleToFit.FILL)
            selectionMatrix.postConcat(overlayMappingMatrix)
        }
    }

    private fun createSelectionBox(): EditorElement = EditorElement(SelectedElementGuideRenderer())

    private fun createScaleControlThumb(element: EditorElement): EditorElement {
        val controlPoint = ThumbRenderer.ControlPoint.SCALE_ROT_RIGHT
        val thumbElement = EditorElement(CropThumbRenderer(controlPoint, element.id))
        thumbElement.localMatrix.preTranslate(controlPoint.x, controlPoint.y)
        return thumbElement
    }

    private fun createRotateControlThumb(element: EditorElement): EditorElement {
        val controlPoint = ThumbRenderer.ControlPoint.SCALE_ROT_LEFT
        val rotateThumbElement = EditorElement(CropThumbRenderer(controlPoint, element.id))
        rotateThumbElement.localMatrix.preTranslate(controlPoint.x, controlPoint.y)
        return rotateThumbElement
    }

    fun addFade(invalidate: Runnable) {
        fade.flags.setVisible(true).persist()
        invalidate.run()
    }

    fun removeFade(invalidate: Runnable) {
        fade.flags.setVisible(false).persist()
        invalidate.run()
    }

    fun startCrop(invalidate: Runnable, scaleIn: Float) {
        val editor = Matrix()
        editor.postScale(scaleIn, scaleIn)
        root.animateEditorTo(editor, invalidate)

        cropEditorElement.flags.setVisible(true)
        blackout.flags.setVisible(false)
        thumbs.flags.setChildrenVisible(true)
        thumbs.forAllInTree { it.flags.setSelectable(true) }
        imageRoot.forAllInTree { it.flags.setSelectable(false) }

        getMainImage()?.flags?.setSelectable(true)
        invalidate.run()
    }

    fun doneCrop(visibleViewPort: RectF, invalidate: Runnable?) {
        updateViewToCrop(visibleViewPort, invalidate)
        root.rollbackEditorMatrix(invalidate)
        root.forAllInTree { it.flags.reset() }
    }

    fun updateViewToCrop(visibleViewPort: RectF, invalidate: Runnable?) {
        val dst = RectF()
        getCropFinalMatrix().mapRect(dst, Bounds.FULL_BOUNDS)
        val temp = Matrix()
        temp.setRectToRect(dst, visibleViewPort, Matrix.ScaleToFit.CENTER)
        view.animateLocalTo(temp, invalidate)
    }

    private fun getCropFinalMatrix(): Matrix {
        val matrix = Matrix(flipRotate.localMatrix)
        matrix.preConcat(imageCrop.localMatrix)
        matrix.preConcat(cropEditorElement.localMatrix)
        return matrix
    }

    fun imageMatrixRelativeToCrop(): Matrix? {
        val mainImage = getMainImage() ?: return null
        val matrix1 = Matrix(imageCrop.localMatrix)
        matrix1.preConcat(cropEditorElement.localMatrix)
        matrix1.preConcat(cropEditorElement.editorMatrix)

        val matrix2 = Matrix(mainImage.localMatrix)
        matrix2.preConcat(mainImage.editorMatrix)
        matrix2.preConcat(imageCrop.localMatrix)

        val inverse = Matrix()
        return if (matrix2.invert(inverse)) {
            inverse.preConcat(matrix1)
            inverse
        } else null
    }

    fun dragDropRelease(visibleViewPort: RectF, invalidate: Runnable) {
        if (cropEditorElement.flags.isVisible()) {
            updateViewToCrop(visibleViewPort, invalidate)
        }
    }

    fun getCropRect(): RectF {
        val dst = RectF()
        getCropFinalMatrix().mapRect(dst, Bounds.FULL_BOUNDS)
        return dst
    }

    fun flipRotate(degrees: Float, scaleX: Int, scaleY: Int, visibleViewPort: RectF, invalidate: Runnable?) {
        val newLocal = Matrix(flipRotate.localMatrix)
        if (degrees != 0f) newLocal.postRotate(degrees)
        newLocal.postScale(scaleX.toFloat(), scaleY.toFloat())
        flipRotate.animateLocalTo(newLocal, invalidate)
        updateViewToCrop(visibleViewPort, invalidate)
    }

    fun getMainImage(): EditorElement? = if (imageRoot.getChildCount() > 0) imageRoot.getChild(0) else null

    fun getMainImageFullMatrix(): Matrix {
        val matrix = Matrix()
        matrix.preConcat(view.localMatrix)
        matrix.preConcat(getMainImageFullMatrixFromFlipRotate())
        return matrix
    }

    fun getMainImageFullMatrixFromFlipRotate(): Matrix {
        val matrix = Matrix()
        matrix.preConcat(flipRotate.localMatrix)
        matrix.preConcat(imageRoot.localMatrix)
        getMainImage()?.let { matrix.preConcat(it.localMatrix) }
        return matrix
    }

    fun getOutputSize(inputSize: Point): PointF {
        val matrix = Matrix()
        matrix.preConcat(flipRotate.localMatrix)
        matrix.preConcat(cropEditorElement.localMatrix)
        matrix.preConcat(cropEditorElement.editorMatrix)
        getMainImage()?.let {
            val xScale = 1f / (xScale(it.localMatrix) * xScale(it.editorMatrix))
            matrix.preScale(xScale, xScale)
        }
        val dst = FloatArray(4)
        matrix.mapPoints(dst, floatArrayOf(0f, 0f, inputSize.x.toFloat(), inputSize.y.toFloat()))
        return PointF(Math.abs(dst[0] - dst[2]), Math.abs(dst[1] - dst[3]))
    }

    companion object {
        fun create(@ColorInt blackoutColor: Int): EditorElementHierarchy = EditorElementHierarchy(createRoot(CropStyle.RECTANGLE, blackoutColor))
        fun createForCircleEditing(@ColorInt blackoutColor: Int): EditorElementHierarchy = EditorElementHierarchy(createRoot(CropStyle.CIRCLE, blackoutColor))
        fun createForPinchAndPanCropping(@ColorInt blackoutColor: Int): EditorElementHierarchy = EditorElementHierarchy(createRoot(CropStyle.PINCH_AND_PAN, blackoutColor))
        fun create(root: EditorElement): EditorElementHierarchy = EditorElementHierarchy(root)

        private fun createRoot(cropStyle: CropStyle, @ColorInt blackoutColor: Int): EditorElement {
            val root = EditorElement(null)
            val view = EditorElement(null)
            root.addElement(view)
            
            val flipRotate = EditorElement(null)
            view.addElement(flipRotate)

            val imageRoot = EditorElement(null)
            flipRotate.addElement(imageRoot)

            val overlay = EditorElement(null)
            flipRotate.addElement(overlay)

            val imageCrop = EditorElement(null)
            overlay.addElement(imageCrop)

            val selection = EditorElement(null)
            overlay.addElement(selection)

            val renderCenterThumbs = cropStyle == CropStyle.RECTANGLE
            val cropEditorElement = EditorElement(CropAreaRenderer(ColorUtils.setAlphaComponent(blackoutColor, 0x7F), renderCenterThumbs))
            cropEditorElement.flags.setRotateLocked(true).setAspectLocked(true).setSelectable(false).setVisible(false).persist()
            imageCrop.addElement(cropEditorElement)

            val blackout = EditorElement(InverseFillRenderer(ColorUtils.setAlphaComponent(blackoutColor, 0xFF)))
            blackout.flags.setSelectable(false).setEditable(false).persist()
            cropEditorElement.addElement(blackout)

            val thumbs = if (cropStyle == CropStyle.PINCH_AND_PAN) EditorElement(null) else createThumbs(cropEditorElement, renderCenterThumbs)
            cropEditorElement.addElement(thumbs)

            val fade = EditorElement(FillRenderer(ColorUtils.setAlphaComponent(blackoutColor, 0x66)), 1) // Z_FADE
            fade.flags.setSelectable(false).setEditable(false).setVisible(false).persist()
            cropEditorElement.addElement(fade)

            val trash = EditorElement(TrashRenderer(), 3) // Z_TRASH
            trash.flags.setSelectable(false).setEditable(false).setVisible(false).persist()
            cropEditorElement.addElement(trash)
            
            if (cropStyle == CropStyle.CIRCLE) {
                val circle = EditorElement(OvalGuideRenderer(R.color.crop_circle_guide_color), 4) // Z_CIRCLE
                circle.flags.setSelectable(false).persist()
                cropEditorElement.addElement(circle)
            }

            return root
        }

        private fun createThumbs(cropEditorElement: EditorElement, centerThumbs: Boolean): EditorElement {
            val thumbs = EditorElement(null)
            thumbs.flags.setChildrenVisible(false).setSelectable(false).setVisible(false).persist()
            
            if (centerThumbs) {
                thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.CENTER_LEFT))
                thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.CENTER_RIGHT))
                thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.TOP_CENTER))
                thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.BOTTOM_CENTER))
            }
            thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.TOP_LEFT))
            thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.TOP_RIGHT))
            thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.BOTTOM_LEFT))
            thumbs.addElement(newThumb(cropEditorElement, ThumbRenderer.ControlPoint.BOTTOM_RIGHT))
            return thumbs
        }

        private fun newThumb(toControl: EditorElement, controlPoint: ThumbRenderer.ControlPoint): EditorElement {
            val element = EditorElement(CropThumbRenderer(controlPoint, toControl.id))
            element.flags.setSelectable(false).persist()
            element.localMatrix.preTranslate(controlPoint.x, controlPoint.y)
            return element
        }

        fun xScale(matrix: Matrix): Float {
            val values = FloatArray(9)
            matrix.getValues(values)
            return Math.sqrt((values[0] * values[0] + values[3] * values[3]).toDouble()).toFloat()
        }
    }
}

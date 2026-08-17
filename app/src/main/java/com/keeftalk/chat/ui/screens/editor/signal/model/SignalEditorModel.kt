package com.keeftalk.chat.ui.screens.editor.signal.model

import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import com.keeftalk.chat.ui.screens.editor.signal.*
import com.keeftalk.chat.ui.screens.editor.signal.renderers.FaceBlurRenderer
import com.keeftalk.chat.ui.screens.editor.signal.renderers.MultiLineTextRenderer
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class SignalEditorModel : Parcelable, RendererContext.Ready {

    private var invalidate: Runnable = NULL_RUNNABLE
    private var undoRedoStackListener: UndoRedoStackListener? = null

    private val undoRedoStacks: UndoRedoStacks
    private val cropUndoRedoStacks: UndoRedoStacks
    private val inBoundsMemory = InBoundsMemory()

    private var editorElementHierarchy: EditorElementHierarchy

    val root: EditorElement get() = editorElementHierarchy.root

    fun getMainImage(): EditorElement? = editorElementHierarchy.getMainImage()
    fun getSelectedElement(): EditorElement? = editorElementHierarchy.selectedElement

    private val visibleViewPort = RectF()
    private val size: Point
    private val editingPurpose: EditingPurpose
    private var fixedRatio: Float

    private enum class EditingPurpose {
        IMAGE, AVATAR_CAPTURE, AVATAR_EDIT, WALLPAPER
    }

    constructor(@ColorInt blackoutColor: Int) : this(EditingPurpose.IMAGE, 0f, EditorElementHierarchy.create(blackoutColor))

    private constructor(editingPurpose: EditingPurpose, fixedRatio: Float, editorElementHierarchy: EditorElementHierarchy) {
        this.editingPurpose = editingPurpose
        this.fixedRatio = fixedRatio
        this.size = Point(1024, 1024)
        this.editorElementHierarchy = editorElementHierarchy
        this.undoRedoStacks = UndoRedoStacks(50)
        this.cropUndoRedoStacks = UndoRedoStacks(50)
    }

    private constructor(`in`: Parcel) {
        val classLoader = javaClass.classLoader
        this.editingPurpose = EditingPurpose.values()[`in`.readInt()]
        this.fixedRatio = `in`.readFloat()
        this.size = Point(`in`.readInt(), `in`.readInt())
        this.editorElementHierarchy = EditorElementHierarchy.create(`in`.readParcelable<EditorElement>(classLoader)!!)
        this.undoRedoStacks = `in`.readParcelable<UndoRedoStacks>(classLoader)!!
        this.cropUndoRedoStacks = `in`.readParcelable<UndoRedoStacks>(classLoader)!!
    }

    fun setInvalidate(invalidate: Runnable?) {
        this.invalidate = invalidate ?: NULL_RUNNABLE
    }

    fun setUndoRedoStackListener(undoRedoStackListener: UndoRedoStackListener?) {
        this.undoRedoStackListener = undoRedoStackListener
        updateUndoRedoAvailableState(getActiveUndoRedoStacks(isCropping()))
    }

    fun setSelected(editorElement: EditorElement?) {
        if (editorElement == null) {
            editorElementHierarchy.removeAllSelectionArtifacts()
        } else {
            val overlayMappingMatrix = findRelativeMatrix(editorElement, editorElementHierarchy.overlay)
            editorElementHierarchy.setOrUpdateSelectionThumbsForElement(editorElement, overlayMappingMatrix)
        }
    }

    fun updateSelectionThumbsIfSelected(editorElement: EditorElement) {
        val overlayMappingMatrix = findRelativeMatrix(editorElement, editorElementHierarchy.overlay)
        editorElementHierarchy.updateSelectionThumbsForElement(editorElement, overlayMappingMatrix)
    }

    fun setSelectionVisible(visible: Boolean) {
        editorElementHierarchy.selection.flags
            .setVisible(visible)
            .setChildrenVisible(visible)
            .persist()
    }

    fun setMainImageEditorMatrixRotation(angle: Float, minScaleDown: Float) {
        setEditorMatrixToRotationMatrixAboutParentsOrigin(editorElementHierarchy.getMainImage()!!, angle)
        scaleMainImageEditorMatrixToFitInsideCropBounds(minScaleDown, 2f)
        invalidate.run()
    }

    private fun scaleMainImageEditorMatrixToFitInsideCropBounds(minScaleDown: Float, maxScaleUp: Float) {
        val mainImage = editorElementHierarchy.getMainImage() ?: return
        val mainImageLocalBackup = Matrix(mainImage.localMatrix)
        val mainImageEditorBackup = Matrix(mainImage.editorMatrix)

        mainImage.commitEditorMatrix()
        val combinedLocal = Matrix(mainImage.localMatrix)
        val newLocal = Bisect.bisectToTest(
            mainImage,
            minScaleDown,
            maxScaleUp,
            { cropIsWithinMainImageBounds() },
            { matrix, scale -> matrix.preScale(scale, scale) }
        )

        val invertLocal = Matrix()
        if (newLocal != null && combinedLocal.invert(invertLocal)) {
            invertLocal.preConcat(newLocal)
            mainImageEditorBackup.preConcat(invertLocal)
        }
        mainImage.localMatrix.set(mainImageLocalBackup)
        mainImage.editorMatrix.set(mainImageEditorBackup)
    }

    private fun setEditorMatrixToRotationMatrixAboutParentsOrigin(element: EditorElement, degrees: Float) {
        val localMatrix = element.localMatrix
        val editorMatrix = element.editorMatrix
        localMatrix.invert(editorMatrix)
        editorMatrix.preRotate(degrees)
        editorMatrix.preConcat(localMatrix)
    }

    fun draw(rendererContext: RendererContext, renderOnTop: EditorElement?) {
        val root = editorElementHierarchy.root
        if (renderOnTop != null) {
            root.forAllInTree { it.flags.mark() }
            renderOnTop.flags.setVisible(false)
        }

        root.draw(rendererContext)

        if (renderOnTop != null) {
            try {
                root.forAllInTree { it.flags.setVisible(renderOnTop === it) }
                root.draw(rendererContext)
            } finally {
                root.forAllInTree { it.flags.restore() }
            }
        }
    }

    fun findElementInverseMatrix(element: EditorElement, viewMatrix: Matrix): Matrix? {
        val inverse = Matrix()
        return if (findElement(element, viewMatrix, inverse)) inverse else null
    }

    private fun findElementMatrix(element: EditorElement, viewMatrix: Matrix): Matrix? {
        val inverse = findElementInverseMatrix(element, viewMatrix)
        if (inverse != null) {
            val regular = Matrix()
            if (inverse.invert(regular)) return regular
        }
        return null
    }

    fun findElementAtPoint(point: PointF, viewMatrix: Matrix, outInverseModelMatrix: Matrix): EditorElement? {
        return editorElementHierarchy.root.findElementAt(point.x, point.y, viewMatrix, outInverseModelMatrix)
    }

    fun checkTrashIntersectsPoint(point: PointF): Boolean {
        val trash = editorElementHierarchy.trash
        if (trash.flags.isVisible()) {
            trash.flags.setSelectable(true).persist()
            val isIntersecting = trash.findElementAt(point.x, point.y, Matrix(), Matrix()) != null
            trash.flags.setSelectable(false).persist()
            return isIntersecting
        }
        return false
    }

    private fun findElement(element: EditorElement, viewMatrix: Matrix, outInverseModelMatrix: Matrix): Boolean {
        return editorElementHierarchy.root.findElement(element, viewMatrix, outInverseModelMatrix) === element
    }

    fun createSnapshot(): ByteArray = ElementStack.getBytes(editorElementHierarchy.root)

    fun restoreFromSnapshot(snapshot: ByteArray) {
        val oldRootElement = editorElementHierarchy.root
        val parcel = Parcel.obtain()
        try {
            parcel.unmarshall(snapshot, 0, snapshot.size)
            parcel.setDataPosition(0)
            val newRoot = parcel.readParcelable<EditorElement>(EditorElement::class.java.classLoader)
            if (newRoot != null) {
                editorElementHierarchy = EditorElementHierarchy.create(newRoot)
                restoreStateWithAnimations(oldRootElement, editorElementHierarchy.root, invalidate, false)
                invalidate.run()
                editorElementHierarchy.updateViewToCrop(visibleViewPort, invalidate)
                inBoundsMemory.push(editorElementHierarchy.getMainImage(), editorElementHierarchy.cropEditorElement)
            }
        } finally {
            parcel.recycle()
        }
    }

    fun pushUndoPoint() {
        val cropping = isCropping()
        if (cropping && !currentCropIsAcceptable()) return
        getActiveUndoRedoStacks(cropping).pushState(editorElementHierarchy.root)
    }

    fun updateUndoRedoAvailabilityState() {
        updateUndoRedoAvailableState(getActiveUndoRedoStacks(isCropping()))
    }

    fun clearUndoStack() {
        var root = editorElementHierarchy.root
        val original = root
        val cropping = isCropping()
        val stacks = getActiveUndoRedoStacks(cropping)
        var didPop = false

        while (stacks.canUndo(root)) {
            val oldRootElement = root
            val popped = stacks.undoStack.pop(oldRootElement) ?: break
            didPop = true
            editorElementHierarchy = EditorElementHierarchy.create(popped)
            stacks.redoStack.tryPush(oldRootElement)
            root = editorElementHierarchy.root
        }

        if (didPop) {
            restoreStateWithAnimations(original, editorElementHierarchy.root, invalidate, cropping)
            invalidate.run()
            editorElementHierarchy.updateViewToCrop(visibleViewPort, invalidate)
            inBoundsMemory.push(editorElementHierarchy.getMainImage(), editorElementHierarchy.cropEditorElement)
        }
        updateUndoRedoAvailableState(stacks)
    }

    fun undo() {
        val cropping = isCropping()
        val stacks = getActiveUndoRedoStacks(cropping)
        undoRedo(stacks.undoStack, stacks.redoStack, cropping)
        updateUndoRedoAvailableState(stacks)
    }

    fun redo() {
        val cropping = isCropping()
        val stacks = getActiveUndoRedoStacks(cropping)
        undoRedo(stacks.redoStack, stacks.undoStack, cropping)
        updateUndoRedoAvailableState(stacks)
    }

    private fun undoRedo(fromStack: ElementStack, toStack: ElementStack, keepEditorState: Boolean) {
        val oldRootElement = editorElementHierarchy.root
        val popped = fromStack.pop(oldRootElement) ?: return

        setEditorElementHierarchy(EditorElementHierarchy.create(popped))
        toStack.tryPush(oldRootElement)

        restoreStateWithAnimations(oldRootElement, editorElementHierarchy.root, invalidate, keepEditorState)
        invalidate.run()
        editorElementHierarchy.updateViewToCrop(visibleViewPort, invalidate)
        inBoundsMemory.push(editorElementHierarchy.getMainImage(), editorElementHierarchy.cropEditorElement)
    }

    private fun setEditorElementHierarchy(hierarchy: EditorElementHierarchy) {
        val selectedElement = editorElementHierarchy.selectedElement
        editorElementHierarchy = hierarchy
        setSelected(if (selectedElement != null) findById(selectedElement.id) else null)
    }

    private fun restoreStateWithAnimations(fromRootElement: EditorElement, toRootElement: EditorElement, onInvalidate: Runnable, keepEditorState: Boolean) {
        val fromMap = mutableMapOf<UUID, EditorElement>()
        val toMap = mutableMapOf<UUID, EditorElement>()
        fromRootElement.buildMap(fromMap)
        toRootElement.buildMap(toMap)

        for (fromElement in fromMap.values) {
            fromElement.stopAnimation()
            val toElement = toMap[fromElement.id]
            if (toElement != null) {
                toElement.animateFrom(fromElement.getLocalMatrixAnimating(), onInvalidate)
                if (keepEditorState) {
                    toElement.editorMatrix.set(fromElement.editorMatrix)
                    toElement.flags.set(fromElement.flags)
                }
            } else {
                val parentFrom = fromRootElement.parentOf(fromElement)
                if (parentFrom != null) {
                    val toParent = toMap[parentFrom.id]
                    toParent?.addDeletedChildFadingOut(fromElement, onInvalidate)
                }
            }
        }

        for (toElement in toMap.values) {
            if (!fromMap.containsKey(toElement.id)) {
                toElement.animateFadeIn(onInvalidate)
            }
        }
    }

    private fun updateUndoRedoAvailableState(currentStack: UndoRedoStacks) {
        undoRedoStackListener?.onAvailabilityChanged(currentStack.canUndo(editorElementHierarchy.root), currentStack.canRedo(editorElementHierarchy.root))
    }

    fun addFade() = editorElementHierarchy.addFade(invalidate)
    fun removeFade() = editorElementHierarchy.removeFade(invalidate)

    fun startCrop() {
        val scaleIn = if (editingPurpose == EditingPurpose.WALLPAPER) 1f else 0.8f
        pushUndoPoint()
        cropUndoRedoStacks.clear(editorElementHierarchy.root)
        editorElementHierarchy.startCrop(invalidate, scaleIn)
        inBoundsMemory.push(editorElementHierarchy.getMainImage(), editorElementHierarchy.cropEditorElement)
        updateUndoRedoAvailableState(cropUndoRedoStacks)
    }

    fun doneCrop() {
        editorElementHierarchy.doneCrop(visibleViewPort, invalidate)
        updateUndoRedoAvailableState(undoRedoStacks)
    }

    fun setCropAspectLock(locked: Boolean) {
        val flags = editorElementHierarchy.cropEditorElement.flags
        val currentState = flags.setAspectLocked(locked).getCurrentState()
        flags.reset()
        flags.setAspectLocked(locked).persist()
        flags.restoreState(currentState)
    }

    fun isCropAspectLocked(): Boolean = editorElementHierarchy.cropEditorElement.flags.isAspectLocked()

    fun postEdit(allowScaleToRepairCrop: Boolean) {
        val cropping = isCropping()
        if (cropping) ensureFitsBounds(allowScaleToRepairCrop)
        updateUndoRedoAvailableState(getActiveUndoRedoStacks(cropping))
        invalidate.run()
    }

    private fun getActiveUndoRedoStacks(cropping: Boolean): UndoRedoStacks = if (cropping) cropUndoRedoStacks else undoRedoStacks

    private fun ensureFitsBounds(allowScaleToRepairCrop: Boolean) {
        val mainImage = editorElementHierarchy.getMainImage() ?: return
        val cropEditorElement = editorElementHierarchy.cropEditorElement

        if (!currentCropIsAcceptable()) {
            if (allowScaleToRepairCrop) {
                if (!tryToScaleToFit(cropEditorElement, 0.9f)) {
                    tryToScaleToFit(mainImage, 2f)
                }
            } else {
                tryToFixTranslationOutOfBounds(mainImage, inBoundsMemory.getLastKnownGoodMainImageMatrix())
            }

            if (!currentCropIsAcceptable()) {
                inBoundsMemory.restore(mainImage, cropEditorElement, invalidate)
            } else {
                inBoundsMemory.push(mainImage, cropEditorElement)
            }
        }
        editorElementHierarchy.dragDropRelease(visibleViewPort, invalidate)
    }

    private fun tryToScaleToFit(element: EditorElement, scaleAtMost: Float): Boolean {
        return Bisect.bisectToTest(
            element,
            1f,
            scaleAtMost,
            { cropIsWithinMainImageBounds() },
            { matrix, scale -> matrix.preScale(scale, scale) },
            invalidate
        )
    }

    private fun tryToTranslateToFit(element: EditorElement, translateXAtMost: Float, translateYAtMost: Float): Matrix? {
        return Bisect.bisectToTest(
            element,
            0f,
            1f,
            { cropIsWithinMainImageBounds() }
        ) { matrix, factor -> matrix.postTranslate(factor * translateXAtMost, factor * translateYAtMost) }
    }

    private fun tryToFixTranslationOutOfBounds(element: EditorElement, lastKnownGoodPosition: Matrix): Boolean {
        val elementMatrix = element.localMatrix
        val original = Matrix(elementMatrix)
        val current = FloatArray(9)
        val lastGood = FloatArray(9)
        elementMatrix.getValues(current)
        lastKnownGoodPosition.getValues(lastGood)

        val xTranslate = current[2] - lastGood[2]
        val yTranslate = current[5] - lastGood[5]

        if (Math.abs(xTranslate) < Bisect.ACCURACY && Math.abs(yTranslate) < Bisect.ACCURACY) return false

        val pass1X: Float
        val pass1Y: Float
        val pass2X: Float
        val pass2Y: Float

        if (Math.abs(xTranslate) < Math.abs(yTranslate)) {
            pass1X = -xTranslate; pass1Y = 0f
            pass2X = 0f; pass2Y = -yTranslate
        } else {
            pass1X = 0f; pass1Y = -yTranslate
            pass2X = -xTranslate; pass2Y = 0f
        }

        var matrix = tryToTranslateToFit(element, pass1X, pass1Y)
        if (matrix != null) {
            element.animateLocalTo(matrix, invalidate)
            return true
        }

        matrix = tryToTranslateToFit(element, pass2X, pass2Y)
        if (matrix != null) {
            element.animateLocalTo(matrix, invalidate)
            return true
        }

        elementMatrix.postTranslate(pass1X, pass1Y)
        matrix = tryToTranslateToFit(element, pass2X, pass2Y)
        elementMatrix.set(original)

        if (matrix != null) {
            element.animateLocalTo(matrix, invalidate)
            return true
        }
        return false
    }

    fun dragDropRelease() = editorElementHierarchy.dragDropRelease(visibleViewPort, invalidate)

    private fun currentCropIsAcceptable(): Boolean {
        val outputSize = getOutputSize()
        val outputPixelCount = outputSize.x * outputSize.y
        val minimumPixelCount = Math.min(size.x * size.y, 100) // MINIMUM_CROP_PIXEL_COUNT

        var thinnestRatio = Point(15, 1) // MINIMUM_RATIO
        if (compareRatios(size, thinnestRatio) < 0) thinnestRatio = size

        return compareRatios(outputSize, thinnestRatio) >= 0 &&
                outputPixelCount >= minimumPixelCount &&
                cropIsWithinMainImageBounds()
    }

    private fun compareRatios(a: Point, b: Point): Int {
        val smallA = Math.min(a.x, a.y)
        val largeA = Math.max(a.x, a.y)
        val smallB = Math.min(b.x, b.y)
        val largeB = Math.max(b.x, b.y)
        return (smallA * largeB).compareTo(smallB * largeA)
    }

    private fun cropIsWithinMainImageBounds(): Boolean = Bounds.boundsRemainInBounds(editorElementHierarchy.imageMatrixRelativeToCrop())

    fun moving(editorElement: EditorElement) {
        if (!isCropping()) {
            updateSelectionThumbsIfSelected(editorElement)
            return
        }
        val mainImage = editorElementHierarchy.getMainImage()
        val cropEditorElement = editorElementHierarchy.cropEditorElement
        if (editorElement === mainImage || editorElement === cropEditorElement) {
            if (currentCropIsAcceptable()) inBoundsMemory.push(mainImage, cropEditorElement)
        }
    }

    fun setVisibleViewPort(visibleViewPort: RectF) {
        this.visibleViewPort.set(visibleViewPort)
        this.editorElementHierarchy.updateViewToCrop(visibleViewPort, invalidate)
    }

    fun getUniqueColorsIgnoringAlpha(): Set<Int> {
        val colors = LinkedHashSet<Int>()
        editorElementHierarchy.root.forAllInTree { element ->
            // val renderer = element.renderer
            // TODO: Implement ColorableRenderer interface check
        }
        return colors
    }

    @WorkerThread
    fun render(context: Context, typefaceProvider: RendererContext.TypefaceProvider): Bitmap {
        return render(context, null, typefaceProvider)
    }

    @WorkerThread
    fun render(context: Context, size: Point?, typefaceProvider: RendererContext.TypefaceProvider): Bitmap {
        val image = editorElementHierarchy.flipRotate
        val cropRect = editorElementHierarchy.getCropRect()
        val outputSize = size ?: getOutputSize()

        val bitmap = Bitmap.createBitmap(outputSize.x, outputSize.y, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val rendererContext = RendererContext(context, canvas, RendererContext.Ready.NULL, RendererContext.Invalidate.NULL, typefaceProvider)

            val bitmapArea = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            val viewMatrix = Matrix()
            viewMatrix.setRectToRect(cropRect, bitmapArea, Matrix.ScaleToFit.FILL)

            rendererContext.isEditing = false
            rendererContext.isBlockingLoad = true

            val overlay = editorElementHierarchy.overlay
            
            // Temporary hide selection/thumbs/fade/trash for clean render
            editorElementHierarchy.selection.flags.mark()
            editorElementHierarchy.thumbs.flags.mark()
            editorElementHierarchy.fade.flags.mark()
            editorElementHierarchy.trash.flags.mark()
            
            editorElementHierarchy.selection.flags.setVisible(false).setChildrenVisible(false)
            editorElementHierarchy.thumbs.flags.setVisible(false).setChildrenVisible(false)
            editorElementHierarchy.fade.flags.setVisible(false)
            editorElementHierarchy.trash.flags.setVisible(false)

            try {
                rendererContext.canvasMatrix.initial(viewMatrix)
                image.draw(rendererContext)
            } finally {
                editorElementHierarchy.selection.flags.restore()
                editorElementHierarchy.thumbs.flags.restore()
                editorElementHierarchy.fade.flags.restore()
                editorElementHierarchy.trash.flags.restore()
            }
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }
        return bitmap
    }

    @WorkerThread
    fun renderAnnotationsOnly(context: Context, size: Point, typefaceProvider: RendererContext.TypefaceProvider): Bitmap {
        val overlay = editorElementHierarchy.overlay
        val cropRect = editorElementHierarchy.getCropRect()

        val bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val rendererContext = RendererContext(context, canvas, RendererContext.Ready.NULL, RendererContext.Invalidate.NULL, typefaceProvider)

            val bitmapArea = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            val viewMatrix = Matrix()
            viewMatrix.setRectToRect(cropRect, bitmapArea, Matrix.ScaleToFit.FILL)

            rendererContext.isEditing = false
            rendererContext.isBlockingLoad = true

            // Temporary hide selection/thumbs/fade/trash
            editorElementHierarchy.selection.flags.mark()
            editorElementHierarchy.thumbs.flags.mark()
            editorElementHierarchy.fade.flags.mark()
            editorElementHierarchy.trash.flags.mark()

            editorElementHierarchy.selection.flags.setVisible(false).setChildrenVisible(false)
            editorElementHierarchy.thumbs.flags.setVisible(false).setChildrenVisible(false)
            editorElementHierarchy.fade.flags.setVisible(false)
            editorElementHierarchy.trash.flags.setVisible(false)

            try {
                rendererContext.canvasMatrix.initial(viewMatrix)
                overlay.draw(rendererContext)
            } finally {
                editorElementHierarchy.selection.flags.restore()
                editorElementHierarchy.thumbs.flags.restore()
                editorElementHierarchy.fade.flags.restore()
                editorElementHierarchy.trash.flags.restore()
            }
        } catch (e: Exception) {
            bitmap.recycle()
            throw e
        }
        return bitmap
    }

    private fun getOutputSize(): Point {
        val outputSize = editorElementHierarchy.getOutputSize(size)
        val width = Math.max(1024, outputSize.x.toInt())
        val height = (width * outputSize.y / outputSize.x).toInt()
        return Point(width, height)
    }

    override fun onReady(renderer: Renderer, cropMatrix: Matrix?, size: Point?) {
        if (cropMatrix != null && size != null && isRendererOfMainImage(renderer)) {
            val changedBefore = isChanged()
            val imageCropMatrix = editorElementHierarchy.imageCrop.localMatrix
            this.size.set(size.x, size.y)
            if (imageCropMatrix.isIdentity) {
                imageCropMatrix.set(cropMatrix)
                if (editingPurpose == EditingPurpose.AVATAR_CAPTURE || editingPurpose == EditingPurpose.WALLPAPER || editingPurpose == EditingPurpose.AVATAR_EDIT) {
                    val userCropMatrix = editorElementHierarchy.cropEditorElement.localMatrix
                    if (size.x > size.y) {
                        userCropMatrix.setScale(fixedRatio * size.y / size.x.toFloat(), 1f)
                    } else {
                        userCropMatrix.setScale(1f, size.x / size.y.toFloat())
                    }
                }
                editorElementHierarchy.doneCrop(visibleViewPort, null)
                if (!changedBefore) undoRedoStacks.clear(editorElementHierarchy.root)
                
                when (editingPurpose) {
                    EditingPurpose.AVATAR_CAPTURE -> startCrop()
                    EditingPurpose.WALLPAPER -> { setFixedRatio(fixedRatio); startCrop() }
                    else -> {}
                }
            }
        }
    }

    fun setFixedRatio(r: Float) {
        fixedRatio = r
        val userCropMatrix = editorElementHierarchy.cropEditorElement.localMatrix
        val w = size.x.toFloat()
        val h = size.y.toFloat()
        val imageRatio = w / h
        if (imageRatio > r) {
            userCropMatrix.setScale(r / imageRatio, 1f)
        } else {
            userCropMatrix.setScale(1f, imageRatio / r)
        }
        editorElementHierarchy.doneCrop(visibleViewPort, null)
        startCrop()
    }

    private fun isRendererOfMainImage(renderer: Renderer): Boolean {
        val mainImage = editorElementHierarchy.getMainImage()
        return mainImage?.renderer === renderer
    }

    fun addElementCentered(element: EditorElement, scale: Float) {
        val localMatrix = element.localMatrix
        val inverse = Matrix()
        if (editorElementHierarchy.getMainImageFullMatrix().invert(inverse)) {
            localMatrix.set(inverse)
        }
        localMatrix.preScale(scale, scale)
        addElement(element)
    }

    fun addElement(element: EditorElement) {
        pushUndoPoint()
        addElementWithoutPushUndo(element)
    }

    fun addElementWithoutPushUndo(element: EditorElement) {
        val mainImage = editorElementHierarchy.getMainImage()
        val parent = mainImage ?: editorElementHierarchy.imageRoot
        parent.addElement(element)
        if (parent !== mainImage) undoRedoStacks.clear(editorElementHierarchy.root)
        updateUndoRedoAvailableState(undoRedoStacks)
    }

    fun isChanged(): Boolean = undoRedoStacks.isChanged(editorElementHierarchy.root)

    fun findCropRelativeToRoot(): RectF = findRelativeBounds(editorElementHierarchy.cropEditorElement, editorElementHierarchy.root)

    private fun findRelativeBounds(from: EditorElement, to: EditorElement): RectF {
        val relative = findRelativeMatrix(from, to)
        val dst = RectF(Bounds.FULL_BOUNDS)
        relative?.mapRect(dst, Bounds.FULL_BOUNDS)
        return dst
    }

    fun findRelativeMatrix(from: EditorElement, to: EditorElement): Matrix? {
        val matrix = findElementInverseMatrix(to, Matrix())
        val outOf = findElementMatrix(from, Matrix())
        if (outOf != null && matrix != null) {
            matrix.preConcat(outOf)
            return matrix
        }
        return null
    }

    fun rotate90anticlockwise() = flipRotate(-90f, 1, 1)
    fun flipHorizontal() = flipRotate(0f, -1, 1)

    private fun flipRotate(degrees: Float, scaleX: Int, scaleY: Int) {
        pushUndoPoint()
        editorElementHierarchy.flipRotate(degrees, scaleX, scaleY, visibleViewPort, invalidate)
        updateUndoRedoAvailableState(getActiveUndoRedoStacks(isCropping()))
    }

    fun delete(editorElement: EditorElement) {
        editorElementHierarchy.imageRoot.forAllInTree { it.deleteChild(editorElement, invalidate) }
        setSelected(null)
    }

    fun findById(uuid: UUID): EditorElement? = editorElementHierarchy.root.findElementWithId(uuid)

    fun zoomToTextElement(entity: EditorElement, textRenderer: MultiLineTextRenderer) {
        val elementInverseMatrix = findElementInverseMatrix(entity, Matrix())
        if (elementInverseMatrix != null) {
            val root = editorElementHierarchy.root
            elementInverseMatrix.preConcat(root.editorMatrix)
            textRenderer.applyRecommendedEditorMatrix(elementInverseMatrix)
            root.animateEditorTo(elementInverseMatrix, invalidate)
        }
    }

    fun zoomOut() = editorElementHierarchy.root.rollbackEditorMatrix(invalidate)

    fun indicateSelected(selected: EditorElement) = selected.singleScalePulse(invalidate)

    fun isCropping(): Boolean = editorElementHierarchy.cropEditorElement.flags.isVisible()

    interface UndoRedoStackListener {
        fun onAvailabilityChanged(undoAvailable: Boolean, redoAvailable: Boolean)
    }

    companion object {
        private val NULL_RUNNABLE = Runnable {}

        @JvmField
        val CREATOR = object : Parcelable.Creator<SignalEditorModel> {
            override fun createFromParcel(`in`: Parcel): SignalEditorModel = SignalEditorModel(`in`)
            override fun newArray(size: Int): Array<SignalEditorModel?> = arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(editingPurpose.ordinal)
        dest.writeFloat(fixedRatio)
        dest.writeInt(size.x)
        dest.writeInt(size.y)
        dest.writeParcelable(editorElementHierarchy.root, flags)
        dest.writeParcelable(undoRedoStacks, flags)
        dest.writeParcelable(cropUndoRedoStacks, flags)
    }
}

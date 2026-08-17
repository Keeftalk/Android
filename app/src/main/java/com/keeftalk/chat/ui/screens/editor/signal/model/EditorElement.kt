package com.keeftalk.chat.ui.screens.editor.signal.model

import android.graphics.Matrix
import android.os.Parcel
import android.os.Parcelable
import com.keeftalk.chat.ui.screens.editor.signal.CanvasMatrix
import com.keeftalk.chat.ui.screens.editor.signal.MatrixUtils
import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class EditorElement : Parcelable {
    val id: UUID
    val flags: EditorFlags
    val localMatrix = Matrix()
    val editorMatrix = Matrix()
    val zOrder: Int
    
    val renderer: Renderer?

    private val temp = Matrix()
    private val tempMatrix = Matrix()

    private val children = LinkedList<EditorElement>()
    private val deletedChildren = LinkedList<EditorElement>()

    private var animationMatrix = AnimationMatrix.NULL
    private var alphaAnimation = AlphaAnimation.NULL_1

    constructor(renderer: Renderer?, zOrder: Int = 0) {
        this.id = UUID.randomUUID()
        this.flags = EditorFlags()
        this.renderer = renderer
        this.zOrder = zOrder
    }

    private constructor(`in`: Parcel) {
        id = ParcelUtils.readUUID(`in`)
        flags = EditorFlags(`in`.readInt())
        ParcelUtils.readMatrix(localMatrix, `in`)
        renderer = `in`.readParcelable(Renderer::class.java.classLoader)
        zOrder = `in`.readInt()
        `in`.readTypedList(children, CREATOR)
    }

    fun draw(rendererContext: RendererContext) {
        if (!flags.isVisible() && !flags.isChildrenVisible()) return

        rendererContext.save()
        rendererContext.canvasMatrix.concat(localMatrix)

        if (rendererContext.isEditing) {
            rendererContext.canvasMatrix.concat(editorMatrix)
            animationMatrix.preConcatValueTo(rendererContext.canvasMatrix)
        }

        if (flags.isVisible()) {
            val alpha = alphaAnimation.getValue()
            if (alpha > 0) {
                rendererContext.setFade(alpha)
                rendererContext.children = children
                drawSelf(rendererContext)
                rendererContext.setFade(1f)
            }
        }

        if (flags.isChildrenVisible()) {
            drawChildren(children, rendererContext)
            drawChildren(deletedChildren, rendererContext)
        }

        rendererContext.restore()
    }

    private fun drawSelf(rendererContext: RendererContext) {
        renderer?.render(rendererContext)
    }

    private fun drawChildren(children: List<EditorElement>, rendererContext: RendererContext) {
        for (element in children) {
            if (element.zOrder >= 0) {
                element.draw(rendererContext)
            }
        }
    }

    fun addElement(element: EditorElement) {
        children.add(element)
        Collections.sort(children, Z_ORDER_COMPARATOR)
    }

    fun findElement(toFind: EditorElement, viewMatrix: Matrix, outInverseModelMatrix: Matrix): EditorElement? {
        return findElement(viewMatrix, outInverseModelMatrix) { element, _ -> toFind === element }
    }

    fun findElementAt(x: Float, y: Float, viewModelMatrix: Matrix, outInverseModelMatrix: Matrix): EditorElement? {
        val dst = FloatArray(2)
        val src = floatArrayOf(x, y)

        return findElement(viewModelMatrix, outInverseModelMatrix) { element, inverseMatrix ->
            val r = element.renderer ?: return@findElement false
            inverseMatrix.mapPoints(dst, src)
            element.flags.isSelectable() && r.hitTest(dst[0], dst[1])
        }
    }

    fun findElement(viewModelMatrix: Matrix, outInverseModelMatrix: Matrix, predicate: (EditorElement, Matrix) -> Boolean): EditorElement? {
        temp.set(viewModelMatrix)
        temp.preConcat(localMatrix)
        temp.preConcat(editorMatrix)

        if (temp.invert(tempMatrix)) {
            for (i in children.size - 1 downTo 0) {
                val elementAt = children[i].findElement(temp, outInverseModelMatrix, predicate)
                if (elementAt != null) return elementAt
            }

            if (predicate(this, tempMatrix)) {
                outInverseModelMatrix.set(tempMatrix)
                return this
            }
        }
        return null
    }

    fun forAllInTree(function: (EditorElement) -> Unit) {
        function(this)
        for (child in children) {
            child.forAllInTree(function)
        }
    }

    fun findParent(editorElement: EditorElement): EditorElement? {
        for (child in children) {
            if (child === editorElement) {
                return this
            } else {
                val element = child.findParent(editorElement)
                if (element != null) return element
            }
        }
        return null
    }

    fun findElementWithId(id: UUID): EditorElement? {
        if (this.id == id) return this
        for (child in children) {
            val element = child.findElementWithId(id)
            if (element != null) return element
        }
        return null
    }

    fun deleteChild(editorElement: EditorElement, invalidate: Runnable?) {
        val iterator = children.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() === editorElement) {
                iterator.remove()
                addDeletedChildFadingOut(editorElement, invalidate)
            }
        }
    }

    fun addDeletedChildFadingOut(fromElement: EditorElement, invalidate: Runnable?) {
        deletedChildren.add(fromElement)
        fromElement.animateFadeOut(invalidate)
    }

    fun animateFadeOut(invalidate: Runnable?) {
        alphaAnimation = AlphaAnimation.animate(1f, 0f, invalidate)
    }

    fun animateFadeIn(invalidate: Runnable?) {
        alphaAnimation = AlphaAnimation.animate(0f, 1f, invalidate)
    }

    fun animatePartialFadeOut(invalidate: Runnable?) {
        alphaAnimation = AlphaAnimation.animate(alphaAnimation.getValue(), 0.5f, invalidate)
    }

    fun animatePartialFadeIn(invalidate: Runnable?) {
        alphaAnimation = AlphaAnimation.animate(alphaAnimation.getValue(), 1f, invalidate)
    }

    fun parentOf(element: EditorElement): EditorElement? {
        if (children.contains(element)) return this
        for (child in children) {
            val parent = child.parentOf(element)
            if (parent != null) return parent
        }
        return null
    }

    fun singleScalePulse(invalidate: Runnable?) {
        val scale = Matrix()
        scale.setScale(1.2f, 1.2f)
        animationMatrix = AnimationMatrix.singlePulse(scale, invalidate)
    }

    fun deleteAllChildren() {
        children.clear()
    }

    fun getLocalRotationAngle(): Float = MatrixUtils.getRotationAngle(localMatrix)

    fun getLocalScaleX(): Float = MatrixUtils.getScaleX(localMatrix)

    fun commitEditorMatrix() {
        if (flags.isEditable()) {
            localMatrix.preConcat(editorMatrix)
            editorMatrix.reset()
        } else {
            rollbackEditorMatrix(null)
        }
    }

    fun rollbackEditorMatrix(invalidate: Runnable?) {
        animateEditorTo(Matrix(), invalidate)
    }

    fun buildMap(map: MutableMap<UUID, EditorElement>) {
        map[id] = this
        for (child in children) {
            child.buildMap(map)
        }
    }

    fun animateFrom(oldMatrix: Matrix, invalidate: Runnable?) {
        val oldMatrixCopy = Matrix(oldMatrix)
        animationMatrix.stop()
        animationMatrix.preConcatValueTo(oldMatrixCopy)
        animationMatrix = AnimationMatrix.animate(oldMatrixCopy, localMatrix, invalidate)
    }

    fun animateEditorTo(newEditorMatrix: Matrix, invalidate: Runnable?) {
        setMatrixWithAnimation(editorMatrix, newEditorMatrix, invalidate)
    }

    fun animateLocalTo(newLocalMatrix: Matrix, invalidate: Runnable?) {
        setMatrixWithAnimation(localMatrix, newLocalMatrix, invalidate)
    }

    private fun setMatrixWithAnimation(destination: Matrix, source: Matrix, invalidate: Runnable?) {
        val old = Matrix(destination)
        animationMatrix.stop()
        animationMatrix.preConcatValueTo(old)
        destination.set(source)
        animationMatrix = AnimationMatrix.animate(old, destination, invalidate)
    }

    fun getLocalMatrixAnimating(): Matrix {
        val matrix = Matrix(localMatrix)
        animationMatrix.preConcatValueTo(matrix)
        return matrix
    }

    fun stopAnimation() {
        animationMatrix.stop()
    }

    fun getChildCount(): Int = children.size
    fun getChild(i: Int): EditorElement = children[i]

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        ParcelUtils.writeUUID(dest, id)
        dest.writeInt(this.flags.asInt())
        ParcelUtils.writeMatrix(dest, localMatrix)
        dest.writeParcelable(renderer, flags)
        dest.writeInt(zOrder)
        dest.writeTypedList(children)
    }

    companion object {
        private val Z_ORDER_COMPARATOR = Comparator<EditorElement> { e1, e2 -> e1.zOrder.compareTo(e2.zOrder) }

        @JvmField
        val CREATOR = object : Parcelable.Creator<EditorElement> {
            override fun createFromParcel(`in`: Parcel): EditorElement = EditorElement(`in`)
            override fun newArray(size: Int): Array<EditorElement?> = arrayOfNulls(size)
        }
    }
}

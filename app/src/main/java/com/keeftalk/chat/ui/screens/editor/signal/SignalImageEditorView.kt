package com.keeftalk.chat.ui.screens.editor.signal

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.GestureDetectorCompat
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.model.SignalEditorModel
import com.keeftalk.chat.ui.screens.editor.signal.renderers.BezierDrawingRenderer
import com.keeftalk.chat.ui.screens.editor.signal.renderers.MultiLineTextRenderer
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class SignalImageEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val editText: HiddenEditText
    private val touchHandler = ImageEditorTouchHandler()
    private var model: SignalEditorModel = SignalEditorModel(Color.BLACK)
    private var mode: Mode = Mode.MoveAndResize
    
    private val viewMatrix = Matrix()
    private val viewPort = Bounds.newFullBounds()
    private val visibleViewPort = Bounds.newFullBounds()
    private val screen = RectF()

    private var rendererContext: RendererContext? = null
    private var typefaceProvider: RendererContext.TypefaceProvider = object : RendererContext.TypefaceProvider {
        override fun getSelectedTypeface(context: Context, renderer: Renderer, invalidate: RendererContext.Invalidate): Typeface {
            return Typeface.DEFAULT
        }
    }

    private val doubleTap: GestureDetectorCompat
    var tapListener: TapListener? = null

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        
        editText = createHiddenTextEntryField()
        doubleTap = GestureDetectorCompat(context, DoubleTapGestureListener())
        
        setOnTouchListener { _, event ->
            doubleTap.onTouchEvent(event)
            handleTouchEvent(event)
        }
    }

    private fun createHiddenTextEntryField(): HiddenEditText {
        val et = HiddenEditText(context)
        addView(et)
        et.clearFocus()
        et.setOnEndEdit { doneTextEditing() }
        et.setOnEditOrSelectionChange(object : HiddenEditText.OnEditOrSelectionChange {
            override fun onChange(editorElement: EditorElement, textRenderer: MultiLineTextRenderer) {
                zoomToFitText(editorElement, textRenderer)
            }
        })
        return et
    }

    fun setModel(model: SignalEditorModel) {
        if (this.model !== model) {
            this.model.setInvalidate(null)
            this.model.setUndoRedoStackListener(null)
            this.model = model
            this.model.setInvalidate { invalidate() }
            this.model.setVisibleViewPort(visibleViewPort)
            invalidate()
        }
    }

    fun getModel(): SignalEditorModel = model

    fun setMode(mode: Mode) {
        if (this.mode != mode) {
            this.mode = mode
            touchHandler.setDrawing(mode == Mode.Draw || mode == Mode.Blur, mode == Mode.Blur)
        }
    }

    fun setDrawingBrush(color: Int, thickness: Float, cap: Paint.Cap) {
        touchHandler.setDrawingBrush(color, thickness, cap)
    }

    fun addText() {
        val renderer = MultiLineTextRenderer("", Color.WHITE, MultiLineTextRenderer.Mode.REGULAR)
        val element = EditorElement(renderer, 2) // Z_TEXT = 2
        model.addElementCentered(element, 1f)
        invalidate()
        startTextEditing(element)
    }

    fun startCrop() {
        model.startCrop()
        invalidate()
    }

    fun doneCrop() {
        model.doneCrop()
        invalidate()
    }

    fun startTextEditing(element: EditorElement) {
        model.addFade()
        if (element.renderer is MultiLineTextRenderer) {
            model.setSelectionVisible(false)
            editText.setCurrentTextEditorElement(element)
            editText.requestFocus()
            
            // Force keyboard
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun isTextEditing(): Boolean = editText.getCurrentTextEntity() != null

    fun doneTextEditing() {
        model.zoomOut()
        model.removeFade()
        model.setSelectionVisible(true)
        if (editText.getCurrentTextEntity() != null) {
            model.setSelected(null)
            editText.setCurrentTextEditorElement(null)
            editText.hideKeyboard()
        }
    }

    private fun zoomToFitText(element: EditorElement, renderer: MultiLineTextRenderer) {
        model.zoomToTextElement(element, renderer)
    }

    override fun onDraw(canvas: Canvas) {
        if (rendererContext == null || rendererContext!!.canvas !== canvas) {
            rendererContext = RendererContext(context, canvas, rendererReady, rendererInvalidate, typefaceProvider)
        }
        rendererContext!!.save()
        try {
            rendererContext!!.canvasMatrix.initial(viewMatrix)
            model.draw(rendererContext!!, editText.getCurrentTextEditorElement())
        } finally {
            rendererContext!!.restore()
        }
    }

    private val rendererReady = object : RendererContext.Ready {
        override fun onReady(renderer: Renderer, cropMatrix: Matrix?, size: Point?) {
            model.onReady(renderer, cropMatrix, size)
            invalidate()
        }
    }

    private val rendererInvalidate = object : RendererContext.Invalidate {
        override fun onInvalidate(renderer: Renderer) {
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewMatrix()
    }

    private fun updateViewMatrix() {
        screen.right = width.toFloat()
        screen.bottom = height.toFloat()

        viewMatrix.setRectToRect(viewPort, screen, Matrix.ScaleToFit.FILL)
        val values = FloatArray(9)
        viewMatrix.getValues(values)
        val scale = values[0] / values[4]

        val tempViewPort = Bounds.newFullBounds()
        if (scale < 1) {
            tempViewPort.top /= scale
            tempViewPort.bottom /= scale
        } else {
            tempViewPort.left *= scale
            tempViewPort.right *= scale
        }

        visibleViewPort.set(tempViewPort)
        viewMatrix.setRectToRect(visibleViewPort, screen, Matrix.ScaleToFit.CENTER)
        model.setVisibleViewPort(visibleViewPort)
        invalidate()
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        val point = PointF(event.x, event.y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val selected = touchHandler.onDown(model, viewMatrix, point)
                tapListener?.onEntityDown(selected)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val pointers = Array(event.pointerCount) { i -> PointF(event.getX(i), event.getY(i)) }
                touchHandler.onMove(model, pointers)
                invalidate()
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    touchHandler.onSecondPointerDown(model, viewMatrix, point, event.actionIndex)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.actionIndex < 2) {
                    touchHandler.onSecondPointerUp(model, viewMatrix, event.actionIndex)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                touchHandler.onUp(model)
                invalidate()
                return true
            }
        }
        return false
    }

    enum class Mode { MoveAndResize, Draw, Blur }

    interface TapListener {
        fun onEntityDown(element: EditorElement?)
        fun onEntitySingleTap(element: EditorElement?)
        fun onEntityDoubleTap(element: EditorElement)
    }

    private inner class DoubleTapGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val point = PointF(e.x, e.y)
            val selected = model.findElementAtPoint(point, viewMatrix, Matrix())
            if (selected != null) tapListener?.onEntityDoubleTap(selected)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val point = PointF(e.x, e.y)
            val selected = model.findElementAtPoint(point, viewMatrix, Matrix())
            if (selected != null) {
                model.indicateSelected(selected)
                model.setSelected(selected)
                tapListener?.onEntitySingleTap(selected)
            } else {
                model.setSelected(null)
                tapListener?.onEntitySingleTap(null)
            }
            return true
        }
    }
}

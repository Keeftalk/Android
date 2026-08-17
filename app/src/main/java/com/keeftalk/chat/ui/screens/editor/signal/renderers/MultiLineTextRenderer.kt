package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.animation.ValueAnimator
import android.graphics.*
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.view.animation.Interpolator
import com.keeftalk.chat.ui.screens.editor.signal.*
import com.keeftalk.chat.ui.screens.editor.signal.Bounds
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class MultiLineTextRenderer : InvalidateableRenderer, ColorableRenderer, SelectableRenderer {

    private var text: String = ""
    override var color: Int = 0
        set(value) {
            if (field != value) {
                field = value
                setColorInternal(value)
            }
        }

    private val paint = Paint()
    private val selectionPaint = Paint()
    private val modePaint = Paint()
    private val textScale: Float
    private var selStart: Int = 0
    private var selEnd: Int = 0
    private var hasFocus: Boolean = false
    private var mode: Mode = Mode.REGULAR
    private var lines: MutableList<Line> = ArrayList()
    private var cursorAnimator: ValueAnimator? = null
    private var cursorAnimatedValue: Float = 0f
    private val recommendedEditorMatrix = Matrix()
    private val textBounds = RectF()

    constructor(text: String?, color: Int, mode: Mode) {
        this.mode = mode
        modePaint.isAntiAlias = true
        modePaint.textSize = 100f
        
        setColorInternal(color)
        
        val regularTextSize = paint.textSize
        paint.isAntiAlias = true
        paint.textSize = 100f
        textScale = paint.textSize / regularTextSize
        
        selectionPaint.isAntiAlias = true
        
        setText(text ?: "")
        createLinesForText()
    }

    override fun render(rendererContext: RendererContext) {
        super.render(rendererContext)
        
        paint.typeface = rendererContext.typefaceProvider.getSelectedTypeface(rendererContext.context, this, rendererContext.invalidate)
        modePaint.typeface = rendererContext.typefaceProvider.getSelectedTypeface(rendererContext.context, this, rendererContext.invalidate)
        
        var height = 0f
        var width = 0f
        for (line in lines) {
            line.render(rendererContext)
            height += line.heightInBounds - line.ascentInBounds + line.descentInBounds
            width = Math.max(line.textBounds.width(), width)
        }
        
        textBounds.set(-width - PADDING, -PADDING, width + PADDING, height / 2f + PADDING)
    }

    fun getText(): String = text

    fun setText(text: String) {
        if (this.text != text) {
            this.text = text
            createLinesForText()
        }
    }

    fun nextMode() {
        setMode(Mode.fromCode(mode.code + 1))
    }

    fun getMode(): Mode = mode

    fun applyRecommendedEditorMatrix(matrix: Matrix) {
        recommendedEditorMatrix.reset()
        var scale = 1f
        for (line in lines) {
            if (line.scale < scale) scale = line.scale
        }
        
        var yOff = 0f
        for (line in lines) {
            if (line.containsSelectionEnd()) break else yOff -= line.heightInBounds
        }
        
        recommendedEditorMatrix.postTranslate(0f, Bounds.TOP / 1.5f + yOff)
        recommendedEditorMatrix.postScale(scale, scale)
        matrix.postConcat(recommendedEditorMatrix)
    }

    private fun createLinesForText() {
        val split = text.split("\n").toTypedArray()
        if (split.size == lines.size) {
            for (i in split.indices) lines[i].setText(split[i])
        } else {
            lines = ArrayList(split.size)
            for (s in split) lines.add(Line(s))
        }
        setSelection(selStart, selEnd)
    }

    private inner class Line(var lineText: String) {
        val ascentMatrix = Matrix()
        val descentMatrix = Matrix()
        val projectionMatrix = Matrix()
        val inverseProjectionMatrix = Matrix()
        val selectionBounds = RectF()
        val textBounds = RectF()
        val hitBounds = RectF()
        val modeBounds = RectF()
        val outlinerPath = Path()
        
        var selStartLine: Int = 0
        var selEndLine: Int = 0
        var ascentInBounds: Float = 0f
        var descentInBounds: Float = 0f
        var scale: Float = 1f
        var heightInBounds: Float = 0f

        init {
            recalculate()
        }

        fun recalculate() {
            val maxTextBounds = RectF()
            val temp = Rect()
            
            getTextBoundsWithoutTrim(lineText, 0, lineText.length, temp)
            textBounds.set(temp)
            hitBounds.set(textBounds)
            
            val hitPadding = EditorResources.dpToPx(30f)
            hitBounds.left -= hitPadding
            hitBounds.right += hitPadding
            hitBounds.top -= hitPadding
            hitBounds.bottom += hitPadding
            
            maxTextBounds.set(textBounds)
            val widthLimit = 150 * textScale
            scale = 1f / Math.max(1f, maxTextBounds.right / widthLimit)
            maxTextBounds.right = widthLimit
            
            if (showSelectionOrCursor()) {
                val startTemp = Rect()
                val startInString = Math.min(lineText.length, Math.max(0, selStartLine))
                val endInString = Math.min(lineText.length, Math.max(0, selEndLine))
                val startText = lineText.substring(0, startInString)
                
                getTextBoundsWithoutTrim(startText, 0, startInString, startTemp)
                
                if (selStartLine != selEndLine) {
                    getTextBoundsWithoutTrim(lineText, startInString, endInString, temp)
                } else {
                    paint.getTextBounds("|", 0, 1, temp)
                    val w = temp.width()
                    temp.left -= w
                    temp.right -= w
                }
                
                temp.left += startTemp.right
                temp.right += startTemp.right
                selectionBounds.set(temp)
            }
            
            projectionMatrix.setRectToRect(RectF(maxTextBounds), Bounds.FULL_BOUNDS, Matrix.ScaleToFit.CENTER)
            removeTranslate(projectionMatrix)
            
            val pts = floatArrayOf(0f, paint.ascent(), 0f, paint.descent())
            projectionMatrix.mapPoints(pts)
            ascentInBounds = pts[1]
            descentInBounds = pts[3]
            heightInBounds = descentInBounds - ascentInBounds
            
            projectionMatrix.preTranslate(-textBounds.centerX(), 0f)
            projectionMatrix.invert(inverseProjectionMatrix)
            
            ascentMatrix.setTranslate(0f, -ascentInBounds)
            descentMatrix.setTranslate(0f, descentInBounds + EditorResources.dpToPx(10f) + EditorResources.dpToPx(6f)) // HIGHLIGHT_TOP/BOTTOM
            
            invalidate()
        }

        private fun removeTranslate(matrix: Matrix) {
            val values = FloatArray(9)
            matrix.getValues(values)
            values[2] = 0f
            values[5] = 0f
            matrix.setValues(values)
        }

        fun showSelectionOrCursor(): Boolean = (selStartLine >= 0 || selEndLine >= 0) && (selStartLine <= lineText.length || selEndLine <= lineText.length)
        fun containsSelectionEnd(): Boolean = selEndLine >= 0 && selEndLine <= lineText.length

        private fun getTextBoundsWithoutTrim(text: String, start: Int, end: Int, result: Rect) {
            val extra = Rect()
            val xBounds = Rect()
            val cannotBeTrimmed = "x" + text.substring(Math.max(0, start), Math.min(text.length, end)) + "x"
            paint.getTextBounds(cannotBeTrimmed, 0, cannotBeTrimmed.length, extra)
            paint.getTextBounds("x", 0, 1, xBounds)
            result.set(extra)
            result.right -= 2 * xBounds.width()
            val t = result.left
            result.left -= t
            result.right -= t
        }

        fun setText(text: String) {
            if (this.lineText != text) {
                this.lineText = text
                recalculate()
            }
        }

        fun render(rendererContext: RendererContext) {
            rendererContext.canvasMatrix.concat(ascentMatrix)
            rendererContext.save()
            rendererContext.canvasMatrix.concat(projectionMatrix)
            
            if (mode == Mode.HIGHLIGHT) {
                if (lineText.isEmpty()) modeBounds.setEmpty()
                else modeBounds.set(
                    textBounds.left - EditorResources.dpToPx(8f),
                    selectionBounds.top - EditorResources.dpToPx(10f),
                    textBounds.right + EditorResources.dpToPx(8f),
                    selectionBounds.bottom + EditorResources.dpToPx(6f)
                )
                val alpha = modePaint.alpha
                modePaint.alpha = rendererContext.getAlpha(alpha)
                val radius = EditorResources.dpToPx(4f)
                rendererContext.canvas.drawRoundRect(modeBounds, radius, radius, modePaint)
                modePaint.alpha = alpha
            } else if (mode == Mode.UNDERLINE) {
                if (lineText.isEmpty()) modeBounds.setEmpty()
                else {
                    modeBounds.set(textBounds.left, selectionBounds.top, textBounds.right, selectionBounds.bottom)
                    val inset = EditorResources.dpToPx(2f)
                    modeBounds.inset(-inset, -inset)
                    modeBounds.set(
                        modeBounds.left,
                        Math.max(modeBounds.top, modeBounds.bottom - EditorResources.dpToPx(6f)),
                        modeBounds.right,
                        modeBounds.bottom - EditorResources.dpToPx(2f)
                    )
                }
                val alpha = modePaint.alpha
                modePaint.alpha = rendererContext.getAlpha(alpha)
                rendererContext.canvas.drawRect(modeBounds, modePaint)
                modePaint.alpha = alpha
            }
            
            if (hasFocus && showSelectionOrCursor()) {
                selectionPaint.alpha = if (selStartLine == selEndLine) (cursorAnimatedValue * 128).toInt() else 128
                rendererContext.canvas.drawRect(selectionBounds, selectionPaint)
            }
            
            val paintAlpha = paint.alpha
            paint.alpha = rendererContext.getAlpha(paintAlpha)
            rendererContext.canvas.drawText(lineText, 0f, 0f, paint)
            paint.alpha = paintAlpha
            
            if (mode == Mode.OUTLINE) {
                val modeAlpha = modePaint.alpha
                modePaint.alpha = rendererContext.getAlpha(paintAlpha)
                if (Build.VERSION.SDK_INT >= 31) {
                    outlinerPath.reset()
                    modePaint.getTextPath(lineText, 0, lineText.length, 0f, 0f, outlinerPath)
                    rendererContext.canvas.drawPath(outlinerPath, modePaint)
                } else {
                    rendererContext.canvas.drawText(lineText, 0f, 0f, modePaint)
                }
                modePaint.alpha = modeAlpha
            }
            
            rendererContext.restore()
            rendererContext.canvasMatrix.concat(descentMatrix)
        }

        fun setSelection(start: Int, end: Int) {
            if (selStartLine != start || selEndLine != end) {
                selStartLine = start
                selEndLine = end
                recalculate()
            }
        }
    }

    override fun onSelected(selected: Boolean) {}

    override fun getSelectionBounds(bounds: RectF) {
        bounds.set(textBounds)
    }

    override fun hitTest(x: Float, y: Float): Boolean = textBounds.contains(x, y)

    fun setSelection(start: Int, end: Int) {
        var s = start
        var e = end
        selStart = start
        selEnd = end
        for (line in lines) {
            line.setSelection(s, e)
            val length = line.lineText.length + 1
            s -= length
            e -= length
        }
    }

    fun setFocused(hasFocus: Boolean) {
        if (this.hasFocus != hasFocus) {
            this.hasFocus = hasFocus
            cursorAnimator?.cancel()
            cursorAnimator = null
            if (hasFocus) {
                cursorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    interpolator = pulseInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    duration = 1000
                    addUpdateListener { animation ->
                        cursorAnimatedValue = animation.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            } else {
                invalidate()
            }
        }
    }

    private fun setMode(mode: Mode) {
        if (this.mode != mode) {
            this.mode = mode
            setColorInternal(color)
        }
    }

    private fun setColorInternal(color: Int) {
        this.color = color
        if (mode == Mode.REGULAR) {
            paint.color = color
            selectionPaint.color = color
        } else {
            paint.color = Color.WHITE
            selectionPaint.color = Color.WHITE
        }
        
        if (mode == Mode.OUTLINE) {
            modePaint.strokeWidth = EditorResources.dpToPx(15f) / 10f
            modePaint.style = Paint.Style.STROKE
        } else {
            modePaint.style = Paint.Style.FILL
        }
        modePaint.color = color
        invalidate()
    }

    enum class Mode(val code: Int) {
        REGULAR(0), HIGHLIGHT(1), UNDERLINE(2), OUTLINE(3);
        companion object {
            fun fromCode(code: Int): Mode = entries.find { it.code == code % entries.size } ?: REGULAR
        }
    }

    companion object {
        private const val PADDING = 10f

        @JvmField
        val CREATOR = object : Parcelable.Creator<MultiLineTextRenderer> {
            override fun createFromParcel(`in`: Parcel): MultiLineTextRenderer = MultiLineTextRenderer(`in`.readString(), `in`.readInt(), Mode.fromCode(`in`.readInt()))
            override fun newArray(size: Int): Array<MultiLineTextRenderer?> = arrayOfNulls(size)
        }

        private fun pulseInterpolator() = Interpolator { input ->
            var t = input * 5
            if (t > 1) t = 4 - t
            Math.max(0f, Math.min(1f, t))
        }
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(text)
        dest.writeInt(color)
        dest.writeInt(mode.code)
    }
}

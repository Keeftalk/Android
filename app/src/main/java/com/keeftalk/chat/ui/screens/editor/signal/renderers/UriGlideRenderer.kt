package com.keeftalk.chat.ui.screens.editor.signal.renderers

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.keeftalk.chat.ui.screens.editor.signal.*
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.model.SignalEditorModel

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class UriGlideRenderer : SelectableRenderer {

    private val imageUri: Uri
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    private val imageProjectionMatrix = Matrix()
    private val temp = Matrix()
    private val blurScaleMatrix = Matrix()
    private val decryptable: Boolean
    private val maxWidth: Int
    private val maxHeight: Int
    private val blurRadius: Float
    private val bitmapRequestListener: RequestListener<Bitmap>?

    private var selected: Boolean = false
    private var bitmap: Bitmap? = null
    private var blurredBitmap: Bitmap? = null
    private var blurPaint: Paint? = null

    constructor(imageUri: Uri, decryptable: Boolean, maxWidth: Int, maxHeight: Int, blurRadius: Float = 25f, bitmapRequestListener: RequestListener<Bitmap>? = null) {
        this.imageUri = imageUri
        this.decryptable = decryptable
        this.maxWidth = maxWidth
        this.maxHeight = maxHeight
        this.blurRadius = blurRadius
        this.bitmapRequestListener = bitmapRequestListener
    }

    override fun render(rendererContext: RendererContext) {
        if (getBitmap() == null) {
            if (rendererContext.isBlockingLoad) {
                try {
                    val resource = getGlideRequestBuilder(rendererContext.context, false).submit().get()
                    setBitmap(rendererContext, resource)
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            } else {
                getGlideRequestBuilder(rendererContext.context, true).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        setBitmap(rendererContext, resource)
                        rendererContext.invalidate.onInvalidate(this@UriGlideRenderer)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        bitmap = null
                    }
                })
            }
        }

        val currentBitmap = getBitmap()
        if (currentBitmap != null) {
            rendererContext.save()
            rendererContext.canvasMatrix.concat(imageProjectionMatrix)
            
            val alpha = paint.alpha
            paint.alpha = rendererContext.getAlpha(alpha)
            
            rendererContext.canvas.drawBitmap(currentBitmap, 0f, 0f, rendererContext.maskPaint ?: paint)
            paint.alpha = alpha
            rendererContext.restore()
            
            renderBlurOverlay(rendererContext)
        } else if (rendererContext.isBlockingLoad) {
            rendererContext.canvas.drawRect(Bounds.FULL_BOUNDS, paint)
        }
    }

    private fun renderBlurOverlay(rendererContext: RendererContext) {
        var renderMask = false
        for (child in rendererContext.children) {
            if (child.zOrder == -1) { // EditorModel.Z_MASK
                renderMask = true
                if (blurPaint == null) {
                    blurPaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                        isDither = true
                    }
                }
                blurPaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                rendererContext.maskPaint = blurPaint
                child.draw(rendererContext)
            }
        }

        if (renderMask) {
            rendererContext.save()
            rendererContext.canvasMatrix.concat(imageProjectionMatrix)
            
            val bp = blurPaint!!
            bp.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_ATOP)
            bp.maskFilter = null
            
            if (blurredBitmap == null) {
                blurredBitmap = blur(getBitmap()!!, rendererContext.context, blurRadius)
                blurScaleMatrix.setRectToRect(
                    RectF(0f, 0f, blurredBitmap!!.width.toFloat(), blurredBitmap!!.height.toFloat()),
                    RectF(0f, 0f, getBitmap()!!.width.toFloat(), getBitmap()!!.height.toFloat()),
                    Matrix.ScaleToFit.FILL
                )
            }
            
            rendererContext.canvas.concat(blurScaleMatrix)
            rendererContext.canvas.drawBitmap(blurredBitmap!!, 0f, 0f, bp)
            bp.xfermode = null
            rendererContext.restore()
        }
    }

    private fun getGlideRequestBuilder(context: Context, preview: Boolean): RequestBuilder<Bitmap> {
        var width = maxWidth
        var height = maxHeight
        if (preview) {
            width = Math.min(width, 2048)
            height = Math.min(height, 2048)
        }
        return Glide.with(context)
            .asBitmap()
            .override(width, height)
            .centerInside()
            .addListener(bitmapRequestListener)
            .load(imageUri)
    }

    override fun hitTest(x: Float, y: Float): Boolean {
        return if (selected) Bounds.contains(x, y) else pixelAlphaNotZero(x, y)
    }

    private fun pixelAlphaNotZero(x: Float, y: Float): Boolean {
        val b = getBitmap() ?: return false
        imageProjectionMatrix.invert(temp)
        val onBmp = FloatArray(2)
        temp.mapPoints(onBmp, floatArrayOf(x, y))
        val xInt = onBmp[0].toInt()
        val yInt = onBmp[1].toInt()
        return if (xInt in 0 until b.width && yInt in 0 until b.height) {
            (b.getPixel(xInt, yInt) and -0x1000000) != 0
        } else false
    }

    fun getBitmap(): Bitmap? {
        if (bitmap?.isRecycled == true) bitmap = null
        return bitmap
    }

    private fun setBitmap(rendererContext: RendererContext, bitmap: Bitmap?) {
        this.bitmap = bitmap
        bitmap?.let {
            val from = RectF(0f, 0f, it.width.toFloat(), it.height.toFloat())
            imageProjectionMatrix.setRectToRect(from, Bounds.FULL_BOUNDS, Matrix.ScaleToFit.CENTER)
            rendererContext.rendererReady.onReady(this, cropMatrix(it), Point(it.width, it.height))
        }
    }

    private fun cropMatrix(bitmap: Bitmap): Matrix {
        val matrix = Matrix()
        if (bitmap.width > bitmap.height) {
            matrix.preScale(1f, bitmap.height.toFloat() / bitmap.width)
        } else {
            matrix.preScale(bitmap.width.toFloat() / bitmap.height, 1f)
        }
        return matrix
    }

    private fun blur(bitmap: Bitmap, context: Context, blurRadius: Float): Bitmap {
        val width = (bitmap.width * 0.2f).toInt().coerceAtLeast(1)
        val height = (bitmap.height * 0.2f).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, width, height, false)
        return Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
    }

    override fun onSelected(selected: Boolean) {
        this.selected = selected
    }

    override fun getSelectionBounds(bounds: RectF) {
        bounds.set(Bounds.FULL_BOUNDS)
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(imageUri.toString())
        dest.writeInt(if (decryptable) 1 else 0)
        dest.writeInt(maxWidth)
        dest.writeInt(maxHeight)
        dest.writeFloat(blurRadius)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<UriGlideRenderer> {
            override fun createFromParcel(`in`: Parcel): UriGlideRenderer {
                return UriGlideRenderer(Uri.parse(`in`.readString()), `in`.readInt() == 1, `in`.readInt(), `in`.readInt(), `in`.readFloat())
            }
            override fun newArray(size: Int): Array<UriGlideRenderer?> = arrayOfNulls(size)
        }
    }
}

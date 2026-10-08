package tn.keeftalk.chat.ui.books.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

data class PdfCacheKey(
    val pageNumber: Int,
    val dpi: Int,
    val autoCrop: Boolean
)

object PdfFastCrop {

    /**
     * Crops uniform margins. Scans the bitmap one row at a time, so it never allocates a
     * second full-size IntArray (the old version needed w*h*4 extra bytes, ~64 MB at 16 MP).
     * Returns the SAME instance when nothing should be cropped.
     */
    fun autoCropPdfBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 40 || h <= 40) return bitmap

        val samplePoints = try {
            intArrayOf(
                bitmap.getPixel(0, 0),
                bitmap.getPixel(w - 1, 0),
                bitmap.getPixel(0, h - 1),
                bitmap.getPixel(w - 1, h - 1),
                bitmap.getPixel(w / 2, 0),
                bitmap.getPixel(w / 2, h - 1),
                bitmap.getPixel(0, h / 2),
                bitmap.getPixel(w - 1, h / 2)
            )
        } catch (_: Exception) {
            return bitmap
        }

        var sumR = 0; var sumG = 0; var sumB = 0
        for (p in samplePoints) {
            sumR += (p shr 16) and 0xFF
            sumG += (p shr 8) and 0xFF
            sumB += p and 0xFF
        }
        val avgR = sumR / samplePoints.size
        val avgG = sumG / samplePoints.size
        val avgB = sumB / samplePoints.size
        val bgIsNearWhite = avgR > 220 && avgG > 220 && avgB > 220

        fun isContent(pixel: Int): Boolean {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val isDifferent = abs(r - avgR) > 8 || abs(g - avgG) > 8 || abs(b - avgB) > 8
            val isNotPureWhite = bgIsNearWhite && (r < 246 || g < 246 || b < 246)
            return isDifferent || isNotPureWhite
        }

        val row = IntArray(w)
        var top = -1
        var bottom = -1
        var left = w
        var right = -1

        try {
            for (y in 0 until h) {
                bitmap.getPixels(row, 0, w, 0, y, w, 1)
                var first = -1
                for (x in 0 until w) {
                    if (isContent(row[x])) { first = x; break }
                }
                if (first == -1) continue
                var last = first
                for (x in w - 1 downTo first) {
                    if (isContent(row[x])) { last = x; break }
                }
                if (top == -1) top = y
                bottom = y
                if (first < left) left = first
                if (last > right) right = last
            }
        } catch (_: Exception) {
            return bitmap
        }

        if (top == -1 || right == -1) return bitmap // blank page

        val padX = maxOf(24, (w * 0.03f).toInt())
        val padY = maxOf(24, (h * 0.03f).toInt())

        val safeLeft = maxOf(0, left - padX)
        val safeTop = maxOf(0, top - padY)
        val safeRight = minOf(w - 1, right + padX)
        val safeBottom = minOf(h - 1, bottom + padY)

        val cropW = safeRight - safeLeft + 1
        val cropH = safeBottom - safeTop + 1

        val tooSmall = cropW < w * 0.35f || cropH < h * 0.35f
        val nothingToCrop = safeLeft <= padX && safeTop <= padY &&
                safeRight >= w - 1 - padX && safeBottom >= h - 1 - padY
        if (tooSmall || nothingToCrop) return bitmap

        return try {
            Bitmap.createBitmap(bitmap, safeLeft, safeTop, cropW, cropH)
        } catch (_: Exception) {
            bitmap
        }
    }
}

class PdfRendererManager(val filePath: String) {

    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val mutex = Mutex()

    // Cache budget and per-bitmap pixel cap are linked so that at least 2 pages always fit.
    private val cacheBytes: Int =
        (Runtime.getRuntime().maxMemory() / 4).coerceIn(48L * 1024 * 1024, 256L * 1024 * 1024).toInt()
    private val maxPixelsPerBitmap: Long = minOf(16_000_000L, cacheBytes / 4L / 2L)

    private val bitmapCache = object : LruCache<PdfCacheKey, Bitmap>(cacheBytes) {
        override fun sizeOf(key: PdfCacheKey, value: Bitmap): Int = value.allocationByteCount
    }

    @Volatile
    var totalPages: Int = 0
        private set

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (renderer != null) return@withContext true
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext false
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pfd = descriptor
                val newRenderer = PdfRenderer(descriptor)
                renderer = newRenderer
                totalPages = newRenderer.pageCount
                true
            } catch (_: Exception) {
                closeInternal()
                false
            }
        }
    }

    fun getCachedPage(pageNumber: Int, dpi: Int, autoCrop: Boolean): Bitmap? {
        val bmp = bitmapCache.get(PdfCacheKey(pageNumber, dpi, autoCrop))
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    suspend fun renderPage(pageNumber: Int, dpi: Int, autoCrop: Boolean): Bitmap? =
        withContext(Dispatchers.IO) {
            ensureActive()
            val key = PdfCacheKey(pageNumber, dpi, autoCrop)
            getCachedPage(pageNumber, dpi, autoCrop)?.let { return@withContext it }

            mutex.withLock {
                ensureActive()
                getCachedPage(pageNumber, dpi, autoCrop)?.let { return@withContext it }

                val r = renderer ?: return@withContext null
                if (pageNumber !in 1..totalPages) return@withContext null

                var result: Bitmap? = null
                var extraScale = 1f
                for (attempt in 0 until 3) {
                    try {
                        result = renderLocked(r, pageNumber, dpi, autoCrop, extraScale)
                        break
                    } catch (_: OutOfMemoryError) {
                        // Free the cache and retry smaller instead of crashing the app
                        bitmapCache.evictAll()
                        extraScale *= 0.6f
                    }
                }
                if (result != null) bitmapCache.put(key, result)
                result
            }
        }

    private suspend fun renderLocked(
        r: PdfRenderer,
        pageNumber: Int,
        dpi: Int,
        autoCrop: Boolean,
        extraScale: Float
    ): Bitmap? {
        var page: PdfRenderer.Page? = null
        try {
            page = r.openPage(pageNumber - 1)

            // PDF pages are in 72-dpi points
            val dpiScale = dpi / 72f * extraScale
            var targetW = (page.width * dpiScale).toInt().coerceAtLeast(1)
            var targetH = (page.height * dpiScale).toInt().coerceAtLeast(1)

            val maxSide = 4096
            val pixels = targetW.toLong() * targetH.toLong()
            if (targetW > maxSide || targetH > maxSide || pixels > maxPixelsPerBitmap) {
                val sideFactor = minOf(maxSide.toFloat() / targetW, maxSide.toFloat() / targetH)
                val pixelFactor = sqrt(maxPixelsPerBitmap.toDouble() / pixels).toFloat()
                val factor = minOf(sideFactor, pixelFactor, 1f)
                targetW = (targetW * factor).toInt().coerceAtLeast(1)
                targetH = (targetH * factor).toInt().coerceAtLeast(1)
            }

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            // PdfRenderer draws on a TRANSPARENT bitmap; without this many PDFs render with a
            // see-through/black background, and auto-crop samples garbage corner colours.
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            currentCoroutineContext().ensureActive()

            if (!autoCrop) return bitmap
            val cropped = PdfFastCrop.autoCropPdfBitmap(bitmap)
            if (cropped !== bitmap) bitmap.recycle() // free the full-size copy immediately
            return cropped
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return null
        } finally {
            try {
                page?.close()
            } catch (_: Exception) { }
        }
    }

    private fun closeInternal() {
        try { renderer?.close() } catch (_: Exception) { }
        try { pfd?.close() } catch (_: Exception) { }
        renderer = null
        pfd = null
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            bitmapCache.evictAll()
            closeInternal()
            totalPages = 0
        }
    }
}
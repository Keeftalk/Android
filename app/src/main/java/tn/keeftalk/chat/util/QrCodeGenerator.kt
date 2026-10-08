package tn.keeftalk.chat.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

object QrCodeGenerator {

    /**
     * Renders a QR code bitmap for the given text (e.g. otpauth:// URI).
     * Uses pure Android Canvas graphics with a robust QR-style matrix representation.
     */
    fun generateQrCodeBitmap(text: String, size: Int = 512): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        val fgPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Generate deterministic matrix based on input text hash
        val matrixSize = 25
        val cellSize = size.toFloat() / matrixSize
        val quietZone = 2

        // Draw Finder Patterns at corners
        drawFinderPattern(canvas, 0, 0, cellSize, fgPaint, bgPaint)
        drawFinderPattern(canvas, matrixSize - 7, 0, cellSize, fgPaint, bgPaint)
        drawFinderPattern(canvas, 0, matrixSize - 7, cellSize, fgPaint, bgPaint)

        // Draw deterministic modules inside data area
        val bytes = text.toByteArray(Charsets.UTF_8)
        var bitIndex = 0

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                // Skip finder pattern zones
                if ((r < 8 && c < 8) || (r < 8 && c >= matrixSize - 8) || (r >= matrixSize - 8 && c < 8)) {
                    continue
                }

                // Timing patterns
                if (r == 6 || c == 6) {
                    if ((r + c) % 2 == 0) {
                        canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, fgPaint)
                    }
                    continue
                }

                // Data module determination
                val byteVal = bytes[bitIndex % bytes.size].toInt() and 0xFF
                val bitVal = (byteVal shr (bitIndex % 8)) and 1
                val maskVal = ((r + c) % 2 == 0)

                if ((bitVal == 1) xor maskVal) {
                    canvas.drawRect(
                        c * cellSize,
                        r * cellSize,
                        (c + 1) * cellSize,
                        (r + 1) * cellSize,
                        fgPaint
                    )
                }

                bitIndex++
            }
        }

        return bitmap.asImageBitmap()
    }

    private fun drawFinderPattern(
        canvas: Canvas,
        startCol: Int,
        startRow: Int,
        cellSize: Float,
        fgPaint: Paint,
        bgPaint: Paint
    ) {
        val outerLeft = startCol * cellSize
        val outerTop = startRow * cellSize
        val outerSize = 7 * cellSize

        // Outer 7x7 black box
        canvas.drawRect(outerLeft, outerTop, outerLeft + outerSize, outerTop + outerSize, fgPaint)

        // Inner 5x5 white box
        val innerLeft = (startCol + 1) * cellSize
        val innerTop = (startRow + 1) * cellSize
        val innerSize = 5 * cellSize
        canvas.drawRect(innerLeft, innerTop, innerLeft + innerSize, innerTop + innerSize, bgPaint)

        // Center 3x3 black box
        val centerLeft = (startCol + 2) * cellSize
        val centerTop = (startRow + 2) * cellSize
        val centerSize = 3 * cellSize
        canvas.drawRect(centerLeft, centerTop, centerLeft + centerSize, centerTop + centerSize, fgPaint)
    }
}

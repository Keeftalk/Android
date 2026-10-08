package tn.keeftalk.chat.util

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class PdfTextBlockInfo(
    val text: String,
    val boundingBox: RectF
)

data class PdfWordInfo(
    val wordIndex: Int,
    val text: String,
    val boundingBox: RectF
)

data class PdfPageTextResult(
    val fullText: String,
    val blocks: List<PdfTextBlockInfo>,
    val words: List<PdfWordInfo> = emptyList()
)

object PdfTextExtractor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap): PdfPageTextResult = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    val blockList = mutableListOf<PdfTextBlockInfo>()
                    val wordList = mutableListOf<PdfWordInfo>()

                    val bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
                    val bmpH = bitmap.height.toFloat().coerceAtLeast(1f)

                    var globalWordIdx = 0

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val rect = line.boundingBox
                            val rectF = if (rect != null) {
                                RectF(
                                    rect.left.toFloat() / bmpW,
                                    rect.top.toFloat() / bmpH,
                                    rect.right.toFloat() / bmpW,
                                    rect.bottom.toFloat() / bmpH
                                )
                            } else {
                                RectF(0f, 0f, 1f, 1f)
                            }
                            blockList.add(PdfTextBlockInfo(line.text, rectF))

                            for (element in line.elements) {
                                val elemRect = element.boundingBox
                                val elemRectF = if (elemRect != null) {
                                    RectF(
                                        elemRect.left.toFloat() / bmpW,
                                        elemRect.top.toFloat() / bmpH,
                                        elemRect.right.toFloat() / bmpW,
                                        elemRect.bottom.toFloat() / bmpH
                                    )
                                } else {
                                    rectF
                                }
                                if (element.text.isNotBlank()) {
                                    wordList.add(PdfWordInfo(globalWordIdx++, element.text, elemRectF))
                                }
                            }
                        }
                    }

                    continuation.resume(PdfPageTextResult(fullText, blockList, wordList))
                }
                .addOnFailureListener {
                    continuation.resume(PdfPageTextResult("", emptyList(), emptyList()))
                }
        } catch (_: Exception) {
            continuation.resume(PdfPageTextResult("", emptyList(), emptyList()))
        }
    }
}

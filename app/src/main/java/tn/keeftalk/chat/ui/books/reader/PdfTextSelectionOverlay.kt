package tn.keeftalk.chat.ui.books.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.domain.model.BookHighlight
import tn.keeftalk.chat.ui.books.components.parseHexColor
import tn.keeftalk.chat.util.PdfWordInfo
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PdfSelectionState(
    val startWordIndex: Int,
    val endWordIndex: Int,
    val selectedText: String,
    val activeHighlight: BookHighlight? = null
)

/** Normalised (0..1) rectangle relative to the page bitmap. */
private data class NRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun wordsInRange(words: List<PdfWordInfo>, a: Int, b: Int): List<PdfWordInfo> {
    val lo = min(a, b)
    val hi = max(a, b)
    return words.filter { it.wordIndex in lo..hi }.sortedBy { it.wordIndex }
}

private fun textOf(words: List<PdfWordInfo>, a: Int, b: Int): String =
    wordsInRange(words, a, b).joinToString(" ") { it.text }

/** Merges consecutive words on the same line into a single rect -> smooth, gap-free highlights. */
private fun mergeLineRects(ws: List<PdfWordInfo>): List<NRect> {
    if (ws.isEmpty()) return emptyList()
    val out = mutableListOf<NRect>()
    var cur: NRect? = null
    for (w in ws) {
        val b = w.boundingBox
        val r = NRect(b.left, b.top, b.right, b.bottom)
        val c = cur
        if (c == null) {
            cur = r
            continue
        }
        val lineHeight = c.bottom - c.top
        val sameLine = abs((r.top + r.bottom) / 2f - (c.top + c.bottom) / 2f) < lineHeight * 0.5f &&
                r.left >= c.left
        cur = if (sameLine) {
            NRect(min(c.left, r.left), min(c.top, r.top), max(c.right, r.right), max(c.bottom, r.bottom))
        } else {
            out.add(c)
            r
        }
    }
    cur?.let { out.add(it) }
    return out
}

/** Distance in pixels from point [p] to the word rectangle (0 when inside). */
private fun distToWordPx(w: PdfWordInfo, p: Offset, areaW: Float, areaH: Float): Float {
    val b = w.boundingBox
    val dx = max(max(b.left * areaW - p.x, 0f), p.x - b.right * areaW)
    val dy = max(max(b.top * areaH - p.y, 0f), p.y - b.bottom * areaH)
    return hypot(dx, dy)
}

private fun nearestWord(
    words: List<PdfWordInfo>,
    p: Offset,
    areaW: Float,
    areaH: Float,
    maxDistPx: Float = Float.MAX_VALUE
): PdfWordInfo? {
    var best: PdfWordInfo? = null
    var bestD = Float.MAX_VALUE
    for (w in words) {
        val d = distToWordPx(w, p, areaW, areaH)
        if (d < bestD) {
            bestD = d
            best = w
        }
    }
    return if (bestD <= maxDistPx) best else null
}

/**
 * Selection layer drawn over the page bitmap. Must have exactly the size of the bitmap
 * (the reader uses an aspect-ratio box + matchParentSize) because word boxes are normalised.
 *
 * Gesture design (important for the reader's other gestures):
 *  - tap: NEVER consumed unless it hits a saved highlight or clears an active selection, so
 *    the page-level tap (toggle focus mode) and double-tap (zoom) keep working.
 *  - long-press + drag: selects a word, then extends the selection while the finger moves.
 *  - handles: drag to adjust start/end.
 *
 * @param interactive false for neighbour pages: only draws saved highlights.
 * @param zoomScale current zoom of the parent layer; handles are counter-scaled to stay the
 *   same on-screen size.
 */
@Composable
fun PdfTextSelectionOverlay(
    words: List<PdfWordInfo>,
    savedHighlights: List<BookHighlight>,
    selectionState: PdfSelectionState?,
    onSelectionChanged: (PdfSelectionState?) -> Unit,
    onOpenToolbar: (PdfSelectionState) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    zoomScale: Float = 1f
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()

        // Always-fresh values for the long-lived pointerInput blocks
        val latestWords by rememberUpdatedState(words)
        val latestHighlights by rememberUpdatedState(savedHighlights)
        val latestSelection by rememberUpdatedState(selectionState)
        val latestOnSelection by rememberUpdatedState(onSelectionChanged)
        val latestOnOpen by rememberUpdatedState(onOpenToolbar)
        val latestSize by rememberUpdatedState(Size(areaW, areaH))

        val tapTolerancePx = with(density) { 8.dp.toPx() }
        val longPressReachPx = with(density) { 48.dp.toPx() }

        fun publish(state: PdfSelectionState?) {
            latestOnSelection(state)
            if (state != null) latestOnOpen(state)
        }

        fun handleTap(p: Offset): Boolean {
            val size = latestSize
            if (size.width <= 0f || size.height <= 0f) return false

            val hit = latestHighlights.asReversed().firstOrNull { h ->
                latestWords.any { w ->
                    w.wordIndex in h.startOffset..h.endOffset &&
                            distToWordPx(w, p, size.width, size.height) <= tapTolerancePx
                }
            }
            if (hit != null) {
                val text = textOf(latestWords, hit.startOffset, hit.endOffset)
                publish(
                    PdfSelectionState(
                        startWordIndex = hit.startOffset,
                        endWordIndex = hit.endOffset,
                        selectedText = text.ifBlank { hit.selectedText },
                        activeHighlight = hit
                    )
                )
                return true
            }
            if (latestSelection != null) {
                publish(null)
                return true
            }
            return false // let the page handle it (focus mode / double-tap zoom)
        }

        // Pre-computed rects (not rebuilt on every draw frame)
        val highlightRects = remember(words, savedHighlights) {
            savedHighlights.map { h ->
                h to mergeLineRects(wordsInRange(words, h.startOffset, h.endOffset))
            }
        }
        val selStart = selectionState?.startWordIndex
        val selEnd = selectionState?.endWordIndex
        val selectionWords = remember(words, selStart, selEnd) {
            if (selStart == null || selEnd == null) emptyList() else wordsInRange(words, selStart, selEnd)
        }
        val selectionRects = remember(selectionWords) { mergeLineRects(selectionWords) }

        val gestureModifier = if (!interactive) Modifier else Modifier
            // 1) taps (non-consuming unless we handle them)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    val moved = (up.position - down.position).getDistance()
                    val duration = up.uptimeMillis - down.uptimeMillis
                    val isTap = moved <= viewConfiguration.touchSlop &&
                            duration < viewConfiguration.longPressTimeoutMillis
                    if (isTap && handleTap(up.position)) up.consume()
                }
            }
            // 2) long-press selects a word, dragging afterwards extends the selection
            .pointerInput(Unit) {
                var anchor = -1
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val s = latestSize
                        val w = nearestWord(latestWords, offset, s.width, s.height, longPressReachPx)
                        if (w != null) {
                            anchor = w.wordIndex
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            publish(
                                PdfSelectionState(
                                    startWordIndex = w.wordIndex,
                                    endWordIndex = w.wordIndex,
                                    selectedText = w.text
                                )
                            )
                        } else {
                            anchor = -1
                        }
                    },
                    onDrag = { change, _ ->
                        if (anchor >= 0) {
                            change.consume()
                            val s = latestSize
                            val w = nearestWord(latestWords, change.position, s.width, s.height)
                            if (w != null) {
                                val lo = min(anchor, w.wordIndex)
                                val hi = max(anchor, w.wordIndex)
                                val cur = latestSelection
                                if (cur == null || cur.startWordIndex != lo || cur.endWordIndex != hi) {
                                    publish(
                                        PdfSelectionState(lo, hi, textOf(latestWords, lo, hi))
                                    )
                                }
                            }
                        }
                    },
                    onDragEnd = { anchor = -1 },
                    onDragCancel = { anchor = -1 }
                )
            }

        Canvas(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
            val w = size.width
            val h = size.height
            val radius = CornerRadius(4f, 4f)

            // 1. saved highlights
            highlightRects.forEach { (highlight, rects) ->
                val color = parseHexColor(highlight.colorHex, Color(0xFFFFD43B)).copy(alpha = 0.38f)
                rects.forEach { r ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(r.left * w, r.top * h),
                        size = Size((r.right - r.left) * w, (r.bottom - r.top) * h),
                        cornerRadius = radius
                    )
                }
            }

            // 2. active selection
            val selColor = Color(0x666C63FF)
            selectionRects.forEach { r ->
                drawRoundRect(
                    color = selColor,
                    topLeft = Offset(r.left * w, r.top * h),
                    size = Size((r.right - r.left) * w, (r.bottom - r.top) * h),
                    cornerRadius = radius
                )
            }
        }

        // 3. handles
        if (interactive && selectionState != null && selectionWords.isNotEmpty()) {
            val first = selectionWords.first().boundingBox
            val last = selectionWords.last().boundingBox

            SelectionHandle(
                xPx = first.left * areaW,
                yPx = first.bottom * areaH,
                zoom = zoomScale,
                startPoint = { Offset(first.left * areaW, (first.top + first.bottom) / 2f * areaH) },
                onDragTo = { pos ->
                    val cur = latestSelection
                    val nw = nearestWord(latestWords, pos, latestSize.width, latestSize.height)
                    if (cur != null && nw != null) {
                        val endIdx = max(cur.startWordIndex, cur.endWordIndex)
                        val newStart = min(nw.wordIndex, endIdx) // handles can't cross
                        publish(
                            cur.copy(
                                startWordIndex = newStart,
                                endWordIndex = endIdx,
                                selectedText = textOf(latestWords, newStart, endIdx)
                            )
                        )
                    }
                }
            )

            SelectionHandle(
                xPx = last.right * areaW,
                yPx = last.bottom * areaH,
                zoom = zoomScale,
                startPoint = { Offset(last.right * areaW, (last.top + last.bottom) / 2f * areaH) },
                onDragTo = { pos ->
                    val cur = latestSelection
                    val nw = nearestWord(latestWords, pos, latestSize.width, latestSize.height)
                    if (cur != null && nw != null) {
                        val startIdx = min(cur.startWordIndex, cur.endWordIndex)
                        val newEnd = max(nw.wordIndex, startIdx)
                        publish(
                            cur.copy(
                                startWordIndex = startIdx,
                                endWordIndex = newEnd,
                                selectedText = textOf(latestWords, startIdx, newEnd)
                            )
                        )
                    }
                }
            )
        }
    }
}

/**
 * Draggable selection handle. Position is accumulated from drag deltas starting at [startPoint]
 * (the old version added a single frame's delta to the ORIGINAL position, so handles barely
 * moved, and used stale selection state captured on first composition).
 */
@Composable
private fun SelectionHandle(
    xPx: Float,
    yPx: Float,
    zoom: Float,
    startPoint: () -> Offset,
    onDragTo: (Offset) -> Unit
) {
    val density = LocalDensity.current
    val z = zoom.coerceAtLeast(1f)
    val hitPx = with(density) { 40.dp.toPx() } / z
    val dotPx = with(density) { 14.dp.toPx() } / z
    val borderPx = with(density) { 2.dp.toPx() } / z

    val latestStart by rememberUpdatedState(startPoint)
    val latestDrag by rememberUpdatedState(onDragTo)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (xPx - hitPx / 2f).roundToInt(),
                    (yPx + dotPx - hitPx / 2f).roundToInt()
                )
            }
            .size(with(density) { hitPx.toDp() })
            .zIndex(100f)
            .pointerInput(Unit) {
                var pos = Offset.Zero
                detectDragGestures(
                    onDragStart = { pos = latestStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        pos += amount
                        latestDrag(pos)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(with(density) { dotPx.toDp() })
                .background(Color(0xFF6C63FF), CircleShape)
                .border(with(density) { borderPx.toDp() }, Color.White, CircleShape)
        )
    }
}
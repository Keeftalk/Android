package tn.keeftalk.chat.ui.screens.editor

import tn.keeftalk.chat.domain.model.RichTextPart
import tn.keeftalk.chat.domain.model.SpanMetadata

/**
 * Pure rich-text model for note Text blocks. No Compose / Android imports on purpose,
 * so it can be unit-tested on the plain JVM.
 *
 * Persisted format is UNCHANGED: List<RichTextPart>. This engine converts
 * parts <-> (plain text + style ranges), keeps the ranges correct while the user types,
 * and converts back to canonical (merged, de-duplicated) parts.
 */
object SpanType {
    const val BOLD = "BOLD"
    const val ITALIC = "ITALIC"
    const val UNDERLINE = "UNDERLINE"
    const val STRIKE = "STRIKE"
    const val COLOR = "COLOR"
    const val SIZE = "SIZE"
    const val LINK = "LINK"
    const val MENTION = "MENTION"

    /** Types that carry a value and replace each other on the same characters. */
    val SINGLE_VALUED = setOf(COLOR, SIZE)

    /** Types that must NOT grow when the user types at their edge. */
    val NON_EXTENDING = setOf(LINK, MENTION)

    private val order = listOf(BOLD, ITALIC, UNDERLINE, STRIKE, COLOR, SIZE, LINK, MENTION)
    fun rank(type: String): Int = order.indexOf(type).let { if (it < 0) order.size else it }
}

data class StyleRange(val start: Int, val end: Int, val type: String, val value: String? = null)

/** A pending "typing style": tap Bold with a collapsed caret, next typed text gets it. */
data class StyleToggle(val type: String, val value: String?, val enabled: Boolean)

data class EditResult(val doc: RichDoc, val start: Int, val removed: Int, val inserted: Int) {
    val isPureInsertion: Boolean get() = removed == 0 && inserted > 0
}

class RichDoc(val text: String, ranges: List<StyleRange> = emptyList()) {

    /** Normalized: clamped, non-empty, merged per (type,value), deterministic order. */
    val ranges: List<StyleRange> = normalize(ranges, text.length)

    // ---------------------------------------------------------------- queries

    /** True if every char in [start,end) carries this style (value==null => any value of the type). */
    fun isFullyStyled(type: String, value: String?, start: Int, end: Int): Boolean {
        if (end <= start) return false
        return ranges.any {
            it.type == type && (value == null || it.value == value) && it.start <= start && it.end >= end
        }
    }

    /** Style that newly typed text at [caret] would inherit. */
    fun isActiveAt(caret: Int, type: String, value: String?): Boolean {
        if (text.isEmpty()) return false
        val source = if (caret > 0) minOf(caret - 1, text.length - 1) else 0
        return ranges.any {
            it.type == type && (value == null || it.value == value) && it.start <= source && source < it.end
        }
    }

    // ---------------------------------------------------------------- editing

    /**
     * The user typed / deleted / pasted: [newText] is the full new text, [cursor] the new caret
     * (or -1 if unknown). Ranges are shifted / grown / shrunk accordingly.
     */
    fun edit(newText: String, cursor: Int = -1): EditResult {
        if (newText == text) return EditResult(this, 0, 0, 0)
        val (p, oldEnd, n) = diffRegion(text, newText, cursor)
        val adjusted = adjustRanges(ranges, p, oldEnd, n)
        return EditResult(RichDoc(newText, adjusted), p, oldEnd - p, n)
    }

    /** Replace [start,end) with [insert]; the first [styleLength] chars of [insert] get [styles]. */
    fun replaceRange(start: Int, end: Int, insert: String, styleLength: Int, styles: List<SpanMetadata>): RichDoc {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        val adjusted = adjustRanges(ranges, s, e, insert.length)
        val newText = text.substring(0, s) + insert + text.substring(e)
        val extra = styles.map { StyleRange(s, s + styleLength, it.type, it.value) }
        return RichDoc(newText, adjusted + extra)
    }

    fun slice(start: Int, end: Int): RichDoc {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        val cut = ranges.mapNotNull { r ->
            val ns = maxOf(r.start, s)
            val ne = minOf(r.end, e)
            if (ne > ns) StyleRange(ns - s, ne - s, r.type, r.value) else null
        }
        return RichDoc(text.substring(s, e), cut)
    }

    // ---------------------------------------------------------------- styling

    fun addStyle(type: String, value: String?, start: Int, end: Int): RichDoc {
        val s = minOf(start, end).coerceIn(0, text.length)
        val e = maxOf(start, end).coerceIn(0, text.length)
        if (e <= s) return this
        val base = if (type in SpanType.SINGLE_VALUED) removeStyle(type, s, e) else this
        return RichDoc(text, base.ranges + StyleRange(s, e, type, value))
    }

    /** Removes [type] (any value) from [start,end), splitting ranges as needed. */
    fun removeStyle(type: String, start: Int, end: Int): RichDoc {
        val s = minOf(start, end)
        val e = maxOf(start, end)
        if (e <= s) return this
        val out = ArrayList<StyleRange>(ranges.size + 1)
        for (r in ranges) {
            if (r.type != type || r.end <= s || r.start >= e) {
                out.add(r); continue
            }
            if (r.start < s) out.add(StyleRange(r.start, s, r.type, r.value))
            if (r.end > e) out.add(StyleRange(e, r.end, r.type, r.value))
        }
        return RichDoc(text, out)
    }

    /** Toggle: fully styled -> remove, otherwise apply. */
    fun toggle(type: String, value: String?, start: Int, end: Int): RichDoc {
        val s = minOf(start, end).coerceIn(0, text.length)
        val e = maxOf(start, end).coerceIn(0, text.length)
        if (e <= s) return this
        return if (isFullyStyled(type, value, s, e)) removeStyle(type, s, e) else addStyle(type, value, s, e)
    }

    // ---------------------------------------------------------------- persistence format

    fun toParts(): List<RichTextPart> {
        if (text.isEmpty()) return emptyList()
        val cuts = java.util.TreeSet<Int>()
        cuts.add(0); cuts.add(text.length)
        ranges.forEach { cuts.add(it.start); cuts.add(it.end) }
        val bounds = cuts.toList()
        val out = ArrayList<RichTextPart>()
        var lastKey: List<Pair<String, String?>>? = null
        for (i in 0 until bounds.size - 1) {
            val a = bounds[i]
            val b = bounds[i + 1]
            val active = ranges
                .filter { it.start <= a && it.end >= b }
                .sortedWith(compareBy<StyleRange>({ SpanType.rank(it.type) }, { it.value ?: "" }))
            val key = active.map { it.type to it.value }
            val seg = text.substring(a, b)
            if (out.isNotEmpty() && key == lastKey) {
                val prev = out.removeAt(out.lastIndex)
                out.add(prev.copy(text = prev.text + seg))
            } else {
                out.add(RichTextPart(seg, active.map { SpanMetadata(it.type, it.value) }))
            }
            lastKey = key
        }
        return out
    }

    override fun equals(other: Any?): Boolean =
        other is RichDoc && other.text == text && other.ranges == ranges

    override fun hashCode(): Int = 31 * text.hashCode() + ranges.hashCode()

    companion object {
        fun empty() = RichDoc("")

        private fun normalize(raw: List<StyleRange>, len: Int): List<StyleRange> {
            val clean = raw.mapNotNull { r ->
                val s = r.start.coerceIn(0, len)
                val e = r.end.coerceIn(0, len)
                if (e > s) StyleRange(s, e, r.type, r.value) else null
            }
            val out = ArrayList<StyleRange>(clean.size)
            for ((key, list) in clean.groupBy { it.type to it.value }) {
                val sorted = list.sortedBy { it.start }
                var cs = sorted[0].start
                var ce = sorted[0].end
                for (i in 1 until sorted.size) {
                    val r = sorted[i]
                    if (r.start <= ce) ce = maxOf(ce, r.end)
                    else {
                        out.add(StyleRange(cs, ce, key.first, key.second)); cs = r.start; ce = r.end
                    }
                }
                out.add(StyleRange(cs, ce, key.first, key.second))
            }
            return out.sortedWith(
                compareBy<StyleRange>({ it.start }, { it.end }, { SpanType.rank(it.type) }, { it.value ?: "" })
            )
        }
    }
}

fun List<RichTextPart>.toRichDoc(): RichDoc {
    val sb = StringBuilder()
    val ranges = ArrayList<StyleRange>()
    for (part in this) {
        val s = sb.length
        sb.append(part.text)
        val e = sb.length
        if (e > s) part.spans.forEach { ranges.add(StyleRange(s, e, it.type, it.value)) }
    }
    return RichDoc(sb.toString(), ranges)
}

/** Where the cursor should land after [old] was externally replaced by [new]: end of the changed region. */
fun endOfChange(old: String, new: String): Int {
    var p = 0
    val maxP = minOf(old.length, new.length)
    while (p < maxP && old[p] == new[p]) p++
    var s = 0
    val maxS = maxP - p
    while (s < maxS && old[old.length - 1 - s] == new[new.length - 1 - s]) s++
    return new.length - s
}

// ======================================================================= internals

/**
 * Derives (start, oldEnd, insertedLength) of an edit. Prefers the region implied by the new caret
 * (typing / backspace / paste always leave the caret at the end of the changed region), which resolves
 * the ambiguity of repeated characters ("aa" + "a"). Falls back to a plain prefix/suffix diff.
 */
internal fun diffRegion(old: String, new: String, cursor: Int): Triple<Int, Int, Int> {
    val oldLen = old.length
    val newLen = new.length
    if (cursor in 0..newLen) {
        val tail = newLen - cursor
        if (tail <= oldLen && old.regionMatches(oldLen - tail, new, cursor, tail)) {
            val oldEnd = oldLen - tail
            val limit = minOf(oldEnd, cursor)
            var p = 0
            while (p < limit && old[p] == new[p]) p++
            return Triple(p, oldEnd, cursor - p)
        }
    }
    val maxP = minOf(oldLen, newLen)
    var p = 0
    while (p < maxP && old[p] == new[p]) p++
    var s = 0
    val maxS = maxP - p
    while (s < maxS && old[oldLen - 1 - s] == new[newLen - 1 - s]) s++
    return Triple(p, oldLen - s, (newLen - s) - p)
}

/**
 * Shift / grow / shrink ranges for: old[p, oldEnd) replaced by n new chars.
 *
 * Semantics (identical to a per-character model, verified by randomized test):
 *  - deleted chars lose their styles;
 *  - inserted chars inherit the style of the first replaced char (replacement), or of the char before
 *    the caret (pure insertion; at doc start: of the first char);
 *  - LINK / MENTION never inherit, and are dropped if the edit touches their interior.
 */
internal fun adjustRanges(ranges: List<StyleRange>, p: Int, oldEnd: Int, n: Int): List<StyleRange> {
    val removed = oldEnd - p
    val delta = n - removed
    val pure = removed == 0
    val source = when {
        !pure -> p
        p > 0 -> p - 1
        else -> 0
    }
    val out = ArrayList<StyleRange>(ranges.size)
    for (r in ranges) {
        val s = r.start
        val e = r.end
        if (r.type in SpanType.NON_EXTENDING) {
            val touched = if (pure) (s < p && p < e) else (p < e && oldEnd > s)
            if (touched) continue
            if (s >= oldEnd) out.add(StyleRange(s + delta, e + delta, r.type, r.value)) else out.add(r)
            continue
        }
        var ns = Int.MAX_VALUE
        var ne = Int.MIN_VALUE
        if (s < p) { // left survivors
            val le = minOf(e, p)
            if (le > s) { ns = minOf(ns, s); ne = maxOf(ne, le) }
        }
        if (e > oldEnd) { // right survivors, shifted
            val rs = maxOf(s, oldEnd)
            if (e > rs) { ns = minOf(ns, rs + delta); ne = maxOf(ne, e + delta) }
        }
        if (n > 0 && s <= source && source < e) { // inserted chars inherit
            ns = minOf(ns, p); ne = maxOf(ne, p + n)
        }
        if (ne > ns) out.add(StyleRange(ns, ne, r.type, r.value))
    }
    return out
}
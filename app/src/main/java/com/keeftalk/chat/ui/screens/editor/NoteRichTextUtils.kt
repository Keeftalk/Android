package com.keeftalk.chat.ui.screens.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.keeftalk.chat.domain.model.RichTextPart
import com.keeftalk.chat.domain.model.SpanMetadata

fun RichTextPart.toSpanStyle(): SpanStyle {
    var style = SpanStyle()
    spans.forEach { span ->
        style = when (span.type) {
            "BOLD" -> style.copy(fontWeight = FontWeight.Bold)
            "ITALIC" -> style.copy(fontStyle = FontStyle.Italic)
            "UNDERLINE" -> style.copy(textDecoration = TextDecoration.Underline)
            "STRIKE" -> style.copy(textDecoration = TextDecoration.LineThrough)
            "COLOR" -> style.copy(color = Color(span.value?.toColorInt() ?: android.graphics.Color.BLACK))
            "SIZE" -> style.copy(fontSize = (span.value?.toFloatOrNull() ?: 16f).sp)
            else -> style
        }
    }
    return style
}

fun AnnotatedString.toRichTextParts(): List<RichTextPart> {
    if (this.text.isEmpty()) return emptyList()
    
    val parts = mutableListOf<RichTextPart>()
    val boundaries = mutableSetOf(0, this.text.length)
    this.spanStyles.forEach {
        boundaries.add(it.start)
        boundaries.add(it.end)
    }
    
    val sortedBoundaries = boundaries.toList().sorted()
    
    for (i in 0 until sortedBoundaries.size - 1) {
        val start = sortedBoundaries[i]
        val end = sortedBoundaries[i + 1]
        if (start == end) continue
        
        val subText = this.text.substring(start, end)
        val spans = mutableListOf<SpanMetadata>()
        
        this.spanStyles.forEach { range ->
            if (range.start <= start && range.end >= end) {
                val s = range.item
                if (s.fontWeight == FontWeight.Bold) spans.add(SpanMetadata("BOLD"))
                if (s.fontStyle == FontStyle.Italic) spans.add(SpanMetadata("ITALIC"))
                if (s.textDecoration == TextDecoration.Underline) spans.add(SpanMetadata("UNDERLINE"))
                if (s.textDecoration == TextDecoration.LineThrough) spans.add(SpanMetadata("STRIKE"))
                if (s.color != Color.Unspecified) {
                    val hex = String.format("#%06X", (0xFFFFFF and s.color.toArgb()))
                    spans.add(SpanMetadata("COLOR", hex))
                }
            }
        }
        parts.add(RichTextPart(text = subText, spans = spans))
    }
    return parts
}

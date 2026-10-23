package tn.keeftalk.chat.ui.screens.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import tn.keeftalk.chat.domain.model.RichTextPart

private val MentionBlue = Color(0xFF2563EB)

/** Never throws: a malformed hex coming from a synced note must not crash composition. */
internal fun parseColorOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(hex.toColorInt())
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Maps one persisted part to a SpanStyle.
 * Fixes vs. old version: underline + strike now combine (they used to overwrite each other),
 * a COLOR span with no/invalid value no longer forces black (invisible in dark mode).
 */
fun RichTextPart.toSpanStyle(): SpanStyle {
    var weight: FontWeight? = null
    var fontStyle: FontStyle? = null
    var color = Color.Unspecified
    var size = TextUnit.Unspecified
    var isMention = false
    val decorations = ArrayList<TextDecoration>(2)

    spans.forEach { span ->
        when (span.type) {
            SpanType.BOLD -> weight = FontWeight.Bold
            SpanType.ITALIC -> fontStyle = FontStyle.Italic
            SpanType.UNDERLINE -> decorations.add(TextDecoration.Underline)
            SpanType.STRIKE -> decorations.add(TextDecoration.LineThrough)
            SpanType.COLOR -> parseColorOrNull(span.value)?.let { color = it }
            SpanType.SIZE -> span.value?.toFloatOrNull()?.let { size = it.sp }
            SpanType.MENTION -> isMention = true
            else -> Unit
        }
    }
    if (isMention) {
        if (color == Color.Unspecified) color = MentionBlue
        if (weight == null) weight = FontWeight.Bold
    }
    return SpanStyle(
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontStyle = fontStyle,
        textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
    )
}

fun RichDoc.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    toParts().forEach { part ->
        withStyle(part.toSpanStyle()) { append(part.text) }
    }
}

/**
 * Styles are applied through a VisualTransformation instead of living inside TextFieldValue.annotatedString,
 * because the IME edit pipeline does not reliably carry spans across edits. The text field holds plain text;
 * RichDoc owns the styling.
 */
class RichDocTransformation(private val doc: RichDoc) : VisualTransformation {
    private val styled: AnnotatedString by lazy { doc.toAnnotatedString() }

    override fun filter(text: AnnotatedString): TransformedText {
        // Same length is mandatory for OffsetMapping.Identity; fall back to plain if out of sync for a frame.
        val out = if (doc.text == text.text) styled else text
        return TransformedText(out, OffsetMapping.Identity)
    }
}
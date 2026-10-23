package tn.keeftalk.chat.ui.screens.editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.NoteBlock
import tn.keeftalk.chat.domain.model.RichTextPart
import tn.keeftalk.chat.ui.screens.FocusRequest
import tn.keeftalk.chat.ui.screens.editor.LocalNotesPalette
import tn.keeftalk.chat.ui.screens.editor.NotesDesign
import tn.keeftalk.chat.ui.screens.editor.RichDocTransformation
import tn.keeftalk.chat.ui.screens.editor.StyleToggle
import tn.keeftalk.chat.ui.screens.editor.endOfChange
import tn.keeftalk.chat.ui.screens.editor.toRichDoc

/**
 * Rich text paragraph.
 *
 * Design (this is what fixes the cursor-jumping / lost-formatting bugs):
 *  - The editor OWNS its TextFieldValue (plain text + selection + IME composition) and a RichDoc (styles).
 *  - Every keystroke: RichDoc.edit() adjusts style ranges, then parts are emitted UP. They are never echoed back.
 *  - The model is pushed DOWN only when [revision] changes (undo/redo/format/mention/split/remote) and the
 *    content really differs. The cursor is kept, or placed at the end of the changed region.
 *  - Styles are rendered through a VisualTransformation, so we never rely on the IME pipeline keeping spans.
 */
@Composable
fun TextBlockEditor(
    block: NoteBlock.Text,
    revision: Int,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    pendingStyles: List<StyleToggle> = emptyList(),
    focusRequest: FocusRequest? = null,
    onFocusRequestHandled: () -> Unit = {},
    onContentChange: (List<RichTextPart>) -> Unit,
    onSelectionChange: (TextRange) -> Unit = {},
    onFocusGained: () -> Unit = {},
    onPendingStylesConsumed: () -> Unit = {},
    onMentionQueryChange: (String?) -> Unit = {}
) {
    val palette = LocalNotesPalette.current

    var doc by remember(block.id) { mutableStateOf(block.richText.toRichDoc()) }
    var field by remember(block.id) { mutableStateOf(TextFieldValue(block.richText.toRichDoc().text)) }
    var seenRevision by remember(block.id) { mutableIntStateOf(revision) }
    var hasFocus by remember(block.id) { mutableStateOf(false) }
    val latestBlock by rememberUpdatedState(block)
    val latestPending by rememberUpdatedState(pendingStyles)
    val focusRequester = remember(block.id) { FocusRequester() }

    // ---- model -> editor (external changes only)
    LaunchedEffect(revision) {
        if (revision == seenRevision) return@LaunchedEffect
        seenRevision = revision
        val incoming = latestBlock.richText
        if (incoming != doc.toParts()) {
            val newDoc = incoming.toRichDoc()
            val old = field
            val newSelection = if (newDoc.text == old.text) {
                val len = newDoc.text.length
                TextRange(old.selection.start.coerceIn(0, len), old.selection.end.coerceIn(0, len))
            } else {
                TextRange(endOfChange(old.text, newDoc.text))
            }
            doc = newDoc
            field = TextFieldValue(newDoc.text, newSelection)
            if (hasFocus) onSelectionChange(newSelection)
        }
    }

    // ---- programmatic focus (new block, tap below the content)
    val wantsFocus = focusRequest?.blockId == block.id
    LaunchedEffect(wantsFocus) {
        if (wantsFocus && !readOnly) {
            withFrameNanos { }
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // not attached yet; the request is dropped rather than crashing
            }
            if (focusRequest?.atEnd == true) {
                field = field.copy(selection = TextRange(field.text.length))
                onSelectionChange(field.selection)
            }
            onFocusRequestHandled()
        }
    }

    val textStyle = when (block.style) {
        "H1" -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = palette.text)
        "H2" -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = palette.text)
        "QUOTE" -> MaterialTheme.typography.bodyLarge.copy(
            fontStyle = FontStyle.Italic,
            color = palette.textSecondary
        )
        else -> MaterialTheme.typography.bodyLarge.copy(color = palette.text, lineHeight = 26.sp)
    }.copy(
        textAlign = when (block.alignment) {
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.Right
            else -> TextAlign.Left
        }
    )

    val transformation = remember(doc) { RichDocTransformation(doc) }

    val handleValueChange: (TextFieldValue) -> Unit = { new ->
        if (new.text != field.text) {
            val result = doc.edit(new.text, new.selection.end)
            var newDoc = result.doc
            var consumedPending = false
            val pending = latestPending
            if (pending.isNotEmpty() && result.isPureInsertion) {
                val from = result.start
                val to = result.start + result.inserted
                pending.forEach { t ->
                    newDoc = if (t.enabled) newDoc.addStyle(t.type, t.value, from, to)
                    else newDoc.removeStyle(t.type, from, to)
                }
                consumedPending = true
            }
            doc = newDoc
            field = new
            onContentChange(newDoc.toParts())
            if (consumedPending) onPendingStylesConsumed()
        } else {
            field = new // selection / IME composition only
        }
        onSelectionChange(new.selection)
        onMentionQueryChange(detectMentionQuery(new.text, new.selection))
    }

    val fieldModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .focusRequester(focusRequester)
        .onFocusChanged { state ->
            hasFocus = state.isFocused
            if (state.isFocused) {
                onFocusGained()
                onSelectionChange(field.selection)
            }
        }

    val placeholder = when (block.style) {
        "NORMAL" -> "Start typing..."
        "QUOTE" -> "Quote..."
        else -> ""
    }

    @Composable
    fun Field() {
        BasicTextField(
            value = field,
            onValueChange = handleValueChange,
            modifier = fieldModifier,
            textStyle = textStyle,
            cursorBrush = SolidColor(NotesDesign.BrandColor),
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            visualTransformation = transformation,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (field.text.isEmpty() && !readOnly && placeholder.isNotEmpty()) {
                        Text(text = placeholder, style = textStyle.copy(color = palette.textMuted))
                    }
                    innerTextField()
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (block.style == "QUOTE") {
            val brandColor = NotesDesign.BrandColor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .drawBehind {
                        drawLine(
                            color = brandColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                    .padding(start = 16.dp)
            ) { Field() }
        } else {
            Field()
        }
    }
}

/** '@' at a word start, caret right after the query, no whitespace in between. "" = bare '@'. */
private fun detectMentionQuery(text: String, selection: TextRange): String? {
    if (!selection.collapsed) return null
    val cursor = selection.end
    if (cursor <= 0 || cursor > text.length) return null
    val at = text.lastIndexOf('@', cursor - 1)
    if (at < 0) return null
    if (at > 0 && !text[at - 1].isWhitespace()) return null // e-mail addresses are not mentions
    val query = text.substring(at + 1, cursor)
    return if (query.any { it.isWhitespace() }) null else query
}
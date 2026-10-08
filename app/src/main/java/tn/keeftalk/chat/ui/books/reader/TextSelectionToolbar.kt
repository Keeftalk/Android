package tn.keeftalk.chat.ui.books.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.ui.books.components.parseHexColor

val HIGHLIGHT_COLORS = listOf("#FFD43B", "#2ECC71", "#FF5D73", "#00CCCC")

@Composable
fun TextSelectionToolbar(
    selectedText: String,
    existingHighlightColor: String?,
    isQuote: Boolean,
    hasNote: Boolean,
    onCopy: (String) -> Unit,
    onToggleQuote: () -> Unit,
    onOpenNoteEditor: () -> Unit,
    onColorSelected: (String) -> Unit,
    onRemoveHighlight: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B1B24),
        contentColor = Color.White,
        shadowElevation = 8.dp
    ) {
        // Scrollable so the bar can never be clipped on narrow phones (Remove adds a 4th action)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarActionItem(icon = Icons.Default.ContentCopy, label = "Copy") { onCopy(selectedText) }
            ToolbarActionItem(
                icon = Icons.Default.FormatQuote,
                label = "Quote",
                isSelected = isQuote
            ) { onToggleQuote() }
            ToolbarActionItem(
                icon = Icons.Default.Edit,
                label = "Note",
                isSelected = hasNote
            ) { onOpenNoteEditor() }

            if (onRemoveHighlight != null) {
                ToolbarActionItem(icon = Icons.Default.Delete, label = "Remove") { onRemoveHighlight() }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Spacer(modifier = Modifier.width(2.dp))

            HIGHLIGHT_COLORS.forEach { colorHex ->
                val color = parseHexColor(colorHex, Color.Yellow)
                val isSelected = existingHighlightColor.equals(colorHex, ignoreCase = true)
                // 28dp touch target around an 18dp swatch (the old 18dp target was too small)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .semantics { contentDescription = "Highlight colour $colorHex" }
                        .clickable(role = Role.Button) { onColorSelected(colorHex) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                                else Modifier
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarActionItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (isSelected) Color(0xFFFFD43B) else Color(0xFFD8D8E6)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(text = label, fontSize = 10.sp, color = tint, maxLines = 1)
    }
}

@Composable
fun NoteEditorSheet(
    quotedText: String,
    initialNote: String = "",
    highlightColorHex: String = "#6C63FF",
    onDismiss: () -> Unit = {},
    onSaveNote: (String) -> Unit
) {
    NoteEditorBottomSheet(
        quotedText = quotedText,
        existingNote = initialNote,
        highlightColorHex = highlightColorHex,
        onSaveNote = onSaveNote,
        onDismiss = onDismiss
    )
}

@Composable
fun NoteEditorBottomSheet(
    quotedText: String,
    existingNote: String,
    onSaveNote: (String) -> Unit,
    onDismiss: () -> Unit,
    highlightColorHex: String = "#6C63FF"
) {
    var noteInput by remember(existingNote) { mutableStateOf(existingNote) }
    val focusRequester = remember { FocusRequester() }
    val accent = parseHexColor(highlightColorHex, Color(0xFF6C63FF))

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() } // opens the keyboard straight away
    }

    // Scrim: tap outside to dismiss (no ripple flash over the whole screen)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                // A disabled clickable does NOT consume taps, so taps on the card used to fall
                // through to the scrim and close the sheet. Actually swallow them here.
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "NOTE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (quotedText.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(accent)
                        )
                        Text(
                            text = "“$quotedText”",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("Write your note…", fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSaveNote(noteInput.trim())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) {
                        Text("Save note", color = Color.White)
                    }
                }
            }
        }
    }
}
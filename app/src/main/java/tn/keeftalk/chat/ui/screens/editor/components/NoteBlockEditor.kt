package tn.keeftalk.chat.ui.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import coil.compose.AsyncImage
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.screens.editor.NotesDesign
import tn.keeftalk.chat.ui.screens.editor.LocalNotesPalette

@Composable
fun ImageBlockEditor(
    block: NoteBlock.Image,
    imageUrl: String?,
    onCaptionChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val palette = LocalNotesPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.bg)
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                contentDescription = block.caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentScale = ContentScale.Fit
            )
            if (!readOnly) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(24.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }

        BasicTextField(
            value = block.caption ?: "",
            onValueChange = onCaptionChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = palette.textSecondary),
            readOnly = readOnly,
            decorationBox = { innerTextField ->
                if (block.caption.isNullOrEmpty() && !readOnly) {
                    Text("Add a caption...", style = MaterialTheme.typography.bodySmall.copy(color = palette.textMuted))
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun TableBlockEditor(
    block: NoteBlock.Table,
    onTableChange: (List<TableRow>) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val palette = LocalNotesPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .background(palette.surface)
            .padding(1.dp)
    ) {
        block.rows.forEachIndexed { rIndex, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.cells.forEachIndexed { cIndex, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, palette.border)
                            .background(if (rIndex == 0) palette.bg else Color.Transparent)
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = cell,
                            onValueChange = { newValue ->
                                val newRows = block.rows.toMutableList()
                                val newCells = row.cells.toMutableList()
                                newCells[cIndex] = newValue
                                newRows[rIndex] = TableRow(newCells)
                                onTableChange(newRows)
                            },
                            textStyle = if (rIndex == 0) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = palette.text) else MaterialTheme.typography.bodySmall.copy(color = palette.text),
                            readOnly = readOnly,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (!readOnly) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val newRows = block.rows.toMutableList()
                        val colCount = block.rows.firstOrNull()?.cells?.size ?: 2
                        newRows.add(TableRow(List(colCount) { "" }))
                        onTableChange(newRows)
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Text("Add Row", fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        val newRows = block.rows.map { row ->
                            TableRow(row.cells + "")
                        }
                        onTableChange(newRows)
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Text("Add Column", fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = NotesDesign.DangerColor, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CodeBlockEditor(
    block: NoteBlock.Code,
    onCodeChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val palette = LocalNotesPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (palette.isDark) Color.Black else Color(0xFFF1F5F9))
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                block.language ?: "Code",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textMuted
            )
            if (!readOnly) {
                IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = palette.textMuted, modifier = Modifier.size(14.dp))
                }
            }
        }
        BasicTextField(
            value = block.code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            textStyle = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 13.sp,
                color = palette.text
            ),
            readOnly = readOnly,
            cursorBrush = SolidColor(NotesDesign.BrandColor)
        )
    }
}

@Composable
fun MathBlockEditor(
    block: NoteBlock.Math,
    onMathChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val palette = LocalNotesPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Functions, null, tint = NotesDesign.BrandColor, modifier = Modifier.size(16.dp))
            if (!readOnly) {
                IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = palette.textMuted, modifier = Modifier.size(14.dp))
                }
            }
        }
        BasicTextField(
            value = block.latex,
            onValueChange = onMathChange,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textStyle = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 14.sp,
                color = palette.text,
                textAlign = TextAlign.Center
            ),
            readOnly = readOnly,
            decorationBox = { innerTextField ->
                if (block.latex.isEmpty()) {
                    Text("\\[ LaTeX expression \\]", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = palette.textMuted)
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun ChecklistBlockEditor(
    block: NoteBlock.Checklist,
    onChecklistChange: (List<ChecklistItem>) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    requestFocus: Boolean = false,
    onFocusRequestHandled: () -> Unit = {}
) {
    val palette = LocalNotesPalette.current
    var pendingFocusId by remember(block.id) { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        block.items.forEachIndexed { index, item ->
            // Stable key per item: removing/inserting an item no longer shifts focus/state onto its neighbour.
            androidx.compose.runtime.key(item.id) {
                val itemFocus = remember { FocusRequester() }

                LaunchedEffect(pendingFocusId) {
                    if (pendingFocusId == item.id) {
                        withFrameNanos { }
                        try { itemFocus.requestFocus() } catch (_: IllegalStateException) { }
                        pendingFocusId = null
                    }
                }
                LaunchedEffect(requestFocus) {
                    if (requestFocus && index == 0 && !readOnly) {
                        withFrameNanos { }
                        try { itemFocus.requestFocus() } catch (_: IllegalStateException) { }
                        onFocusRequestHandled()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newList = block.items.toMutableList()
                            newList[index] = item.copy(isChecked = !item.isChecked)
                            onChecklistChange(newList)
                        },
                        enabled = !readOnly,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            null,
                            tint = if (item.isChecked) NotesDesign.BrandColor else palette.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = item.text,
                        onValueChange = { newValue ->
                            val nl = newValue.indexOf('\n')
                            if (nl < 0) {
                                val newList = block.items.toMutableList()
                                newList[index] = item.copy(text = newValue)
                                onChecklistChange(newList)
                            } else {
                                // Enter pressed: split here, new item below, focus it.
                                val newItem = ChecklistItem(text = newValue.substring(nl + 1).replace("\n", " "))
                                val newList = block.items.toMutableList()
                                newList[index] = item.copy(text = newValue.substring(0, nl))
                                newList.add(index + 1, newItem)
                                onChecklistChange(newList)
                                pendingFocusId = newItem.id
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(itemFocus)
                            .onPreviewKeyEvent { event ->
                                if (!readOnly &&
                                    event.type == KeyEventType.KeyDown &&
                                    event.key == Key.Backspace &&
                                    item.text.isEmpty() &&
                                    block.items.size > 1
                                ) {
                                    val newList = block.items.toMutableList()
                                    newList.removeAt(index)
                                    onChecklistChange(newList)
                                    pendingFocusId = block.items.getOrNull(index - 1)?.id
                                    true
                                } else {
                                    false
                                }
                            },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (item.isChecked) palette.textSecondary else palette.text,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                        ),
                        cursorBrush = SolidColor(NotesDesign.BrandColor),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        readOnly = readOnly,
                        decorationBox = { innerTextField ->
                            Box {
                                if (item.text.isEmpty() && !readOnly) {
                                    Text("List item...", style = MaterialTheme.typography.bodyLarge.copy(color = palette.textMuted))
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (!readOnly) {
                        IconButton(onClick = {
                            val newList = block.items.toMutableList()
                            newList.removeAt(index)
                            onChecklistChange(newList)
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = palette.textMuted)
                        }
                    }
                }
            }
        }

        if (!readOnly) {
            TextButton(
                onClick = {
                    val newItem = ChecklistItem(text = "")
                    onChecklistChange(block.items + newItem)
                    pendingFocusId = newItem.id
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Text("Add Item", fontSize = 12.sp)
            }
        }
    }
}
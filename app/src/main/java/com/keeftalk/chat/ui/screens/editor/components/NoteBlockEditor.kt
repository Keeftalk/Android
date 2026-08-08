package com.keeftalk.chat.ui.screens.editor.components

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
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.*
import com.keeftalk.chat.ui.screens.editor.NotesDesign
import com.keeftalk.chat.ui.screens.editor.toSpanStyle
import com.keeftalk.chat.ui.screens.editor.toRichTextParts

@Composable
fun TextBlockEditor(
    block: NoteBlock.Text,
    onContentChange: (List<RichTextPart>) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean, TextRange) -> Unit = { _, _ -> },
    onMentionQueryChange: (String) -> Unit = {},
    readOnly: Boolean = false
) {
    val palette = NotesDesign.colors()
    
    val annotatedString = remember(block.richText) {
        buildAnnotatedString {
            if (block.richText.isEmpty()) {
                append("")
            } else {
                block.richText.forEach { part ->
                    withStyle(style = part.toSpanStyle()) {
                        append(part.text)
                    }
                }
            }
        }
    }

    var textFieldValue by remember(block.id) {
        mutableStateOf(TextFieldValue(annotatedString))
    }

    var lastSentRichText by remember(block.id) {
        mutableStateOf(block.richText)
    }

    // Sync external changes (Undo/Redo)
    LaunchedEffect(block.richText) {
        if (block.richText != lastSentRichText) {
            textFieldValue = textFieldValue.copy(
                annotatedString = annotatedString,
                selection = TextRange(annotatedString.length)
            )
            lastSentRichText = block.richText
        }
    }

    // Selection reporting
    LaunchedEffect(textFieldValue.selection) {
        onFocusChanged(true, textFieldValue.selection)
    }

    // Mention Detection
    LaunchedEffect(textFieldValue.selection) {
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.end
        if (cursor > 0 && text.isNotEmpty()) {
            val lastAt = text.lastIndexOf('@', cursor - 1)
            if (lastAt != -1) {
                val query = text.substring(lastAt + 1, cursor)
                if (!query.contains(" ")) {
                    onMentionQueryChange(query)
                    return@LaunchedEffect
                }
            }
        }
        onMentionQueryChange("")
    }

    val style = when (block.style) {
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

    Column(modifier = modifier.fillMaxWidth()) {
        if (block.style == "QUOTE") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .drawBehind {
                        drawLine(
                            color = NotesDesign.BrandColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                    .padding(start = 16.dp)
            ) {
                BasicRichTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        val newParts = it.annotatedString.toRichTextParts()
                        if (newParts != lastSentRichText) {
                            lastSentRichText = newParts
                            onContentChange(newParts)
                        }
                    },
                    style = style,
                    readOnly = readOnly,
                    placeholder = "Quote..."
                )
            }
        } else {
            BasicRichTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    onContentChange(it.annotatedString.toRichTextParts())
                },
                style = style,
                readOnly = readOnly,
                placeholder = if (block.style == "NORMAL") "Start typing..." else ""
            )
        }
    }
}

@Composable
fun BasicRichTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    style: TextStyle,
    readOnly: Boolean,
    placeholder: String
) {
    val palette = NotesDesign.colors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        textStyle = style,
        cursorBrush = SolidColor(NotesDesign.BrandColor),
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.text.isEmpty() && !readOnly) {
                    Text(
                        text = placeholder,
                        style = style.copy(color = palette.textMuted)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun ImageBlockEditor(
    block: NoteBlock.Image,
    imageUrl: String?,
    onCaptionChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    val palette = NotesDesign.colors()
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
    val palette = NotesDesign.colors()
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
                            textStyle = if (rIndex == 0) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
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
    val palette = NotesDesign.colors()
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
    val palette = NotesDesign.colors()
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
    readOnly: Boolean = false
) {
    val palette = NotesDesign.colors()
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        block.items.forEachIndexed { index, item ->
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
                        val newList = block.items.toMutableList()
                        newList[index] = item.copy(text = newValue)
                        onChecklistChange(newList)
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (item.isChecked) palette.textSecondary else palette.text,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                    ),
                    readOnly = readOnly,
                    decorationBox = { innerTextField ->
                        if (item.text.isEmpty() && !readOnly) {
                            Text("List item...", style = MaterialTheme.typography.bodyLarge.copy(color = palette.textMuted))
                        }
                        innerTextField()
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
        
        if (!readOnly) {
            TextButton(
                onClick = {
                    val newList = block.items.toMutableList()
                    newList.add(ChecklistItem(text = ""))
                    onChecklistChange(newList)
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Text("Add Item", fontSize = 12.sp)
            }
        }
    }
}

package com.keeftalk.chat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.util.NotesUtils
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorColors
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

/**
 * A stable version of the Rich Text Editor that ensures typing works correctly
 * and supports custom blocks like Tables and centered Media.
 */
@OptIn(ExperimentalRichTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FixedRichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    colors: RichTextEditorColors = RichTextEditorDefaults.richTextEditorColors(
        containerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )
) {
    val density = LocalDensity.current

    // Use reflection to get the underlying TextFieldValue for stable typing
    val textFieldValue = remember(state.annotatedString, state.selection) {
        try {
            val field = state.javaClass.getDeclaredField("textFieldValue")
            field.isAccessible = true
            field.get(state) as TextFieldValue
        } catch (_: Exception) {
            TextFieldValue(state.annotatedString, state.selection)
        }
    }

    val onValueChangeMethod = remember(state) {
        try {
            val method = state.javaClass.getDeclaredMethod("onTextFieldValueChange", TextFieldValue::class.java)
            method.isAccessible = true
            method
        } catch (_: Exception) { null }
    }

    val visualTransformation = remember(state) {
        try {
            val field = state.javaClass.getDeclaredField("visualTransformation")
            field.isAccessible = true
            field.get(state) as? VisualTransformation
        } catch (_: Exception) { null }
    } ?: VisualTransformation.None

    val onTextLayoutMethod = remember(state) {
        try {
            val method = state.javaClass.getDeclaredMethod("onTextLayout", TextLayoutResult::class.java, Density::class.java)
            method.isAccessible = true
            method
        } catch (_: Exception) { null }
    }

    val inlineContentMap = remember(state) {
        val originalMap = NotesUtils.getInlineContentMap(state)
        originalMap.mapValues { (id, content) ->
            androidx.compose.foundation.text.InlineTextContent(content.placeholder) { alternateText ->
                val currentHtml = state.toHtml()
                // Use alternateText (alt attribute) to detect table
                val isTable = alternateText.startsWith("table:")
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTable) {
                        NoteTableBlock(onRemove = {
                            // Find the specific image tag with this alt and delete it
                            val escapedAlt = alternateText.replace("{", "\\{").replace("}", "\\}")
                            val regex = """<img[^>]+alt=["']$escapedAlt["'][^>]*>""".toRegex()
                            state.setHtml(currentHtml.replace(regex, ""))
                        })
                    } else {
                        // Regular Media (Image/Video) handled by ImageLoader
                        content.children(alternateText)
                    }
                }
            }
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { onValueChangeMethod?.invoke(state, it) },
        modifier = modifier.fillMaxWidth().heightIn(min = 100.dp),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.copy(color = Color.Transparent), // Underlying text is transparent
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        onTextLayout = { onTextLayoutMethod?.invoke(state, it, density) },
        decorationBox = { innerTextField ->
            RichTextEditorDefaults.RichTextEditorDecorationBox(
                value = textFieldValue.text,
                visualTransformation = visualTransformation,
                innerTextField = {
                    Box {
                        BasicRichText(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                            style = textStyle,
                            inlineContent = inlineContentMap
                        )
                        innerTextField() // The actual text field
                    }
                },
                placeholder = placeholder,
                enabled = enabled,
                isError = false,
                interactionSource = remember { MutableInteractionSource() },
                colors = colors,
                contentPadding = PaddingValues(0.dp),
                singleLine = false
            )
        }
    )
}

@Composable
fun NoteTableBlock(onRemove: () -> Unit) {
    val rows = 3
    val cols = 3
    
    Box(modifier = Modifier.fillMaxWidth(0.8f).padding(8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(1.dp)) {
                repeat(rows) { r ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(cols) { c ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("Cell ${r+1}-${'A'+c}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
        
        IconButton(
            onClick = onRemove, 
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).offset(x = 12.dp, y = (-12).dp)
        ) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

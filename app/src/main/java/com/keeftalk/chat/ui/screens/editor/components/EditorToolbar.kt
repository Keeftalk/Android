package com.keeftalk.chat.ui.screens.editor.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.keeftalk.chat.ui.screens.editor.NotesDesign

@Composable
fun EditorToolbar(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAction: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = NotesDesign.colors()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
    ) {
        // Toggle Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build,
                    null,
                    tint = NotesDesign.BrandColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Formatting Tools",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(palette.bg, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("16 tools", fontSize = 10.sp, color = palette.textSecondary)
                }
            }
            Icon(
                Icons.Default.ExpandMore,
                null,
                tint = palette.textMuted,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(if (isExpanded) 180f else 0f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider(color = palette.border)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Group 1: Text Style
                    ToolbarGroup {
                        ToolbarButton(Icons.Default.FormatBold, "Bold") { onAction(ToolbarAction.Bold) }
                        ToolbarButton(Icons.Default.FormatItalic, "Italic") { onAction(ToolbarAction.Italic) }
                        ToolbarButton(Icons.Default.FormatUnderlined, "Underline") { onAction(ToolbarAction.Underline) }
                        ToolbarButton(Icons.Default.StrikethroughS, "Strike") { onAction(ToolbarAction.Strike) }
                    }

                    // Group 2: Font Size
                    ToolbarGroup {
                        ToolbarTextButton("A-") { onAction(ToolbarAction.FontSizeDecrease) }
                        ToolbarTextButton("A+") { onAction(ToolbarAction.FontSizeIncrease) }
                    }

                    // Group 3: Lists
                    ToolbarGroup {
                        ToolbarButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet") { onAction(ToolbarAction.BulletList) }
                        ToolbarButton(Icons.Default.FormatListNumbered, "Numbered") { onAction(ToolbarAction.OrderedList) }
                        ToolbarButton(Icons.Default.CheckBox, "Checklist") { onAction(ToolbarAction.Checklist) }
                    }

                    // Group 4: Alignment
                    ToolbarGroup {
                        ToolbarButton(Icons.AutoMirrored.Filled.FormatAlignLeft, "Left") { onAction(ToolbarAction.AlignLeft) }
                        ToolbarButton(Icons.Default.FormatAlignCenter, "Center") { onAction(ToolbarAction.AlignCenter) }
                        ToolbarButton(Icons.AutoMirrored.Filled.FormatAlignRight, "Right") { onAction(ToolbarAction.AlignRight) }
                    }

                    // Group 5: Insert
                    ToolbarGroup {
                        ToolbarButton(Icons.Default.Image, "Image") { onAction(ToolbarAction.InsertImage) }
                        ToolbarButton(Icons.Default.VideoLibrary, "Video") { onAction(ToolbarAction.InsertVideo) }
                        ToolbarButton(Icons.Default.Link, "Link") { onAction(ToolbarAction.InsertLink) }
                        ToolbarButton(Icons.Default.TableChart, "Table") { onAction(ToolbarAction.InsertTable) }
                        ToolbarButton(Icons.Default.Code, "Code") { onAction(ToolbarAction.InsertCode) }
                        ToolbarButton(Icons.Default.Superscript, "Math") { onAction(ToolbarAction.InsertMath) }
                    }

                    // Group 6: Blocks & Extra
                    ToolbarGroup {
                        ToolbarButton(Icons.Default.FormatQuote, "Quote") { onAction(ToolbarAction.Quote) }
                        ToolbarButton(Icons.Default.AlternateEmail, "Mention") { onAction(ToolbarAction.Mention) }
                        ToolbarButton(Icons.Default.Share, "Share", tint = NotesDesign.InfoColor) { onAction(ToolbarAction.Share) }
                    }

                    // Group 7: Colors
                    ToolbarGroup(isLast = true) {
                        ColorPalette(onColorSelect = { onAction(ToolbarAction.Color(it)) })
                    }
                }
            }
        }
    }
}

@Composable
fun ToolbarGroup(isLast: Boolean = false, content: @Composable RowScope.() -> Unit) {
    val palette = NotesDesign.colors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = if (isLast) 0.dp else 4.dp)
    ) {
        content()
        if (!isLast) {
            Spacer(modifier = Modifier.width(8.dp))
            VerticalDivider(
                modifier = Modifier.height(24.dp),
                color = palette.border
            )
        }
    }
}

@Composable
fun ToolbarButton(
    icon: ImageVector,
    tooltip: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val palette = NotesDesign.colors()
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        Icon(
            icon,
            contentDescription = tooltip,
            tint = tint ?: palette.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ToolbarTextButton(text: String, onClick: () -> Unit) {
    val palette = NotesDesign.colors()
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = palette.text
        )
    }
}

@Composable
fun ColorPalette(onColorSelect: (String) -> Unit) {
    val colors = listOf("#1e293b", "#dc2626", "#2563eb", "#16a34a", "#eab308", "#ffffff")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("A", fontSize = 12.sp, color = NotesDesign.LightTextMuted)
        colors.forEach { hex ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(hex)))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                    .clickable { onColorSelect(hex) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

sealed class ToolbarAction {
    object Bold : ToolbarAction()
    object Italic : ToolbarAction()
    object Underline : ToolbarAction()
    object Strike : ToolbarAction()
    object FontSizeIncrease : ToolbarAction()
    object FontSizeDecrease : ToolbarAction()
    object BulletList : ToolbarAction()
    object OrderedList : ToolbarAction()
    object Checklist : ToolbarAction()
    object AlignLeft : ToolbarAction()
    object AlignCenter : ToolbarAction()
    object AlignRight : ToolbarAction()
    object InsertImage : ToolbarAction()
    object InsertVideo : ToolbarAction()
    object InsertLink : ToolbarAction()
    object InsertTable : ToolbarAction()
    object InsertCode : ToolbarAction()
    object InsertMath : ToolbarAction()
    object Quote : ToolbarAction()
    object Mention : ToolbarAction()
    object Share : ToolbarAction()
    data class Color(val hex: String) : ToolbarAction()
}

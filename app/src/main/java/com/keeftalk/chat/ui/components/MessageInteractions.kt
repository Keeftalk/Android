package com.keeftalk.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.keeftalk.chat.domain.model.MessageReaction

val ReactionEmojis = listOf("❤️", "😆", "🫡", "🫣", "🥱", "😱", "🥹", "🤬")

@Composable
fun ReactionsPopup(
    onDismiss: () -> Unit,
    onReactionSelect: (String) -> Unit,
    onMoreClick: () -> Unit,
    currentReaction: String? = null,
    isMe: Boolean
) {
    Popup(
        alignment = if (isMe) Alignment.TopEnd else Alignment.TopStart,
        offset = IntOffset(if (isMe) -20 else 20, -110),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReactionEmojis.forEach { emoji ->
                    val isSelected = currentReaction == emoji
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onReactionSelect(emoji); onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onMoreClick(); onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageReactions(
    reactions: List<MessageReaction>,
    onReactionClick: (String) -> Unit,
    onReactionBubbleClick: () -> Unit
) {
    if (reactions.isEmpty()) return

    val groupedReactions = remember(reactions) {
        reactions.groupBy { it.emoji }
            .toList()
            .sortedByDescending { it.second.size }
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onReactionBubbleClick() },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        groupedReactions.take(3).forEach { (emoji, list) ->
            Surface(
                onClick = { onReactionClick(emoji) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = emoji, fontSize = 13.sp)
                    Text(
                        text = list.size.toString(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionDetailsSheet(
    reactions: List<MessageReaction>,
    onDismiss: () -> Unit,
    getUserProfile: (String) -> com.keeftalk.chat.domain.model.User?,
    currentUserId: String
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val groupedReactions = remember(reactions) {
            reactions.groupBy { it.emoji }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Reactions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                groupedReactions.forEach { (emoji, list) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = list.size.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    items(list) { reaction ->
                        val isMe = reaction.userId == currentUserId
                        val user = getUserProfile(reaction.userId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeeftalkAvatar(
                                avatarUrl = user?.avatarUrl,
                                initials = com.keeftalk.chat.util.AvatarUtils.getInitials(user?.name ?: "U"),
                                seed = user?.id,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isMe) "You" else (user?.name ?: "Unknown User"),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isMe) {
                                Text(
                                    text = emoji,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                    
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onReply: () -> Unit,
    onReport: () -> Unit,
    onBump: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onDownload: () -> Unit = {},
    onTranslate: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    isOwnMessage: Boolean,
    hasText: Boolean,
    isMedia: Boolean = false,
    isLocked: Boolean = false
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            ContextMenuItem(Icons.AutoMirrored.Filled.Reply, "Reply", onReply, onDismiss)
            if (hasText) {
                ContextMenuItem(Icons.Default.ContentCopy, "Copy", onCopy, onDismiss)
                ContextMenuItem(Icons.Default.Translate, "Translate", onTranslate, onDismiss)
            }

            val canExport = !isLocked || isOwnMessage

            if (canExport) {
                ContextMenuItem(Icons.AutoMirrored.Filled.Forward, "Forward", onForward, onDismiss)
                if (isMedia) {
                    ContextMenuItem(Icons.Default.Download, "Download", onDownload, onDismiss)
                }
            } else if (isMedia) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Transfer restricted by sender", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }

            ContextMenuItem(Icons.Default.Event, "Add to Calendar", onCreateEvent, onDismiss)
            ContextMenuItem(Icons.Default.ArrowUpward, "Bump", onBump, onDismiss)
            ContextMenuItem(Icons.Default.Info, "Message Info", onInfo, onDismiss)
            ContextMenuItem(Icons.Default.Report, "Report", onReport, onDismiss)
            if (isOwnMessage) {
                ContextMenuItem(
                    Icons.Default.Delete, 
                    "Delete", 
                    onDelete, 
                    onDismiss, 
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(); onDismiss() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

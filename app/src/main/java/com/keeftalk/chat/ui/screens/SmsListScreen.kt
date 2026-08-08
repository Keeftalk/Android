package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.keeftalk.chat.domain.repository.SmsConversation
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.FabGradient
import com.keeftalk.chat.util.AvatarUtils

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SmsListScreen(
    viewModel: SmsViewModel,
    onConversationClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val isDefaultSms by viewModel.isDefaultSmsApp.collectAsState()
    val context = LocalContext.current
    val icons = LocalAppIcons.current
    val smsPermissionState = rememberPermissionState(android.Manifest.permission.READ_SMS)
    val pullToRefreshState = rememberPullToRefreshState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        if (!smsPermissionState.status.isGranted) {
            SmsPermissionOnboarding(onAllow = { smsPermissionState.launchPermissionRequest() })
        } else if (!isDefaultSms) {
            DefaultSmsBanner(onRequestDefault = {
                (context as? android.app.Activity)?.let { viewModel.requestDefaultSmsRole(it) }
            })
        }

        Surface(
            modifier = Modifier.weight(1f),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = { viewModel.onResume() },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (conversations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icons.messageSquare,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No SMS messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(conversations, key = { it.threadId }) { conversation ->
                            SmsConversationItem(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation.threadId, conversation.address) },
                                onAction = { action ->
                                    when (action) {
                                        ChatAction.DELETE -> viewModel.deleteConversation(conversation.threadId)
                                        ChatAction.ARCHIVE -> viewModel.toggleArchive(conversation.threadId, !conversation.isArchived)
                                        ChatAction.MARK_AS_READ -> viewModel.markAsRead(conversation.threadId)
                                        else -> {}
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SmsConversationItem(
    conversation: SmsConversation,
    onClick: () -> Unit,
    onAction: (ChatAction) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val icons = LocalAppIcons.current
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(88.dp.toPx(), size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            )
            .background(
                if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                else Color.Transparent
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeeftalkAvatar(
                avatarUrl = conversation.avatarUrl,
                initials = AvatarUtils.getInitials(conversation.contactName ?: conversation.address),
                seed = conversation.address,
                size = 56.dp,
                isSkeleton = false
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.contactName ?: conversation.address,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 17.sp,
                            letterSpacing = (-0.1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conversation.isKeeftalkUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Keeftalk User",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    if (conversation.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            icons.pin,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = conversation.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatRelativeTime(conversation.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(FabGradient, CircleShape)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                            .widthIn(min = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            conversation.unreadCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp
                            )
                        )
                    }
                }
            }
        }

        if (showContextMenu) {
            SmsContextMenu(
                conversation = conversation,
                onAction = { 
                    onAction(it)
                    showContextMenu = false
                },
                onDismiss = { showContextMenu = false }
            )
        }
    }
}

@Composable
fun SmsContextMenu(
    conversation: SmsConversation,
    onAction: (ChatAction) -> Unit,
    onDismiss: () -> Unit
) {
    val icons = LocalAppIcons.current
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Mark as Read") },
            leadingIcon = { Icon(Icons.Default.DoneAll, null) },
            enabled = conversation.unreadCount > 0,
            onClick = { onAction(ChatAction.MARK_AS_READ) }
        )
        DropdownMenuItem(
            text = { Text(if (conversation.isArchived) "Unarchive" else "Archive") },
            leadingIcon = { Icon(icons.archive, null) },
            onClick = { onAction(ChatAction.ARCHIVE) }
        )
        DropdownMenuItem(
            text = { Text("Delete Conversation") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error),
            onClick = { onAction(ChatAction.DELETE) }
        )
    }
}

@Composable
fun SmsPermissionOnboarding(onAllow: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Manage your SMS in Keeftalk", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Keeftalk can now handle your SMS and MMS messages. Grant permission to see them here.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAllow, modifier = Modifier.align(Alignment.End)) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun DefaultSmsBanner(onRequestDefault: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Make Keeftalk your default SMS app",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRequestDefault) {
                Text("Set as Default")
            }
        }
    }
}

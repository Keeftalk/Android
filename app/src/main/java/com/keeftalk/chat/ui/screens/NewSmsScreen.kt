package com.keeftalk.chat.ui.screens

import android.provider.Telephony
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.util.AvatarUtils
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSmsScreen(
    viewModel: NewSmsViewModel,
    onBack: () -> Unit,
    onSmsThreadSelected: (Long, String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isValidNumber by viewModel.isQueryValidNumber.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val icons = LocalAppIcons.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Column {
                    KeeftalkTopBar(
                        title = "New SMS",
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                            }
                        }
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Enter name or phone number...") },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )

                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(400.milliseconds)
                            try {
                                focusRequester.requestFocus()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isValidNumber) {
                    item {
                        SendToNumberItem(number = searchQuery) {
                            scope.launch {
                                val threadId = Telephony.Threads.getOrCreateThreadId(context, searchQuery)
                                onSmsThreadSelected(threadId, searchQuery)
                            }
                        }
                    }
                }

                items(searchResults, key = { it.id }) { user ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
                    ) {
                        NewSmsUserItem(user = user) {
                            user.phone?.let { phone ->
                                scope.launch {
                                    val threadId = Telephony.Threads.getOrCreateThreadId(context, phone)
                                    onSmsThreadSelected(threadId, phone)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SendToNumberItem(number: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer { shadowElevation = 2.dp.toPx() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Send SMS to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(LocalAppIcons.current.chevronRight, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun NewSmsUserItem(user: Profile, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer { shadowElevation = 2.dp.toPx() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KeeftalkAvatar(
                avatarUrl = user.avatarUrl,
                initials = AvatarUtils.getInitials(user.fullName ?: user.username),
                seed = user.id,
                size = 60.dp,
                modifier = Modifier
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName ?: user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                user.phone?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(LocalAppIcons.current.chevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

package tn.keeftalk.chat.ui.screens

import android.provider.Telephony
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.launch
import tn.keeftalk.chat.ui.components.KeeftalkAvatar
import tn.keeftalk.chat.ui.components.KeeftalkTopBar
import tn.keeftalk.chat.ui.theme.KeeftalkDimensions
import tn.keeftalk.chat.ui.theme.LocalAppIcons
import tn.keeftalk.chat.util.AvatarUtils
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSmsScreen(
    viewModel: NewSmsViewModel,
    onBack: () -> Unit,
    onSmsThreadSelected: (Long, String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedRecipients by viewModel.selectedRecipients.collectAsState()
    val isValidNumber by viewModel.isQueryValidNumber.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val icons = LocalAppIcons.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                KeeftalkTopBar(
                    title = "New SMS",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                        }
                    }
                )

                // Selected recipients chips row
                if (selectedRecipients.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(selectedRecipients, key = { it.phoneNumber }) { recipient ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.removeRecipient(recipient) },
                                label = {
                                    Text(
                                        text = recipient.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Search or phone entry field
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Enter contact name or phone number...") },
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
                        kotlinx.coroutines.delay(300.milliseconds)
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {}
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding().imePadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type SMS message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = selectedRecipients.isNotEmpty() && messageText.isNotBlank() && !isSending

                    IconButton(
                        onClick = {
                            if (canSend) {
                                isSending = true
                                scope.launch {
                                    val result = viewModel.sendSmsToAll(messageText)
                                    isSending = false
                                    if (result != null) {
                                        onSmsThreadSelected(result.first, result.second)
                                    } else {
                                        onBack()
                                    }
                                }
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isValidNumber) {
                item {
                    SendToNumberItem(number = searchQuery) {
                        viewModel.addManualNumber(searchQuery)
                    }
                }
            }

            items(searchResults, key = { it.phoneNumber }) { recipient ->
                NewSmsUserItem(recipient = recipient) {
                    viewModel.addRecipient(recipient)
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Add Phone Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(text = number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(LocalAppIcons.current.chevronRight, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun NewSmsUserItem(recipient: SmsRecipient, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KeeftalkAvatar(
                avatarUrl = recipient.avatarUrl,
                initials = AvatarUtils.getInitials(recipient.name),
                seed = recipient.phoneNumber,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = recipient.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

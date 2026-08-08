package com.keeftalk.chat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.util.AvatarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    viewModel: NewGroupViewModel,
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val icons = LocalAppIcons.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.onAvatarChange(uri) }
    )

    LaunchedEffect(uiState.createdChatId) {
        uiState.createdChatId?.let { onGroupCreated(it) }
    }

    Scaffold(
        topBar = {
            KeeftalkTopBar(
                title = "New Group",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.createGroup() },
                        enabled = uiState.name.isNotBlank() && uiState.selectedUserIds.isNotEmpty() && !uiState.isCreating
                    ) {
                        if (uiState.isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Group Info Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Picker
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.avatarUri != null) {
                        AsyncImage(
                            model = uiState.avatarUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Pick Avatar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Group Name Input
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    placeholder = { Text("Group Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search users...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

            // Selected Users Horizontal List
            AnimatedVisibility(
                visible = uiState.selectedUserIds.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.selectedUserIds.toList()) { userId ->
                        val user = uiState.recentUsers.find { it.id == userId } ?: return@items
                        SelectedUserItem(user = user, onRemove = { viewModel.toggleUserSelection(userId) })
                    }
                }
            }

            // User List
            val filteredUsers = remember(uiState.recentUsers, uiState.searchQuery) {
                uiState.recentUsers.filter {
                    it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                            it.username.contains(uiState.searchQuery, ignoreCase = true)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredUsers) { user ->
                    val isSelected = uiState.selectedUserIds.contains(user.id)
                    UserSelectionItem(
                        user = user,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleUserSelection(user.id) }
                    )
                }
            }
        }

        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(uiState.error!!) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun SelectedUserItem(user: User, onRemove: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            KeeftalkAvatar(
                avatarUrl = user.avatarUrl,
                initials = user.initials,
                seed = user.id,
                size = 50.dp
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            user.name.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun UserSelectionItem(user: User, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeeftalkAvatar(
                avatarUrl = user.avatarUrl,
                initials = user.initials,
                seed = user.id,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

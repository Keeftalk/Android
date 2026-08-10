package com.keeftalk.chat.ui.share

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.vault.ChatPickerDialog
import com.keeftalk.chat.ui.vault.VaultBreadcrumbs
import com.keeftalk.chat.ui.vault.VaultMovePicker
import com.keeftalk.chat.ui.vault.VaultNewFolderDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showChatPicker by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onClose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isVaultFlow) "Save to My Vault" else "Send to...") },
                actions = {
                    if (uiState.isVaultFlow) {
                        IconButton(onClick = { showNewFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.isVaultFlow && uiState.sharedFiles.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    Button(
                        onClick = { viewModel.saveToVault() },
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        enabled = !uiState.isProcessing
                    ) {
                        Text("Save to Vault")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.sharedFiles.isNotEmpty()) {
                FilesPreviewSection(
                    files = uiState.sharedFiles,
                    onRemove = { viewModel.removeFile(it) }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files selected")
                }
            }

            if (!uiState.isVaultFlow && uiState.sharedFiles.isNotEmpty()) {
                Text(
                    "RECENT CHATS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    gridItems(uiState.recentChats) { chat ->
                        RecentChatShareItem(
                            chat = chat,
                            onClick = { viewModel.onChatSelected(chat.id) }
                        )
                    }
                }
            }
            
            if (uiState.isVaultFlow) {
                Spacer(modifier = Modifier.height(8.dp))
                VaultBreadcrumbs(
                    path = uiState.vaultNavigationStack,
                    onBreadcrumbClick = { viewModel.navigateToBreadcrumb(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                VaultMovePicker(
                    currentFolders = uiState.vaultFolders,
                    path = uiState.vaultNavigationStack,
                    onFolderSelected = { /* Already handled by breadcrumbs and navigation */ },
                    onNavigateInto = { viewModel.navigateToFolder(it) },
                    onNavigateBack = { viewModel.navigateBack() },
                    onDismiss = { /* Not applicable here */ }
                )
            }
        }
        
        if (uiState.isProcessing) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        
        if (showChatPicker) {
            ChatPickerDialog(
                chats = uiState.recentChats,
                contacts = uiState.contacts,
                onDismiss = { showChatPicker = false },
                onChatsSelected = { chatIds ->
                    viewModel.sendToChats(chatIds)
                    showChatPicker = false
                }
            )
        }

        if (showNewFolderDialog) {
            VaultNewFolderDialog(
                onDismiss = { showNewFolderDialog = false },
                onConfirm = { name, _ ->
                    viewModel.createFolder(name)
                    showNewFolderDialog = false
                }
            )
        }

        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

@Composable
fun FilesPreviewSection(
    files: List<SharedFile>,
    onRemove: (SharedFile) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
        Text(
            text = "${files.size} items selected",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                FilePreviewItem(file = file, onRemove = { onRemove(file) })
            }
        }
    }
}

@Composable
fun RecentChatShareItem(
    chat: com.keeftalk.chat.domain.model.Chat,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        KeeftalkAvatar(
            avatarUrl = chat.avatarUrl,
            initials = chat.initials,
            seed = chat.id,
            size = 64.dp,
            isKeeftalkUser = true // Small logo badge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = chat.displayName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FilePreviewItem(
    file: SharedFile,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

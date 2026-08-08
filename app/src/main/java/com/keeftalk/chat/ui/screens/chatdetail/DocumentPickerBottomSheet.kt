package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.DocumentModel
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPickerBottomSheet(
    viewModel: DocumentPickerViewModel,
    onDismiss: () -> Unit,
    onUnlockFullAccess: () -> Unit,
    onBrowseSystem: () -> Unit,
    onNext: (Set<DocumentModel>) -> Unit,
    onSend: (DocumentModel) -> Unit
) {
    val recentDocuments by viewModel.recentDocuments.collectAsState()
    val vaultDocuments by viewModel.vaultDocuments.collectAsState()
    val sharedDocuments by viewModel.sharedDocuments.collectAsState()
    val selectedDocuments by viewModel.selectedDocuments.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val isShowingFolders by viewModel.isShowingFolders.collectAsState()
    val isVaultEmpty by viewModel.isVaultEmpty.collectAsState()
    val isFullAccessUnlocked by viewModel.isFullAccessUnlocked.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 14.dp)
                    .size(44.dp, 5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Documents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onBrowseSystem,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Browse System",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Surface(
                    onClick = onDismiss,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            null, 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Close", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DocumentPickerTab.entries.forEach { tab ->
                    val isActive = activeTab == tab
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { 
                                if (tab == DocumentPickerTab.RECENT) {
                                    if (activeTab == DocumentPickerTab.RECENT) {
                                        viewModel.toggleFolderList()
                                    } else {
                                        viewModel.setTab(tab)
                                    }
                                } else {
                                    viewModel.setTab(tab)
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when(tab) {
                                DocumentPickerTab.RECENT -> if (selectedFolder != null) selectedFolder!!.name else "Recent"
                                DocumentPickerTab.VAULT -> "Vault"
                                DocumentPickerTab.SHARED -> "Shared"
                            },
                            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (tab == DocumentPickerTab.RECENT) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Folder",
                                tint = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (activeTab == DocumentPickerTab.RECENT && isShowingFolders) {
                    if (!isFullAccessUnlocked) {
                        item {
                            Surface(
                                onClick = onUnlockFullAccess,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Unlock Full Storage", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Cannot find your folders? Grant access to see all files.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (folders.isNotEmpty()) {
                        items(folders) { folder ->
                            FolderItem(
                                name = folder.name,
                                icon = if (folder.id == "ALL_RECENT") Icons.Default.History else Icons.Default.Folder,
                                count = folder.documentCount,
                                onClick = { viewModel.selectFolder(folder) }
                            )
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No folders found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    if (activeTab == DocumentPickerTab.RECENT) {
                        item {
                            Surface(
                                onClick = onBrowseSystem,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp, 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text("Browse other documents...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                        Text("Google Drive, SD card, and more", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    val currentList = when(activeTab) {
                        DocumentPickerTab.RECENT -> recentDocuments
                        DocumentPickerTab.VAULT -> vaultDocuments
                        DocumentPickerTab.SHARED -> sharedDocuments
                    }

                    if (currentList.isNotEmpty()) {
                        items(currentList, key = { it.id }) { doc ->
                            DocumentItem(
                                document = doc,
                                isSelected = selectedDocuments.contains(doc), 
                                onClick = { viewModel.toggleSelection(doc) }
                            )
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(activeTab) {
                                        DocumentPickerTab.RECENT -> {
                                            if (selectedFolder != null) {
                                                "No documents in ${selectedFolder!!.name}"
                                            } else {
                                                "No documents found on device.\nTry using the 'Browse' button in the top right."
                                            }
                                        }
                                        DocumentPickerTab.VAULT -> if (isVaultEmpty) "Vault is empty" else "No documents in vault"
                                        DocumentPickerTab.SHARED -> "No documents shared with you"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (selectedDocuments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onNext(selectedDocuments) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (selectedDocuments.size == 1) "Next" else "Next (${selectedDocuments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
private fun FolderItem(
    name: String,
    icon: ImageVector,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            if (count >= 0) {
                Text("$count documents", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

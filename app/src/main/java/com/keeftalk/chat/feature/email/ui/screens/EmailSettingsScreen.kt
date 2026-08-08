package com.keeftalk.chat.feature.email.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.feature.email.viewmodel.EmailViewModel
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.util.AvatarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSettingsScreen(
    viewModel: EmailViewModel,
    onBack: () -> Unit,
    onAddAccount: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mail Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(accounts) { account ->
                ListItem(
                    headlineContent = { Text(account.displayName, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(account.emailAddress) },
                    leadingContent = {
                        KeeftalkAvatar(
                            avatarUrl = null,
                            initials = AvatarUtils.getInitials(account.displayName),
                            seed = account.emailAddress,
                            size = 40.dp
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeAccount(account.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }

            item {
                TextButton(
                    onClick = onAddAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add another account")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "General",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                var mergedInboxEnabled by remember { mutableStateOf(true) }
                ListItem(
                    headlineContent = { Text("Unified Inbox") },
                    supportingContent = { Text("Show messages from all accounts in one view") },
                    trailingContent = {
                        Switch(
                            checked = mergedInboxEnabled,
                            onCheckedChange = { mergedInboxEnabled = it }
                        )
                    }
                )
            }

            item {
                val syncFrequency by viewModel.syncFrequency.collectAsState()
                var showSyncDialog by remember { mutableStateOf(false) }
                
                ListItem(
                    headlineContent = { Text("Sync Frequency") },
                    supportingContent = { Text(syncFrequency) },
                    modifier = Modifier.clickable { showSyncDialog = true },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                )

                if (showSyncDialog) {
                    AlertDialog(
                        onDismissRequest = { showSyncDialog = false },
                        title = { Text("Sync Frequency") },
                        text = {
                            Column {
                                listOf("Manual", "15 minutes", "30 minutes", "Hourly", "Daily").forEach { freq ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.setSyncFrequency(freq)
                                                showSyncDialog = false
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = syncFrequency == freq, onClick = null)
                                        Spacer(Modifier.width(16.dp))
                                        Text(freq)
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showSyncDialog = false }) { Text("Cancel") } }
                    )
                }
            }
            
            item {
                val signature by viewModel.signature.collectAsState()
                var showSignatureDialog by remember { mutableStateOf(false) }
                var tempSignature by remember { mutableStateOf(signature) }

                ListItem(
                    headlineContent = { Text("Signature") },
                    supportingContent = { Text(signature) },
                    modifier = Modifier.clickable { 
                        tempSignature = signature
                        showSignatureDialog = true 
                    },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                )

                if (showSignatureDialog) {
                    AlertDialog(
                        onDismissRequest = { showSignatureDialog = false },
                        title = { Text("Edit Signature") },
                        text = {
                            TextField(
                                value = tempSignature,
                                onValueChange = { tempSignature = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = { 
                            TextButton(onClick = { 
                                viewModel.setSignature(tempSignature)
                                showSignatureDialog = false 
                            }) { Text("Save") } 
                        },
                        dismissButton = { 
                            TextButton(onClick = { showSignatureDialog = false }) { Text("Cancel") } 
                        }
                    )
                }
            }

            item {
                val theme by viewModel.theme.collectAsState()
                var showThemeDialog by remember { mutableStateOf(false) }

                ListItem(
                    headlineContent = { Text("Email Theme") },
                    supportingContent = { Text(theme) },
                    modifier = Modifier.clickable { showThemeDialog = true },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                )

                if (showThemeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeDialog = false },
                        title = { Text("Email Theme") },
                        text = {
                            Column {
                                listOf("System default", "Light", "Dark").forEach { t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.setTheme(t)
                                                showThemeDialog = false
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = theme == t, onClick = null)
                                        Spacer(Modifier.width(16.dp))
                                        Text(t)
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") } }
                    )
                }
            }

            item {
                val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
                ListItem(
                    headlineContent = { Text("Notifications") },
                    supportingContent = { Text("Show notifications for new emails") },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Data & Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Clear Cache", color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("Remove downloaded email content and attachments") },
                    modifier = Modifier.clickable { viewModel.clearCache() },
                    trailingContent = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

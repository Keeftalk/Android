package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.keeftalk.chat.domain.model.User
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Sync
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.keeftalk.chat.ui.components.DialerButton
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.components.KeeftalkFab
import com.keeftalk.chat.ui.components.KeeftalkLogo
import com.keeftalk.chat.ui.components.NotificationsDropdown
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.components.KeeftalkSearchTopBar
import com.keeftalk.chat.util.AvatarUtils
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CallListScreen(
    viewModel: CallListViewModel,
    modifier: Modifier = Modifier,
    onChatStarted: (String) -> Unit,
    onSmsStarted: (String) -> Unit = {},
    onContactDetail: (String) -> Unit = {},
    onMenuAction: (MenuAction) -> Unit,
    showTopBar: Boolean = true,
    isSearching: Boolean = false,
    onIsSearchingChange: (Boolean) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    fabActionFlow: kotlinx.coroutines.flow.SharedFlow<com.keeftalk.chat.ui.components.FabActionType>? = null
) {
    val currentComposeContext = LocalContext.current
    val recentCalls by viewModel.recentCalls.collectAsState(initial = emptyList())
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDialer by remember { mutableStateOf(true) }
    val icons = LocalAppIcons.current
    val scope = rememberCoroutineScope()

    val vcfImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentComposeContext.contentResolver.openInputStream(it)?.use { stream ->
                val content = stream.bufferedReader().readText()
                viewModel.importContactsFromVcf(content)
            }
        }
    }

    val vcfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-vcard")
    ) { uri ->
        uri?.let {
            scope.launch {
                val vcfContent = viewModel.exportContactsToVcf()
                currentComposeContext.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(vcfContent.toByteArray())
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fabActionFlow?.collect { action ->
            if (action == com.keeftalk.chat.ui.components.FabActionType.DIALER) {
                showDialer = true
            }
        }
    }

    var showNotifications by remember { mutableStateOf(false) }
    val mainViewModel: com.keeftalk.chat.ui.MainViewModel = viewModel()
    val notifications by mainViewModel.notifications.collectAsState()

    val callLogPermissionState = rememberPermissionState(android.Manifest.permission.READ_CALL_LOG)
    val contactsPermissionState = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)

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

    val content = @Composable { padding: PaddingValues ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (!isSearching) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
            
            val allWeight by animateFloatAsState(targetValue = if (selectedTab == 0) 0.6f else 0.2f, label = "allWeight")
            val missedWeight by animateFloatAsState(targetValue = if (selectedTab == 1) 0.6f else 0.2f, label = "missedWeight")
            val contactsWeight by animateFloatAsState(targetValue = if (selectedTab == 2) 0.6f else 0.2f, label = "contactsWeight")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                listOf("All" to 0, "Missed" to 1, "Contacts" to 2).forEach { (label, index) ->
                    val weight = when(index) {
                        0 -> allWeight
                        1 -> missedWeight
                        else -> contactsWeight
                    }
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (selectedTab == index) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .height(2.dp)
                                        .width(20.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            if (!callLogPermissionState.status.isGranted && selectedTab == 0) {
                CallLogOnboardingCard(onAllow = { callLogPermissionState.launchPermissionRequest() })
            }

            if (!contactsPermissionState.status.isGranted && selectedTab == 2) {
                PhoneContactsOnboardingCard(onAllow = { contactsPermissionState.launchPermissionRequest() })
            }

            val filteredCalls = remember(recentCalls, selectedTab, searchQuery) {
                val list = if (selectedTab == 1) {
                    recentCalls.filter { it.type == com.keeftalk.chat.data.local.CallLogType.MISSED }
                } else {
                    recentCalls
                }
                if (searchQuery.isBlank()) list else {
                    list.filter { it.name?.contains(searchQuery, ignoreCase = true) == true || it.number.contains(searchQuery) }
                }
            }

            val filteredContacts = remember(contacts, searchQuery) {
                if (searchQuery.isBlank()) contacts else {
                    contacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone?.contains(searchQuery) == true }
                }
            }

            if (selectedTab == 2) {
                ContactsList(
                    contacts = filteredContacts,
                    onContactClick = onContactDetail,
                    onCallClick = { viewModel.smartCall(it) },
                    onImportVcf = { vcfImportLauncher.launch("text/x-vcard") },
                    onExportVcf = { vcfExportLauncher.launch("contacts.vcf") },
                    onSync = { viewModel.syncContactsWithSupabase() }
                )
            } else {
                val isDefaultDialer by viewModel.isDefaultDialer.collectAsState()
                if (!isDefaultDialer) {
                    DefaultDialerBanner(onRequestDefault = { 
                        (currentComposeContext as? android.app.Activity)?.let { viewModel.requestDefaultDialerRole(it) }
                    })
                }

                RecentCallsList(
                    calls = filteredCalls,
                    onCallClick = { viewModel.smartCall(it) },
                    onAvatarClick = onContactDetail,
                    onEditBeforeCall = { 
                        viewModel.onDialerInput(it)
                        showDialer = true
                    },
                    onBlock = { 
                        viewModel.blockUser(it, true)
                    },
                    onDelete = {
                        viewModel.deleteCallLog(it)
                    },
                    onSync = { viewModel.syncCallLogsWithSupabase() }
                )
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                if (isSearching) {
                    KeeftalkSearchTopBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onCancel = {
                            onIsSearchingChange(false)
                            onSearchQueryChange("")
                        }
                    )
                } else {
                    KeeftalkTopBar(
                        title = "Calls",
                        actions = {
                            IconButton(onClick = { onIsSearchingChange(true) }) {
                                Icon(icons.search, contentDescription = "Search", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                            }
                            Box {
                                IconButton(onClick = { showNotifications = true }) {
                                    Box(modifier = Modifier.size(24.dp)) {
                                        BadgedBox(badge = {
                                            if (notifications.any { !it.isRead }) {
                                                Badge { Text(notifications.count { !it.isRead }.toString()) }
                                            }
                                        }) {
                                            Icon(icons.bell, contentDescription = "Notifications", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                                        }
                                    }
                                }
                                NotificationsDropdown(
                                    notifications = notifications,
                                    onNotificationClick = {
                                        showNotifications = false
                                        mainViewModel.handleNotificationClick(it)
                                    },
                                    onMarkAllAsRead = { mainViewModel.markAllNotificationsAsRead() },
                                    onDismiss = { showNotifications = false },
                                    expanded = showNotifications
                                )
                            }
                            IconButton(onClick = { onMenuAction(MenuAction.APPEARANCE) }) {
                                Icon(icons.more, contentDescription = "Menu", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            content(PaddingValues(0.dp))
        }
    }

    if (showDialer) {
        DialerBottomSheet(
            viewModel = viewModel,
            onDismiss = { showDialer = false },
            onChatStarted = { onChatStarted(it); showDialer = false },
            onSmsStarted = { onSmsStarted(it); showDialer = false }
        )
    }
}

@Composable
fun PhoneContactsOnboardingCard(onAllow: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Access your phone contacts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Allow Keeftalk to see your phone contacts so you can call them directly from the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAllow,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Allow Access")
            }
        }
    }
}

@Composable
fun DefaultDialerBanner(onRequestDefault: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Make Keeftalk your default phone app",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Manage GSM calls and enjoy smart calling features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = onRequestDefault,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Set as Default", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun CallLogOnboardingCard(onAllow: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "See all your calls in one place",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Import your phone's call history so Keeftalk can combine GSM and Keeftalk calls into a single timeline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAllow,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Allow Access")
            }
        }
    }
}


@Composable
fun ContactsList(
    contacts: List<User>,
    onContactClick: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onImportVcf: () -> Unit,
    onExportVcf: () -> Unit,
    onSync: () -> Unit
) {
    val icons = LocalAppIcons.current
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Contacts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Import contacts") },
                        leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                        onClick = { showMenu = false; onImportVcf() }
                    )
                    DropdownMenuItem(
                        text = { Text("Export contacts") },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                        onClick = { showMenu = false; onExportVcf() }
                    )
                    DropdownMenuItem(
                        text = { Text("Sync") },
                        leadingIcon = { Icon(Icons.Default.Sync, null) },
                        onClick = { showMenu = false; onSync() }
                    )
                }
            }
        }

        if (contacts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(contacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContactClick(contact.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KeeftalkAvatar(
                            avatarUrl = contact.avatarUrl,
                            initials = AvatarUtils.getInitials(contact.name),
                            seed = contact.id,
                            size = 52.dp,
                            isKeeftalkUser = contact.isActive
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = contact.phone ?: contact.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { contact.phone?.let { onCallClick(it) } }) {
                            Icon(icons.phone, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentCallsList(
    calls: List<com.keeftalk.chat.data.local.CallLogEntry>,
    onCallClick: (String) -> Unit,
    onAvatarClick: (String) -> Unit,
    onEditBeforeCall: (String) -> Unit,
    onBlock: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSync: () -> Unit
) {
    val icons = LocalAppIcons.current
    val haptic = LocalHapticFeedback.current
    var selectedEntryForMenu by remember { mutableStateOf<com.keeftalk.chat.data.local.CallLogEntry?>(null) }
    var historyPhone by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Calls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onSync) {
                Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (calls.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No recent calls", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Surface(
                modifier = Modifier.weight(1f),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(calls) { entry ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { historyPhone = entry.peerId ?: entry.number },
                                onLongClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedEntryForMenu = entry 
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.clickable { 
                            entry.peerId?.let { onAvatarClick(it) } 
                        }) {
                            KeeftalkAvatar(
                                avatarUrl = entry.avatarUrl, 
                                initials = AvatarUtils.getInitials(entry.name ?: entry.number), 
                                seed = entry.peerId ?: entry.number,
                                size = 52.dp,
                                isKeeftalkUser = entry.isKeeftalk
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = entry.name ?: entry.number, 
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (entry.isKeeftalk) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = icons.chat, 
                                        contentDescription = "Keeftalk",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (entry.name != null) {
                                Text(
                                    text = entry.number,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (entry.type) {
                                        com.keeftalk.chat.data.local.CallLogType.MISSED -> icons.callMissed
                                        com.keeftalk.chat.data.local.CallLogType.OUTGOING -> icons.callMade
                                        else -> icons.callReceived
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (entry.type == com.keeftalk.chat.data.local.CallLogType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val date = formatRecentCallDate(entry.timestamp)
                                Text(
                                    "${if (entry.isKeeftalk) "Keeftalk" else "Mobile"} • $date", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { onCallClick(entry.peerId ?: entry.number) }
                        ) {
                            Icon(icons.phone, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 84.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                }
            }
            }
        }
    }

    if (selectedEntryForMenu != null) {
        CallItemContextMenu(
            entry = selectedEntryForMenu!!,
            onDismiss = { selectedEntryForMenu = null },
            onEditBeforeCall = { 
                onEditBeforeCall(selectedEntryForMenu!!.number)
                selectedEntryForMenu = null
            },
            onBlock = { 
                onBlock(selectedEntryForMenu!!.peerId ?: "")
                selectedEntryForMenu = null
            },
            onDelete = {
                onDelete(selectedEntryForMenu!!.id)
                selectedEntryForMenu = null
            }
        )
    }

    if (historyPhone != null) {
        CallHistoryDialog(
            phoneNumber = historyPhone!!,
            calls = calls,
            onDismiss = { historyPhone = null }
        )
    }
    }
}

@Composable
fun CallHistoryDialog(
    phoneNumber: String,
    calls: List<com.keeftalk.chat.data.local.CallLogEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Call History") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(calls.filter { it.number == phoneNumber || it.peerId == phoneNumber }) { call ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = call.type.name, style = MaterialTheme.typography.bodyMedium)
                            Text(text = formatRecentCallDate(call.timestamp), style = MaterialTheme.typography.labelSmall)
                        }
                        if (call.duration > 0) {
                            Text(text = "${call.duration}s", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun CallItemContextMenu(
    entry: com.keeftalk.chat.data.local.CallLogEntry,
    onDismiss: () -> Unit,
    onEditBeforeCall: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Copy Number") },
            onClick = { 
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(entry.number))
                onDismiss() 
            }
        )
        DropdownMenuItem(
            text = { Text("Edit before call") },
            onClick = { 
                onEditBeforeCall()
                onDismiss() 
            }
        )
        DropdownMenuItem(
            text = { Text("Block / Report") },
            onClick = { 
                onBlock()
                onDismiss() 
            }
        )
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick = { 
                onDelete()
                onDismiss() 
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DialerBottomSheet(
    viewModel: CallListViewModel,
    onDismiss: () -> Unit,
    onChatStarted: (String) -> Unit,
    onSmsStarted: (String) -> Unit = {}
) {
    val dialerInput by viewModel.dialerInput.collectAsState()
    val formattedNumber by viewModel.formattedNumber.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val currentCountry by viewModel.currentCountry.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val decision by viewModel.decision.collectAsState()
    
    val haptic = LocalHapticFeedback.current
    val icons = LocalAppIcons.current
    val clipboardManager = LocalClipboardManager.current
    val currentComposeContext = LocalContext.current
    
    var showCountryPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var gsmOfferNumber by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DialerEvent.LaunchGsmCall -> {
                    val uri = android.net.Uri.fromParts("tel", event.phoneNumber, null)
                    Log.d("CallListScreen", "Initiating GSM Call/USSD: ${event.phoneNumber}")
                    
                    val telecomManager = currentComposeContext.getSystemService(android.content.Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                    val isDefault = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val roleManager = currentComposeContext.getSystemService(android.app.role.RoleManager::class.java)
                        roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER) == true
                    } else {
                        telecomManager.defaultDialerPackage == currentComposeContext.packageName
                    }

                    if (isDefault) {
                        Log.d("CallListScreen", "Keeftalk is default dialer, using placeCall")
                        try {
                            telecomManager.placeCall(uri, null)
                        } catch (_: SecurityException) {
                            Log.e("CallListScreen", "SecurityException using placeCall")
                            val intent = android.content.Intent(android.content.Intent.ACTION_CALL, uri).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            currentComposeContext.startActivity(intent)
                        }
                    } else {
                        Log.d("CallListScreen", "Keeftalk is NOT default dialer, using ACTION_CALL intent")
                        val intent = android.content.Intent(android.content.Intent.ACTION_CALL, uri).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            currentComposeContext.startActivity(intent)
                        } catch (e: SecurityException) {
                            Log.e("CallListScreen", "SecurityException using ACTION_CALL, falling back to ACTION_DIAL", e)
                            val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            currentComposeContext.startActivity(dialIntent)
                        }
                    }
                }
                is DialerEvent.LaunchSms -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = "sms:${event.phoneNumber}".toUri()
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    currentComposeContext.startActivity(intent)
                }
                is DialerEvent.NavigateToChat -> {
                    onChatStarted(event.chatId)
                    onDismiss()
                }
                is DialerEvent.NavigateToSmsDetail -> {
                    onSmsStarted(event.phoneNumber)
                    onDismiss()
                }
                is DialerEvent.NavigateToCall -> {
                    com.keeftalk.chat.ui.call.CallActivity.start(currentComposeContext, event.callId, true)
                    onDismiss()
                }
                is DialerEvent.OfferGsmCall -> {
                    gsmOfferNumber = event.phoneNumber
                }
                is DialerEvent.Error -> errorMessage = event.message
            }
        }
    }

    if (gsmOfferNumber != null) {
        AlertDialog(
            onDismissRequest = { gsmOfferNumber = null },
            title = { Text("User Offline") },
            text = { Text("This user is currently offline. Would you like to call them via GSM instead?") },
            confirmButton = {
                TextButton(onClick = { 
                    val number = gsmOfferNumber!!
                    gsmOfferNumber = null
                    val uri = android.net.Uri.fromParts("tel", number, null)
                    val intent = android.content.Intent(android.content.Intent.ACTION_CALL, uri).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        currentComposeContext.startActivity(intent)
                    } catch (e: SecurityException) {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, uri).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        currentComposeContext.startActivity(dialIntent)
                    }
                }) { Text("Call GSM") }
            },
            dismissButton = {
                TextButton(onClick = { gsmOfferNumber = null }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // T9 Suggestions
            if (suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onDialerInput(suggestion.phoneNumber) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeeftalkAvatar(
                                avatarUrl = suggestion.avatarUrl, 
                                initials = AvatarUtils.getInitials(suggestion.name), 
                                seed = suggestion.phoneNumber,
                                size = 32.dp,
                                isKeeftalkUser = suggestion.isKeeftalkUser
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(suggestion.name, style = MaterialTheme.typography.bodyLarge)
                                Text(suggestion.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Country Picker & Search Result
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showCountryPicker = true }) {
                        Text("${currentCountry.flagEmoji} ${currentCountry.phoneCode}", style = MaterialTheme.typography.labelLarge)
                    }
                    if (searchResult != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            KeeftalkAvatar(
                                avatarUrl = searchResult?.avatarUrl, 
                                initials = AvatarUtils.getInitials(searchResult?.fullName ?: searchResult?.username), 
                                seed = searchResult?.id,
                                size = 24.dp,
                                isKeeftalkUser = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = searchResult?.fullName ?: searchResult?.username ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(icons.close, contentDescription = "Close", modifier = Modifier.size(24.dp))
                }
            }

            // Display Number
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.weight(1f).clickable {
                        clipboardManager.getText()?.let { textData ->
                            viewModel.onDialerInput(textData.text.filter { char -> char.isDigit() || char == '+' || char == '*' || char == '#' })
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedNumber.ifEmpty { "Enter number" },
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = if (formattedNumber.length > 12) 28.sp else 42.sp
                        ),
                        color = if (formattedNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 48.dp) // Offset for backspace
                    )
                }

                if (dialerInput.isNotEmpty()) {
                    IconButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.backspace() 
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = icons.backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad
            val keys = listOf(
                "1" to "", "2" to "ABC", "3" to "DEF",
                "4" to "GHI", "5" to "JKL", "6" to "MNO",
                "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
                "*" to "", "0" to "+", "#" to ""
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        for (j in 0 until 3) {
                            val index = i * 3 + j
                            val key = keys[index]
                            DialerButton(
                                digit = key.first,
                                letters = key.second,
                                onClick = { viewModel.appendDigit(key.first) },
                                onLongClick = { 
                                    if (key.first == "0") viewModel.appendDigit("+")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Message Action (Left)
                IconButton(
                    onClick = { viewModel.onMessageClick() },
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    enabled = dialerInput.isNotEmpty()
                ) {
                    Icon(
                        imageVector = icons.chat,
                        contentDescription = "Message", 
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                var showCallMenu by remember { mutableStateOf(false) }

                // Call Action (Center)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KeeftalkFab(
                        icon = icons.phone,
                        onClick = { viewModel.onCallClick() },
                        enabled = dialerInput.isNotEmpty(),
                        size = 80.dp,
                        showPulse = false,
                        onLongClick = {
                            if (decision is DialerDecision.KeeftalkCall) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showCallMenu = true
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = showCallMenu,
                        onDismissRequest = { showCallMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Keeftalk Voice") },
                            leadingIcon = { Icon(icons.phone, null) },
                            onClick = {
                                showCallMenu = false
                                viewModel.onCallClick(false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Keeftalk Video") },
                            leadingIcon = { Icon(icons.camera, null) },
                            onClick = {
                                showCallMenu = false
                                viewModel.onCallClick(true)
                            }
                        )
                    }
                }

                // Clear All / Trash (Right)
                IconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearDialer() 
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape),
                    enabled = dialerInput.isNotEmpty()
                ) {
                    Icon(
                        imageVector = icons.delete,
                        contentDescription = "Clear All",
                        tint = if (dialerInput.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }

    if (showCountryPicker) {
        com.keeftalk.chat.ui.components.CountryPickerDialog(
            onCountrySelected = { viewModel.updateCountryCode(it) },
            onDismissRequest = { showCountryPicker = false }
        )
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("User not registered") },
            text = { Text("This phone number is not registered on Keeftalk.") },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Invite User") }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            },
            title = { Text("Notice") },
            text = { Text(errorMessage!!) }
        )
    }
}

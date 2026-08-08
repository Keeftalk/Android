package com.keeftalk.chat.feature.email.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.keeftalk.chat.feature.email.model.EmailAccount
import com.keeftalk.chat.feature.email.model.EmailMessage
import com.keeftalk.chat.feature.email.model.FolderType
import com.keeftalk.chat.feature.email.viewmodel.EmailViewModel
import com.keeftalk.chat.feature.email.viewmodel.OnboardingStep
import com.keeftalk.chat.ui.components.FabActionType
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailScreen(
    viewModel: EmailViewModel,
    onBack: () -> Unit,
    onCompose: (String?, String?, String?) -> Unit,
    onSettingsClick: () -> Unit,
    onEmailClick: (String) -> Unit,
    fabActionFlow: kotlinx.coroutines.flow.SharedFlow<com.keeftalk.chat.ui.components.FabActionType>? = null
) {
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val onboardingStep by viewModel.onboardingStep.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("EmailScreen", "Google Sign-In result received: ${result.resultCode}, data null: ${result.data == null}")
        // Always attempt to handle the result to capture detailed error codes in the ViewModel
        viewModel.handleGoogleSignInResult(result.data)
    }

    if (showOnboarding) {
        when (onboardingStep) {
            OnboardingStep.ProviderSelection -> {
                EmailOnboardingScreen(
                    onBack = onBack,
                    onConnectGmail = { googleSignInLauncher.launch(viewModel.getGoogleSignInIntent()) },
                    onConnectMicrosoft = { 
                        (context as? android.app.Activity)?.let { viewModel.connectMicrosoft(it) }
                    },
                    onConnectOther = { viewModel.setOnboardingStep(OnboardingStep.ImapSetup) }
                )
            }
            OnboardingStep.ImapSetup -> {
                val isSyncing by viewModel.isSyncing.collectAsState()
                ImapSetupScreen(
                    isSyncing = isSyncing,
                    onBack = { viewModel.setOnboardingStep(OnboardingStep.ProviderSelection) },
                    onTestConnection = { email, pass, imapH, imapP, smtpH, smtpP ->
                        viewModel.testImapConnection(email, pass, imapH, imapP, smtpH, smtpP)
                    }
                )
            }
            is OnboardingStep.Syncing -> {
                SyncingProgressScreen(
                    provider = (onboardingStep as OnboardingStep.Syncing).provider,
                    onSkip = { viewModel.selectAccount(null) } // This will force show merged inbox if any account exists
                )
            }
        }
    } else {
        EmailInboxContent(
            viewModel = viewModel,
            onBack = onBack,
            onCompose = onCompose,
            onSettingsClick = onSettingsClick,
            onEmailClick = onEmailClick,
            fabActionFlow = fabActionFlow
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun EmailInboxContent(
    viewModel: EmailViewModel,
    onBack: () -> Unit,
    onCompose: (String?, String?, String?) -> Unit,
    onSettingsClick: () -> Unit,
    onEmailClick: (String) -> Unit,
    fabActionFlow: kotlinx.coroutines.flow.SharedFlow<com.keeftalk.chat.ui.components.FabActionType>? = null
) {
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedFolderType by viewModel.selectedFolderType.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    val selectedMessageIds by viewModel.selectedMessageIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()

    LaunchedEffect(fabActionFlow) {
        fabActionFlow?.collect { action ->
            if (action == com.keeftalk.chat.ui.components.FabActionType.TOGGLE_DRAWER) {
                scope.launch { drawerState.open() }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isSelectionMode,
        drawerContent = {
            EmailDrawerContent(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                selectedFolderType = selectedFolderType,
                onAccountSelect = { 
                    viewModel.selectAccount(it)
                    scope.launch { 
                        drawerState.close()
                        if (navigator.canNavigateBack()) navigator.navigateBack()
                    }
                },
                onFolderSelect = {
                    viewModel.selectFolderType(it)
                    scope.launch { 
                        drawerState.close()
                        if (navigator.canNavigateBack()) navigator.navigateBack()
                    }
                },
                onSettingsClick = {
                    onSettingsClick()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (isSelectionMode) {
                            SelectionTopAppBar(
                                count = selectedMessageIds.size,
                                onClear = { viewModel.clearSelection() },
                                onDelete = { viewModel.deleteSelected() },
                                onArchive = { viewModel.archiveSelected() },
                                onMarkRead = { viewModel.markSelectedAsRead(true) },
                                onStar = { viewModel.starSelected(true) }
                            )
                        } else if (isSearchActive) {
                            SearchTopAppBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                onClose = { 
                                    isSearchActive = false
                                    viewModel.setSearchQuery("") 
                                }
                            )
                        }
                        // Global top bar is handled in MainActivity
                    },
                    floatingActionButton = {
                        // FAB is now managed globally by AdaptiveFab.kt
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        if (!isSelectionMode && !isSearchActive) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                                if (isSyncing) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        PullToRefreshBox(
                            state = pullToRefreshState,
                            isRefreshing = isSyncing,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.weight(1f)
                        ) {
                            if (messages.itemCount == 0 && (messages.loadState.refresh is androidx.paging.LoadState.NotLoading)) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    item {
                                        EmptyInboxState(modifier = Modifier.fillParentMaxSize())
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        count = messages.itemCount,
                                        key = messages.itemKey { it.id },
                                        contentType = messages.itemContentType { "email" }
                                    ) { index ->
                                        val message = messages[index]
                                        if (message != null) {
                                            val isSelected = selectedMessageIds.contains(message.id)
                                            EmailItem(
                                                message = message,
                                                isSelected = isSelected,
                                                selectionMode = isSelectionMode,
                                                onClick = { 
                                                    if (isSelectionMode) {
                                                        viewModel.toggleMessageSelection(message.id)
                                                    } else {
                                                        scope.launch {
                                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, message.id)
                                                        }
                                                        onEmailClick(message.id) 
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isSelectionMode) {
                                                        viewModel.toggleMessageSelection(message.id)
                                                    }
                                                },
                                                onStarClick = { viewModel.toggleStar(message.id, !message.isStarred) },
                                                onArchive = { 
                                                    viewModel.archiveMessage(message.id)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Message archived")
                                                    }
                                                },
                                                onDelete = { 
                                                    viewModel.deleteMessage(message.id)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Message deleted")
                                                    }
                                                }
                                            )
                                            HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(start = 72.dp)
                                            )
                                        } else {
                                            EmailItemPlaceholder()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            detailPane = {
                val emailId = navigator.currentDestination?.contentKey
                if (emailId != null) {
                    EmailDetailScreen(
                        messageId = emailId,
                        viewModel = viewModel,
                        onBack = { 
                            scope.launch { navigator.navigateBack() }
                        },
                        onDelete = {
                            scope.launch { navigator.navigateBack() }
                        },
                        onReply = { msg ->
                            onCompose(msg.senderEmail, "Re: ${msg.subject}", "<br><br>---<br>${msg.content}")
                        },
                        onForward = { msg ->
                            onCompose("", "Fwd: ${msg.subject}", "<br><br>--- Forwarded message ---<br>From: ${msg.senderName} &lt;${msg.senderEmail}&gt;<br>Subject: ${msg.subject}<br><br>${msg.content}")
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select an email to read", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}

@Composable
fun EmailDrawerContent(
    accounts: List<EmailAccount>,
    selectedAccountId: String?,
    selectedFolderType: FolderType,
    onAccountSelect: (String?) -> Unit,
    onFolderSelect: (FolderType) -> Unit,
    onSettingsClick: () -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        Text(
            "Keeftalk Mail",
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("All Inboxes") },
            selected = selectedAccountId == null,
            onClick = { onAccountSelect(null) },
            icon = { Icon(Icons.Default.AllInbox, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        accounts.forEach { account ->
            NavigationDrawerItem(
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(account.emailAddress, modifier = Modifier.weight(1f))
                        if (account.unreadCount > 0) {
                            Text(
                                text = account.unreadCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                selected = selectedAccountId == account.id,
                onClick = { onAccountSelect(account.id) },
                icon = { 
                    KeeftalkAvatar(
                        avatarUrl = account.profilePicUrl,
                        initials = getInitials(account.displayName),
                        seed = account.emailAddress,
                        size = 24.dp
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        DrawerFolderItem("Inbox", Icons.Default.Inbox, FolderType.INBOX, selectedFolderType, 0, onFolderSelect) // Unread count for folder is harder without per-folder flow, keeping at 0 or passing if available
        DrawerFolderItem("Starred", Icons.Default.Star, FolderType.STARRED, selectedFolderType, 0, onFolderSelect)
        DrawerFolderItem("Important", Icons.AutoMirrored.Filled.LabelImportant, FolderType.IMPORTANT, selectedFolderType, 0, onFolderSelect)
        DrawerFolderItem("Sent", Icons.AutoMirrored.Filled.Send, FolderType.SENT, selectedFolderType, 0, onFolderSelect)
        DrawerFolderItem("Drafts", Icons.Default.Description, FolderType.DRAFTS, selectedFolderType, 0, onFolderSelect)
        DrawerFolderItem("Archive", Icons.Default.Archive, FolderType.ARCHIVE, selectedFolderType, 0, onFolderSelect)
        DrawerFolderItem("Spam", Icons.Default.Report, FolderType.SPAM, selectedFolderType, 0, onFolderSelect)
        DrawerFolderItem("Trash", Icons.Default.Delete, FolderType.TRASH, selectedFolderType, 0, onFolderSelect)
        DrawerFolderItem("Shared", Icons.Default.Share, FolderType.SHARED, selectedFolderType, 0, onFolderSelect)

        Spacer(Modifier.weight(1f))
        
        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun DrawerFolderItem(
    label: String,
    icon: ImageVector,
    folderType: FolderType,
    selectedFolderType: FolderType,
    unreadCount: Int,
    onFolderSelect: (FolderType) -> Unit
) {
    NavigationDrawerItem(
        label = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.weight(1f))
                if (unreadCount > 0) {
                    Text(
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        selected = selectedFolderType == folderType,
        onClick = { onFolderSelect(folderType) },
        icon = { Icon(icon, null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun SyncingProgressScreen(
    provider: com.keeftalk.chat.feature.email.model.EmailProvider,
    onSkip: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Syncing your $provider...",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onSkip) {
            Text("Go to Inbox (Debug)")
        }
    }
}

@Composable
fun EmptyInboxState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No emails yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Your inbox is empty.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search emails") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    count: Int,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onMarkRead: () -> Unit,
    onStar: () -> Unit
) {
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
            }
        },
        actions = {
            IconButton(onClick = onMarkRead) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = "Mark as Read")
            }
            IconButton(onClick = onStar) {
                Icon(Icons.Default.Star, contentDescription = "Star")
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Archive, contentDescription = "Archive")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EmailItem(
    message: EmailMessage,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStarClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (selectionMode) return@rememberSwipeToDismissBoxState false
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onArchive()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        },
        // Adjust threshold to be more deliberate (0.6f means 60% of width)
        positionalThreshold = { it * 0.6f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !selectionMode,
        enableDismissFromEndToStart = !selectionMode,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            // Only show background if actually swiping to prevent leak through transparent surfaces
            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                val color = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                ) {
                    Icon(
                        imageVector = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive
                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                            else -> Icons.Default.Delete
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            color = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else if (message.isUnread) 
                MaterialTheme.colorScheme.surface
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // Use solid-ish variant for read
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(contentAlignment = Alignment.Center) {
                    KeeftalkAvatar(
                        avatarUrl = message.senderProfilePicUrl,
                        initials = getInitials(message.senderName),
                        seed = message.senderEmail,
                        size = 40.dp
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message.senderName.substringBefore("<").trim(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (message.isUnread) FontWeight.Bold else FontWeight.Normal,
                                color = if (message.isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = " | ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                text = message.senderEmail.replace("<", "").replace(">", "").trim(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.65f
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (message.isUnread) FontWeight.Bold else FontWeight.Normal,
                            color = if (message.isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    Text(
                        text = message.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (message.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (message.isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (message.isUnread) 0.7f else 0.4f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (message.isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(start = 4.dp),
                                tint = Color(0xFFFFB300)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmailItemPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).background(Color.Gray.copy(alpha = 0.2f), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
            }
        }
    }
}

private fun getInitials(name: String): String {
    return name.split(" ")
        .filter { it.isNotEmpty() }
        .mapNotNull { it.firstOrNull() }
        .take(2)
        .joinToString("")
        .uppercase()
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val msgDate = Calendar.getInstance().apply { time = date }
    
    return if (now.get(Calendar.DATE) == msgDate.get(Calendar.DATE)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

package tn.keeftalk.chat.ui.components

import tn.keeftalk.chat.ui.theme.LucideIcons

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.AppScreen
import tn.keeftalk.chat.ui.vault.CloudImportFab
import tn.keeftalk.chat.ui.theme.FabCyan
import tn.keeftalk.chat.ui.theme.FabGradient
import tn.keeftalk.chat.ui.theme.FabPurple
import tn.keeftalk.chat.ui.theme.LocalAppIcons

enum class FabActionType {
    NEW_CHAT, NEW_GROUP, AI, NEW_SMS, ADD_CONTACT, DIALER, NEW_NOTE,
    EVENT, TASK, REMINDER, BIRTHDAY, MEETING, GOAL,
    UPLOAD_FILE, NEW_FOLDER, QR, WALLET, EMAIL, COMPOSE_EMAIL,
    TOGGLE_NIGHT_MODE, INCREASE_SCALE, DECREASE_SCALE, TOGGLE_DRAWER,
    CLOUD_IMPORT, NEW_HABIT, EDIT_HABITS, ADD_BOOK_FILE, IMPORT_BOOK_VAULT,
    ADD_FINANCES, FINANCES_TOOLS, SEARCH,
    IMPORT_DOC_FILES, IMPORT_DOC_FOLDER,
    ADD_2FA_ENTRY, SCAN_QR_CAMERA_SECRETS, SCAN_QR_IMAGE_SECRETS, ADD_SECRET_CREDENTIAL, SECRETS_GENERATOR, IMPORT_CSV_SECRETS
}

private data class QuickToolItem(
    val label: String,
    val icon: ImageVector,
    val isActive: Boolean,
    val action: () -> Unit
)

data class FabMenuItem(
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit,
    val isPrimary: Boolean = false,
    val isDivider: Boolean = false,
    val isActive: Boolean = false,
    val trailingAvatars: List<String?> = emptyList(),
    val module: tn.keeftalk.chat.domain.model.KeeftalkModule? = null,
    val actionType: tn.keeftalk.chat.domain.model.FabAction? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeeftalkFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    showPulse: Boolean = true,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bouncyEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 100)
        } else {
            spring(
                dampingRatio = 0.6f,
                stiffness = 400f
            )
        },
        label = "fab_scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "fab_rotation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongClick
            )
    ) {
        // Pulse Ring
        if (showPulse && !isExpanded && enabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .border(2.dp, FabCyan.copy(alpha = 0.15f), CircleShape)
            )
        }

        // FAB Background & Shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (enabled) 28.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = FabCyan.copy(alpha = 0.3f),
                    ambientColor = Color(0x99000000)
                )
                .clip(CircleShape)
                .background(FabGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(size * 0.47f)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            )
        }
    }
}

@Composable
fun AdaptiveFab(
    currentScreen: AppScreen,
    bottomTab: tn.keeftalk.chat.domain.model.KeeftalkModule,
    chatTab: Int,
    emailAccounts: List<tn.keeftalk.chat.feature.email.model.EmailAccount> = emptyList(),
    isSearching: Boolean = false,
    onNavigate: (AppScreen) -> Unit,
    onBottomTabChange: (tn.keeftalk.chat.domain.model.KeeftalkModule) -> Unit = {},
    onAction: (FabActionType) -> Unit,
    modifier: Modifier = Modifier,
    showBottomBar: Boolean = false
) {
    val icons = LocalAppIcons.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showNotificationsView by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (!expanded) {
            showNotificationsView = false
        }
    }
    
    val mainViewModel: tn.keeftalk.chat.ui.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userPrefs by mainViewModel.userPreferences.collectAsState()
    val customization = userPrefs.appCustomization
    val notifications by mainViewModel.notifications.collectAsState()

    val currentChatIcon = if (chatTab == 0) icons.messageCircle else icons.messageSquare
    val emailAvatars = emailAccounts.map { it.profilePicUrl }

    val isNightActive = userPrefs.extraNightModeEnabled
    val isIncreaseActive = userPrefs.uiScale > 1.0f
    val isDecreaseActive = userPrefs.uiScale < 1.0f
    val isSearchActive = isSearching
    val isNotificationsActive = showNotificationsView || notifications.any { !it.isRead }

    val quickTools = listOf(
        QuickToolItem("Extra Night Mode", Icons.Default.Nightlight, isNightActive) { onAction(FabActionType.TOGGLE_NIGHT_MODE) },
        QuickToolItem("Increase Size (A+)", Icons.Default.Add, isIncreaseActive) { onAction(FabActionType.INCREASE_SCALE) },
        QuickToolItem("Decrease Size (A-)", Icons.Default.Remove, isDecreaseActive) { onAction(FabActionType.DECREASE_SCALE) },
        QuickToolItem("Search", icons.search, isSearchActive) { onAction(FabActionType.SEARCH); expanded = false },
        QuickToolItem("Notifications", icons.bell, isNotificationsActive) { showNotificationsView = !showNotificationsView }
    )

    // Configuration state based on current screen
    val closeMenu = { expanded = false }
    val commonNavItems = buildCommonNavigationItems(currentScreen, bottomTab, icons, currentChatIcon, emailAvatars, onNavigate, onBottomTabChange, closeMenu)
    val qrAndAiItems = listOf(
        FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); closeMenu() }),
        FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); closeMenu() }, actionType = tn.keeftalk.chat.domain.model.FabAction.AI)
    )
    val divider = FabMenuItem("", Icons.Default.Add, {}, isDivider = true)

    val primaryAction: FabMenuItem? = FabMenuItem("+", icons.add, { expanded = !expanded }, isPrimary = true)
    val secondaryAction: FabMenuItem? = if (currentScreen == AppScreen.ChatList && bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS) {
        FabMenuItem("Dialer", icons.dialpad, { onAction(FabActionType.DIALER) })
    } else {
        null
    }

    val rawMenuItems: List<FabMenuItem> = when {
        currentScreen == AppScreen.ChatList && bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS && chatTab == 0 -> {
            listOf(
                FabMenuItem("Find and Discover People", icons.messageCircle, { onAction(FabActionType.NEW_CHAT); closeMenu() }),
                FabMenuItem("New Group", Icons.Default.Group, { onAction(FabActionType.NEW_GROUP); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.ChatList && bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS && chatTab == 1 -> {
            listOf(
                FabMenuItem("New SMS", icons.messageSquare, { onAction(FabActionType.NEW_SMS); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.ChatList && bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS -> {
            listOf(
                FabMenuItem("Dialer", icons.dialpad, { onAction(FabActionType.DIALER); closeMenu() }),
                FabMenuItem("Add New Contact", icons.user, { onAction(FabActionType.ADD_CONTACT); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen is AppScreen.Notes -> {
            listOf(
                FabMenuItem("New Note", icons.edit, { onAction(FabActionType.NEW_NOTE); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Calendar || currentScreen is AppScreen.CalendarEditor -> {
            listOf(
                FabMenuItem("Event", Icons.Default.Event, { onAction(FabActionType.EVENT); closeMenu() }),
                FabMenuItem("Task", Icons.Default.TaskAlt, { onAction(FabActionType.TASK); closeMenu() }),
                FabMenuItem("Reminder", Icons.Default.NotificationsActive, { onAction(FabActionType.REMINDER); closeMenu() }),
                FabMenuItem("Birthday", Icons.Default.Cake, { onAction(FabActionType.BIRTHDAY); closeMenu() }),
                FabMenuItem("Meeting", Icons.Default.Groups, { onAction(FabActionType.MEETING); closeMenu() }),
                FabMenuItem("Goal", Icons.Default.Flag, { onAction(FabActionType.GOAL); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Vault -> {
            listOf(
                FabMenuItem("Upload File", Icons.Default.UploadFile, { onAction(FabActionType.UPLOAD_FILE); closeMenu() }),
                FabMenuItem("New Folder", Icons.Default.CreateNewFolder, { onAction(FabActionType.NEW_FOLDER); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Secrets || currentScreen == AppScreen.SecretsSettings -> {
            listOf(
                FabMenuItem("Add 2FA Entry", Icons.Default.Timer, { onAction(FabActionType.ADD_2FA_ENTRY); closeMenu() }),
                FabMenuItem("Scan QR Code", Icons.Default.QrCodeScanner, { onAction(FabActionType.SCAN_QR_CAMERA_SECRETS); closeMenu() }),
                FabMenuItem("Read QR from Image", Icons.Default.Image, { onAction(FabActionType.SCAN_QR_IMAGE_SECRETS); closeMenu() }),
                FabMenuItem("Add Credential", LucideIcons.Key, { onAction(FabActionType.ADD_SECRET_CREDENTIAL); closeMenu() }),
                FabMenuItem("Password Generator", Icons.Default.Refresh, { onAction(FabActionType.SECRETS_GENERATOR); closeMenu() }),
                FabMenuItem("Import from CSV", Icons.Default.UploadFile, { onAction(FabActionType.IMPORT_CSV_SECRETS); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Wallet -> {
            listOf(
                FabMenuItem("Scan & Pay", icons.qrCode, { onAction(FabActionType.QR); closeMenu() }),
                FabMenuItem("Add Funds", Icons.Default.AddCard, { onAction(FabActionType.WALLET); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Email -> {
            listOf(
                FabMenuItem("New E-Mail", Icons.Default.Edit, { onAction(FabActionType.COMPOSE_EMAIL); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Habits -> {
            listOf(
                FabMenuItem("New Habit", Icons.Default.CheckCircle, { onAction(FabActionType.NEW_HABIT); closeMenu() }),
                FabMenuItem("Edit Habits", Icons.Default.Edit, { onAction(FabActionType.EDIT_HABITS); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Feed -> {
            listOf(
                FabMenuItem("Manage Feeds", icons.edit, { onNavigate(AppScreen.ManageFeeds); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Books -> {
            listOf(
                FabMenuItem("Add Books ", androidx.compose.material.icons.Icons.AutoMirrored.Filled.MenuBook, { onAction(FabActionType.ADD_BOOK_FILE); closeMenu() }),
                FabMenuItem("Import from Vault", LucideIcons.FolderLock, { onAction(FabActionType.IMPORT_BOOK_VAULT); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Docs -> {
            listOf(
                FabMenuItem("Import Files", Icons.Default.Description, { onAction(FabActionType.IMPORT_DOC_FILES); closeMenu() }),
                FabMenuItem("Import Folder", Icons.Default.FolderOpen, { onAction(FabActionType.IMPORT_DOC_FOLDER); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        currentScreen == AppScreen.Finances -> {
            listOf(
                FabMenuItem("Add to My Finances", Icons.Default.Add, { onAction(FabActionType.ADD_FINANCES); closeMenu() }),
                FabMenuItem("Tools", Icons.Default.Build, { onAction(FabActionType.FINANCES_TOOLS); closeMenu() }),
                divider
            ) + qrAndAiItems + divider + commonNavItems
        }
        else -> {
            qrAndAiItems + divider + commonNavItems
        }
    }

    val menuItems: List<FabMenuItem> = rawMenuItems.filter { item ->
        if (customization.isMinimalMode) {
            true
        } else if (customization.isEnabled) {
            val moduleOk = item.module == null || customization.enabledModules.contains(item.module)
            val actionOk = item.actionType == null || customization.enabledFabActions.contains(item.actionType)
            
            // SPECIAL: Hide "My Chats" if it's the only module enabled (beside mandatory actions)
            // Chats is always enabled. So if size is 1, it's just Chats.
            val isRedundantChats = item.module == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS && customization.enabledModules.size == 1
            
            moduleOk && actionOk && !isRedundantChats
        } else {
            // Revert to mandatory items if customization is disabled
            // Mandatory Modules: CHATS. Mandatory Actions: NEW_CHAT, NEW_GROUP, QR.
            // We also show CALLS by default if customization is OFF generally.
            val isMandatoryModule = item.module == null || item.module == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS || item.module == tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS
            val isMandatoryAction = item.actionType == null || tn.keeftalk.chat.domain.model.AppCustomization.MANDATORY_FAB_ACTIONS.contains(item.actionType)
            isMandatoryModule && isMandatoryAction
        }
    }

    if (primaryAction == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(bottom = if (showBottomBar) 80.dp else 16.dp)
            .padding(end = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Menu content
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(400)) + expandVertically(animationSpec = tween(500), expandFrom = Alignment.Bottom),
            exit = fadeOut(animationSpec = tween(400)) + shrinkVertically(animationSpec = tween(500), shrinkTowards = Alignment.Bottom)
        ) {
            val fabMenuShape = RoundedCornerShape(24.dp)
            Surface(
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .fillMaxWidth(0.75f)
                    .shadow(8.dp, shape = fabMenuShape)
                    .clip(fabMenuShape),
                shape = fabMenuShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                AnimatedContent(
                    targetState = showNotificationsView,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "fab_content_transition"
                ) { isNotifications ->
                    if (isNotifications) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 250.dp, max = 380.dp)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { showNotificationsView = false }) {
                                    Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Notifications",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { mainViewModel.markAllNotificationsAsRead() }) {
                                    Text("Mark All Read", fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                            if (notifications.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No notifications", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f, false),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(notifications) { notif ->
                                        Surface(
                                            onClick = {
                                                mainViewModel.handleNotificationClick(notif)
                                                showNotificationsView = false
                                                expanded = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(notif.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(fabMenuShape)
                                .padding(vertical = 4.dp)
                        ) {
                        // Reduced-size Quick Tools Row (Night, +, -, Search, Bell)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            quickTools.forEach { tool ->
                                IconButton(
                                    onClick = tool.action,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .then(
                                            if (tool.isActive) {
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(FabGradient)
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    Icon(
                                        tool.icon,
                                        contentDescription = tool.label,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (tool.isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        val actions = menuItems.filter { item ->
                            !item.label.startsWith("My ") && item.label != "Settings" && !item.isDivider && 
                            item.label != "Extra Night Mode" && 
                            item.label != "Increase Size (A+)" && 
                            item.label != "Decrease Size (A-)"
                        }
                        val navItems = menuItems.filter { it.label.startsWith("My ") || it.label == "Settings" }
                        
                        actions.forEachIndexed { index, item ->
                            MenuItem(item, index)
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            navItems.chunked(2).forEachIndexed { rowIndex, rowItems ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEachIndexed { colIndex, item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            MenuItem(item, actions.size + rowIndex * 2 + colIndex, isCompact = true)
                                        }
                                    }
                                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // FABs
        val primarySize = 64.dp * customization.fabSizeMultiplier
        val secondarySize = 56.dp * customization.secondaryFabSizeMultiplier

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 0.dp) // Reset local padding to rely on outer Box
        ) {
            if (currentScreen == AppScreen.Vault) {
                CloudImportFab(
                    onClick = { onAction(FabActionType.CLOUD_IMPORT) }
                )
            }

            // Secondary FAB (e.g. The shared + FAB in Email/SMS/Calls)
            if (secondaryAction != null) {
                AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    KeeftalkFab(
                        icon = secondaryAction.icon,
                        onClick = secondaryAction.action,
                        modifier = Modifier.size(secondarySize), // A bit smaller
                        isExpanded = false,
                        showPulse = false // Only pulse the main one if it's +
                    )
                }
            }

            // Primary FAB (Always in the same spot as the '+' in main screen)
            val unreadCount = notifications.count { !it.isRead }
            val showFabBadge = customization.isMinimalMode && unreadCount > 0

            if (showFabBadge) {
                BadgedBox(
                    badge = {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color(0xFFEF4444), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(unreadCount.toString(), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    KeeftalkFab(
                        icon = primaryAction.icon,
                        onClick = primaryAction.action,
                        modifier = Modifier.size(primarySize),
                        isExpanded = if (secondaryAction == null) expanded else false,
                        showPulse = primaryAction.icon == icons.add && !expanded
                    )
                }
            } else {
                KeeftalkFab(
                    icon = primaryAction.icon,
                    onClick = primaryAction.action,
                    modifier = Modifier.size(primarySize),
                    isExpanded = if (secondaryAction == null) expanded else false,
                    showPulse = primaryAction.icon == icons.add && !expanded
                )
            }
        }
    }
}

@Composable
fun MenuItem(
    item: FabMenuItem,
    index: Int,
    isCompact: Boolean = false
) {
    val animDelay = index * 100
    val transition = updateTransition(targetState = true, label = "MenuItem")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 600, delayMillis = animDelay) },
        label = "alpha"
    ) { if (it) 1f else 0f }
    val slide by transition.animateDp(
        transitionSpec = { tween(durationMillis = 600, delayMillis = animDelay) },
        label = "slide"
    ) { if (it) 0.dp else 20.dp }

    val itemShape = RoundedCornerShape(12.dp)
    Surface(
        onClick = item.action,
        color = Color.Transparent,
        shape = itemShape,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = slide)
            .padding(horizontal = if (isCompact) 4.dp else 8.dp, vertical = if (isCompact) 1.dp else 2.dp)
            .clip(itemShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (item.isActive) {
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(FabGradient)
                    } else Modifier
                )
                .padding(
                    horizontal = if (isCompact) 8.dp else 12.dp,
                    vertical = if (isCompact) 6.dp else 10.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (isCompact) 18.dp else 20.dp),
                    tint = if (item.isActive) Color.White else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 16.dp))
                Text(
                    item.label,
                    style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = if (item.isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (item.isActive) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (item.trailingAvatars.isNotEmpty()) {
                    Box(
                        modifier = Modifier.padding(start = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        item.trailingAvatars.take(3).forEachIndexed { avatarIndex, avatarUrl ->
                            KeeftalkAvatar(
                                avatarUrl = avatarUrl,
                                initials = "?",
                                seed = avatarUrl,
                                size = 24.dp,
                                modifier = Modifier
                                    .padding(end = (avatarIndex * 12).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildCommonNavigationItems(
    currentScreen: AppScreen,
    bottomTab: tn.keeftalk.chat.domain.model.KeeftalkModule,
    icons: tn.keeftalk.chat.ui.theme.AppIcons,
    currentChatIcon: ImageVector,
    emailAvatars: List<String?>,
    onNavigate: (AppScreen) -> Unit,
    onBottomTabChange: (tn.keeftalk.chat.domain.model.KeeftalkModule) -> Unit,
    closeMenu: () -> Unit
): List<FabMenuItem> {
    return listOf(
        FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); onBottomTabChange(tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS); closeMenu() }, isActive = currentScreen is AppScreen.ChatList && bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS, module = tn.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
        FabMenuItem("My Calls", icons.phone, { onNavigate(AppScreen.ChatList); onBottomTabChange(tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS); closeMenu() }, isActive = currentScreen is AppScreen.ChatList && bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS, module = tn.keeftalk.chat.domain.model.KeeftalkModule.CALLS),
        FabMenuItem("My SMS/MMS", icons.messageSquare, { onNavigate(AppScreen.ChatList); onBottomTabChange(tn.keeftalk.chat.domain.model.KeeftalkModule.SMS); closeMenu() }, isActive = currentScreen is AppScreen.ChatList && bottomTab == tn.keeftalk.chat.domain.model.KeeftalkModule.SMS, module = tn.keeftalk.chat.domain.model.KeeftalkModule.SMS),
        FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); closeMenu() }, isActive = currentScreen is AppScreen.Notes, module = tn.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
        FabMenuItem("My Vault", icons.folderLock, { onNavigate(AppScreen.Vault); closeMenu() }, isActive = currentScreen == AppScreen.Vault, module = tn.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
        FabMenuItem("My Secrets", icons.lock, { onNavigate(AppScreen.Secrets); closeMenu() }, isActive = currentScreen == AppScreen.Secrets || currentScreen == AppScreen.SecretsSettings, module = tn.keeftalk.chat.domain.model.KeeftalkModule.SECRETS),
        FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); closeMenu() }, isActive = currentScreen == AppScreen.Calendar || currentScreen is AppScreen.CalendarEditor, module = tn.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
        FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); closeMenu() }, isActive = currentScreen == AppScreen.Wallet, module = tn.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
        FabMenuItem("My Finances", icons.finances, { onNavigate(AppScreen.Finances); closeMenu() }, isActive = currentScreen == AppScreen.Finances, module = tn.keeftalk.chat.domain.model.KeeftalkModule.FINANCES),
        FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); closeMenu() }, isActive = currentScreen == AppScreen.Email, trailingAvatars = emailAvatars, module = tn.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
        FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); closeMenu() }, isActive = currentScreen == AppScreen.Feed, module = tn.keeftalk.chat.domain.model.KeeftalkModule.FEED),
        FabMenuItem("My Books", Icons.AutoMirrored.Filled.MenuBook, { onNavigate(AppScreen.Books); closeMenu() }, isActive = currentScreen == AppScreen.Books, module = tn.keeftalk.chat.domain.model.KeeftalkModule.BOOKS),
        FabMenuItem("My Docs", Icons.Default.Description, { onNavigate(AppScreen.Docs); closeMenu() }, isActive = currentScreen == AppScreen.Docs, module = tn.keeftalk.chat.domain.model.KeeftalkModule.DOCS),
        FabMenuItem("My Market", Icons.Default.Storefront, { onNavigate(AppScreen.Market); closeMenu() }, isActive = currentScreen == AppScreen.Market, module = tn.keeftalk.chat.domain.model.KeeftalkModule.MARKET),
        FabMenuItem("My Habits", Icons.Default.CheckCircle, { onNavigate(AppScreen.Habits); closeMenu() }, isActive = currentScreen == AppScreen.Habits, module = tn.keeftalk.chat.domain.model.KeeftalkModule.HABITS),
        FabMenuItem("My Todo", tn.keeftalk.chat.ui.theme.LucideIcons.ListTodo, { onNavigate(AppScreen.Todo); closeMenu() }, isActive = currentScreen == AppScreen.Todo, module = tn.keeftalk.chat.domain.model.KeeftalkModule.TODO),
        FabMenuItem("My Browser", Icons.Default.Language, { onNavigate(AppScreen.Browser()); closeMenu() }, isActive = currentScreen is AppScreen.Browser, module = tn.keeftalk.chat.domain.model.KeeftalkModule.BROWSER),
        FabMenuItem("My Maps", tn.keeftalk.chat.ui.theme.LucideIcons.MapPinned, { onNavigate(AppScreen.MyMaps); closeMenu() }, isActive = currentScreen is AppScreen.MyMaps, module = tn.keeftalk.chat.domain.model.KeeftalkModule.MAPS),
        FabMenuItem("My Din", Icons.Default.Mosque, { onNavigate(AppScreen.MyDin); closeMenu() }, isActive = currentScreen == AppScreen.MyDin, module = tn.keeftalk.chat.domain.model.KeeftalkModule.MY_DIN),
        FabMenuItem("Settings", Icons.Default.Settings, { onNavigate(AppScreen.Settings); closeMenu() }, isActive = currentScreen == AppScreen.Settings)
    )
}

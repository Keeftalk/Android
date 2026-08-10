package com.keeftalk.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import com.keeftalk.chat.AppScreen
import com.keeftalk.chat.ui.vault.CloudImportFab
import com.keeftalk.chat.ui.theme.FabCyan
import com.keeftalk.chat.ui.theme.FabGradient
import com.keeftalk.chat.ui.theme.FabPurple
import com.keeftalk.chat.ui.theme.LocalAppIcons

enum class FabActionType {
    NEW_CHAT, NEW_GROUP, AI, NEW_SMS, ADD_CONTACT, DIALER, NEW_NOTE,
    EVENT, TASK, REMINDER, BIRTHDAY, MEETING, GOAL,
    UPLOAD_FILE, NEW_FOLDER, QR, WALLET, EMAIL, COMPOSE_EMAIL,
    TOGGLE_NIGHT_MODE, INCREASE_SCALE, DECREASE_SCALE, TOGGLE_DRAWER,
    CLOUD_IMPORT
}

data class FabMenuItem(
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit,
    val isPrimary: Boolean = false,
    val isDivider: Boolean = false,
    val isActive: Boolean = false,
    val trailingAvatars: List<String?> = emptyList(),
    val module: com.keeftalk.chat.domain.model.KeeftalkModule? = null,
    val actionType: com.keeftalk.chat.domain.model.FabAction? = null
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
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
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
    bottomTab: com.keeftalk.chat.domain.model.KeeftalkModule,
    chatTab: Int,
    emailAccounts: List<com.keeftalk.chat.feature.email.model.EmailAccount> = emptyList(),
    onNavigate: (AppScreen) -> Unit,
    onAction: (FabActionType) -> Unit,
    modifier: Modifier = Modifier,
    showBottomBar: Boolean = false
) {
    val icons = LocalAppIcons.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    val mainViewModel: com.keeftalk.chat.ui.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userPrefs by mainViewModel.userPreferences.collectAsState()
    val customization = userPrefs.appCustomization

    val currentChatIcon = if (chatTab == 0) icons.messageCircle else icons.messageSquare
    val emailAvatars = emailAccounts.map { it.profilePicUrl }

    val accessibilityEnabled = customization.enabledFabActions.contains(com.keeftalk.chat.domain.model.FabAction.ACCESSIBILITY)
    val accessibilityItems = if (accessibilityEnabled) listOf(
        FabMenuItem("Extra Night Mode", Icons.Default.Nightlight, { onAction(FabActionType.TOGGLE_NIGHT_MODE) }),
        FabMenuItem("Increase Size (A+)", Icons.Default.Add, { onAction(FabActionType.INCREASE_SCALE) }),
        FabMenuItem("Decrease Size (A-)", Icons.Default.Remove, { onAction(FabActionType.DECREASE_SCALE) }),
        FabMenuItem("", Icons.Default.Add, {}, isDivider = true)
    ) else emptyList()

    // Configuration state based on current screen
    val (primaryAction, secondaryAction, rawMenuItems) = when {
        currentScreen == AppScreen.ChatList && bottomTab == com.keeftalk.chat.domain.model.KeeftalkModule.CHATS && chatTab == 0 -> {
            // Keeftalk Chats
            Triple(
                FabMenuItem("+", icons.add, { expanded = !expanded }, isPrimary = true),
                null,
                listOf(
                    FabMenuItem("New Chat", icons.messageCircle, { onAction(FabActionType.NEW_CHAT); expanded = false }),
                    FabMenuItem("New Group", Icons.Default.Group, { onAction(FabActionType.NEW_GROUP); expanded = false }),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, isActive = true, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen == AppScreen.ChatList && bottomTab == com.keeftalk.chat.domain.model.KeeftalkModule.CHATS && chatTab == 1 -> {
            // Keeftalk SMS
            Triple(
                FabMenuItem("New SMS", icons.messageSquare, { onAction(FabActionType.NEW_SMS); expanded = false }, isPrimary = true),
                FabMenuItem("+", icons.add, { expanded = !expanded }),
                listOf(
                    FabMenuItem("New SMS", icons.messageSquare, { onAction(FabActionType.NEW_SMS); expanded = false }),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen == AppScreen.ChatList && bottomTab == com.keeftalk.chat.domain.model.KeeftalkModule.CALLS -> {
            // Keeftalk Calls
            Triple(
                FabMenuItem("Dialer", icons.dialpad, { onAction(FabActionType.DIALER); expanded = false }, isPrimary = true),
                FabMenuItem("+", icons.add, { expanded = !expanded }),
                listOf(
                    FabMenuItem("Add New Contact", icons.user, { onAction(FabActionType.ADD_CONTACT); expanded = false }),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen is AppScreen.Notes -> {
            // Keeftalk Notes
            Triple(
                FabMenuItem("+", icons.add, { expanded = !expanded }, isPrimary = true),
                null,
                listOf(
                    FabMenuItem("New Note", icons.edit, { onAction(FabActionType.NEW_NOTE); expanded = false }),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, isActive = true, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen == AppScreen.Calendar || currentScreen is AppScreen.CalendarEditor -> {
            // Keeftalk Calendar
            Triple(
                FabMenuItem("+", icons.add, { expanded = !expanded }, isPrimary = true),
                null,
                listOf(
                    FabMenuItem("Event", Icons.Default.Event, { onAction(FabActionType.EVENT); expanded = false }),
                    FabMenuItem("Task", Icons.Default.TaskAlt, { onAction(FabActionType.TASK); expanded = false }),
                    FabMenuItem("Reminder", Icons.Default.NotificationsActive, { onAction(FabActionType.REMINDER); expanded = false }),
                    FabMenuItem("Birthday", Icons.Default.Cake, { onAction(FabActionType.BIRTHDAY); expanded = false }),
                    FabMenuItem("Meeting", Icons.Default.Groups, { onAction(FabActionType.MEETING); expanded = false }),
                    FabMenuItem("Goal", Icons.Default.Flag, { onAction(FabActionType.GOAL); expanded = false }),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen == AppScreen.Vault -> {
            // Keeftalk Vault
            Triple(
                FabMenuItem("+", icons.add, { expanded = !expanded }, isPrimary = true),
                null,
                listOf(
                    FabMenuItem("Upload File", Icons.Default.UploadFile, { onAction(FabActionType.UPLOAD_FILE); expanded = false }),
                    FabMenuItem("New Folder", Icons.Default.CreateNewFolder, { onAction(FabActionType.NEW_FOLDER); expanded = false }),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen == AppScreen.Wallet -> {
            // Keeftalk Wallet
            Triple(
                FabMenuItem("+", icons.add, { expanded = !expanded }, isPrimary = true),
                null,
                listOf(
                    FabMenuItem("Scan & Pay", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Add Funds", Icons.Default.AddCard, { onAction(FabActionType.WALLET); expanded = false }),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen == AppScreen.Email -> {
            // Keeftalk E-Mail: Primary is Compose, Secondary is Menu (+)
            Triple(
                FabMenuItem("Compose", Icons.Default.Edit, { onAction(FabActionType.COMPOSE_EMAIL); expanded = false }, isPrimary = true),
                FabMenuItem("+", icons.add, { expanded = !expanded }),
                listOf(
                    FabMenuItem("New E-Mail", Icons.Default.Edit, { onAction(FabActionType.COMPOSE_EMAIL); expanded = false }, isActive = true),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        currentScreen == AppScreen.AccessibilitySettings -> {
            // Accessibility Settings (Show global nav items)
            Triple(
                FabMenuItem("+", icons.add, { expanded = !expanded }, isPrimary = true),
                null,
                listOf(
                    FabMenuItem("Scan / Show QR", icons.qrCode, { onAction(FabActionType.QR); expanded = false }),
                    FabMenuItem("Chat with AI", icons.brain, { onAction(FabActionType.AI); expanded = false }, actionType = com.keeftalk.chat.domain.model.FabAction.AI),
                    FabMenuItem("", Icons.Default.Add, {}, isDivider = true),
                    FabMenuItem("My Chats", currentChatIcon, { onNavigate(AppScreen.ChatList); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.CHATS),
                    FabMenuItem("My Notes", icons.notes, { onNavigate(AppScreen.Notes()); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.NOTES),
                    FabMenuItem("My Vault", icons.lock, { onNavigate(AppScreen.Vault); expanded = false }, isActive = currentScreen == AppScreen.Vault, module = com.keeftalk.chat.domain.model.KeeftalkModule.VAULT),
                    FabMenuItem("My Calendar", icons.calendar, { onNavigate(AppScreen.Calendar); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.AGENDA),
                    FabMenuItem("My Wallet", icons.wallet, { onNavigate(AppScreen.Wallet); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.WALLET),
                    FabMenuItem("My E-Mail", Icons.Default.Email, { onNavigate(AppScreen.Email); expanded = false }, trailingAvatars = emailAvatars, module = com.keeftalk.chat.domain.model.KeeftalkModule.EMAIL),
                    FabMenuItem("My Feed", icons.feed, { onNavigate(AppScreen.Feed); expanded = false }, module = com.keeftalk.chat.domain.model.KeeftalkModule.FEED)
                )
            )
        }
        else -> Triple(null, null, emptyList())
    }

    val menuItems: List<FabMenuItem> = rawMenuItems.filter { item ->
        if (customization.isEnabled) {
            val moduleOk = item.module == null || customization.enabledModules.contains(item.module)
            val actionOk = item.actionType == null || customization.enabledFabActions.contains(item.actionType)
            
            // SPECIAL: Hide "My Chats" if it's the only module enabled (beside mandatory actions)
            // Chats is always enabled. So if size is 1, it's just Chats.
            val isRedundantChats = item.module == com.keeftalk.chat.domain.model.KeeftalkModule.CHATS && customization.enabledModules.size == 1
            
            moduleOk && actionOk && !isRedundantChats
        } else {
            // Revert to mandatory items if customization is disabled
            // Mandatory Modules: CHATS. Mandatory Actions: NEW_CHAT, NEW_GROUP, QR.
            // We also show CALLS by default if customization is OFF generally.
            val isMandatoryModule = item.module == null || item.module == com.keeftalk.chat.domain.model.KeeftalkModule.CHATS || item.module == com.keeftalk.chat.domain.model.KeeftalkModule.CALLS
            val isMandatoryAction = item.actionType == null || com.keeftalk.chat.domain.model.AppCustomization.MANDATORY_FAB_ACTIONS.contains(item.actionType)
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
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .fillMaxWidth(0.75f),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Accessibility Row (Compact)
                    if (accessibilityItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            accessibilityItems.filter { !it.isDivider }.forEach { item ->
                                IconButton(
                                    onClick = item.action,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Icon(item.icon, contentDescription = item.label, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Helper to check if item is in accessibility row to avoid duplication
                    // We can't use .contains on FabMenuItem because of lambdas, but we can check types if we mapped them
                    // However, we can just look at labels or icons as a proxy, or better, pass the action type
                    
                    val actions = menuItems.filter { item ->
                        !item.label.startsWith("My ") && !item.isDivider && 
                        // Manual check for labels we know are in the row
                        item.label != "Extra Night Mode" && 
                        item.label != "Increase Size (A+)" && 
                        item.label != "Decrease Size (A-)"
                    }
                    val navItems = menuItems.filter { it.label.startsWith("My ") }
                    
                    // Action Items (New Chat, AI, etc.)
                    actions.forEachIndexed { index, item ->
                        MenuItem(item, index)
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    // Navigation Grid (Compact 2-columns to fit without scrolling)
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
                KeeftalkFab(
                    icon = secondaryAction.icon,
                    onClick = secondaryAction.action,
                    modifier = Modifier.size(secondarySize), // A bit smaller
                    isExpanded = expanded,
                    showPulse = false // Only pulse the main one if it's +
                )
            }

            // Primary FAB (Always in the same spot as the '+' in main screen)
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

@Composable
fun MenuItem(
    item: FabMenuItem,
    index: Int,
    isCompact: Boolean = false
) {
    val animDelay = index * 50
    val transition = updateTransition(targetState = true, label = "MenuItem")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300, delayMillis = animDelay) },
        label = "alpha"
    ) { if (it) 1f else 0f }
    val slide by transition.animateDp(
        transitionSpec = { tween(durationMillis = 300, delayMillis = animDelay) },
        label = "slide"
    ) { if (it) 0.dp else 20.dp }

    Surface(
        onClick = item.action,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = slide)
            .padding(horizontal = if (isCompact) 4.dp else 8.dp, vertical = if (isCompact) 1.dp else 2.dp)
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

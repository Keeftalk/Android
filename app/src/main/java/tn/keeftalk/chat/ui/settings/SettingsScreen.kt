package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.data.prefs.UserPreferences
import tn.keeftalk.chat.domain.model.Profile
import tn.keeftalk.chat.ui.components.KeeftalkAvatar
import tn.keeftalk.chat.ui.theme.AppTheme
import tn.keeftalk.chat.ui.theme.LocalAppIcons
import tn.keeftalk.chat.util.AvatarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onArchivedChatsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAccountSettingsClick: () -> Unit = {},
    onChatSettingsClick: () -> Unit = {},
    onCallSettingsClick: () -> Unit = {},
    onNoteSettingsClick: () -> Unit = {},
    onVaultSettingsClick: () -> Unit = {},
    onCalendarSettingsClick: () -> Unit = {},
    onHabitSettingsClick: () -> Unit = {},
    onFinancesSettingsClick: () -> Unit = {},
    onEmailSettingsClick: () -> Unit = {},
    onBrowserSettingsClick: () -> Unit = {},
    onParentalControlsClick: () -> Unit = {},
    onAppCustomizationClick: () -> Unit = {},
    onDisplaySettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onTestScreensPreviewClick: () -> Unit = {},
    title: String = "Settings"
) {
    val profile by viewModel.currentUserProfile.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLegalDocumentType by remember { mutableStateOf<String?>(null) }
    val icons = LocalAppIcons.current

    Scaffold(
        topBar = {
            SettingsHeader(title = title, onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    onClick = onViewProfile,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    ProfileHeader(profile, onViewProfile)
                }
            }

            item {
                SettingsSection(title = "Account") {
                    SettingsItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Account Settings",
                        subtitle = "Identity, privacy, security, and storage",
                        onClick = onAccountSettingsClick
                    )
                    SettingsItem(
                        icon = icons.bell,
                        title = "Notifications",
                        subtitle = "Messages, groups, calls, sounds",
                        onClick = onNotificationsClick,
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Experience") {
                    SettingsItem(
                        icon = icons.palette,
                        title = "App Customization",
                        subtitle = "Personalize features, FAB and navigation",
                        onClick = onAppCustomizationClick
                    )
                    SettingsItem(
                        icon = icons.chat,
                        title = "My Chats",
                        subtitle = "Chat display, archive, visibility",
                        onClick = onChatSettingsClick
                    )
                    SettingsItem(
                        icon = icons.phone,
                        title = "My Calls",
                        subtitle = "Call history, dialer, ringtones",
                        onClick = onCallSettingsClick
                    )
                    SettingsItem(
                        icon = icons.notes,
                        title = "My Notes",
                        subtitle = "Notebooks, syncing, formatting",
                        onClick = onNoteSettingsClick
                    )
                    SettingsItem(
                        icon = icons.folderLock,
                        title = "My Vault",
                        subtitle = "Encrypted storage, password, backup",
                        onClick = onVaultSettingsClick
                    )
                    SettingsItem(
                        icon = icons.calendar,
                        title = "My Calendar",
                        subtitle = "Events, reminders, holidays",
                        onClick = onCalendarSettingsClick
                    )
                    SettingsItem(
                        icon = Icons.Default.CheckCircle,
                        title = "My Habits",
                        subtitle = "Streaks, categories, reminders",
                        onClick = onHabitSettingsClick
                    )
                    SettingsItem(
                        icon = icons.finances,
                        title = "My Finances",
                        subtitle = "Budgets, currency, privacy, reminders",
                        onClick = onFinancesSettingsClick
                    )
                    SettingsItem(
                        icon = icons.wallet,
                        title = "My Wallet",
                        subtitle = "Soon",
                        enabled = false,
                        onClick = { /* Not clickable */ }
                    )
                    SettingsItem(
                        icon = icons.email,
                        title = "My E-Mail",
                        subtitle = "Accounts, signature, filters",
                        onClick = onEmailSettingsClick
                    )
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "My Browser",
                        subtitle = "Search engine, homepage, privacy",
                        onClick = onBrowserSettingsClick
                    )
                    SettingsItem(
                        icon = Icons.Default.FamilyRestroom,
                        title = "Parental Controls",
                        subtitle = "Manage family and usage limits",
                        onClick = onParentalControlsClick
                    )
                    SettingsItem(
                        icon = icons.palette,
                        title = "Display",
                        subtitle = "Theme, fonts, accessibility",
                        onClick = onDisplaySettingsClick,
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Support") {
                    SettingsItem(
                        icon = icons.info,
                        title = "About",
                        subtitle = "App info, version, licenses",
                        onClick = onAboutClick
                    )
                    SettingsItem(
                        icon = icons.help,
                        title = "Help & Support",
                        subtitle = "FAQ, contact us",
                        onClick = onHelpClick
                    )
                    SettingsItem(
                        icon = icons.logout,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        contentColor = MaterialTheme.colorScheme.error,
                        onClick = { showLogoutDialog = true },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Development") {
                    SettingsItem(
                        icon = Icons.Default.Science,
                        title = "Test Screens Preview",
                        subtitle = "Preview new UI components",
                        onClick = onTestScreensPreviewClick,
                        showDivider = false
                    )
                }
            }
        }
    }

    showLegalDocumentType?.let { type ->
        tn.keeftalk.chat.ui.components.LegalViewerModal(
            typeOrUrl = type,
            onDismiss = { showLegalDocumentType = null }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout from Keeftalk?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    showLogoutDialog = false
                    onBack()
                }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@Composable
fun ProfileHeader(profile: Profile?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeeftalkAvatar(
            avatarUrl = profile?.avatarUrl,
            initials = AvatarUtils.getInitials(profile?.fullName ?: profile?.username),
            seed = profile?.id,
            size = 64.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = profile?.fullName ?: profile?.username ?: "User",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View your profile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
    )
}

@Composable
fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = contentColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

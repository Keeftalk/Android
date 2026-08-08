package com.keeftalk.chat.ui.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.data.prefs.UserPreferences
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.ui.theme.AppTheme
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.util.AvatarUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onArchivedChatsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAccountSettingsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onSecurityClick: () -> Unit = {},
    onChatSettingsClick: () -> Unit = {},
    onAppCustomizationClick: () -> Unit = {},
    onAccessibilityClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    title: String = "Settings"
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val profile by viewModel.currentUserProfile.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
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
                        subtitle = "Identity, password, data portability",
                        onClick = onAccountSettingsClick
                    )
                    SettingsItem(
                        icon = icons.user,
                        title = "Privacy",
                        subtitle = "Online status, read receipts, visibility",
                        onClick = onPrivacyClick
                    )
                    SettingsItem(
                        icon = icons.shield,
                        title = "Security",
                        subtitle = "Two-step verification, active sessions",
                        onClick = onSecurityClick
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
                        onClick = { /* TODO: Navigate to Chats Settings */ }
                    )
                    SettingsItem(
                        icon = icons.phone,
                        title = "My Calls",
                        subtitle = "Call history, dialer, ringtones",
                        onClick = { /* TODO: Navigate to Calls Settings */ }
                    )
                    SettingsItem(
                        icon = icons.notes,
                        title = "My Notes",
                        subtitle = "Notebooks, syncing, formatting",
                        onClick = { /* TODO: Navigate to Notes Settings */ }
                    )
                    SettingsItem(
                        icon = icons.lock,
                        title = "My Vault",
                        subtitle = "Encrypted storage, password, backup",
                        onClick = { /* TODO: Navigate to Vault Settings */ }
                    )
                    SettingsItem(
                        icon = icons.calendar,
                        title = "My Calendar",
                        subtitle = "Events, reminders, holidays",
                        onClick = { /* TODO: Navigate to Calendar Settings */ }
                    )
                    SettingsItem(
                        icon = icons.wallet,
                        title = "My Wallet",
                        subtitle = "Payments, cards, transaction history",
                        onClick = { /* TODO: Navigate to Wallet Settings */ }
                    )
                    SettingsItem(
                        icon = icons.email,
                        title = "My E-Mail",
                        subtitle = "Accounts, signature, filters",
                        onClick = { /* TODO: Navigate to Email Settings */ }
                    )
                    SettingsItem(
                        icon = Icons.Default.FamilyRestroom,
                        title = "Parental Controls",
                        subtitle = "Manage family and usage limits",
                        onClick = { /* TODO: Navigate to Parental Controls */ }
                    )
                    SettingsItem(
                        icon = icons.palette,
                        title = "Theme",
                        subtitle = prefs.themeMode.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { showThemeDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.AccessibilityNew,
                        title = "Accessibility",
                        subtitle = "Extra night mode, UI scaling",
                        onClick = onAccessibilityClick,
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
        }
    }

    showLegalDocumentType?.let { type ->
        com.keeftalk.chat.ui.components.LegalViewerModal(
            typeOrUrl = type,
            onDismiss = { showLegalDocumentType = null }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = prefs.themeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                viewModel.updateThemeMode(theme)
                showThemeDialog = false
            }
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
fun ThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                AppTheme.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme.name) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentTheme == theme.name),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = theme.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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

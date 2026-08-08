package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.SecurityEvent
import com.keeftalk.chat.domain.model.SecurityEventType
import com.keeftalk.chat.domain.model.UserSecuritySettings
import com.keeftalk.chat.domain.model.UserSession
import com.keeftalk.chat.ui.theme.LocalAppIcons
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: SecuritySettingsViewModel,
    onBack: () -> Unit,
    onBlockedUsersClick: () -> Unit,
    onSeeAllActivityClick: () -> Unit
) {
    val settings by viewModel.securitySettings.collectAsState()
    val sessions by viewModel.activeSessions.collectAsState()
    val events by viewModel.securityEvents.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val icons = LocalAppIcons.current

    var showPinDialog by remember { mutableStateOf<PinDialogMode?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSessionDetail by remember { mutableStateOf<UserSession?>(null) }
    var showAppLockDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            SettingsHeader(title = "Security", onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (settings?.twoFactorEnabled != true) {
                item {
                    RecommendationCard(
                        title = "Enable 2-Step Verification",
                        description = "Protect your account with an extra security PIN",
                        icon = icons.shield,
                        onClick = { showPinDialog = PinDialogMode.ENABLE }
                    )
                }
            }

            item {
                SettingsSection(title = "Protection") {
                    SettingsItem(
                        icon = icons.lock,
                        title = "App Lock",
                        subtitle = if (settings?.appLockEnabled == true) "Secured" else "Inactive",
                        onClick = { showAppLockDialog = true }
                    )
                    SettingsItem(
                        icon = icons.shield,
                        title = "Two-Step Verification",
                        subtitle = if (settings?.twoFactorEnabled == true) "Active" else "Off",
                        onClick = {
                            if (settings?.twoFactorEnabled == true) {
                                showPinDialog = PinDialogMode.DISABLE
                            } else {
                                showPinDialog = PinDialogMode.ENABLE
                            }
                        },
                        showDivider = false
                    )
                }
            }

            item {
                SettingsSection(title = "Active Sessions") {
                    sessions.take(3).forEachIndexed { index, session ->
                        SessionItem(
                            session = session,
                            onLogout = { viewModel.logoutSession(session.id) },
                            onDetailClick = { showSessionDetail = session },
                            showDivider = index != sessions.take(3).size - 1 || sessions.size > 3
                        )
                    }
                    if (sessions.size > 3) {
                        TextButton(
                            onClick = onSeeAllActivityClick, // Assuming this leads to sessions too
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text("View All Sessions (${sessions.size})")
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "History") {
                    events.take(3).forEachIndexed { index, event ->
                        SecurityEventItem(
                            event = event,
                            showDivider = index != events.take(3).size - 1
                        )
                    }
                    SettingsItem(
                        icon = icons.info,
                        title = "Security Activity",
                        subtitle = "Review logins and security changes",
                        onClick = onSeeAllActivityClick,
                        showDivider = false
                    )
                }
            }
        }
    }

    // Dialogs ... (keep existing dialog implementations but maybe wrap in Surface for style)


    // Dialogs
    if (showPinDialog != null) {
        PinEntryDialog(
            mode = showPinDialog!!,
            onDismiss = { showPinDialog = null },
            onConfirm = { pin, recoveryEmail ->
                when (showPinDialog) {
                    PinDialogMode.ENABLE -> viewModel.enable2FA(pin, recoveryEmail)
                    PinDialogMode.DISABLE -> viewModel.disable2FA(pin)
                    PinDialogMode.CHANGE -> { /* Handle change PIN flow */ }
                    else -> {}
                }
                showPinDialog = null
            }
        )
    }

    if (showAppLockDialog) {
        AppLockDialog(
            settings = settings,
            onDismiss = { showAppLockDialog = false },
            onSave = { enabled, timeout, biometric ->
                viewModel.updateAppLock(enabled, timeout, biometric)
                showAppLockDialog = false
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new ->
                viewModel.changePassword(current, new)
                showPasswordDialog = false
            }
        )
    }

    if (showSessionDetail != null) {
        SessionDetailDialog(
            session = showSessionDetail!!,
            onDismiss = { showSessionDetail = null }
        )
    }
}

enum class PinDialogMode { ENABLE, DISABLE, CHANGE }

@Composable
fun AppLockDialog(
    settings: UserSecuritySettings?,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricManager = remember { activity?.let { com.keeftalk.chat.util.BiometricAuthManager(it) } }
    
    var enabled by remember { mutableStateOf(settings?.appLockEnabled ?: false) }
    var timeout by remember { mutableStateOf(settings?.appLockTimeoutSeconds ?: 0) }
    var biometric by remember { mutableStateOf(settings?.biometricUnlockEnabled ?: false) }
    
    var isAuthenticating by remember { mutableStateOf(false) }

    val timeoutOptions = listOf(
        "Immediately" to 0,
        "After 1 minute" to 60,
        "After 5 minutes" to 300,
        "After 15 minutes" to 900,
        "After 1 hour" to 3600
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Lock Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isAuthenticating) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable App Lock", modifier = Modifier.weight(1f))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { newValue ->
                                if (newValue && activity != null) {
                                    isAuthenticating = true
                                    biometricManager?.showBiometricPrompt(
                                        activity = activity,
                                        title = "Confirm Security",
                                        subtitle = "Please authenticate to enable app lock",
                                        onSuccess = {
                                            enabled = true
                                            isAuthenticating = false
                                        },
                                        onError = {
                                            isAuthenticating = false
                                            android.widget.Toast.makeText(context, "Authentication failed", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    enabled = newValue
                                }
                            }
                        )
                    }

                    if (enabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Use Biometrics (if available)", modifier = Modifier.weight(1f))
                            Switch(checked = biometric, onCheckedChange = { biometric = it })
                        }

                        Text("Auto-lock timeout", style = MaterialTheme.typography.labelMedium)
                        timeoutOptions.forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { timeout = value }
                            ) {
                                RadioButton(selected = timeout == value, onClick = null)
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(enabled, timeout, biometric) },
                enabled = !isAuthenticating
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SessionItem(
    session: UserSession,
    onLogout: () -> Unit,
    onDetailClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDetailClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (session.platform?.contains("Android") == true) Icons.Default.Android else Icons.Default.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.deviceName ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${session.country ?: "Unknown Location"} • ${session.ipAddress?.take(7)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!session.isCurrent) {
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        }
    }
}

@Composable
fun SessionDetailDialog(session: UserSession, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Device", session.deviceName ?: "Unknown")
                DetailRow("Manufacturer", session.manufacturer ?: "Unknown")
                DetailRow("Model", session.model ?: "Unknown")
                DetailRow("Platform", session.platform ?: "Unknown")
                DetailRow("App Version", session.appVersion ?: "Unknown")
                DetailRow("IP Address", session.ipAddress ?: "Unknown")
                DetailRow("Location", "${session.city ?: ""}, ${session.country ?: ""}")
                DetailRow("Login Time", formatTimestamp(session.createdAt))
                DetailRow("Last Activity", formatTimestamp(session.lastActive))
                DetailRow("Auth Method", session.authMethod ?: "Password")
                DetailRow("Session ID", session.id)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecurityEventItem(event: SecurityEvent, showDivider: Boolean = true) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (event.eventType) {
                    SecurityEventType.LOGIN -> Icons.AutoMirrored.Filled.Login
                    SecurityEventType.PASSWORD_CHANGED -> Icons.Default.Password
                    SecurityEventType.TWO_FACTOR_ENABLED -> Icons.Default.VerifiedUser
                    SecurityEventType.SUSPICIOUS_ACTIVITY -> Icons.Default.Warning
                    SecurityEventType.DEVICE_ADDED -> Icons.Default.Add
                    SecurityEventType.DEVICE_REMOVED -> Icons.Default.Remove
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = if (event.eventType == SecurityEventType.SUSPICIOUS_ACTIVITY) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = event.description ?: event.eventType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${formatTimestamp(event.createdAt)} • ${event.country ?: "Unknown"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        }
    }
}

@Composable
fun RecommendationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PinEntryDialog(
    mode: PinDialogMode,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var recoveryEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when(mode) {
            PinDialogMode.ENABLE -> "Enable Two-Step Verification"
            PinDialogMode.DISABLE -> "Disable Two-Step Verification"
            PinDialogMode.CHANGE -> "Change PIN"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter a 6-digit PIN that you'll be asked for when you register your phone number with Keeftalk again.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("6-digit PIN") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (mode == PinDialogMode.ENABLE) {
                    Text("Optionally add an email address to your account which will be used to reset two-step verification if you forget your PIN.")
                    OutlinedTextField(
                        value = recoveryEmail,
                        onValueChange = { recoveryEmail = it },
                        label = { Text("Recovery Email (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin, if (recoveryEmail.isBlank()) null else recoveryEmail) },
                enabled = pin.length == 6
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(current, new) },
                enabled = current.isNotEmpty() && new.isNotEmpty() && new == confirm
            ) {
                Text("Update Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

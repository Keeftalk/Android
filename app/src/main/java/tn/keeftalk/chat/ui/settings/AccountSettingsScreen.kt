package tn.keeftalk.chat.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.Profile
import java.text.SimpleDateFormat
import java.util.*

enum class AccountSubSetting {
    // Identity
    ID_USER, ID_NAME, ID_BIO, ID_QR,
    // Security
    SEC_PASS, SEC_2FA, SEC_SESSIONS, SEC_LOG, SEC_APP_LOCK,
    // Cloud
    CLOUD_VAULT, CLOUD_NOTES, CLOUD_CACHE,
    // Privacy
    PRIV_VISIBILITY, PRIV_INTERACTIONS, PRIV_PERMS, PRIV_BLOCKED,
    // Subscription & Storage
    SUB_STORAGE, SUB_REDEEM_CODE,
    // Usage
    TIME_ON_KEEFTALK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSubscriptionPlansClick: () -> Unit
) {
    var selectedSubSetting by remember { mutableStateOf<AccountSubSetting?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            SettingsHeader(
                title = if (selectedSubSetting == null) "Account Center" else selectedSubSetting!!.toDisplayName(),
                onBack = {
                    if (selectedSubSetting != null) {
                        selectedSubSetting = null
                    } else {
                        onBack()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AnimatedContent(
            targetState = selectedSubSetting,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "AccountContentTransition"
        ) { subSetting ->
            if (subSetting == null) {
                // Main Account Dashboard
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactAccountDashboardHeader()

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsSection(title = "Billing & Storage") {
                            SettingsItem(
                                icon = Icons.Rounded.Storage,
                                title = "Subscription & Storage",
                                subtitle = "Membership & Quota",
                                onClick = { selectedSubSetting = AccountSubSetting.SUB_STORAGE },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.ConfirmationNumber,
                                title = "Redeem Access Code",
                                subtitle = "Activate Pro & Feature Syncs",
                                onClick = { selectedSubSetting = AccountSubSetting.SUB_REDEEM_CODE },
                                showDivider = false,
                                trailing = { ChevronRightIcon() }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSection(title = "Identity") {
                            SettingsItem(
                                icon = Icons.Rounded.AlternateEmail,
                                title = "Username Handle",
                                subtitle = "Manage @username",
                                onClick = { selectedSubSetting = AccountSubSetting.ID_USER },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Badge,
                                title = "Display Name",
                                subtitle = "Public identity",
                                onClick = { selectedSubSetting = AccountSubSetting.ID_NAME },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Info,
                                title = "Profile Bio",
                                subtitle = "Personal info",
                                onClick = { selectedSubSetting = AccountSubSetting.ID_BIO },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.QrCode2,
                                title = "My QR Code",
                                subtitle = "Share identity",
                                onClick = { selectedSubSetting = AccountSubSetting.ID_QR },
                                showDivider = false,
                                trailing = { ChevronRightIcon() }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSection(title = "Security & Trust") {
                            SettingsItem(
                                icon = Icons.Rounded.Password,
                                title = "Password",
                                subtitle = "Auth credentials",
                                onClick = { selectedSubSetting = AccountSubSetting.SEC_PASS },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.VpnKey,
                                title = "2-Step Verification",
                                subtitle = "MFA setup",
                                onClick = { selectedSubSetting = AccountSubSetting.SEC_2FA },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.LockPerson,
                                title = "App Lock",
                                subtitle = "Biometric protection",
                                onClick = { selectedSubSetting = AccountSubSetting.SEC_APP_LOCK },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Devices,
                                title = "Active Sessions",
                                subtitle = "Logged devices",
                                onClick = { selectedSubSetting = AccountSubSetting.SEC_SESSIONS },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.HistoryEdu,
                                title = "Security Activity",
                                subtitle = "Action logs",
                                onClick = { selectedSubSetting = AccountSubSetting.SEC_LOG },
                                showDivider = false,
                                trailing = { ChevronRightIcon() }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSection(title = "Cloud & Storage") {
                            SettingsItem(
                                icon = Icons.Rounded.Lock,
                                title = "Secure Vault",
                                subtitle = "Encrypted files",
                                onClick = { selectedSubSetting = AccountSubSetting.CLOUD_VAULT },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.EditNote,
                                title = "Notes Sync",
                                subtitle = "Data health",
                                onClick = { selectedSubSetting = AccountSubSetting.CLOUD_NOTES },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Storage,
                                title = "Cache Manager",
                                subtitle = "Storage cleanup",
                                onClick = { selectedSubSetting = AccountSubSetting.CLOUD_CACHE },
                                showDivider = false,
                                trailing = { ChevronRightIcon() }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSection(title = "Privacy & Safety") {
                            SettingsItem(
                                icon = Icons.Rounded.Visibility,
                                title = "Profile Visibility",
                                subtitle = "Online status & info",
                                onClick = { selectedSubSetting = AccountSubSetting.PRIV_VISIBILITY },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.AutoMirrored.Rounded.Message,
                                title = "Interactions",
                                subtitle = "Receipts & typing",
                                onClick = { selectedSubSetting = AccountSubSetting.PRIV_INTERACTIONS },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.AutoMirrored.Rounded.Rule,
                                title = "Permissions",
                                subtitle = "System access",
                                onClick = { selectedSubSetting = AccountSubSetting.PRIV_PERMS },
                                trailing = { ChevronRightIcon() }
                            )
                            SettingsItem(
                                icon = Icons.Rounded.Block,
                                title = "Blocked List",
                                subtitle = "Restricted users",
                                onClick = { selectedSubSetting = AccountSubSetting.PRIV_BLOCKED },
                                showDivider = false,
                                trailing = { ChevronRightIcon() }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSection(title = "Usage & Activity") {
                            SettingsItem(
                                icon = Icons.Rounded.QueryStats,
                                title = "Time on Keeftalk",
                                subtitle = "Screen time charts, stats & activity breakdown",
                                onClick = { selectedSubSetting = AccountSubSetting.TIME_ON_KEEFTALK },
                                showDivider = false,
                                trailing = { ChevronRightIcon() }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        CriticalAccountActions(
                            onDelete = { viewModel.deleteAccount() },
                            onLogout = { viewModel.logout() }
                        )
                    }
                }
            } else if (subSetting == AccountSubSetting.TIME_ON_KEEFTALK) {
                TimeOnKeeftalkScreen(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    onBack = { selectedSubSetting = null }
                )
            } else {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    AccountSubSettingDetail(
                        setting = subSetting,
                        viewModel = viewModel,
                        onSubscriptionPlansClick = onSubscriptionPlansClick,
                        onRedeemCodeClick = { selectedSubSetting = AccountSubSetting.SUB_REDEEM_CODE }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSubSettingDetail(
    setting: AccountSubSetting,
    viewModel: SettingsViewModel,
    onSubscriptionPlansClick: () -> Unit,
    onRedeemCodeClick: () -> Unit = {}
) {
    val profile by viewModel.currentUserProfile.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        CompactSubSettingHeader(setting)
        Spacer(modifier = Modifier.height(24.dp))

        when (setting) {
            AccountSubSetting.ID_USER -> UserHandleView(profile, viewModel)
            AccountSubSetting.ID_NAME -> NameView(profile, viewModel)
            AccountSubSetting.ID_BIO -> BioView(profile, viewModel)
            AccountSubSetting.ID_QR -> QrCodeView(profile)
            AccountSubSetting.SEC_PASS -> PasswordView(viewModel)
            AccountSubSetting.SEC_2FA -> Security2FAView(viewModel)
            AccountSubSetting.SEC_APP_LOCK -> SecurityAppLockView(viewModel)
            AccountSubSetting.SEC_SESSIONS -> SessionsView(viewModel)
            AccountSubSetting.SEC_LOG -> ActivityLogView(viewModel)
            AccountSubSetting.CLOUD_VAULT -> VaultView(profile)
            AccountSubSetting.CLOUD_NOTES -> NotesView(viewModel)
            AccountSubSetting.CLOUD_CACHE -> CacheView()
            AccountSubSetting.PRIV_VISIBILITY -> PrivacyVisibilityView(viewModel)
            AccountSubSetting.PRIV_INTERACTIONS -> PrivacyInteractionsView(viewModel)
            AccountSubSetting.PRIV_PERMS -> PermsView()
            AccountSubSetting.PRIV_BLOCKED -> BlockedView(viewModel)
            AccountSubSetting.SUB_STORAGE -> SubscriptionView(profile, onSubscriptionPlansClick, onRedeemCodeClick)
            AccountSubSetting.SUB_REDEEM_CODE -> RedeemCodeView(viewModel)
            AccountSubSetting.TIME_ON_KEEFTALK -> {}
        }
    }
}

@Composable
fun CompactSubSettingHeader(setting: AccountSubSetting) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(getIconForSubSetting(setting), null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(setting.toDisplayName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Professional Settings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- COMPACT SUB-VIEWS ---

@Composable fun UserHandleView(profile: Profile?, viewModel: SettingsViewModel) { 
    var text by remember { mutableStateOf(profile?.username ?: "") }
    Column {
        PremiumTextField(text, "Username", Icons.Rounded.AlternateEmail) { text = it }
        StatusIndicator("Username available", true)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.updateProfile(username = text) }, modifier = Modifier.fillMaxWidth()) {
            Text("Update Username")
        }
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard("Handles are unique and used for cross-network identification.")
    }
}
@Composable fun NameView(profile: Profile?, viewModel: SettingsViewModel) { 
    var text by remember { mutableStateOf(profile?.fullName ?: "") }
    
    val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    val lastChanged = profile?.lastNameChangeAt ?: 0L
    val elapsed = now - lastChanged
    val remainingMs = ninetyDaysMs - elapsed
    val isCooldownActive = lastChanged > 0 && remainingMs > 0
    val remainingDays = (remainingMs / (1000L * 60 * 60 * 24)).coerceAtLeast(1)

    Column {
        PremiumTextField(text, "Display Name", Icons.Rounded.Badge, enabled = !isCooldownActive) { text = it }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isCooldownActive) {
            InfoCard("Name change cooldown active. You can change your display name again in $remainingDays days.")
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.updateProfile(fullName = text) }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCooldownActive && text.isNotBlank() && text != (profile?.fullName ?: "")
        ) {
            Text(if (isCooldownActive) "Cooldown Active ($remainingDays days left)" else "Update Name")
        }
    }
}
@Composable fun BioView(profile: Profile?, viewModel: SettingsViewModel) { 
    var text by remember { mutableStateOf(profile?.bio ?: "") }
    Column {
        PremiumTextField(text, "Bio", Icons.Rounded.Info, false, 3) { text = it }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.updateProfile(bio = text) }, modifier = Modifier.fillMaxWidth()) {
            Text("Update Bio")
        }
    }
}
@Composable fun QrCodeView(profile: Profile?) { 
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(modifier = Modifier.size(200.dp), shape = RoundedCornerShape(24.dp), color = Color.White, tonalElevation = 8.dp) {
            Box(contentAlignment = Alignment.Center) {
                tn.keeftalk.chat.ui.screens.MyQrCode(userId = profile?.id ?: "")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Your personal QR code for quick peer sharing.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}
@Composable fun PasswordView(viewModel: SettingsViewModel) { 
    var showDialog by remember { mutableStateOf(false) }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    
    Column {
        SecureField(currentPass, "Current Password") { currentPass = it }
        Spacer(modifier = Modifier.height(12.dp))
        SecureField(newPass, "New Password") { newPass = it }
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Change Password")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
    
    if (showDialog) {
        ChangePasswordDialog(
            onDismiss = { showDialog = false },
            onConfirm = { current, new ->
                viewModel.changePassword(current, new)
                showDialog = false
            }
        )
    }
}
@Composable fun Security2FAView(viewModel: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val twoFactorViewModel: tn.keeftalk.chat.ui.security.TwoFactorViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        tn.keeftalk.chat.ui.security.TwoFactorViewModel(
            tn.keeftalk.chat.di.AppModule.provideTwoFactorRepository(context)
        )
    }

    tn.keeftalk.chat.ui.security.TwoFactorSettingsScreen(
        viewModel = twoFactorViewModel,
        isEmbedded = true
    )
}

@Composable
fun MfaMethodItem(title: String, subtitle: String, icon: ImageVector, active: Boolean, onRevoke: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, if (active) Color(0xFF2ECC71).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (active) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRevoke != null && active) {
                IconButton(onClick = onRevoke) {
                    Icon(Icons.Rounded.Cancel, "Revoke", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            } else {
                Switch(checked = active, onCheckedChange = {}, modifier = Modifier.scale(0.7f))
            }
        }
    }
}

@Composable fun SecurityAppLockView(viewModel: SettingsViewModel) {
    val settings by viewModel.securitySettings.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricManager = remember { activity?.let { tn.keeftalk.chat.util.BiometricAuthManager(it) } }
    
    var isAuthenticating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PremiumToggleItem(
            title = "App Lock",
            subtitle = if (settings?.appLockEnabled == true) "Secured" else "Inactive",
            checked = settings?.appLockEnabled == true,
            onCheckedChange = { newValue: Boolean ->
                if (newValue && activity != null) {
                    isAuthenticating = true
                    biometricManager?.showBiometricPrompt(
                        activity = activity,
                        title = "Confirm Security",
                        subtitle = "Authenticate to enable lock",
                        onSuccess = {
                            viewModel.updateAppLock(true, settings?.appLockTimeoutSeconds ?: 0, true)
                            isAuthenticating = false
                        },
                        onError = {
                            isAuthenticating = false
                        }
                    )
                } else {
                    viewModel.updateAppLock(false, settings?.appLockTimeoutSeconds ?: 0, false)
                }
            }
        )

        if (settings?.appLockEnabled == true) {
            InfoCard("Lock timeout is currently set to ${settings?.appLockTimeoutSeconds ?: 0} seconds.")
        }
        
        if (isAuthenticating) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
@Composable fun SessionsView(viewModel: SettingsViewModel) { 
    val sessions by viewModel.activeSessions.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard("Devices currently logged into your Keeftalk account.")
        sessions.forEach { session ->
            SessionItem(
                session = session,
                onLogout = { viewModel.logoutSession(session.id) }
            )
        }
        if (sessions.size > 1) {
            Button(onClick = { viewModel.logoutAllOtherSessions() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { 
                Text("Logout All Other Devices") 
            }
        }
    }
}
@Composable fun ActivityLogView(viewModel: SettingsViewModel) { 
    val events by viewModel.securityEvents.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEach { event ->
            LogItem(
                action = event.eventType.name.replace("_", " "),
                detail = event.description ?: "",
                time = formatTimestamp(event.createdAt)
            )
        }
    }
}

@Composable fun PrivacyVisibilityView(viewModel: SettingsViewModel) {
    val settings by viewModel.fullSettings.collectAsState()
    val prefs = settings.privacySettings
    var showVisibilityPicker by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumActionCard("Profile Photo", prefs.profilePhotoVisibility.name, Icons.Rounded.Portrait) {
            showVisibilityPicker = "profilePhotoVisibility"
        }
        PremiumActionCard("About Info", prefs.aboutVisibility.name, Icons.Rounded.Info) {
            showVisibilityPicker = "aboutVisibility"
        }
        PremiumActionCard("Last Seen", prefs.lastSeenVisibility.name, Icons.Rounded.History) {
            showVisibilityPicker = "lastSeenVisibility"
        }
        PremiumActionCard("Connections", prefs.connectionsVisibility.name, Icons.Rounded.People) {
            showVisibilityPicker = "connectionsVisibility"
        }
    }

    if (showVisibilityPicker != null) {
        val key = showVisibilityPicker!!
        val options = listOf("EVERYONE", "CONTACTS", "NOBODY")
        val selected = when(key) {
            "profilePhotoVisibility" -> prefs.profilePhotoVisibility.name
            "aboutVisibility" -> prefs.aboutVisibility.name
            "lastSeenVisibility" -> prefs.lastSeenVisibility.name
            "connectionsVisibility" -> prefs.connectionsVisibility.name
            else -> ""
        }

        AlertDialog(
            onDismissRequest = { showVisibilityPicker = null },
            title = { Text("Select Visibility") },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.updatePrivacySetting(key, option)
                                showVisibilityPicker = null
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (option == selected), onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(option.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVisibilityPicker = null }) { Text("Cancel") } }
        )
    }
}

@Composable fun PrivacyInteractionsView(viewModel: SettingsViewModel) {
    val settings by viewModel.fullSettings.collectAsState()
    val prefs = settings.privacySettings
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumToggleItem(
            title = "Read Receipts",
            subtitle = "Allow others to see when you've read messages",
            checked = prefs.readReceiptsEnabled,
            onCheckedChange = { enabled: Boolean -> viewModel.updatePrivacySetting("readReceiptsEnabled", enabled) }
        )
        PremiumToggleItem(
            title = "Typing Indicators",
            subtitle = "Show when you are typing",
            checked = prefs.typingIndicatorsEnabled,
            onCheckedChange = { enabled: Boolean -> viewModel.updatePrivacySetting("typingIndicatorsEnabled", enabled) }
        )
        PremiumToggleItem(
            title = "Screenshot Protection",
            subtitle = "Block screenshots in private chats",
            checked = prefs.screenshotProtectionEnabled,
            onCheckedChange = { enabled: Boolean -> viewModel.updatePrivacySetting("screenshotProtectionEnabled", enabled) }
        )
    }
}
@Composable fun VaultView(profile: Profile?) { 
    val used = profile?.storageUsed ?: 0L
    val limit = profile?.storageLimit ?: tn.keeftalk.chat.domain.model.SubscriptionPlan.FREE.storageLimit
    val usedGb = used.toFloat() / (1024 * 1024 * 1024)
    val limitGb = limit.toFloat() / (1024 * 1024 * 1024)
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StorageVisualizer(usedGb, limitGb, isError = used > limit)
        PremiumStatusBox("Zero-Knowledge Vault", "Encryption keys stored on-device", true)
        InfoCard("Your Vault is protected by hardware security.")
    }
}
@Composable fun NotesView(viewModel: SettingsViewModel) { 
    val settings by viewModel.fullSettings.collectAsState()
    PremiumToggleItem(
        title = "Notes Cloud Sync", 
        subtitle = "Real-time synchronization for secure notes", 
        checked = settings.noteSettings.syncNotesAcrossDevices,
        onCheckedChange = { viewModel.updateModuleIntegration("notesSyncEnabled", it) }
    )
}
@Composable fun CacheView() { 
    val context = androidx.compose.ui.platform.LocalContext.current
    var cacheSize by remember { mutableStateOf("Calculating...") }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val size = context.cacheDir.walk().filter { it.isFile }.map { it.length() }.sum()
            cacheSize = "%.2f MB".format(size.toFloat() / (1024 * 1024))
        }
    }

    Column {
        InfoCard("Current application cache: $cacheSize")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { 
            context.cacheDir.deleteRecursively()
            cacheSize = "0.00 MB"
        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { 
            Text("Clear Cache") 
        }
    }
}
@Composable fun PermsView() { 
    val context = androidx.compose.ui.platform.LocalContext.current
    fun hasPerm(perm: String) = androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionItem("Camera", hasPerm(android.Manifest.permission.CAMERA))
        PermissionItem("Microphone", hasPerm(android.Manifest.permission.RECORD_AUDIO))
        PermissionItem("Contacts", hasPerm(android.Manifest.permission.READ_CONTACTS))
        
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard("System permissions are managed via Android Settings.")
    }
}
@Composable fun BlockedView(viewModel: SettingsViewModel) { 
    val blockedUsers by viewModel.blockedUsers.collectAsState(initial = emptyList())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (blockedUsers.isEmpty()) {
            InfoCard("No restricted contacts.")
        } else {
            blockedUsers.forEach { user ->
                BlockedItem(user.name, "@${user.username}") {
                    viewModel.unblockUser(user.id)
                }
            }
        }
    }
}

@Composable
fun SubscriptionView(
    profile: tn.keeftalk.chat.domain.model.Profile?,
    onUpgradeClick: () -> Unit,
    onRedeemCodeClick: () -> Unit = {}
) {
    val plan = profile?.planType ?: tn.keeftalk.chat.domain.model.SubscriptionPlan.FREE
    val used = profile?.storageUsed ?: 0L
    val limit = profile?.storageLimit ?: plan.storageLimit
    val usedGb = used.toFloat() / (1024 * 1024 * 1024)
    val limitGb = limit.toFloat() / (1024 * 1024 * 1024)
    val isOverQuota = used > limit

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = if (isOverQuota) "Your storage is full! Please upgrade or delete files to keep syncing." 
                   else "Keeftalk is free forever. Pay only when you need more space and power",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        StoragePulseCard(used = usedGb, total = limitGb, isOverQuota = isOverQuota)
        
        MembershipStatusCard(
            planName = "Keeftalk ${plan.displayName}", 
            active = plan != tn.keeftalk.chat.domain.model.SubscriptionPlan.FREE,
            onClick = onUpgradeClick
        )
        
        AccountControlSection(title = "${plan.displayName} Advantages") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdvantageItem("End-to-End Encryption (E2EE)")
                AdvantageItem("Hardware-Backed Account Keys")
                AdvantageItem("${plan.formatStorageLimit()} High-Speed Cloud Storage")
                AdvantageItem("${plan.formatMaxFileSize()} Max Upload Size")
                AdvantageItem("Unlimited Module Sync (Vault, Notes)")
                AdvantageItem("Priority Community Support")
                if (plan != tn.keeftalk.chat.domain.model.SubscriptionPlan.FREE) {
                    AdvantageItem("Original Quality Media")
                }
            }
        }
        
        AccountControlSection(title = "Special Offers & Promotions") {
            if (plan != tn.keeftalk.chat.domain.model.SubscriptionPlan.PRO_MONTHLY && plan != tn.keeftalk.chat.domain.model.SubscriptionPlan.FAMILY_MONTHLY) {
                PremiumActionCard("Upgrade Plan", "Get more storage and features", Icons.Rounded.WorkspacePremium) {
                    onUpgradeClick()
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            PremiumActionCard("Redeem Access Code", "Activate Pro or unlock feature syncs", Icons.Rounded.ConfirmationNumber) {
                onRedeemCodeClick()
            }
        }
    }
}

@Composable
fun RedeemCodeView(viewModel: SettingsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entitlementManager = remember { tn.keeftalk.chat.di.AppModule.provideEntitlementManager(context) }
    val isPremium by entitlementManager.isUniversalPremium.collectAsState()
    val activeKeys by entitlementManager.activeEntitlements.collectAsState()

    var codeInput by remember { mutableStateOf("") }
    var redeemResult by remember { mutableStateOf<tn.keeftalk.chat.domain.engine.RedeemResult?>(null) }
    var isRedeeming by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ConfirmationNumber, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Redeem Access Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Enter an access code issued by Keeftalk to unlock Pro features or cloud sync capabilities.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter Redemption Code", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    label = { Text("Access Code") },
                    leadingIcon = { Icon(Icons.Rounded.VpnKey, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (codeInput.isNotEmpty()) {
                            IconButton(onClick = { codeInput = "" }) {
                                Icon(Icons.Rounded.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (codeInput.isNotBlank()) {
                            isRedeeming = true
                            viewModel.redeemCode(context, codeInput) { result ->
                                isRedeeming = false
                                redeemResult = result
                                if (result is tn.keeftalk.chat.domain.engine.RedeemResult.Success) {
                                    codeInput = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = codeInput.isNotBlank() && !isRedeeming
                ) {
                    if (isRedeeming) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying with server...")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Redeem Code", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        redeemResult?.let { result ->
            when (result) {
                is tn.keeftalk.chat.domain.engine.RedeemResult.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2ECC71).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF2ECC71).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(result.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            if (result.unlockedFeatures.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Unlocked: ${result.unlockedFeatures.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                is tn.keeftalk.chat.domain.engine.RedeemResult.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(result.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        AccountControlSection(title = "Account Feature Cloud Sync Status") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val paidFeatures = tn.keeftalk.chat.domain.model.entitlement.FeatureId.values().filter { !it.isAlwaysFree }
                paidFeatures.forEach { feature ->
                    val isActive = isPremium || activeKeys.contains(feature.key)
                    val statusText = when {
                        isPremium -> "Included in Pro"
                        activeKeys.contains(feature.key) -> "Active"
                        else -> "Locked (Local Only)"
                    }
                    FeatureStatusRow(
                        featureName = feature.displayName,
                        statusText = statusText,
                        isActive = isActive
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureStatusRow(featureName: String, statusText: String, isActive: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0xFF2ECC71).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF2ECC71).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isActive) Icons.Rounded.CloudDone else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (isActive) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(featureName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }

            Surface(shape = RoundedCornerShape(6.dp), color = if (isActive) Color(0xFF2ECC71).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StoragePulseCard(used: Float, total: Float, isOverQuota: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, if (isOverQuota) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Global Storage Pulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("Aggregate across all modules", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background((if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CloudQueue, null, tint = if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            StorageVisualizer(used = used, total = total, isError = isOverQuota)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(used / total * 100).toInt()}% consumed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text("${if (total >= 1024) "%.1f TB".format(total/1024) else "%.0f GB".format(total)} total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MembershipStatusCard(planName: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Verified, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(planName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(if (active) "Subscription Active" else "No active membership", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
        }
    }
}

@Composable
fun AdvantageItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- REUSABLE COMPONENTS ---

@Composable
private fun ChevronRightIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(18.dp)
    )
}

@Composable fun CompactAccountDashboardHeader() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ManageAccounts, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Account Center", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Security & Identity Dashboard", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
@Composable fun AccountControlSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp), letterSpacing = 1.sp)
        content()
    }
}
@Composable fun InfoCard(text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
@Composable fun PremiumTextField(value: String, label: String, icon: ImageVector, singleLine: Boolean = true, minLines: Int = 1, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }, singleLine = singleLine, minLines = minLines)
}
@Composable fun StatusIndicator(text: String, success: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Icon(if (success) Icons.Rounded.CheckCircle else Icons.Rounded.Error, null, tint = if (success) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = if (success) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
    }
}
@Composable fun SecureField(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(20.dp)) })
}
@Composable fun PremiumToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
    val shape = RoundedCornerShape(16.dp)
    Surface(modifier = Modifier.fillMaxWidth().clip(shape), shape = shape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), tonalElevation = 0.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.8f))
        }
    }
}
@Composable fun PremiumActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().clip(shape), shape = shape, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}
@Composable fun PremiumStatusBox(title: String, subtitle: String, active: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = if (active) Color(0xFF2ECC71).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, if (active) Color(0xFF2ECC71).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold, color = if (active) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium); Text(subtitle, style = MaterialTheme.typography.labelSmall) }
    }
}
@Composable fun SessionItem(
    session: tn.keeftalk.chat.domain.model.UserSession,
    onLogout: () -> Unit = {}
) {
    val deviceName = session.deviceName?.ifBlank { null }
        ?: listOfNotNull(session.manufacturer, session.model).joinToString(" ").ifBlank { null }
        ?: "Unknown Device"

    val locationParts = mutableListOf<String>()
    if (!session.city.isNullOrBlank()) locationParts.add(session.city)
    if (!session.country.isNullOrBlank()) locationParts.add(session.country)
    if (locationParts.isEmpty() && !session.ipAddress.isNullOrBlank()) {
        locationParts.add(session.ipAddress)
    }
    val locationText = locationParts.joinToString(", ").ifEmpty { "Unknown location" }

    val platformInfo = listOfNotNull(
        session.platform,
        session.appVersion?.let { "v$it" }
    ).joinToString(" • ")

    val locationAndPlatform = if (platformInfo.isNotBlank()) {
        "$locationText • $platformInfo"
    } else {
        locationText
    }

    val icon = when {
        session.platform?.contains("Windows", ignoreCase = true) == true ||
        session.platform?.contains("Mac", ignoreCase = true) == true ||
        session.platform?.contains("Linux", ignoreCase = true) == true ||
        session.platform?.contains("Desktop", ignoreCase = true) == true ||
        deviceName.contains("Laptop", ignoreCase = true) ||
        deviceName.contains("MacBook", ignoreCase = true) -> Icons.Rounded.Laptop
        session.platform?.contains("Web", ignoreCase = true) == true -> Icons.Rounded.Language
        else -> Icons.Rounded.Smartphone
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(deviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(locationAndPlatform, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (session.isCurrent) {
                Text("Current", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            } else {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable fun SessionItem(device: String, location: String, isCurrent: Boolean, onLogout: () -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (device.contains("Pro") || device.contains("Mac")) Icons.Rounded.Laptop else Icons.Rounded.Smartphone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(device, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (isCurrent) Text("Current", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            else IconButton(onClick = onLogout) { Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
        }
    }
}
@Composable fun LogItem(action: String, detail: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text("$detail • $time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
    }
}
@Composable fun StorageVisualizer(used: Float, total: Float, isError: Boolean = false) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Storage: ${"%.2f".format(used)}GB / ${"%.0f".format(total)}GB", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
        Spacer(modifier = Modifier.height(8.dp)); Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((used / total).coerceIn(0f, 1f)).background(if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) }
    }
}
@Composable fun PermissionItem(name: String, granted: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium); Switch(checked = granted, onCheckedChange = {}, modifier = Modifier.graphicsLayer { scaleX = 0.7f; scaleY = 0.7f }) }
}
@Composable fun BlockedItem(name: String, identifier: String, onUnblock: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) { Text(name.take(1), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
            Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(identifier, style = MaterialTheme.typography.labelSmall) }
            TextButton(onClick = onUnblock) { Text("Unblock", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
@Composable fun CriticalAccountActions(onDelete: () -> Unit, onLogout: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Identity Deactivation") },
            text = {
                Column {
                    Text("This process requires email confirmation and involves a 30-day retention period.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Note: Logging back in during this 30-day period will immediately cancel the deactivation process and reset the timer.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Start Process", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(10.dp)); Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium) } }
        Surface(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.error) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.DeleteForever, null, tint = Color.White, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Deactivate Identity", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium); Text("Permanent deletion", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall) } } }
    }
}

fun AccountSubSetting.toDisplayName(): String = when (this) {
    AccountSubSetting.TIME_ON_KEEFTALK -> "Time on Keeftalk"
    else -> name.split("_").drop(1).joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
}

enum class PinDialogMode { ENABLE, DISABLE, CHANGE }

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

fun getIconForSubSetting(setting: AccountSubSetting): ImageVector = when (setting) {
    AccountSubSetting.ID_USER -> Icons.Rounded.AlternateEmail
    AccountSubSetting.ID_NAME -> Icons.Rounded.Badge
    AccountSubSetting.ID_BIO -> Icons.Rounded.Info
    AccountSubSetting.ID_QR -> Icons.Rounded.QrCode2
    AccountSubSetting.SEC_PASS -> Icons.Rounded.Password
    AccountSubSetting.SEC_2FA -> Icons.Rounded.VpnKey
    AccountSubSetting.SEC_APP_LOCK -> Icons.Rounded.LockPerson
    AccountSubSetting.SEC_SESSIONS -> Icons.Rounded.Devices
    AccountSubSetting.SEC_LOG -> Icons.Rounded.HistoryEdu
    AccountSubSetting.CLOUD_VAULT -> Icons.Rounded.Lock
    AccountSubSetting.CLOUD_NOTES -> Icons.Rounded.EditNote
    AccountSubSetting.CLOUD_CACHE -> Icons.Rounded.Storage
    AccountSubSetting.PRIV_VISIBILITY -> Icons.Rounded.Visibility
    AccountSubSetting.PRIV_INTERACTIONS -> Icons.AutoMirrored.Rounded.Message
    AccountSubSetting.PRIV_PERMS -> Icons.Rounded.Rule
    AccountSubSetting.PRIV_BLOCKED -> Icons.Rounded.Block
    AccountSubSetting.SUB_STORAGE -> Icons.Rounded.Storage
    AccountSubSetting.SUB_REDEEM_CODE -> Icons.Rounded.ConfirmationNumber
    AccountSubSetting.TIME_ON_KEEFTALK -> Icons.Rounded.QueryStats
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
